package opower.finagle.resteasy;

import com.twitter.common.quantity.Amount;
import com.twitter.common.zookeeper.ServerSet;
import com.twitter.common.zookeeper.ServerSetImpl;
import com.twitter.common.zookeeper.ZooKeeperClient;
import com.twitter.finagle.Service;
import com.twitter.finagle.builder.ClientBuilder;
import com.twitter.finagle.builder.Cluster;
import com.twitter.finagle.http.Http;
import com.twitter.finagle.zookeeper.ZookeeperServerSetCluster;

import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;

import java.net.InetSocketAddress;

import static com.twitter.common.quantity.Time.SECONDS;

/**
 * FactoryBean implementation that knows how to create a Finagle/RestEASY client proxy using
 * the correct provider factory for the enclosing bean context.
 *
 * @see opower.rest.support.RestEasyProxyFactory
 * @param <T> the type of the intreface being proxied (ie, the service being used)
 * @author jeff (with original attribution to ed.peters)
 */
public class RestEasyFinagleClientProxyFactory<T> implements FactoryBean<T>, InitializingBean {
    private Class<T> proxyInterface;
    private String hostname;
    private int port;
    private String zkHostname;
    private int zkPort;
    private String zkServiceLocator;
    private T proxy;
    private boolean useCluster;
    private boolean useDirect;

    @Required
    public void setProxyInterface(Class<T> proxyInterface) {
        this.proxyInterface = proxyInterface;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setZkHostname(String zkHostname) {
        this.zkHostname = zkHostname;
    }

    public void setZkPort(int zkPort) {
        this.zkPort = zkPort;
    }

    public void setZkServiceLocator(String zkServiceLocator) {
        this.zkServiceLocator = zkServiceLocator;
    }

    @Override
    public void afterPropertiesSet() {
        this.useCluster = (this.zkHostname != null && this.zkPort != 0 && this.zkServiceLocator != null);
        this.useDirect = (this.hostname != null && this.port != 0);
        if (this.useCluster && this.useDirect) {
            throw new IllegalStateException("Configure either direct connections or ZooKeeper clusters, not both");
        }
        if (!this.useCluster && !this.useDirect) {
            throw new IllegalStateException("Neither direct connections nor ZooKeeper clusters configured");
        }
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
            ClientBuilder clientBuilder = ClientBuilder.get()
                .codec(Http.get())
                .hostConnectionLimit(1);
            if (this.useDirect) {
                clientBuilder = clientBuilder.hosts(new InetSocketAddress(this.hostname, this.port));
            }
            else if (this.useCluster) {
                ZooKeeperClient zkClient = new ZooKeeperClient(Amount.of(1, SECONDS),
                        new InetSocketAddress(this.zkHostname, this.zkPort));
                ServerSet serverSet = new ServerSetImpl(zkClient, this.zkServiceLocator);
                Cluster cluster = new ZookeeperServerSetCluster(serverSet);
                clientBuilder = clientBuilder.cluster(cluster);
            }
            else {
                throw new IllegalStateException("Fail like a big fat mo-fo");
            }

            Service<HttpRequest,HttpResponse> httpClient = ClientBuilder.safeBuild(clientBuilder);
            this.proxy = new RestEasyClientBuilder<T>(this.proxyInterface)
                .withService(httpClient)
                .build();
        }
        return this.proxy;
    }
}
