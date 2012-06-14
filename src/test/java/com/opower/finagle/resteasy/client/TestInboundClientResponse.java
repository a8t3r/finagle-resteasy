package com.opower.finagle.resteasy.client;

import com.google.common.collect.ImmutableMap;
import com.opower.finagle.resteasy.util.ServiceUtils;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.resteasy.client.ClientResponse;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.UUID;

import static com.opower.finagle.resteasy.AssertionHelpers.assertMultivaluedMapEquals;
import static org.junit.Assert.assertEquals;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_0;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.NOT_ACCEPTABLE;
import static org.junit.Assert.assertNotNull;

/**
 * Tests Netty-to-Resteasy translation
 *
 * @author ed.peters
 */
public class TestInboundClientResponse {

    public static final byte [] ZERO_BYTES = new byte[0];

    private ResteasyProviderFactory providerFactory;

    @Before
    public void setUp() throws Exception {
        this.providerFactory = ServiceUtils.getDefaultProviderFactory();
    }

    @Test
    public void testWithCustomHeaders() throws Exception {

        HttpResponse nettyResponse = new DefaultHttpResponse(HTTP_1_0, OK);
        nettyResponse.setHeader("single", "a");
        nettyResponse.setHeader("multi", Arrays.asList("a", "b"));

        ClientResponse resteasyResponse = translate(nettyResponse);
        assertStatus(resteasyResponse, Response.Status.OK);
        assertMultivaluedMapEquals(resteasyResponse.getHeaders(),
                ImmutableMap.<String, Object>of(
                        "single", "a",
                        "multi", Arrays.asList("a", "b")
                ));
        assertContent(resteasyResponse, ZERO_BYTES);
    }

    @Test
    public void testWithErrorMessage() throws Exception {

        HttpResponse nettyResponse = new DefaultHttpResponse(HTTP_1_0,
                NOT_ACCEPTABLE);

        ClientResponse resteasyResponse = translate(nettyResponse);
        assertStatus(resteasyResponse, Response.Status.NOT_ACCEPTABLE);
        assertContent(resteasyResponse, ZERO_BYTES);
    }

    @Test
    public void testWithContent() throws Exception {

        byte [] expectedBytes = UUID.randomUUID().toString().getBytes();
        HttpResponse nettyResponse = new DefaultHttpResponse(HTTP_1_0, OK);
        nettyResponse.setContent(ChannelBuffers.wrappedBuffer(expectedBytes));

        ClientResponse resteasyResponse = translate(nettyResponse);
        assertStatus(resteasyResponse, Response.Status.OK);
        assertContent(resteasyResponse, expectedBytes);
    }

    private ClientResponse translate(HttpResponse nettyResponse) {
        return new InboundClientResponse(nettyResponse, this.providerFactory);
    }

    private void assertStatus(ClientResponse response, Response.Status expected) {
        assertEquals("incorrect status",
                expected,
                response.getResponseStatus());
    }

    private void assertContent(ClientResponse response, byte [] expectedBytes) {
        byte [] actualBytes = (byte[]) response.getEntity(byte[].class);
        assertNotNull("content was null", actualBytes);
        assertEquals("content mismatch",
                Arrays.toString(expectedBytes),
                Arrays.toString(actualBytes));
    }
}
