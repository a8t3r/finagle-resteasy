package com.opower.finagle.resteasy.example;

import javax.ws.rs.*;

/**
 * Sample of a JAX-RS annotated service interface
 *
 * @author ed.peters
 */
@Path("/greeting")
@Produces("application/json")
@Consumes("application/json")
public interface ExampleService {

    @POST
    @Path("/{name}")
    String getGreeting(@PathParam("name") String name, Model model);

}
