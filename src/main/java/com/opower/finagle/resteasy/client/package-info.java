/**
 * Classes to support invocation of remote services using a Resteasy proxy
 * on top of Finagle.  The conversion of the Resteasy request to and from Netty
 * works as follows:
 * <ol>
 *
 *     <li>The {@link FinagleBasedClientExecutor} is invoked from inside
 *     Resteasy when the call is made;</li>
 *
 *     <li>It converts the outbound message from Resteasy to Netty
 *     format using an {@link OutboundClientRequest}; and,</li>
 *
 *     <li>It converts the inbound response from Netty back into
 *     Resteasy format using an {@link InboundClientResponse}.</li>
 *
 * </ol>
 *
 */
package com.opower.finagle.resteasy.client;