package com.hiinoono.rest.exceptions;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import org.slf4j.LoggerFactory;


/**
 * Map Exceptions returned to Client in more succinct format.
 *
 * @author Lyle T Harris
 */
@Provider
public class DefaultExceptionMapper implements ExceptionMapper<Throwable> {

    final static private org.slf4j.Logger LOG
            = LoggerFactory.getLogger(DefaultExceptionMapper.class);


    @Override
    public Response toResponse(Throwable ex) {

        ex.printStackTrace(System.err);
        LOG.error(ex.getMessage(), ex);

        javax.ws.rs.ForbiddenException ex3;

        return Response.serverError().entity(ex.getMessage()).build();
    }


}
