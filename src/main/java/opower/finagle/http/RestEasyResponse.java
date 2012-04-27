package opower.finagle.http;

import org.jboss.netty.buffer.BigEndianHeapChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferOutputStream;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Wraps Finagle's Netty HTTP response to provide the semantics of JBoss's RestEASY library.
 * Notes:
 *
 * <ul>
 *
 *      <li>Netty uses fixed-size output buffers, which is tough because we don't
 *      know the output size in advance.  See comments on {@link #prepareResponseHack(HttpRequest)}
 *      for more details.
 *      </li>
 *
 *      <li>This doesn't implement support for cookies.</li>
 *
 * </ul>
 *
 * @author ed.peters
 */
public class RestEasyResponse implements org.jboss.resteasy.spi.HttpResponse {

    private final org.jboss.netty.handler.codec.http.HttpResponse finagleResponse;
    private final MultivaluedMap<String,Object> headerWrapper;
    private final OutputStream outputStream;

    public RestEasyResponse(HttpRequest finagleRequest) {
        this.finagleResponse = prepareResponseHack(finagleRequest);
        this.headerWrapper = new RestEasyUtils.HeaderWrapper(this.finagleResponse);
        this.outputStream = new ChannelBufferOutputStream(this.finagleResponse.getContent());
    }

    /*
     * Netty uses a fixed-size output buffer, and doesn't resize it -- if you wind up writing
     * more than that, you get an ArrayIndexOutOfBoundsException.  The default buffer size is
     * 0 bytes (I know -- helpful, isn't it?)  So here we're simply going to allocate some
     * extra room for the response.
     * TODO what's the best long-term fix?  a dynamically resizing ChannelBufferOutputStream?
     */
    private HttpResponse prepareResponseHack(HttpRequest request) {
        HttpResponse response = new DefaultHttpResponse(request.getProtocolVersion(), HttpResponseStatus.OK);
        response.setChunked(false);
        response.setContent(new BigEndianHeapChannelBuffer(1024));
        return response;
    }

    public HttpResponse getFinagleResponse() {
        return this.finagleResponse;
    }

    // ===================================================================
    // STATUS LINE
    // ===================================================================

    @Override
    public int getStatus() {
        return this.finagleResponse.getStatus().getCode();
    }

    @Override
    public void sendError(int status) throws IOException {
        setStatus(status);
    }

    @Override
    public void sendError(int status, String message) throws IOException {
        this.finagleResponse.setStatus(new HttpResponseStatus(status, message));
    }

    @Override
    public void setStatus(int status) {
        this.finagleResponse.setStatus(HttpResponseStatus.valueOf(status));
    }

    @Override
    public void addNewCookie(NewCookie cookie) {
        // TODO do we want cookie support? does netty even allow it?
        throw new UnsupportedOperationException("addNewCookie");
    }

    @Override
    public MultivaluedMap<String,Object> getOutputHeaders() {
        return this.headerWrapper;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return this.outputStream;
    }

    @Override
    public boolean isCommitted() {
        // TODO no clue how to get this information from netty
        return false;
    }

    @Override
    public void reset() {
        // this is supposed to clear headers and status, but netty doesn't have a notion
        // of a "null" value for status, so we'll do the best we can
        this.finagleResponse.clearHeaders();
    }

}
