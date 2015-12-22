package com.hiinoono.rest.auth;

import com.hiinoono.jaxb.User;
import com.hiinoono.persistence.PersistenceManager;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
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

        String login = values[0];
        String password = values[1];
        if ((login == null) || (password == null)) {
            LOG.info("Missing login or password");
            abort(ctxt);
            return;
        }
        if (login.startsWith("/")) {
            login = login.replaceFirst("/", "");
        }

        if (login.endsWith("/")) {
            login = login.substring(0, login.length() - 1);
        }

        values = login.split("/");
        if (values.length < 2) {
            LOG.info("Invalid syntax for login:  " + login);
            abort(ctxt);
            return;
        }

        String tenant = values[0];
        String username = values[1];

        // So two Users with same password will showup as different hashes.
        String hash = pm.hash(tenant + username + password);
        if (!hash.equals(pm.getHash(tenant, username))) {
            abort(ctxt);
            return;
        }

        User user = new User();
        user.setTenant(tenant);
        user.setName(username);
        user.getRoles().add("DEMO");

        if ("hiinoono".equals(tenant) && "admin".equals(username)) {
            user.getRoles().add(Roles.H_ADMIN);
        }

        if ("admin".equals(username)) {
            // Tenant Administrator
            user.getRoles().add(Roles.T_ADMIN);
            user.getRoles().add(user.getTenant() + "/" + user.getName());
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