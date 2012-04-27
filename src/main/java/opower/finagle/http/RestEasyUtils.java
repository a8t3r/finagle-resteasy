package opower.finagle.http;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.twitter.finagle.Service;
import com.twitter.util.Future;

import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.core.SynchronousDispatcher;
import org.jboss.resteasy.specimpl.HttpHeadersImpl;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.jboss.resteasy.specimpl.PathSegmentImpl;
import org.jboss.resteasy.specimpl.UriBuilderImpl;
import org.jboss.resteasy.specimpl.UriInfoImpl;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import javax.annotation.Nullable;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Helpers for dealing with the annoyances of converting between Netty and JAX-RS structures.
 *
 * @author ed.peters
 */
public final class RestEasyUtils {

    /** HTTP header: content type negotiation */
    public static final String ACCEPT_HEADER = "accept";

    /** HTTP header: language negotiation */
    public static final String ACCEPT_LANGUAGE_HEADER = "accept-language";

    /** HTTP header: content type output */
    public static final String CONTENT_TYPE_HEADER = "content-type";

    /** Converts Strings to MediaTypes */
    public static final Function<String,MediaType> TO_MEDIA_TYPE = new Function<String, MediaType>() {
        @Override
        public MediaType apply(@Nullable String input) {
            return input == null ? null : MediaType.valueOf(input);
        }
    };

    private RestEasyUtils() {

    }

    /**
     * Converts a request URI into a {@link UriInfo}.  This was ripped off from JBoss code that does the
     * same thing starting with a {@link javax.servlet.http.HttpServletRequest}.  This code is simpler
     * because we don't have to deal with the difference between the "context path" and the "servlet prefix".
     *
     * @see org.jboss.resteasy.plugins.server.servlet.ServletUtil#extractUriInfo(javax.servlet.http.HttpServletRequest,String)
     * @return a {@link UriInfo} object for the specified URI
     */
    public static UriInfo extractUriInfo(String uri) {

        String [] split = uri.split("\\?", 2);
        String path = split[0];
        String query = null;
        if (split.length > 1) {
            query = split[1];
        }

        URI absolutePath = new UriBuilderImpl()
                .scheme("http")
                .host("localhost")
                .port(80)
                .path(path)
                .replaceQuery(query)
                .build();

        String rawPath = absolutePath.getRawPath();
        List<PathSegment> pathSegments = PathSegmentImpl.parseSegments(rawPath);

        URI baseURI = absolutePath;
        if (!rawPath.trim().equals("")) {
            String tmpContextPath = "";
            if (!tmpContextPath.endsWith("/")) {
                tmpContextPath += "/";
            }
            baseURI = UriBuilder.fromUri(absolutePath).replacePath(tmpContextPath).build();
        }

        return new UriInfoImpl(absolutePath, baseURI, rawPath, query, pathSegments);
    }

    /**
     * @return the contents of the HTTP headers of the supplied request, as a {@link MultivaluedMap}
     */
    public static MultivaluedMap<String,String> toMultimap(HttpRequest finagleRequest) {
        MultivaluedMap<String,String> map = new MultivaluedMapImpl<String, String>();
        for (String key : finagleRequest.getHeaderNames()) {
            map.put(key.toLowerCase(), finagleRequest.getHeaders(key));
        }
        return map;
    }

    /**
     * @return the contents of the supplied map, as {@link HttpHeaders} (this is more than a
     * simple copy, because JAX-RS special-cases certain headers)
     */
    public static HttpHeaders toHeaders(MultivaluedMap<String,String> map) {
        HttpHeadersImpl impl = new HttpHeadersImpl();
        impl.setRequestHeaders(map);
        impl.setAcceptableLanguages(map.get(ACCEPT_LANGUAGE_HEADER));
        impl.setMediaType(TO_MEDIA_TYPE.apply(map.getFirst(CONTENT_TYPE_HEADER)));
        if (map.containsKey(ACCEPT_HEADER)) {
            // defensive copy because resteasy will modify this list if we're using filename extensions
            List<MediaType> mediaTypes = Lists.transform(map.get(ACCEPT_HEADER), TO_MEDIA_TYPE);
            mediaTypes = Lists.newArrayList(mediaTypes);
            impl.setAcceptableMediaTypes(mediaTypes);
        }
        return impl;
    }

