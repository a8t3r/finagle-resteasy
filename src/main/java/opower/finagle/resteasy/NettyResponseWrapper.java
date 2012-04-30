package opower.finagle.resteasy;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.jboss.netty.buffer.ChannelBufferOutputStream;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Wraps Finagle's Netty HTTP response to provide the semantics of JBoss's RestEASY library.
 *
 * TODO do we want cookie support? does netty even allow it?
 * TODO no clue how to get #isCommitted() from netty
 * TODO should we support chunking?
 *
 * @author ed.peters
 */
public class NettyResponseWrapper implements org.jboss.resteasy.spi.HttpResponse {

    private final org.jboss.netty.handler.codec.http.HttpResponse finagleResponse;
    private final MultivaluedMap<String,Object> headerWrapper;
    private final OutputStream outputStream;

    public NettyResponseWrapper(HttpRequest finagleRequest) {
        this.finagleResponse = new DefaultHttpResponse(finagleRequest.getProtocolVersion(), HttpResponseStatus.OK);
        this.finagleResponse.setChunked(false);
        this.finagleResponse.setContent(ChannelBuffers.dynamicBuffer());
        this.headerWrapper = new HeaderWrapper(this.finagleResponse);
        this.outputStream = new ChannelBufferOutputStream(this.finagleResponse.getContent());
    }

    public HttpResponse getNettyResponse() {
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
        return false;
    }

    @Override
    public void reset() {
        // this is supposed to clear headers and status, but netty doesn't have a notion
        // of a "null" value for status, so we'll do the best we can
        this.finagleResponse.clearHeaders();
    }

    // ===================================================================
    // HELPERS
    // ===================================================================

    /**
     * Implements the {@link javax.ws.rs.core.MultivaluedMap} API on top of a Finagle response object
     */
    public static class HeaderWrapper implements MultivaluedMap<String,Object> {

        private final HttpResponse finagleResponse;

        public HeaderWrapper(HttpResponse finagleResponse) {
            this.finagleResponse = finagleResponse;
        }

        @Override
        public void add(String key, Object value) {
            finagleResponse.addHeader(key, value);
        }

        @Override
        public void putSingle(String key, Object value) {
            finagleResponse.setHeader(key, value);
        }

        @Override
        public Object getFirst(String key) {
            return finagleResponse.getHeader(key);
        }

        @Override
        public int size() {
            return finagleResponse.getHeaderNames().size();
        }

        @Override
        public boolean isEmpty() {
            return size() > 0;
        }

        @Override
        public boolean containsKey(Object key) {
            return finagleResponse.containsHeader(key.toString());
        }

        @Override
        public boolean containsValue(Object value) {
            throw new UnsupportedOperationException("containsValue");
        }

        @Override
        @SuppressWarnings("unchecked")
        public List<Object> get(Object key) {
            // OK because we're returning a List
            return (List) finagleResponse.getHeaders(key.toString());
        }

        @Override
        public List<Object> put(String key, List<Object> values) {
            finagleResponse.setHeader(key, values);
            return null;
        }

        @Override
        public List<Object> remove(Object key) {
            finagleResponse.removeHeader(key.toString());
            return null;
        }

        @Override
        public void putAll(Map<? extends String, ? extends List<Object>> map) {
            for (String key : map.keySet()) {
                put(key, map.get(key));
            }
        }

        @Override
        public void clear() {
            finagleResponse.clearHeaders();
        }

        @Override
        public Set<String> keySet() {
            return finagleResponse.getHeaderNames();
        }

        @Override
        public Collection<List<Object>> values() {
            List<List<Object>> all = Lists.newArrayList();
            for (String key : keySet()) {
                all.add(get(key));
            }
            return all;
        }

        @Override
        public Set<Entry<String,List<Object>>> entrySet() {
            Map<String,List<Object>> map = Maps.newHashMap();
            for (String key : keySet()) {
                map.put(key, get(key));
            }
            return map.entrySet();
        }
    }

}
