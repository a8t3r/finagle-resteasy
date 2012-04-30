package opower.finagle.server;

import com.twitter.finagle.Service;
import com.twitter.finagle.builder.ServerBuilder;
import com.twitter.finagle.http.Http;

import opower.finagle.TestServiceImpl;
import opower.finagle.resteasy.RestEasyServiceBuilder;

import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;

import java.net.InetSocketAddress;

/**
 * Simple server class that does the following:
 *
 * <ol>
 *
 *     <li>Creates a Finagle Service for handling REST requests.</li>
 *
 *     <li>Creates a Finagle Server to server it up.</li>
 *
 * </ol>
 *
 *
 * @author ed.peters
 */
public final class FinagleServer {

    private FinagleServer() {

    }

    public static void main(String [] args) throws Exception {

        // this is what Netty is going to serve up for us
        Service<HttpRequest,HttpResponse> service = new RestEasyServiceBuilder()
                .withEndpoint(new TestServiceImpl())
                .build();

        // and here we go!
        ServerBuilder.safeBuild(service, ServerBuilder.get()
                .sendBufferSize(256)
                .codec(Http.get())
                .name("HttpServer")
                .bindTo(new InetSocketAddress("localhost", 10000)));

    }

}
