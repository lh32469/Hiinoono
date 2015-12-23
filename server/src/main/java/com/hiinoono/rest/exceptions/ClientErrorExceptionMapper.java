package com.hiinoono.rest.exceptions;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import org.slf4j.LoggerFactory;


/**
 * ExceptionMapper that takes the message from the ClientErrorException and
 * includes it as the body/entity of the Response to the Client.
 *
 * @author Lyle T Harris
 */
@Provider
public class ClientErrorExceptionMapper
        implements ExceptionMapper<ClientErrorException> {

    final static private org.slf4j.Logger LOG
            = LoggerFactory.getLogger(ClientErrorExceptionMapper.class);


    @Override
    public Response toResponse(ClientErrorException ex) {
        LOG.error(ex.getMessage(), ex);

        ResponseBuilder bldr = Response.status(ex.getResponse().getStatus());
        bldr.entity(ex.getMessage() + "\n");

        return bldr.build();
    }


}
