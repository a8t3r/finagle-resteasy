package com.opower.finagle.resteasy.client;

import com.twitter.finagle.Service;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.resteasy.client.ClientExecutor;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;
import org.jboss.resteasy.specimpl.ResteasyUriBuilder;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import javax.ws.rs.core.UriBuilder;

/**
 * Implementation of Resteasy {@link org.jboss.resteasy.client.ClientExecutor}
 * interface on top of a Finagle {@link com.twitter.finagle.Service}.  This
 * allows us to make outbound calls using a Resteasy proxy.
 *
 * TODO is there any benefit to delaying the call to Future.get?
 * TODO can we be more efficent about the conversion of ChannelBuffers to bytes and back?
 *
 * @author ed.peters
 */
public class FinagleBasedClientExecutor implements ClientExecutor {

    private static final Log LOG =
            LogFactory.getLog(FinagleBasedClientExecutor.class);

    private final ResteasyProviderFactory providerFactory;
    private final Service<HttpRequest,HttpResponse> finagleService;

    public FinagleBasedClientExecutor(
            ResteasyProviderFactory providerFactory,
            Service<HttpRequest, HttpResponse> finagleService) {
        this.providerFactory = providerFactory;
        this.finagleService = finagleService;
    }

    @Override
    public ClientRequest createRequest(UriBuilder uriBuilder) {
        return new ClientRequest(uriBuilder, this, this.providerFactory);
    }

    @Override
    public ClientRequest createRequest(String uriTemplate) {
        UriBuilder builder = new ResteasyUriBuilder().uri(uriTemplate);
        return createRequest(builder);
    }

    @Override
    public ClientResponse execute(ClientRequest resteasyRequest)
            throws Exception {

        if (LOG.isDebugEnabled()) {
            LOG.debug("outbound "
                    + resteasyRequest.getHttpMethod() + " "
                    + resteasyRequest.getUri());
            for (String name : resteasyRequest.getHeaders().keySet()) {
                LOG.debug(name + ": " + resteasyRequest.getHeaders().get(name));
            }
        }

        HttpRequest nettyRequest = null;
        try {
            nettyRequest = new OutboundClientRequest(resteasyRequest);
        }
        catch (Exception e) {
            throw new RuntimeException(
                    "error converting outbound request (Resteasy --> Netty)", e);
        }

        HttpResponse nettyResponse = null;
        try {
            nettyResponse = this.finagleService.apply(nettyRequest).get();
        }
        catch (Exception e) {
            throw new RuntimeException("error invoking Finagle service", e);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("inbound " + nettyResponse.getStatus());
            for (String name : nettyResponse.getHeaderNames()) {
                LOG.debug(name + ": " + nettyResponse.getHeaders(name));
            }
        }

        ClientResponse response = null;
        try {
            response = new InboundClientResponse(nettyResponse,
                    this.providerFactory);
        }
        catch (Exception e) {
            throw new RuntimeException(
                    "error converting inbound response (Netty --> Resteasy)", e);
        }

        return response;
    }

    @Override
    public void close() throws Exception {
        // Nothing to do here
    }

}
