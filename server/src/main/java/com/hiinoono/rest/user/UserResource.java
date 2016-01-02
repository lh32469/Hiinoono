package com.hiinoono.rest.user;

import com.hiinoono.jaxb.Tenant;
import com.hiinoono.jaxb.User;
import com.hiinoono.persistence.PersistenceManager;
import com.hiinoono.rest.auth.HiinoonoRolesAllowed;
import com.hiinoono.rest.auth.Roles;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import org.slf4j.LoggerFactory;


/**
 *
 * @author Lyle T Harris
 */
@Path(UserResource.PATH)
public class UserResource {

    public static final String PATH = "/user";

    final static private org.slf4j.Logger LOG
            = LoggerFactory.getLogger(UserResource.class);

    @Inject
    private PersistenceManager pm;

    @Context
    private SecurityContext sc;


    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.TEXT_PLAIN)
    @HiinoonoRolesAllowed(roles = {Roles.T_ADMIN},
            message = "You are not permitted to add users.")
    public Response addUser(User u) throws DatatypeConfigurationException {
        LOG.info(u.getName());

        final String tenantName = u.getTenant();

        // Make sure Tenant names match
        if (!sc.isUserInRole(tenantName + "/admin")) {
            throw new ForbiddenException("You are not permitted to"
                    + " add Users to Tenant: " + tenantName);
        }

        Optional<Tenant> t = pm.getTenantByName(tenantName);

        if (!t.isPresent()) {
            throw new NotAcceptableException("Tenant " + tenantName
                    + " doesn't exist.");
        }

        List<User> existingUsers = t.get().getUsers();

        for (User user : existingUsers) {
            if (user.getName().equals(u.getName())) {
                throw new NotAcceptableException("User " + u.getName()
                        + " already exists.");
            }
        }

        String password = UUID.randomUUID().toString().substring(28);
        u.setJoined(now());
        u.setPassword(pm.hash(u.getTenant() + u.getName() + password));
        existingUsers.add(u);

        pm.persist(t.get());
        return Response.ok("Password: " + password).build();
    }


    @GET
    @Path("delete/{name}")
    @HiinoonoRolesAllowed(roles = {Roles.T_ADMIN},
            message = "You are not permitted to delete users.")
    public Response deleteUser(@PathParam("name") String userName) {
        LOG.info(userName);

        // Principal name is tenant/user
        String principalName = sc.getUserPrincipal().getName();
        LOG.info("Principal Name: " + principalName);

        final String tenantName = principalName.split("/")[0];
        LOG.info(tenantName + "/" + userName);

        // Make sure Tenant names match
        if (!sc.isUserInRole(tenantName + "/admin")) {
            throw new ForbiddenException("You are not permitted to"
                    + " add Users to Tenant: " + tenantName);
        }

        Optional<Tenant> t = pm.getTenantByName(tenantName);

        if (!t.isPresent()) {
            throw new NotAcceptableException("Tenant " + tenantName
                    + " doesn't exist.");
        }

        Iterator<User> iter = t.get().getUsers().iterator();

        while (iter.hasNext()) {
            User user = iter.next();
            if (user.getName().equals(userName)) {
                LOG.info("Deleted: " + tenantName + "/" + userName);
                iter.remove();
                break;
            }

        }

        pm.persist(t);

        return Response.ok().build();
    }


    /**
     * Get current date and time.
     *
     * @return
     * @throws DatatypeConfigurationException
     */
    XMLGregorianCalendar now() throws DatatypeConfigurationException {
        GregorianCalendar date = new GregorianCalendar();
        DatatypeFactory dtf = DatatypeFactory.newInstance();
        return dtf.newXMLGregorianCalendar(date);
    }


}
