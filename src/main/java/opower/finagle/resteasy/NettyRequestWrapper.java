package opower.finagle.resteasy;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.resteasy.plugins.providers.FormUrlEncodedProvider;
import org.jboss.resteasy.spi.AsynchronousResponse;
import org.jboss.resteasy.util.Encode;

import javax.annotation.Nullable;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import java.io.InputStream;
import java.io.IOException;
import java.util.Map;

/**
 * Wraps Finagle's Netty HTTP request to provide the semantics of the JBoss RestEASY library.
 * The majority of this is just translating data from one form to another.
 *
 * TODO implement @FormParam support (if we care): @see org.jboss.resteasy.spi.HttpRequest#getFormParameters()
 * TODO implement hooks for asynchronous processing: "http://bill.burkecentral.com/2008/10/09/jax-rs-asynch-http/"
 *
 * @author ed.peters
 */
public class NettyRequestWrapper implements org.jboss.resteasy.spi.HttpRequest {

    /** HTTP header: content type negotiation */
    public static final String ACCEPT_HEADER = "Accept";

    /** HTTP header: language negotiation */
    public static final String ACCEPT_LANGUAGE_HEADER = "Accept-Language";

    /** HTTP header: content type output */
    public static final String CONTENT_TYPE_HEADER = "Content-Type";

    /** Converts Strings to MediaTypes */
    public static final Function<String,MediaType> TO_MEDIA_TYPE = new Function<String, MediaType>() {
        @Override
        public MediaType apply(@Nullable String input) {
            return input == null ? null : MediaType.valueOf(input);
        }
    };

    private final org.jboss.netty.handler.codec.http.HttpRequest nettyRequest;
    private final Map<String,Object> attributeMap;
    private final HttpHeaders jaxrsHeaders;
    private final UriInfo jaxrsUriInfo;
    private InputStream overrideStream;
    private InputStream underlyingStream;
    private String preProcessedPath;

    public NettyRequestWrapper(HttpRequest nettyRequest) {
        this.nettyRequest = nettyRequest;
        this.jaxrsHeaders = RestEasyUtils.toHeaders(RestEasyUtils.getHeadersAsMultimap(nettyRequest));
        this.jaxrsUriInfo = RestEasyUtils.extractUriInfo(nettyRequest.getUri());
        this.attributeMap = Maps.newHashMap();
        this.overrideStream = null;
        this.underlyingStream = new ChannelBufferInputStream(nettyRequest.getContent());
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
        return this.nettyRequest.getMethod().getName();
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
        // this is the same way RestEASY implements this on top of HttpServletRequests: as a
        // temporary override of the underlying input stream
        return this.overrideStream == null ? this.underlyingStream : this.overrideStream;
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
        try {
            return FormUrlEncodedProvider.parseForm(getInputStream());
        }
        catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
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
