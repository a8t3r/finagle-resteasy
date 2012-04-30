package opower.finagle;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Sample REST service
 *
 * @author ed.peters
 */
@Path("/foo")
public class TestServiceImpl implements TestService {

    @Override
    public String method1() {
        return UUID.randomUUID().toString();
    }

    @Override
    public List<String> method2() {
        return Arrays.asList(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString()
        );
    }

    @Override
    public List<String> method2(@PathParam("str") String str) {
        return Arrays.asList(
                str,
                str,
                str
        );
    }

}
