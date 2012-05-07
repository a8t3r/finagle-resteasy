package opower.finagle.resteasy;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import com.twitter.finagle.Service;

import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.core.SynchronousDispatcher;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.util.GetRestful;

import org.springframework.beans.factory.ListableBeanFactory;

import javax.ws.rs.core.MediaType;
import java.util.Map;

/**
 * Builder for a Finagle {@link com.twitter.finagle.Service} that knows how to dispatch to a RestEASY
 * {@link org.jboss.resteasy.core.Dispatcher}.
 *
 * TODO are there any other aspects of RestEASY we want to be able to customize?
 *
 * @author ed.peters
 */
public class RestEasyServiceBuilder {

    /**
     * Default set of file-extension-to-MIME-type mappings
     */
    public static final Map<String,MediaType> DEFAULT_MEDIA_TYPES = ImmutableMap.of(
            "json", MediaType.APPLICATION_JSON_TYPE,
            "bin", MediaType.APPLICATION_OCTET_STREAM_TYPE,
            "html", MediaType.TEXT_HTML_TYPE,
            "xml", MediaType.TEXT_XML_TYPE
    );

    private Dispatcher dispatcher;
    private ResteasyProviderFactory providerFactory;
    private Map<String,MediaType> mediaTypes;

    public RestEasyServiceBuilder() {
        this.providerFactory = ResteasyProviderFactory.getInstance();
        this.dispatcher = new SynchronousDispatcher(this.providerFactory);
        this.mediaTypes = Maps.newHashMap(DEFAULT_MEDIA_TYPES);
    }

    /**
     * Adds a REST-annotated bean to the dispatcher for this service
     */
    public RestEasyServiceBuilder withEndpoint(Object bean) {
        this.dispatcher.getRegistry().addSingletonResource(bean);
        return this;
    }

    /**
     * Adds all REST-annotated beans from the supplied bean factory to the dispatcher for this service
     */
    public RestEasyServiceBuilder withEndpointsFromBeanFactory(ListableBeanFactory beanFactory) {
        for (String beanName : beanFactory.getBeanDefinitionNames()) {
            Object bean = beanFactory.getBean(beanName);
            if (GetRestful.isRootResource(bean.getClass())) {
                withEndpoint(bean);
            }
        }
        return this;
    }

    /**
     * Adds a new file-extension-to-MIME-type mapping
     */
    public RestEasyServiceBuilder withMediaType(String ext, MediaType mediaType) {
        this.mediaTypes.put(ext, mediaType);
        return this;
    }

    /**
     * Creates the service.
     */
    public Service<HttpRequest,HttpResponse> build() {
        this.dispatcher.setMediaTypeMappings(this.mediaTypes);
        return new RestEasyFinagleService(this.dispatcher);
    }

}
