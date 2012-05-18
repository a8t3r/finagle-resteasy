package opower.finagle.server;

import com.twitter.finagle.Service;
import com.twitter.finagle.builder.ClientBuilder;
import com.twitter.finagle.http.Http;
import opower.finagle.TestService;
import opower.finagle.resteasy.RestEasyClientBuilder;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * Makes for a client
 *
 * @author ed.peters
 */
public final class FinagleClient {

    private FinagleClient() {

    }

    public static void main(String [] args) throws Exception {

        Service<HttpRequest,HttpResponse> httpClient =
                ClientBuilder.safeBuild(
                        ClientBuilder.get()
                                .codec(Http.get())
                                .hosts(new InetSocketAddress("localhost", 10000))
                                .hostConnectionLimit(1));

        TestService remote = new RestEasyClientBuilder<TestService>(TestService.class)
                .withService(httpClient)
                .build();

        System.err.println(remote.method2());

        Map<String, String> map = new HashMap<String, String>();
        map.put("one", "ONE");
        map.put("two", "TWO");
        map.put("three", "THREE");
        System.err.println(remote.method4(map));
        System.exit(0);

    }

}
