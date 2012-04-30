package opower.finagle;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Sample REST service
 *
 * @author ed.peters
 */
@Path("/foo")
public interface TestService {

    @GET
    @Path("/m1")
    @Produces(MediaType.WILDCARD)
    public String method1();

    @GET
    @Path("/m2")
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> method2();

    @GET
    @Path("/m3/{str}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> method2(@PathParam("str") String str);

}
