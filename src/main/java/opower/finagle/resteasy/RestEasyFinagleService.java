package opower.finagle.resteasy;

import com.twitter.finagle.Service;
import com.twitter.util.Future;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.resteasy.core.Dispatcher;

import static org.jboss.netty.util.CharsetUtil.UTF_8;
import static opower.util.log.LoggingUtils.info;

/**
 * Implements the Finagle {@link Service} interface by wrapping inbound requests and passing them
 * into a RestEASY {@link org.jboss.resteasy.core.Dispatcher}.
 *
 * TODO how do we invoke the Dispatcher asynchronously?
 * TODO maybe don't execute the request until Future.get() is actually called
 *
 * @author ed.peters
 */
public class RestEasyFinagleService extends Service<HttpRequest,HttpResponse> {

    private static final Log LOG = LogFactory.getLog(RestEasyFinagleService.class);

    private final Dispatcher dispatcher;

    public RestEasyFinagleService(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    public Future<HttpResponse> apply(HttpRequest request) {
        info(LOG, "inbound request %s %s", request.getMethod().getName(), request.getUri());
        if (LOG.isDebugEnabled()) {
            for (String name : request.getHeaderNames()) {
                LOG.debug(name + ": " + request.getHeaders(name));
            }
            LOG.debug("body: " + request.getContent().toString(UTF_8));
        }
        HttpResponse nettyResponse = generateResponse(request);
        info(LOG, "outbound response %s", nettyResponse.getStatus());
        return Future.value(nettyResponse);
    }

    /*
     * Generates the response
     */
    protected HttpResponse generateResponse(HttpRequest nettyRequest) {
        NettyRequestWrapper jaxrsRequest = null;
        NettyResponseWrapper jaxrsResponse = null;
        try {
            jaxrsRequest = new NettyRequestWrapper(nettyRequest);
            jaxrsResponse = new NettyResponseWrapper(nettyRequest);
        }
        catch (Exception e) {
            info(LOG, e, "error creating request/response wrappers");
            return createErrorResponse(nettyRequest, e);
        }
        try {
            this.dispatcher.invoke(jaxrsRequest, jaxrsResponse);
        }
        catch (Exception e) {
            info(LOG, e, "error during dispatch");
            return createErrorResponse(nettyRequest, e);
        }
        return jaxrsResponse.getNettyResponse();
    }

    /*
     * JAX-RS already specifies a mechanism that lets you customize how service exceptions get converted
     * into HTTP responses, and Resteasy has a number of built-in renderings for common exception types.
     * So this should only wind up getting called for strange exceptions that don't get handled normally,
     * like failures inside Resteasy itself.
     * @see "http://docs.jboss.org/resteasy/2.0.0.GA/userguide/html/ExceptionHandling.html"
     */
    protected HttpResponse createErrorResponse(HttpRequest request, Exception e) {
        HttpResponseStatus status = new HttpResponseStatus(500, e.toString());
        return new DefaultHttpResponse(request.getProtocolVersion(), status);
    }

}
