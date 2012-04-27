package opower.finagle.http;

import com.google.common.collect.Maps;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.resteasy.spi.AsynchronousResponse;
import org.jboss.resteasy.util.Encode;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import java.io.InputStream;
import java.util.Map;

/**
 * Wraps Finagle's Netty HTTP request to provide the semantics of the JBoss RestEASY library.
 * The majority of this is just translating data from one form to another.  Notes:
 *
 * <ul>
 *
 *     <li>JBoss includes special methods for dealing with JAX-RS <code>@FormParam</code>
 *     parameters.  I'm leaving this unimplemented because we don't use form parameters.
 *     At some point we might want to revisit this.</li>
 *
 *     <li>RestEASY has support for asynchronous methods.  It might be interesting to
 *     explore this, since Netty is pretty well set up to take advantage of efficiencies
 *     of asynchronous processing.</li>
 *
 * </ul>
 *
 * @see org.jboss.resteasy.spi.HttpRequest#getFormParameters()
 * @see "http://bill.burkecentral.com/2008/10/09/jax-rs-asynch-http/"
 *
 * @author ed.peters
 */
public class RestEasyRequest implements org.jboss.resteasy.spi.HttpRequest {

    private final org.jboss.netty.handler.codec.http.HttpRequest finagleRequest;
    private final Map<String,Object> attributeMap;
    private final HttpHeaders jaxrsHeaders;
    private final UriInfo jaxrsUriInfo;
    private InputStream overrideStream;
    private String preProcessedPath;

    public RestEasyRequest(HttpRequest finagleRequest) {
        this.finagleRequest = finagleRequest;
        this.jaxrsHeaders = RestEasyUtils.toHeaders(RestEasyUtils.toMultimap(finagleRequest));
        this.jaxrsUriInfo = RestEasyUtils.extractUriInfo(finagleRequest.getUri());
        this.attributeMap = Maps.newHashMap();
    }

    // ===================================================================
    // ATTRIBUTES
    // ===================================================================

    @Override
    public Object getAttribute(String name) {
        return this.attributeMap.get(name);
    }

    @Override
    public void removeAttribute(String name) {
        this.attributeMap.remove(name);
    }

    @Override
    public void setAttribute(String name, Object value) {
        this.attributeMap.put(name, value);
    }

    // ===================================================================
    // HTTP HEADERS
    // ===================================================================

    @Override
    public String getHttpMethod() {
        return this.finagleRequest.getMethod().getName();
    }

    @Override
    public HttpHeaders getHttpHeaders() {
        return this.jaxrsHeaders;
    }

    // ===================================================================
    // INPUT STREAM
    // ===================================================================

    @Override
    public InputStream getInputStream() {
        // this is the same way RestEASY implements this on top of HttpServletRequests
        if (this.overrideStream != null) {
            return this.overrideStream;
        }
        return new ChannelBufferInputStream(this.finagleRequest.getContent());
    }

    @Override
    public void setInputStream(InputStream stream) {
        this.overrideStream = stream;
    }

    // ===================================================================
    // FORM PARAMETERS
    // ===================================================================

    @Override
    public MultivaluedMap<String, String> getDecodedFormParameters() {
        return Encode.decode(getFormParameters());
    }

    @Override
    public MultivaluedMap<String, String> getFormParameters() {
        throw new UnsupportedOperationException("getFormParameters");
    }

    // ===================================================================
    // URI & PATH
    // ===================================================================

    @Override
    public UriInfo getUri() {
        return this.jaxrsUriInfo;
    }

    @Override
    public String getPreprocessedPath() {
        return this.preProcessedPath;
    }

    @Override
    public void setPreprocessedPath(String path) {
        this.preProcessedPath = path;
    }

    // ===================================================================
    // ASYNCHRONY
    // ===================================================================

    @Override
    public AsynchronousResponse createAsynchronousResponse(long suspendTimeout) {
        throw new UnsupportedOperationException("createAsynchronousResponse");
    }

    @Override
    public AsynchronousResponse getAsynchronousResponse() {
        throw new UnsupportedOperationException("getAsynchronousResponse");
    }

    @Override
    public void initialRequestThreadFinished() {
        // no-op for now
    }

    @Override
    public boolean isSuspended() {
        // no-op for now
        return false;
    }

    @Override
    public boolean isInitial() {
        // true indicates that this is a regular request being handled for the first time
        // (as opposed to an asynchronous request being re-sent through the stack)
        return true;
    }

}
