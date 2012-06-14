package com.opower.finagle.resteasy.example;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

/**
 * Sample of a JAX-RS annotated service interface
 *
 * @author ed.peters
 */
@Path("/greeting")
public interface ExampleService {

    @GET
    @Produces("application/json")
    String getGreeting();

}
