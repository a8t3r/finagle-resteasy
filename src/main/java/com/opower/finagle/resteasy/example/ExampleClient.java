package com.opower.finagle.resteasy.example;

import com.opower.finagle.resteasy.client.ResteasyClientBuilder;

import java.util.ArrayList;
import java.util.List;

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

        int count = 100;
        Model model = new Model();
        model.setName("slavik");
        model.setAge(count);
        List<Model> friends = new ArrayList<Model>();
        model.setFriends(friends);

        while (count -- > 0) {
            Model friend = new Model();
            friend.setName("slavik" + Integer.toString(count));
            friend.setAge(count);
            friends.add(friend);

            System.out.println(service.getGreeting("gosha", model));
        }
    }

}
