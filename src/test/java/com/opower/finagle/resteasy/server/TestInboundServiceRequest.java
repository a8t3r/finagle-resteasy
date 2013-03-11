package com.opower.finagle.resteasy.server;

import com.google.common.collect.ImmutableMap;
import com.google.common.net.HttpHeaders;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.resteasy.spi.HttpRequest;
import org.junit.Test;

import java.net.URI;
import java.util.Arrays;

import static com.opower.finagle.resteasy.AssertionHelpers.assertMultivaluedMapEquals;
import static com.opower.finagle.resteasy.AssertionHelpers.assertUriInfoEquals;
import static org.jboss.netty.handler.codec.http.HttpMethod.POST;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static org.jboss.netty.handler.codec.http.HttpMethod.GET;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import java.util.List;
import javax.ws.rs.core.MediaType;

/**
 * Tests for the mapping of Netty requests to Resteasy.
 *
 * @author ed.peters
 */
public class TestInboundServiceRequest {

    public static final byte [] ENCODED_PARAMS = "k1=v1&k1=v2&k2=%3F".getBytes();

    @Test
    public void testRequestAttributes() throws Exception {
        DefaultHttpRequest nettyRequest = newRequest(GET, "/foo");
        HttpRequest resteasyRequest = new InboundServiceRequest(nettyRequest);
        assertNull("unexpected attribute", resteasyRequest.getAttribute("k"));
        resteasyRequest.setAttribute("k", this);
        assertSame("wrong attribute", this, resteasyRequest.getAttribute("k"));
        resteasyRequest.removeAttribute("k");
        assertNull("unexpected attribute", resteasyRequest.getAttribute("k"));
    }

    @Test
    public void testRequestHeaders() throws Exception {
        DefaultHttpRequest nettyRequest = newRequest(GET, "/foo");
        nettyRequest.setHeader("single", "a");
        nettyRequest.setHeader("multi", Arrays.asList("a", "b", "c"));
        HttpRequest resteasyRequest = new InboundServiceRequest(nettyRequest);
        assertMultivaluedMapEquals(
            resteasyRequest.getHttpHeaders().getRequestHeaders(),
            ImmutableMap.<String,Object>of(
                "single", "a",
                "multi", Arrays.asList("a", "b", "c")
            ));
    }

    @Test
    public void testRequestHeadersWithMultipleAccepts() throws Exception {
        DefaultHttpRequest nettyRequest = newRequest(GET, "/foo");
        nettyRequest.setHeader(HttpHeaders.ACCEPT, 
                MediaType.APPLICATION_XML + "," + MediaType.APPLICATION_JSON);
        HttpRequest resteasyRequest = new InboundServiceRequest(nettyRequest);
        List<MediaType> mediaTypes = 
            resteasyRequest.getHttpHeaders().getAcceptableMediaTypes();
        assertEquals(2, mediaTypes.size());
        assertEquals(MediaType.APPLICATION_XML_TYPE, mediaTypes.get(0));
        assertEquals(MediaType.APPLICATION_JSON_TYPE, mediaTypes.get(1));
    }

    @Test
    public void testFormParameters() throws Exception {
        DefaultHttpRequest nettyRequest = newRequest(POST, "/foo");
        nettyRequest.setContent(ChannelBuffers.wrappedBuffer(ENCODED_PARAMS));
        HttpRequest resteasyRequest = new InboundServiceRequest(nettyRequest);
        assertMultivaluedMapEquals(resteasyRequest.getFormParameters(),
                ImmutableMap.<String,Object>of(
                        "k1", Arrays.asList("v1", "v2"),
                        "k2", "%3F"
                ));
        assertMultivaluedMapEquals(resteasyRequest.getDecodedFormParameters(),
                ImmutableMap.<String,Object>of(
                        "k1", Arrays.asList("v1", "v2"),
                        "k2", "?"
                ));
    }

    @Test
    public void testGetUriInfo() throws Exception {
        DefaultHttpRequest nettyRequest = newRequest(GET, "/foo?k=v&k=%3F");
        HttpRequest resteasyRequest = new InboundServiceRequest(nettyRequest);
        assertUriInfoEquals(resteasyRequest.getUri(),
                new URI("http://localhost:80/foo?k=v&k=%3F"),
                "/foo",
                ImmutableMap.<String,Object>of("k", Arrays.asList("v", "?")),
                new String[]{"foo"});
    }

    private DefaultHttpRequest newRequest(HttpMethod method, String uri) {
        return new DefaultHttpRequest(HTTP_1_1, method, uri);
    }

}
