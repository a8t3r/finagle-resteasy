package com.opower.finagle.resteasy.example;

import com.opower.finagle.resteasy.server.ResteasyServiceBuilder;
import com.twitter.finagle.Service;
import com.twitter.finagle.builder.Server;
import com.twitter.finagle.builder.ServerBuilder;
import com.twitter.finagle.http.Http;

import java.net.InetSocketAddress;

/**
 * Example of creating a server
 *
 * @author ed.peters
 */
public class ExampleServer implements ExampleService {

    @Override
    public String getGreeting() {
        return "Hello, World!";
    }

    public static void main(String [] args) throws Exception {

        Service service = ResteasyServiceBuilder.get()
                .withEndpoint(new ExampleServer())
                .build();

        ServerBuilder builder = ServerBuilder.get()
                .name("ExampleServer")
                .codec(Http.get())
                .bindTo(new InetSocketAddress("localhost", 10000));

        Server server = ServerBuilder.safeBuild(service, builder);

        // from here your application can continue to do other work.
        // the Server object has a background non-daemon thread that
        // will keep the JVM alive until you call close().

    }

}
