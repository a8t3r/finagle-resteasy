package com.opower.finagle.resteasy.server;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.opower.finagle.resteasy.util.ServiceUtils;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.resteasy.plugins.providers.FormUrlEncodedProvider;
import org.jboss.resteasy.spi.ResteasyAsynchronousContext;
import org.jboss.resteasy.spi.ResteasyAsynchronousResponse;
import org.jboss.resteasy.spi.ResteasyUriInfo;
import org.jboss.resteasy.util.Encode;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Used when we're hosting a Resteasy-annotated service implementation.
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
    private final ResteasyUriInfo jaxrsUriInfo;
    private InputStream overrideStream;
    private InputStream underlyingStream;
    private String preProcessedPath;
    private MultivaluedMap<String,String> rawFormParams;
    private MultivaluedMap<String,String> decodedFormParams;

    public InboundServiceRequest(HttpRequest nettyRequest) {
        this.nettyRequest = nettyRequest;
        this.jaxrsHeaders =
                ServiceUtils.toHeaders(ServiceUtils.toMultimap(nettyRequest));
        this.jaxrsUriInfo = (ResteasyUriInfo) ServiceUtils.toUriInfo(nettyRequest);
        this.attributeMap = Maps.newHashMap();
        this.overrideStream = null;
        this.underlyingStream =
                new ChannelBufferInputStream(nettyRequest.getContent());
    }

    @Override
    public Object getAttribute(String name) {
        Preconditions.checkNotNull(name, "name");
        return this.attributeMap.get(name);
    }

    @Override
    public void removeAttribute(String name) {
        Preconditions.checkNotNull(name, "name");
        this.attributeMap.remove(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return null;
    }

    @Override
    public ResteasyAsynchronousContext getAsyncContext() {
        return new ResteasyAsynchronousContext() {
            @Override
            public boolean isSuspended() {
                return false;
            }

            @Override
            public ResteasyAsynchronousResponse getAsyncResponse() {
                return null;
            }

            @Override
            public ResteasyAsynchronousResponse suspend() throws IllegalStateException {
                return null;
            }

            @Override
            public ResteasyAsynchronousResponse suspend(long millis) throws IllegalStateException {
                return null;
            }

            @Override
            public ResteasyAsynchronousResponse suspend(long time, TimeUnit unit) throws IllegalStateException {
                return null;
            }
        };
    }

    @Override
    public void setAttribute(String name, Object value) {
        Preconditions.checkNotNull(name, "name");
        this.attributeMap.put(name, value);
    }

    @Override
    public String getHttpMethod() {
        return this.nettyRequest.getMethod().getName();
    }

    @Override
    public void setHttpMethod(String method) {

    }

    @Override
    public void setRequestUri(URI requestUri) throws IllegalStateException {

    }

    @Override
    public void setRequestUri(URI baseUri, URI requestUri) throws IllegalStateException {

    }

    @Override
    public HttpHeaders getHttpHeaders() {
        return this.jaxrsHeaders;
    }

    @Override
    public MultivaluedMap<String, String> getMutableHeaders() {
        return null;
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
    public ResteasyUriInfo getUri() {
        return jaxrsUriInfo;
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

    // since parsing form parameters requires reading the request body, we need to
    // synchronize it so it happens only once
    protected synchronized void readFormParams() {
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

    @Override
    public boolean isInitial() {
        // true indicates that this is a regular request being handled for
        // the first time (as opposed to an asynchronous request being re-sent
        // through the stack)
        return true;
    }

    @Override
    public void forward(String path) {

    }

    @Override
    public boolean wasForwarded() {
        return false;
    }
}
