package com.opower.finagle.resteasy.server;

import com.google.common.collect.Maps;
import com.opower.finagle.resteasy.util.ServiceUtils;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.resteasy.plugins.providers.FormUrlEncodedProvider;
import org.jboss.resteasy.spi.AsynchronousResponse;
import org.jboss.resteasy.util.Encode;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Used when we're hosting a Resteasy-annotated service implementation. to
 * Implements Resteasy's {@link org.jboss.resteasy.spi.HttpRequest} interface
 * on top of a Netty {@link org.jboss.netty.handler.codec.http.HttpRequest}
 *
 * Currently doesn't support asynchronous processing.
 *
 * @author ed.peters
 *
 * @see "http://bill.burkecentral.com/2008/10/09/jax-rs-asynch-http/"
 */
public class InboundServiceRequest implements org.jboss.resteasy.spi.HttpRequest {

    private final HttpRequest nettyRequest;
    private final Map<String,Object> attributeMap;
    private final HttpHeaders jaxrsHeaders;
    private final UriInfo jaxrsUriInfo;
    private InputStream overrideStream;
    private InputStream underlyingStream;
    private String preProcessedPath;
    private MultivaluedMap<String,String> rawFormParams;
    private MultivaluedMap<String,String> decodedFormParams;
    private final Object formParamLock;

    public InboundServiceRequest(HttpRequest nettyRequest) {
        this.nettyRequest = nettyRequest;
        this.jaxrsHeaders =
                ServiceUtils.toHeaders(ServiceUtils.toMultimap(nettyRequest));
        this.jaxrsUriInfo = ServiceUtils.toUriInfo(nettyRequest);
        this.attributeMap = Maps.newHashMap();
        this.overrideStream = null;
        this.underlyingStream =
                new ChannelBufferInputStream(nettyRequest.getContent());
        this.formParamLock = new Object();
    }

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

    @Override
    public String getHttpMethod() {
        return this.nettyRequest.getMethod().getName();
    }

    @Override
    public HttpHeaders getHttpHeaders() {
        return this.jaxrsHeaders;
    }

    @Override
    public InputStream getInputStream() {
        // this is the same way RestEASY implements this on top of
        // HttpServletRequests: as a temporary override of the underlying
        // input stream
        return this.overrideStream == null
                ? this.underlyingStream
                : this.overrideStream;
    }

    @Override
    public void setInputStream(InputStream stream) {
        this.overrideStream = stream;
    }

    @Override
    public MultivaluedMap<String,String> getDecodedFormParameters() {
        readFormParams();
        return this.decodedFormParams;
    }

    @Override
    public MultivaluedMap<String,String> getFormParameters() {
        readFormParams();
        return this.rawFormParams;
    }

    /*
     * Since reading form parameters requires reading the request body,
     * we'll make sure we only do it once.  We'll synchronize on a
     * separate lock object just to be safe.
     */
    protected void readFormParams() {
        synchronized (this.formParamLock) {
            if (this.rawFormParams == null) {
                try {
                    this.rawFormParams = FormUrlEncodedProvider.parseForm(
                            getInputStream());
                }
                catch (IOException ioe) {
                    throw new RuntimeException(ioe);
                }
                this.decodedFormParams = Encode.decode(this.rawFormParams);
            }
        }
    }

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
        // true indicates that this is a regular request being handled for
        // the first time (as opposed to an asynchronous request being re-sent
        // through the stack)
        return true;
    }
}
