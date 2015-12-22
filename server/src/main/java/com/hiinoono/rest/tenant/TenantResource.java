package com.hiinoono.rest.tenant;

import com.hiinoono.jaxb.Tenant;
import com.hiinoono.jaxb.Tenants;
import com.hiinoono.persistence.PersistenceManager;
import com.hiinoono.rest.auth.HiinoonoRolesAllowed;
import com.hiinoono.rest.auth.Roles;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
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
    @Path("list")
    @HiinoonoRolesAllowed(roles = {Roles.H_ADMIN},
            message = "You are not permitted to list tenants.")
    public List<Tenant> getTenantsAsList() {

        // Clear passwords before sending
        return pm.getTenants().map((tenant) -> {
            tenant.getAdmin().setPassword("***");
            tenant.getUsers().stream().forEach((user) -> {
                user.setPassword("***");
            });
            return tenant;
        }).collect(Collectors.toList());

    }


    @GET
    @HiinoonoRolesAllowed(roles = {Roles.H_ADMIN},
            message = "You are not permitted to list tenants.")
    public Tenants getTenants() {

        System.out.println("PM: " + pm);
        System.out.println("Principal: " + sc.getUserPrincipal().getName());
        Tenants t = new Tenants();
        t.getTenants().addAll(getTenantsAsList());
        return t;

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

        Tenant t = pm.getTenantByName(name);

        if (t != null) {
            t.getAdmin().setPassword("***");
            t.getUsers().stream().forEach((user) -> {
                user.setPassword("***");
            });
        }

        return t;
    }


}
