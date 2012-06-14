package com.opower.finagle.resteasy.example;

import com.opower.finagle.resteasy.client.ResteasyClientBuilder;

/**
 * @author ed.peters
 */
public class ExampleClient {

    public static void main(String [] args) {

        ExampleService service = ResteasyClientBuilder.get()
                .withHttpClient("localhost", 10000)
                .build(ExampleService.class);

        System.out.println(service.getGreeting());

    }

}
