package com.opower.finagle.resteasy.example;

import com.opower.finagle.resteasy.client.ResteasyClientBuilder;

/**
 * Example of creating a client
 *
 * @author ed.peters
 */
public final class ExampleClient {

    private ExampleClient() { }

    public static void main(String [] args) {

        ExampleService service = ResteasyClientBuilder.get()
                .withHttpClient("localhost", 10000)
                .build(ExampleService.class);

        System.out.println(service.getGreeting());

    }

}
