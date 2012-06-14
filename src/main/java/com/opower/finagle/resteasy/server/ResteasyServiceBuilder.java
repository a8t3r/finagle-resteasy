package com.opower.finagle.resteasy.server;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.opower.finagle.resteasy.util.ServiceUtils;
import com.twitter.finagle.Service;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.core.SynchronousDispatcher;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.util.GetRestful;

import javax.ws.rs.core.MediaType;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Builder for a Finagle {@link com.twitter.finagle.Service} that knows how to
 * dispatch to a RestEASY {@link org.jboss.resteasy.core.Dispatcher}.  Also
 * knows how to pull service beans from a Spring bean factory.
 *
 * TODO what other aspects of RestEASY do we want to be able to customize?
 *
 * @author ed.peters
 */
public class ResteasyServiceBuilder {

    /**
     * Default set of file-extension-to-MIME-type mappings
     */
    public static final Map<String,MediaType> DEFAULT_MEDIA_TYPES = ImmutableMap.of(
        "json", MediaType.APPLICATION_JSON_TYPE,
        "bin", MediaType.APPLICATION_OCTET_STREAM_TYPE,
        "html", MediaType.TEXT_HTML_TYPE,
        "xml", MediaType.TEXT_XML_TYPE
    );

    private ResteasyProviderFactory providerFactory;
    private Map<String,MediaType> mediaTypes;
    private Map<String,String> languages;
    private List<Object> beans;

    protected ResteasyServiceBuilder() {
        this.mediaTypes = Maps.newHashMap(DEFAULT_MEDIA_TYPES);
        this.languages = Maps.newHashMap();
        this.beans = Lists.newArrayList();
    }

    /**
     * Adds a REST-annotated bean to the dispatcher for this service
     * @param bean a service bean
     * @return this (for chaining)
     * @throws NullPointerException if the supplied bean is null
     * @throws IllegalArgumentException if the supplied bean isn't a root
     * resource, as reported by {@link GetRestful}
     */
    public ResteasyServiceBuilder withEndpoint(Object bean) {
        Preconditions.checkNotNull(bean, "null endpoints not allowed");
        Preconditions.checkArgument(
            GetRestful.isRootResource(bean.getClass()),
            "endpoint %s is not a root resource",
            bean
        );
        this.beans.add(bean);
        return this;
    }

    /**
     * Same as calling {@link #withEndpoint(Object)} on each bean in
     * the supplied collection
     */
    public ResteasyServiceBuilder withEndpoints(Collection<?> beans) {
        for (Object bean : beans) {
            withEndpoint(bean);
        }
        return this;
    }

    /**
     * Adds a new file-extension-to-MIME-type mapping to the default set
     * for this service
     * @param ext a file extension without the dot (e.g. "xml")
     * @param mediaType the corresponding "Accept" header to infer for
     *                  requests ending with that file extension
     */
    public ResteasyServiceBuilder withMediaTypeMapping(String ext,
                                                       MediaType mediaType) {
        this.mediaTypes.put(ext, mediaType);
        return this;
    }

    /**
     * Adds a new file-extension-to-language mapping to the default set
     * for this service
     * @param ext a file extension without the dot (e.g. "en")
     * @param lang the corresponding "Accept-Language" header to infer for
     *             requests ending with that extension (e.g. "en")
     */
    public ResteasyServiceBuilder withLanguageMapping(String ext, String lang) {
        this.languages.put(ext, lang);
        return this;
    }

    /**
     * @return a new service
     */
    public Service<HttpRequest,HttpResponse> build() {
        if (this.providerFactory == null) {
            this.providerFactory = ServiceUtils.getDefaultProviderFactory();
        }
        Dispatcher dispatcher = new SynchronousDispatcher(this.providerFactory);
        dispatcher.setMediaTypeMappings(this.mediaTypes);
        for (Object bean : this.beans) {
            dispatcher.getRegistry().addSingletonResource(bean);
        }
        if (this.languages.size() > 0) {
            dispatcher.setLanguageMappings(this.languages);
        }
        return new ResteasyFinagleService(dispatcher);
    }

    public static ResteasyServiceBuilder get() {
        return new ResteasyServiceBuilder();
    }

}
