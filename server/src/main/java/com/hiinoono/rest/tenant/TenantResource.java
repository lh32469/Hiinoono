package com.hiinoono.rest.tenant;

import com.hiinoono.Utils;
import com.hiinoono.jaxb.Tenant;
import com.hiinoono.jaxb.User;
import com.hiinoono.persistence.PersistenceManager;
import com.hiinoono.rest.auth.HiinoonoRolesAllowed;
import com.hiinoono.rest.auth.Roles;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.security.PermitAll;
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
import static org.apache.commons.lang.StringUtils.isBlank;
import org.slf4j.LoggerFactory;


/**
 *
 * @author Lyle T Harris
 */
@Path(TenantResource.PATH)
@PermitAll
public class TenantResource {

    public static final String PATH = "/tenant";

    final static private org.slf4j.Logger LOG
            = LoggerFactory.getLogger(TenantResource.class);

    @Inject
    private PersistenceManager pm;

    @Context
    private SecurityContext sc;


    @GET
    @HiinoonoRolesAllowed(roles = {Roles.H_ADMIN},
            message = "You are not permitted to list tenants.")
    public List<Tenant> getTenants() {

        // Clear passwords before sending
        return pm.getTenants().map((tenant) -> {
            tenant.getUsers().stream().forEach((user) -> {
                user.setPassword("***");
            });
            return tenant;
        }).collect(Collectors.toList());

    }


    @POST
    @Path("add")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Produces(MediaType.TEXT_PLAIN)
    @HiinoonoRolesAllowed(roles = {Roles.H_ADMIN},
            message = "You are not permitted to add tenants.")
    public Response addTenant(Tenant tenant) {

        LOG.info(tenant.getName());

        if (isBlank(tenant.getName())) {
            throw new NotAcceptableException("Tenant Name is not set");
        }

        // Clear any users provided from Client and add default Admin User.
        tenant.getUsers().clear();
        User u = new User();
        u.setName("admin");
        u.setJoined(Utils.now());
        String password = UUID.randomUUID().toString().substring(28);
        u.setPassword(pm.hash(tenant.getName() + "admin" + password));
        tenant.getUsers().add(u);
        tenant.setJoined(Utils.now());

        pm.addTenant(tenant);
        return Response.ok("Password: " + password).build();
    }


    @GET
    @Path("delete/{name}")
    @HiinoonoRolesAllowed(roles = {Roles.H_ADMIN},
            message = "You are not permitted to delete tenants.")
    public Response deleteTenant(@PathParam("name") String tenantName) {
        LOG.info(tenantName);
        if (getTenant(tenantName) == null) {
            throw new NotAcceptableException("Tenant " + tenantName
                    + " doesn't exist.");
        }
        pm.deleteTenant(tenantName);
        return Response.ok().build();
    }


    @GET
    @Path("{name}")
    public Tenant getTenant(@PathParam("name") String name) {
        LOG.debug(name);

        // If User is in Tenant Admin Role make sure Tenant names match
        if (!sc.isUserInRole(Roles.H_ADMIN)
                && !sc.isUserInRole(name + "/admin")) {

            throw new ForbiddenException("You are not permitted to"
                    + " view Tenant: " + name);
        }

        Optional<Tenant> t = pm.getTenantByName(name);

        if (t.isPresent()) {
            t.get().getUsers().stream().forEach((user) -> {
                user.setPassword("***");
            });
        }

        return t.orElse(null);
    }


}
