package opower.finagle.resteasy;

import com.twitter.finagle.Service;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.resteasy.client.ClientExecutor;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;
import org.jboss.resteasy.specimpl.UriBuilderImpl;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import javax.ws.rs.core.UriBuilder;

import static opower.util.log.LoggingUtils.info;

/**
 * Implementation of the RestEASY {@link org.jboss.resteasy.client.ClientExecutor} interface
 * on top of a Finagle {@link com.twitter.finagle.Service}
 *
 * TODO is there any benefit to delaying the call to Future.get?
 * TODO is there any way to be more efficent about the conversion of ChannelBuffers to bytes and back?
 *
 * @author ed.peters
 */
public class FinagleServiceClientExecutor implements ClientExecutor {

    private static final Log LOG = LogFactory.getLog(FinagleServiceClientExecutor.class);

    private final ResteasyProviderFactory providerFactory;
    private final Service<HttpRequest,HttpResponse> finagleService;

    public FinagleServiceClientExecutor(
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
        return createRequest(new UriBuilderImpl().uriTemplate(uriTemplate));
    }

    @Override
    public ClientResponse execute(ClientRequest request) throws Exception {

        info(LOG, "outbound request %s %s", request.getHttpMethod(), request.getUri());
        if (LOG.isDebugEnabled()) {
            for (String name : request.getHeaders().keySet()) {
                LOG.debug(name + ": " + request.getHeaders().get(name));
            }
        }

        HttpRequest nettyRequest = null;
        try {
            nettyRequest = RestEasyUtils.convertToNettyRequest(request);
        }
        catch (Exception e) {
            throw new RuntimeException("error converting outbound RestEASY request to Netty request", e);
        }

        HttpResponse nettyResponse = null;
        try {
            nettyResponse = this.finagleService.apply(nettyRequest).get();
        }
        catch (Exception e) {
            throw new RuntimeException("error invoking Finagle service", e);
        }

        info(LOG, "inbound response %s", nettyResponse.getStatus());
        if (LOG.isDebugEnabled()) {
            for (String name : nettyResponse.getHeaderNames()) {
                LOG.debug(name + ": " + nettyResponse.getHeaders(name));
            }
        }

        ClientResponse response = null;
        try {
            response = RestEasyUtils.convertToClientResponse(nettyResponse, this.providerFactory);
        }
        catch (Exception e) {
            throw new RuntimeException("error converting inbound Netty response to RestEASY response", e);
        }

        return response;
    }

}
