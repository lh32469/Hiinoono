package com.hiinoono.rest.auth;

import com.hiinoono.jaxb.User;
import com.hiinoono.rest.zk.ZooKeeperResource;
import java.io.IOException;
import java.util.Base64;
import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.Priorities;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.glassfish.jersey.server.ContainerRequest;


/**
 *
 * @author Lyle T Harris
 */
@Priority(Priorities.AUTHENTICATION - 100)
public class AuthorizationFilter implements ContainerRequestFilter {

    static final Response BAD_CREDENTIALS
            = Response.status(Status.UNAUTHORIZED)
            .entity("Incorrect credentials")
            .build();

    @Inject
    private ZooKeeperResource zkr;


    @Override
    public void filter(ContainerRequestContext ctxt) throws IOException {

        String path = ctxt.getUriInfo().getPath();
        System.out.println("Path: " + path);

        if (path.startsWith("application.wadl")) {
            // Free access to WADL and schemas
            return;
        }

        String authentication
                = ctxt.getHeaderString(ContainerRequest.AUTHORIZATION);
        System.out.println(ContainerRequest.AUTHORIZATION
                + ": " + authentication);

        authentication = authentication.substring("Basic ".length());
        
        // Possibly just go to ZooKeeper and see if this string exists?
        
        String[] values = new String(Base64.getDecoder()
                .decode(authentication)).split(":");

        if (values.length < 2) {
            throw new WebApplicationException(BAD_CREDENTIALS);
            // "Invalid syntax for username and password"
        }

        String username = values[0];
        String password = values[1];
        if ((username == null) || (password == null)) {
            throw new WebApplicationException(BAD_CREDENTIALS);
            // "Missing username or password"
        }

        System.out.println("User: " + username);
        System.out.println("Password: " + password);

        // Need to pull this out of ZK
        if (!"welcome1".equals(password)) {
            throw new WebApplicationException(BAD_CREDENTIALS);
        }
      
        User user = new User();
        user.setName(username);
        user.getRoles().add("DEMO");
        
        if (Roles.H_ADMIN.equals(username)) {
            user.getRoles().add(Roles.H_ADMIN);
        }

        ctxt.setSecurityContext(new Authorizer(user));

    }


}
