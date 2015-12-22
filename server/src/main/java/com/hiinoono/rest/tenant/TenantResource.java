package com.hiinoono.rest.tenant;

import com.hiinoono.jaxb.Tenant;
import com.hiinoono.jaxb.Tenants;
import com.hiinoono.persistence.PersistenceManager;
import com.hiinoono.rest.auth.Roles;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
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
    @RolesAllowed({Roles.H_ADMIN})
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
    @RolesAllowed({Roles.H_ADMIN})
    public Tenants getTenants() {

        System.out.println("PM: " + pm);
        System.out.println("Principal: " + sc.getUserPrincipal().getName());
        Tenants t = new Tenants();
        t.getTenants().addAll(getTenantsAsList());
        return t;

    }


    @GET
    @Path("{name}")
    @RolesAllowed({Roles.H_ADMIN, Roles.T_ADMIN})
    public Tenant getTenant(@PathParam("name") String name) {

        // If User is in Tenant Admin Role make sure Tenant names match
        if (!sc.isUserInRole(Roles.H_ADMIN)
                && !sc.isUserInRole(name + "/admin")) {

            final String message = "You are not authorized to view Tenant: "
                    + name + "\n";

            throw new WebApplicationException(Response
                    .status(Response.Status.UNAUTHORIZED)
                    .entity(message)
                    .build());
        }

        Tenant t = pm.getTenantByName(name);
        
        t.getAdmin().setPassword("***");
        t.getUsers().stream().forEach((user) -> {
            user.setPassword("***");
        });

        return t;
    }


}
