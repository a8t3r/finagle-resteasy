package com.opower.finagle.resteasy.server;

import com.twitter.finagle.Service;
import com.twitter.util.Future;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.resteasy.core.Dispatcher;

import static com.opower.finagle.resteasy.util.LoggingUtils.info;
import static org.jboss.netty.util.CharsetUtil.UTF_8;

/**
 * Implements the Finagle {@link com.twitter.finagle.Service} interface by
 * wrapping inbound requests and passing them into a RestEASY
 * {@link org.jboss.resteasy.core.Dispatcher}.
 *
 * @author ed.peters
 */
public class ResteasyFinagleService extends Service<HttpRequest,HttpResponse> {

    // TODO put the service invoke into a background thread

    private static final Log LOG = LogFactory.getLog(ResteasyFinagleService.class);

    private final Dispatcher dispatcher;

    public ResteasyFinagleService(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    public Future<HttpResponse> apply(HttpRequest request) {
        info(LOG, "inbound request %s %s",
                request.getMethod().getName(),
                request.getUri());
        HttpResponse nettyResponse = generateResponse(request);
        info(LOG, "outbound response %s", nettyResponse.getStatus());
        return Future.value(nettyResponse);
    }

    /*
     * Generates the response
     */
    protected HttpResponse generateResponse(HttpRequest nettyRequest) {
        InboundServiceRequest jaxrsRequest = null;
        OutboundServiceResponse jaxrsResponse = null;
        HttpVersion version = nettyRequest.getProtocolVersion();
        try {
            if (LOG.isDebugEnabled()) {
                LOG.debug("incoming " + nettyRequest.getUri());
                for (String name : nettyRequest.getHeaderNames()) {
                    LOG.debug(name + ": " + nettyRequest.getHeaders(name));
                }
                LOG.debug("body: " + nettyRequest.getContent().toString(UTF_8));
            }
            jaxrsRequest = new InboundServiceRequest(nettyRequest);
            jaxrsResponse = new OutboundServiceResponse(version);
        }
        catch (Exception e) {
            info(LOG, e, "error creating request/response wrappers");
            return new UnhandledErrorResponse(version, e);
        }
        try {
            this.dispatcher.invoke(jaxrsRequest, jaxrsResponse);
        }
        catch (Exception e) {
            info(LOG, e, "error during dispatch");
            return new UnhandledErrorResponse(version, e);
        }
        return jaxrsResponse.getNettyResponse();
    }

}
