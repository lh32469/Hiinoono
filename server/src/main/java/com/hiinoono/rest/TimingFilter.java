package com.hiinoono.rest;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import org.slf4j.LoggerFactory;


/**
 * Adds "Total-request-time" to the response header to indicate the total
 * processing time of the request.
 *
 * @author Lyle T Harris
 */
public class TimingFilter implements
        ContainerRequestFilter, ContainerResponseFilter {

    private static final String START_TIME = "start-time";

    public static final String PROCESSING_TIME = "X-Total-request-time";

    final static private org.slf4j.Logger LOG
            = LoggerFactory.getLogger(TimingFilter.class);


    @Override
    public void filter(ContainerRequestContext context) {
        // LOG.info(context.getHeaders().toString());
        context.getHeaders().add(START_TIME, "" + System.currentTimeMillis());
    }


    @Override
    public void filter(
            ContainerRequestContext requestContext,
            ContainerResponseContext responseContext) {

        MultivaluedMap<String, String> requestHeaders
                = requestContext.getHeaders();

        MultivaluedMap<String, Object> responseHeaders
                = responseContext.getHeaders();
//
//        LOG.debug("Request Headers: "
//                + requestHeaders.toString());
//
//        LOG.info("Response Headers: "
//                + responseHeaders.toString());

        String start = requestHeaders.getFirst(START_TIME);

        if (start != null) {
            long duration = System.currentTimeMillis() - Long.parseLong(start);
            responseHeaders.add(PROCESSING_TIME, duration + "ms");
        }

    }


}
