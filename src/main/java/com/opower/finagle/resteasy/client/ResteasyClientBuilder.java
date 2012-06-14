package com.opower.finagle.resteasy.client;

import com.google.common.base.Preconditions;
import com.opower.finagle.resteasy.util.ServiceUtils;
import com.twitter.common.quantity.Amount;
import com.twitter.common.quantity.Time;
import com.twitter.common.zookeeper.ServerSet;
import com.twitter.common.zookeeper.ServerSetImpl;
import com.twitter.common.zookeeper.ZooKeeperClient;
import com.twitter.finagle.Service;
import com.twitter.finagle.builder.ClientBuilder;
import com.twitter.finagle.http.Http;
import com.twitter.finagle.zookeeper.ZookeeperServerSetCluster;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.resteasy.client.ClientExecutor;
import org.jboss.resteasy.client.ProxyFactory;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

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

    private static final Log LOG = LogFactory.getLog(ResteasyClientBuilder.class);

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
            throw new RuntimeException("wtf?", e);
        }
    }

    private ResteasyProviderFactory providerFactory;
    private ClientBuilder clientBuilder;

    protected ResteasyClientBuilder() {
    }

    public ResteasyClientBuilder withHttpClient(String host) {
        return withHttpClient(host, 80);
    }

    public ResteasyClientBuilder withHttpClient(String host, int port) {
        Preconditions.checkNotNull(host, "no host supplied");
        info(LOG, "new HTTP client for %s:%s", host, port);
        ClientBuilder builder = ClientBuilder
                .get()
                .codec(Http.get())
                .hostConnectionLimit(1)
                .hosts(new InetSocketAddress(host, port));
        return withClientBuilder(builder);
    }

    public ResteasyClientBuilder withZookeeperClient(String zkHost,
                                                     int zkPort,
                                                     String zkLocator) {
        Preconditions.checkNotNull(zkHost, "no host supplied");
        Preconditions.checkNotNull(zkLocator, "no service locator supplied");
        info(LOG, "new Zookeeper client for %s:%s", zkHost, zkPort, zkLocator);
        InetSocketAddress addr = new InetSocketAddress(zkHost, zkPort);
        ServerSet serverSet = new ServerSetImpl(
                new ZooKeeperClient(Amount.of(1, Time.SECONDS), addr),
                zkLocator);
        ClientBuilder builder = ClientBuilder
                .get()
                .codec(Http.get())
                .hostConnectionLimit(1)
                .cluster(new ZookeeperServerSetCluster(serverSet));
        return withClientBuilder(builder);
    }

    public ResteasyClientBuilder withClientBuilder(ClientBuilder clientBuilder) {
        this.clientBuilder = clientBuilder;
        return this;
    }

    public ResteasyClientBuilder withProviderFactory(
            ResteasyProviderFactory providerFactory) {
        this.providerFactory = providerFactory;
        return this;
    }

    public <T> T build(Class<T> serviceInterface) {
        Preconditions.checkNotNull(this.clientBuilder,
                "no client builder provided");
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
