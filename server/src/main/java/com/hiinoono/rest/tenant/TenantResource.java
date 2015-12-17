package com.hiinoono.rest.tenant;

import com.hiinoono.jaxb.Tenant;
import com.hiinoono.jaxb.Tenants;
import com.hiinoono.jaxb.User;
import com.hiinoono.rest.zk.ZooKeeperResource;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import org.slf4j.LoggerFactory;


/**
 *
 * @author Lyle T Harris
 */
@Path(TenantResource.PATH)
public class TenantResource {

    public static final String PATH = "/tenant";

    final static private org.slf4j.Logger LOG
            = LoggerFactory.getLogger(TenantResource.class);

    @Inject
    private ZooKeeperResource zkr;


    @GET
    @Path("list")
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
    public Tenants getTenants() {

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
