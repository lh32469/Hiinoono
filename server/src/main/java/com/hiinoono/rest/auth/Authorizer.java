package com.hiinoono.rest.auth;

import com.hiinoono.jaxb.User;
import java.security.Principal;
import javax.ws.rs.core.SecurityContext;
import org.slf4j.LoggerFactory;


/**
 *
 * @author Lyle T Harris
 */
public class Authorizer implements SecurityContext {

    private final User user;

    private final Principal principal;

    final static private org.slf4j.Logger LOG
            = LoggerFactory.getLogger(Authorizer.class);


    public Authorizer(final User user) {
        this.user = user;
        this.principal = user::getName;
    }


    @Override
    public Principal getUserPrincipal() {
        return this.principal;
    }


    @Override
    public boolean isUserInRole(String role) {
        boolean result = user.getRoles().contains(role);
        LOG.info(user.getTenant() + "/" + user.getName()
                + " ? " + role + " -> " + result);
        return result;
    }


    @Override
    public boolean isSecure() {
        //return "https".equals(uriInfo.getRequestUri().getScheme());
        return false;
    }


    @Override
    public String getAuthenticationScheme() {
        return SecurityContext.BASIC_AUTH;
    }


}
