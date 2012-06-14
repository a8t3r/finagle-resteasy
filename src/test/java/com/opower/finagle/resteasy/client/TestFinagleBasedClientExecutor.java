package com.opower.finagle.resteasy.client;

import com.opower.finagle.resteasy.util.ServiceUtils;
import com.twitter.finagle.Service;
import com.twitter.util.Future;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.CONFLICT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.junit.Assert.assertNotNull;

/**
 * Tests the adapter between Finagle and Resteasy (this exercises the
 * basic control paths; edge cases of request/response mappings are
 * covered in other tests)
 *
 * @author ed.peters
 */
public class TestFinagleBasedClientExecutor {

    @Test
    public void testHappyPath() throws Exception {

        ClientRequest resteasyRequest = new ClientRequest("/foo/bar");
        resteasyRequest.setHttpMethod("GET");
        resteasyRequest.accept(APPLICATION_JSON_TYPE);

        final HttpResponse nettyResponse =
                new DefaultHttpResponse(HTTP_1_1, CONFLICT);

        Service<HttpRequest,HttpResponse> service =
                new Service<HttpRequest,HttpResponse>() {
                    @Override
                    public Future<HttpResponse> apply(HttpRequest request) {
                        return Future.value(nettyResponse);
                    }
                };

        FinagleBasedClientExecutor executor = new FinagleBasedClientExecutor(
                ServiceUtils.getDefaultProviderFactory(),
                service);

        ClientResponse resteasyResponse = executor.execute(resteasyRequest);
        assertNotNull("null response", resteasyRequest);
        assertEquals(Response.Status.CONFLICT,
                resteasyResponse.getResponseStatus());

    }

}
