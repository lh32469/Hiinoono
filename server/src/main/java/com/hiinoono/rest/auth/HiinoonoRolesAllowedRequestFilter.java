package com.hiinoono.rest.auth;

import java.io.IOException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;


/**
 * Checks the HiinoonoRolesAllowed on the Method being called against the
 * current Roles of the User and throws a Status.FORBIDDEN
 * WebApplicationException which includes the message value from the
 * HiinoonoRolesAllowed annotation for the Method being called.
 *
 * @author Lyle T Harris
 */
public class HiinoonoRolesAllowedRequestFilter implements
        ContainerRequestFilter {

    private final String[] rolesAllowed;

    private final String message;


    HiinoonoRolesAllowedRequestFilter(HiinoonoRolesAllowed ra) {
        this.rolesAllowed = ra.roles();
        this.message = ra.message();
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
