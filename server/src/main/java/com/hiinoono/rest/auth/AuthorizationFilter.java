package com.hiinoono.rest.auth;

import com.hiinoono.jaxb.User;
import com.hiinoono.persistence.PersistenceManager;
import java.io.IOException;
import java.util.Base64;
import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.Priorities;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.server.ContainerRequest;


/**
 *
 * @author Lyle T Harris
 */
@Priority(Priorities.AUTHENTICATION - 100)
public class AuthorizationFilter implements ContainerRequestFilter {

    private static final String BAD_CREDENTIALS = "Incorrect credentials\n";

    @Inject
    private PersistenceManager pm;


    @Override
    public void filter(ContainerRequestContext ctxt) throws IOException {

        String path = ctxt.getUriInfo().getPath();

        if (path.startsWith("application.wadl")) {
            // Free access to WADL and schemas
            return;
        }

        String authorization
                = ctxt.getHeaderString(ContainerRequest.AUTHORIZATION);

        if (authorization == null) {
            // No authorization.
            abort(ctxt);
            return;
        }

        authorization = authorization.substring("Basic ".length());

        String[] values = new String(Base64.getDecoder()
                .decode(authorization)).split(":");

        if (values.length < 2) {
            // "Invalid syntax for username and password"
            abort(ctxt);
            return;
        }

        String username = values[0];
        String password = values[1];
        if ((username == null) || (password == null)) {
            // "Missing username or password"
            abort(ctxt);
            return;
        }

        // Need to pull this out of PersistenceManager
        if (!"welcome1".equals(password)) {
            abort(ctxt);
            return;
        }

        User user = new User();
        user.setName(username);
        user.getRoles().add("DEMO");

        if (Roles.H_ADMIN.equals(username)) {
            user.getRoles().add(Roles.H_ADMIN);
        }

        ctxt.setSecurityContext(new Authorizer(user));

    }


    void abort(ContainerRequestContext ctxt) {
        ctxt.abortWith(Response
                .status(Response.Status.UNAUTHORIZED)
                .entity(BAD_CREDENTIALS)
                .build());
    }


}
