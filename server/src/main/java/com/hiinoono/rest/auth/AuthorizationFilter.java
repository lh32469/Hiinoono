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
import org.slf4j.LoggerFactory;


/**
 *
 * @author Lyle T Harris
 */
@Priority(Priorities.AUTHENTICATION - 100)
public class AuthorizationFilter implements ContainerRequestFilter {

    private static final String BAD_CREDENTIALS = "Incorrect credentials\n";

    final static private org.slf4j.Logger LOG
            = LoggerFactory.getLogger(AuthorizationFilter.class);

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
            LOG.info(" No authorization.");
            abort(ctxt);
            return;
        }

        authorization = authorization.substring("Basic ".length());

        String[] values = new String(Base64.getDecoder()
                .decode(authorization)).split(":");

        if (values.length < 2) {
            LOG.info("Invalid syntax for username and password");
            abort(ctxt);
            return;
        }

        String login = values[0];
        String password = values[1];
        if ((login == null) || (password == null)) {
            LOG.info("Missing login or password");
            abort(ctxt);
            return;
        }

        login = login.replaceAll("/", " ").trim();

        values = login.split(" ");
        if (values.length < 2) {
            LOG.info("Invalid syntax for login:  " + login);
            abort(ctxt);
            return;
        }

        String tenant = values[0];
        String username = values[1];

        Authenticate a = new Authenticate(pm, tenant, username, password);
        if (!a.execute()) {
            abort(ctxt);
            return;
        }

        User user = new User();
        user.setTenant(tenant);
        user.setName(username);

        if ("admin".equals(username)) {
            if ("hiinoono".equals(tenant)) {
                // Hiinoono Administrator
                user.getRoles().add(Roles.H_ADMIN);
            } else {
                // Tenant Administrator
                user.getRoles().add(Roles.T_ADMIN);
                user.getRoles().add(user.getTenant() + "/" + user.getName());
            }
        } else {
            // Basic User
            user.getRoles().add(Roles.USER);
        }

        LOG.info("Logged in: " + user.getTenant() + "/" + user.getName() + " "
                + user.getRoles());

        ctxt.setSecurityContext(new Authorizer(user));

    }


    void abort(ContainerRequestContext ctxt) {
        ctxt.abortWith(Response
                .status(Response.Status.UNAUTHORIZED)
                .entity(BAD_CREDENTIALS)
                .build());
    }


}
