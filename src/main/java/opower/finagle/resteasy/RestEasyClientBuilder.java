package opower.finagle.resteasy;

import com.twitter.finagle.Service;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.resteasy.client.ProxyFactory;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Knows how to build a RestEASY client.
 *
 * @param <T> no fucking clue.  ed?
 * @author ed.peters
 */
public class RestEasyClientBuilder<T> {

    /**
     * Resteasy insists on having a real endpoint URL.  Since we're handling the invoke
     * through Finagle, we'll use a dummy endpoint to keep it happy.
     */
    public static final URI DEFAULT_ENDPOINT_URI;
    static {
        try {
            DEFAULT_ENDPOINT_URI = new URI("http://localhost/");
        }
        catch (URISyntaxException e) {
            throw new RuntimeException("wtf?", e);
        }
    }

    private final ResteasyProviderFactory providerFactory;
    private final Class<T> endpointInterface;
    private Service<HttpRequest,HttpResponse> service;

    public RestEasyClientBuilder(Class<T> endpointInterface) {
        this.providerFactory = ResteasyProviderFactory.getInstance();
        this.endpointInterface = endpointInterface;
    }

    public RestEasyClientBuilder<T> withService(Service<HttpRequest,HttpResponse> service) {
        this.service = service;
        return this;
    }

    public T build() {
        return ProxyFactory.create(
                this.endpointInterface,
                DEFAULT_ENDPOINT_URI,
                new FinagleServiceClientExecutor(providerFactory, service),
                this.providerFactory);
    }

}
