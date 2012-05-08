package opower.finagle.resteasy;

import com.twitter.finagle.Service;
import com.twitter.finagle.builder.ClientBuilder;
import com.twitter.finagle.http.Http;

import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Required;

import java.net.InetSocketAddress;

/**
 * FactoryBean implementation that knows how to create a Finagle/RestEASY client proxy using
 * the correct provider factory for the enclosing bean context.
 *
 * @see opower.rest.support.RestEasyProxyFactory
 * @param <T> the type of the intreface being proxied (ie, the service being used)
 * @author jeff (with original attribution to ed.peters)
 */
public class RestEasyFinagleClientProxyFactory<T> implements FactoryBean<T> {
    private Class<T> proxyInterface;
    private String hostname;
    private int port;
    private T proxy;

    @Required
    public void setProxyInterface(Class<T> proxyInterface) {
        this.proxyInterface = proxyInterface;
    }

    @Required
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    @Required
    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public Class<T> getObjectType() {
        return this.proxyInterface;
    }

    @Override
    public T getObject() throws Exception {
        if (this.proxy == null) {
            Service<HttpRequest,HttpResponse> httpClient =
                ClientBuilder.safeBuild(
                        ClientBuilder.get()
                            .codec(Http.get())
                            .hosts(new InetSocketAddress(this.hostname, this.port))
                            .hostConnectionLimit(1));

            this.proxy = new RestEasyClientBuilder<T>(this.proxyInterface)
                .withService(httpClient)
                .build();
        }
        return this.proxy;
    }
}
