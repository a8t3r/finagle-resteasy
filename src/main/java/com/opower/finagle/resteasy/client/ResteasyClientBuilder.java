package com.opower.finagle.resteasy.client;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.opower.finagle.resteasy.util.ServiceUtils;
import com.twitter.finagle.Service;
import com.twitter.finagle.builder.ClientBuilder;
import com.twitter.finagle.http.Http;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.resteasy.client.ClientExecutor;
import org.jboss.resteasy.client.ProxyFactory;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;

import static com.opower.finagle.resteasy.util.LoggingUtils.info;

/**
 * Fluent-style builder for clients that wrap a Finagle client with an
 * annotated service interface via Resteasy.
 *
 * @author ed.peters
 * @author jeff
 */
public class ResteasyClientBuilder {

    /**
     * Default limit for the number of host connections
     * @see {@link ClientBuilder#hostConnectionLimit(int)}
     */
    public static final int DEFAULT_HOST_CONNECTIONS = 1;

    private static final Logger LOG = LoggerFactory.getLogger(ResteasyClientBuilder.class);

    /*
     * Resteasy insists on having a real endpoint URL.  Since we're handling
     * the invoke through Finagle, this doesn't really matter, so we'll use
     * a dummy endpoint to keep it happy.
     */
    private static final URI DEFAULT_ENDPOINT_URI;
    static {
        try {
            DEFAULT_ENDPOINT_URI = new URI("http://localhost/");
        }
        catch (URISyntaxException e) {
            throw Throwables.propagate(e);
        }
    }

    private ResteasyProviderFactory providerFactory;
    private ClientBuilder clientBuilder;

    protected ResteasyClientBuilder() {
    }

    /**
     * Same as <code>withHttpClient(host, 80)</code>
     */
    public ResteasyClientBuilder withHttpClient(String host) {
        return withHttpClient(host, 80);
    }

    /**
     * Configures an HTTP client builder with the default host connection
     * limit.
     * @param host the remote host to connect to (e.g. "foo.bar.com")
     * @param port the remote port to connect to
     * @return this (for chaining)
     */
    public ResteasyClientBuilder withHttpClient(String host, int port) {
        Preconditions.checkNotNull(host, "host");
        Preconditions.checkArgument(port > 0, "invalid port " + port);
        info(LOG, "new HTTP client for %s:%s", host, port);
        ClientBuilder builder = ClientBuilder
                .get()
                .codec(Http.get())
                .hostConnectionLimit(DEFAULT_HOST_CONNECTIONS)
                .hosts(new InetSocketAddress(host, port));
        return withClientBuilder(builder);
    }

    /**
     * Same as <code>withZookeeperClient(zkHost, 2181, zkLocator)</code>
     */
    public ResteasyClientBuilder withZookeeperClient(String zkHost,
                                                     String zkLocator) {
        return withZookeeperClient(zkHost, 2181, zkLocator);
    }

    /**
     * Configures a Zookeeper client builder with the default host connection
     * limit and timeout.
     * @param zkHost the hostname of a Zookeeper server to connect to
     * @param zkPort the port for the Zookeeper host
     * @param zkLocator the name-service path of the service to connect to
     * @return this (for chaining)
     */
    public ResteasyClientBuilder withZookeeperClient(String zkHost,
                                                     int zkPort,
                                                     String zkLocator) {
        Preconditions.checkNotNull(zkHost, "zkHost");
        Preconditions.checkNotNull(zkLocator, "zkLocator");
        info(LOG, "new Zookeeper client for %s:%s", zkHost, zkPort, zkLocator);
        InetSocketAddress addr = new InetSocketAddress(zkHost, zkPort);

        ClientBuilder builder = ClientBuilder
                .get()
                .codec(Http.get())
                .hostConnectionLimit(DEFAULT_HOST_CONNECTIONS);

        return withClientBuilder(builder);
    }

    /**
     * @param clientBuilder an arbitrary {@link ClientBuilder} to use
     * @return this (for chaining)
     */
    public ResteasyClientBuilder withClientBuilder(ClientBuilder clientBuilder) {
        this.clientBuilder = clientBuilder;
        return this;
    }

    /**
     * @param providerFactory an arbitrary {@link ResteasyProviderFactory} to use
     * @return this (for chaining)
     */
    public ResteasyClientBuilder withProviderFactory(
            ResteasyProviderFactory providerFactory) {
        this.providerFactory = providerFactory;
        return this;
    }

    public <T> T build(Class<T> serviceInterface) {
        Preconditions.checkNotNull(this.clientBuilder, "clientBuilder");
        if (this.providerFactory == null) {
            this.providerFactory = ServiceUtils.getDefaultProviderFactory();
        }
        info(LOG, "creating proxy with interface %s", serviceInterface.getName());
        Service<HttpRequest,HttpResponse> service =
                ClientBuilder.safeBuild(this.clientBuilder);
        ClientExecutor executor =
                new FinagleBasedClientExecutor(this.providerFactory, service);
        return ProxyFactory.create(serviceInterface,
                DEFAULT_ENDPOINT_URI,
                executor,
                this.providerFactory);
    }

    public static ResteasyClientBuilder get() {
        return new ResteasyClientBuilder();
    }

}
