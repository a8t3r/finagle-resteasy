package com.opower.finagle.resteasy.client;

import com.opower.finagle.resteasy.util.ServiceUtils;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.resteasy.client.core.BaseClientResponse;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * Provides Resteasy's {@link org.jboss.resteasy.client.ClientResponse}
 * semantics on top of an inbound Netty
 * {@link org.jboss.netty.handler.codec.http.HttpResponse}.  Used when
 * we're making outbound calls via a Resteasy proxy, on the inbound leg.
 *
 * @author ed.peters
 * @param <T> object type of the response content
 */
public class InboundClientResponse<T> extends BaseClientResponse<T> {

    public InboundClientResponse(
            HttpResponse nettyResponse,
            ResteasyProviderFactory providerFactory) {
        super(new ResponseStreamFactory(nettyResponse));
        setStatus(nettyResponse.getStatus().getCode());
        setHeaders(ServiceUtils.toMultimap(nettyResponse));
        setProviderFactory(providerFactory);
    }

    /**
     * Helper that wraps a Netty response
     */
    static class ResponseStreamFactory implements BaseClientResponseStreamFactory {

        private final InputStream stream;

        public ResponseStreamFactory(HttpResponse nettyResponse) {
            this.stream =
                    new ChannelBufferInputStream(nettyResponse.getContent());
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return this.stream;
        }

        @Override
        public void performReleaseConnection() {
            // Not going to do anything here
        }
    }

}
