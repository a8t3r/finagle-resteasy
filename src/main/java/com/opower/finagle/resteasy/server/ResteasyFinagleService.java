package com.opower.finagle.resteasy.server;

import com.google.common.base.Preconditions;
import com.twitter.finagle.Service;
import com.twitter.util.Future;
import com.twitter.util.Promise;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.resteasy.core.Dispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;

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

    private static final Logger LOG = LoggerFactory.getLogger(ResteasyFinagleService.class);

    private final Dispatcher dispatcher;
    private final Executor executor;

    public ResteasyFinagleService(Dispatcher dispatcher,
                                  Executor executor) {
        this.dispatcher = Preconditions.checkNotNull(dispatcher, "dispatcher");
        this.executor = Preconditions.checkNotNull(executor, "executor");
    }

    /**
     * Schedules a request for completion
     * @param request an inbound Netty request
     * @return a {@link Promise} that will return the result of completing
     * the request
     */
    public Future<HttpResponse> apply(HttpRequest request) {
        Preconditions.checkNotNull(request, "request");
        info(LOG, "inbound request %s %s",
                request.getMethod().getName(),
                request.getUri());
        Promise<HttpResponse> promise = new Promise<HttpResponse>();
        this.executor.execute(new ResponseWorker(request, promise));
        return promise;
    }

    /**
     * {@link Runnable} implementation that converts a Netty request to
     * Resteasy, then uses the Resteasy Dispatcher to satisfy the call.
     */
    protected class ResponseWorker implements Runnable {

        private final HttpRequest nettyRequest;
        private final Promise<HttpResponse> promise;

        public ResponseWorker(HttpRequest nettyRequest,
                              Promise<HttpResponse> promise) {
            this.nettyRequest = nettyRequest;
            this.promise = promise;
        }

        @Override
        public void run() {
            HttpVersion version = nettyRequest.getProtocolVersion();
            HttpResponse nettyResponse = null;
            try {
                nettyResponse = computeResponse(version);
            }
            catch (Exception e) {
                info(LOG, e, "unhandled error creating HTTP response");
                nettyResponse = new UnhandledErrorResponse(version, e);
            }
            info(LOG, "outbound response %s", nettyResponse.getStatus());
            this.promise.setValue(nettyResponse);
        }

        protected HttpResponse computeResponse(HttpVersion version) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("incoming " + nettyRequest.getUri());
                for (String name : nettyRequest.getHeaderNames()) {
                    LOG.debug(name + ": " + nettyRequest.getHeaders(name));
                }
                LOG.debug("body: " +
                        nettyRequest.getContent().toString(UTF_8));
            }
            InboundServiceRequest jaxrsRequest =
                    new InboundServiceRequest(nettyRequest);

            OutboundServiceResponse jaxrsResponse =
                    new OutboundServiceResponse(version);
            dispatcher.invoke(jaxrsRequest, jaxrsResponse);
            return jaxrsResponse.getNettyResponse();
        }

    }

}
