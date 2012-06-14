# What is Finagle?

Finagle is an open-source library from twitter that provides a lightweight
abstraction for HTTP services.  The homepage is here:

http://twitter.github.com/finagle/

# What is Resteasy?

Resteasy is JBoss's implementation of the JAX-RS standard, which general makes
writing REST services a matter of slapping some annotations on your code.  The
homepage is here:

http://www.jboss.org/resteasy

# What is this?

This project bridges the two, allowing you to write REST services as
Java-annotated classes, and service them through Finagle.  Here's an example
of a service interface:

```
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("/greeting")
public interface ExampleService {

    @GET
    @Produces("application/json")
    String getGreeting();

}
```

Here's a simple server example using this package:

```
import com.opower.finagle.resteasy.server.ResteasyServiceBuilder;
import com.twitter.finagle.Service;
import com.twitter.finagle.builder.Server;
import com.twitter.finagle.builder.ServerBuilder;
import com.twitter.finagle.http.Http;

import java.net.InetSocketAddress;

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

        // ... profit!

    }

}
```

and here's an example of a client for that service:

```
import com.opower.finagle.resteasy.client.ResteasyClientBuilder;

public class ExampleClient {

    public static void main(String [] args) {

        ExampleService service = ResteasyClientBuilder.get()
                .withHttpClient("localhost", 10000)
                .build(ExampleService.class);

        System.out.println(service.getGreeting());

    }

}
```

Notice the supple ease.
