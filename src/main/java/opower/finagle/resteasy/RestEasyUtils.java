package opower.finagle.resteasy;

import com.google.common.collect.Lists;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpMessage;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;
import org.jboss.resteasy.client.core.BaseClientResponse;
import org.jboss.resteasy.specimpl.HttpHeadersImpl;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.jboss.resteasy.specimpl.PathSegmentImpl;
import org.jboss.resteasy.specimpl.UriBuilderImpl;
import org.jboss.resteasy.specimpl.UriInfoImpl;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

/**
 * Helpers for dealing with the annoyances of converting between Netty and JAX-RS structures.
 *
 * @author ed.peters
 */
public final class RestEasyUtils {

    /**
     * @return the headers from the supplied Netty message, as a JAX-RS {@link MultivaluedMap}
     */
    public static MultivaluedMap<String,String> getHeadersAsMultimap(HttpMessage message) {
        MultivaluedMap<String,String> map = new MultivaluedMapImpl<String,String>();
        for (String name : message.getHeaderNames()) {
            map.put(name, message.getHeaders(name));
        }
        return map;
    }

    /**
     * @return the contents of the supplied map, as JAX-RS {@link javax.ws.rs.core.HttpHeaders}
     * (this is more than a simple copy, because JAX-RS special-cases certain headers)
     */
    public static HttpHeaders toHeaders(MultivaluedMap<String,String> map) {
        HttpHeadersImpl impl = new HttpHeadersImpl();
        impl.setRequestHeaders(map);
        impl.setAcceptableLanguages(map.get(NettyRequestWrapper.ACCEPT_LANGUAGE_HEADER));
        impl.setMediaType(NettyRequestWrapper.TO_MEDIA_TYPE.apply(map.getFirst(NettyRequestWrapper.CONTENT_TYPE_HEADER)));
        if (map.containsKey(NettyRequestWrapper.ACCEPT_HEADER)) {
            // defensive copy because resteasy will modify this list if we're using filename extensions
            List<MediaType> mediaTypes = Lists.transform(map.get(NettyRequestWrapper.ACCEPT_HEADER), NettyRequestWrapper.TO_MEDIA_TYPE);
            mediaTypes = Lists.newArrayList(mediaTypes);
            impl.setAcceptableMediaTypes(mediaTypes);
        }
        else {
            // need to supply an empty list here or else we'll get an NPE (*shakes fist at JBoss*)
            impl.setAcceptableMediaTypes(Lists.<MediaType>newArrayList());
        }
        return impl;
    }

    /**
     * Converts a request URI into a {@link javax.ws.rs.core.UriInfo}.  This was ripped off
     * from JBoss code that does the same thing starting with a {@link javax.servlet.http.HttpServletRequest}.
     * This code is simpler because we don't have to deal with the difference between the
     * "context path" and the "servlet prefix".
     *
     * @see org.jboss.resteasy.plugins.server.servlet.ServletUtil#extractUriInfo()
     * @return a {@link javax.ws.rs.core.UriInfo} object for the specified URI
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
     * Since we're going to dispatch this call to a Finagle service, we want to make sure the
     * URI is relative, so we strip off any hostname that appears.
     */
    public static String stripProtocolAndHost(String uri) {
        int idx = uri.indexOf("://");
        if (idx >= 0) {
            return uri.substring(uri.indexOf("/", idx+3));
        }
        return uri;
    }

    /**
     * Converts the supplied request from RestEASY to Netty.  There is a bit of intricacy here
     * because we want to let RestEASY handle the actual rendering of the content to bytes,
     * which may involve setting some headers.
     *
     * @return the converted request
     * @throws Exception if there's an error getting the URI from RestEASY
     */
    @SuppressWarnings("unchecked")
    public static HttpRequest convertToNettyRequest(ClientRequest clientRequest) throws Exception {

        HttpVersion version = HttpVersion.HTTP_1_1;
        HttpMethod method = HttpMethod.valueOf(clientRequest.getHttpMethod());
        String uri = stripProtocolAndHost(clientRequest.getUri());

        // making a copy of the headers that were passed in, in case they're immutable
        // (the cast is safe: we're converting Multimap<String,String> to Multimap<String,Object>)
        MultivaluedMap<String,Object> newHeaders = new MultivaluedMapImpl<String,Object>();
        newHeaders.putAll((MultivaluedMap)clientRequest.getHeaders());

        // we'll make extra-sure to get the content type in there
        if (clientRequest.getBodyContentType() != null) {
            newHeaders.putSingle("Content-Type", clientRequest.getBodyContentType().toString());
        }

        ByteArrayOutputStream bodyBytes = new ByteArrayOutputStream();
        clientRequest.writeRequestBody(newHeaders, bodyBytes);

        HttpRequest httpRequest = new DefaultHttpRequest(version, method, uri);
        for (String name : newHeaders.keySet()) {
            httpRequest.setHeader(name, newHeaders.get(name));
        }
        if (bodyBytes.size() > 0) {
            httpRequest.setHeader("Content-Length", Integer.toString(bodyBytes.size()));
            httpRequest.setContent(ChannelBuffers.wrappedBuffer(bodyBytes.toByteArray()));
        }
        else {
            httpRequest.setHeader("Content-Length", "0");
        }

        return httpRequest;

    }

    /**
     * Converts the supplied response from Netty back into RestEASY.  This is straightforward,
     * but cumbersome because of the extra helper class involved.
     *
     * @return the converted response
     */
    public static ClientResponse<?> convertToClientResponse(
            final HttpResponse nettyResponse,
            ResteasyProviderFactory providerFactory) {
        final InputStream nettyStream = new ChannelBufferInputStream(nettyResponse.getContent());
        BaseClientResponse<?> clientResponse = new BaseClientResponse<Object>(new BaseClientResponse.BaseClientResponseStreamFactory() {
            @Override public InputStream getInputStream() throws IOException {
                return nettyStream;
            }
            @Override public void performReleaseConnection() {
            }
        });
        clientResponse.setStatus(nettyResponse.getStatus().getCode());
        clientResponse.setHeaders(getHeadersAsMultimap(nettyResponse));
        clientResponse.setProviderFactory(providerFactory);
        return clientResponse;
    }

}
