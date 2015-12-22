package com.hiinoono.rest.auth;

import java.io.IOException;
import java.util.logging.Logger;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;


/**
 *
 * @author Lyle T Harris
 */
public class HiinoonoRolesAllowedRequestFilter implements ContainerRequestFilter {

    private final String[] rolesAllowed;

    private final String message;


    HiinoonoRolesAllowedRequestFilter(HiinoonoRolesAllowed ra) {
        String[] values = ra.roles();
        for (int i = 0; i < values.length; i++) {
            String value = values[i];
            System.out.println("HiinoonoRolesAllowedRequestFilter  Value: " + value);
        }

        this.rolesAllowed = ra.roles();
        this.message = ra.message();
        System.out.println("HiinoonoRolesAllowedRequestFilter: " + ra.message());
    }


    @Override
    public void filter(ContainerRequestContext ctxt) throws IOException {

        SecurityContext sc = ctxt.getSecurityContext();

        for (final String role : rolesAllowed) {
            if (sc.isUserInRole(role)) {
                return;
            }
        }

        throw new WebApplicationException(Response
                .status(Response.Status.FORBIDDEN)
                .entity(message + "\n")
                .build());

    }


}