    /**
     * Implements the {@link MultivaluedMap} API on top of a Finagle response object
     */
    public static class HeaderWrapper implements MultivaluedMap<String,Object> {

        private final HttpResponse finagleResponse;

        public HeaderWrapper(HttpResponse finagleResponse) {
            this.finagleResponse = finagleResponse;
        }

        @Override
        public void add(String key, Object value) {
            finagleResponse.addHeader(key, value);
        }

        @Override
        public void putSingle(String key, Object value) {
            finagleResponse.setHeader(key, value);
        }

        @Override
        public Object getFirst(String key) {
            return finagleResponse.getHeader(key);
        }

        @Override
        public int size() {
            return finagleResponse.getHeaderNames().size();
        }

        @Override
        public boolean isEmpty() {
            return size() > 0;
        }

        @Override
        public boolean containsKey(Object key) {
            return finagleResponse.containsHeader(key.toString());
        }

        @Override
        public boolean containsValue(Object value) {
            throw new UnsupportedOperationException("containsValue");
        }

        @Override
        @SuppressWarnings("unchecked")
        public List<Object> get(Object key) {
            // OK because we're returning a List
            return (List) finagleResponse.getHeaders(key.toString());
        }

        @Override
        public List<Object> put(String key, List<Object> values) {
            finagleResponse.setHeader(key, values);
            return null;
        }

        @Override
        public List<Object> remove(Object key) {
            finagleResponse.removeHeader(key.toString());
            return null;
        }

        @Override
        public void putAll(Map<? extends String, ? extends List<Object>> map) {
            for (String key : map.keySet()) {
                put(key, map.get(key));
            }
        }

        @Override
        public void clear() {
            finagleResponse.clearHeaders();
        }

        @Override
        public Set<String> keySet() {
            return finagleResponse.getHeaderNames();
        }

        @Override
        public Collection<List<Object>> values() {
            List<List<Object>> all = Lists.newArrayList();
            for (String key : keySet()) {
                all.add(get(key));
            }
            return all;
        }

        @Override
        public Set<Entry<String,List<Object>>> entrySet() {
            Map<String,List<Object>> map = Maps.newHashMap();
            for (String key : keySet()) {
                map.put(key, get(key));
            }
            return map.entrySet();
        }
    }

    /**
     * @return a new RestEASY {@link Dispatcher}
     */
    public static Dispatcher createDispatcher() {
        ResteasyProviderFactory providerFactory = ResteasyProviderFactory.getInstance();
        Dispatcher dispatcher = new SynchronousDispatcher(providerFactory);
        // not strictly necessary unless a resource supports multiple output types,
        // but it's definitely more convenient than using "Accept" headers
        dispatcher.setMediaTypeMappings(ImmutableMap.of(
                "json", MediaType.APPLICATION_JSON_TYPE
        ));
        return dispatcher;
    }

    /**
     * TODO better error handling -- log the exception, diagnose it and return an appropriate HTTP error
     * TODO move the call to invoke into the "get" method of the returned Future?
     * @return a Finagle Service that serves requests to the supplied {@link Dispatcher}
     */
    public static Service<HttpRequest,HttpResponse> createDispatcherService(final Dispatcher dispatcher) {

        return new Service<HttpRequest,HttpResponse>() {
            public Future<HttpResponse> apply(HttpRequest request) {

                RestEasyRequest restEasyRequest = new RestEasyRequest(request);
                RestEasyResponse restEasyResponse = new RestEasyResponse(request);
                HttpResponse finagleResponse = null;
                try {
                    dispatcher.invoke(restEasyRequest, restEasyResponse);
                    finagleResponse = restEasyResponse.getFinagleResponse();
                }
                catch (Exception e) {
                    e.printStackTrace();
                    finagleResponse = new DefaultHttpResponse(
                            request.getProtocolVersion(),
                            new HttpResponseStatus(500, e.toString()));
                }
                return Future.value(finagleResponse);

            }
        };

    }

}
