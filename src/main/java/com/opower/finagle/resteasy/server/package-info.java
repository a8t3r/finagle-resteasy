/**
 * Classes to support hosting Resteasy-annotated service implementation via
 * Finagle.  The conversion of the Netty request to and from Resteasy works
 * as follows:
 * <ol>
 *
 *     <li>The actual service that gets registered with Finagle is an
 *     instance of {@link ResteasyFinagleService};</li>
 *
 *     <li>Incoming requests are converted from Netty to JBoss via an
 *     {@link InboundServiceRequest}; and,</li>
 *
 *     <li>The outbound response is either a regular
 *     {@link OutboundServiceResponse} or an {@link UnhandledErrorResponse}
 *     if something goes badly wrong.</li>
 *
 * </ol>
 */
package com.opower.finagle.resteasy.server;