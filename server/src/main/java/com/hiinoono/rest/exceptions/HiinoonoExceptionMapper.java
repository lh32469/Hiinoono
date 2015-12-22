package com.hiinoono.rest.exceptions;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import org.slf4j.LoggerFactory;


/**
 * Currently placeholder for future capability of handling custom Exceptions.
 *
 * @author Lyle T Harris
 */
@Provider
public class HiinoonoExceptionMapper implements
        ExceptionMapper<HiinoonoException> {

    final static private org.slf4j.Logger LOG
            = LoggerFactory.getLogger(HiinoonoExceptionMapper.class);


    @Override
    public Response toResponse(HiinoonoException ex) {

        ex.printStackTrace(System.err);
        LOG.error(ex.getMessage(), ex);

        return Response.serverError().entity(ex.getMessage()).build();
    }


}
