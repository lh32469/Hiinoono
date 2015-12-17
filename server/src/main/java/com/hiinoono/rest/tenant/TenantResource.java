package com.hiinoono.rest.tenant;

import com.hiinoono.jaxb.Tenant;
import com.hiinoono.jaxb.Tenants;
import com.hiinoono.rest.auth.Roles;
import com.hiinoono.rest.zk.ZooKeeperResource;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
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
    private ZooKeeperResource zkr;

    @Context
    private SecurityContext sc;


    @GET
    @Path("list")
    @RolesAllowed({Roles.H_ADMIN})
    public List<Tenant> getTenantsAsList() {

        // Clear passwords before sending
        return zkr.getTenants().map((tenant) -> {
            tenant.getAdmin().setPassword("***");
            tenant.getUsers().stream().forEach((user) -> {
                user.setPassword("***");
            });
            return tenant;
        }).collect(Collectors.toList());

    }


    @GET
    @RolesAllowed({"ADMIN", "ORG1", "DEMO"})
    public Tenants getTenants() {

        System.out.println("Principal: " + sc.getUserPrincipal().getName());
        Tenants t = new Tenants();
        t.getTenants().addAll(getTenantsAsList());
        return t;

    }


    @GET
    @Path("{name}")
    public Tenant getTenant(@PathParam("name") String name) {
        return zkr.getTenantByName(name);
    }


}
