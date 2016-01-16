package com.hiinoono.rest.container;

import com.hiinoono.jaxb.Container;
import com.hiinoono.jaxb.Containers;
import com.hiinoono.jaxb.Tenant;
import com.hiinoono.jaxb.Users;
import com.hiinoono.os.ContainerDriver;
import com.hiinoono.persistence.PersistenceManager;
import com.hiinoono.rest.auth.HiinoonoRolesAllowed;
import com.hiinoono.rest.auth.Roles;
import java.io.IOException;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import org.slf4j.LoggerFactory;


/**
 *
 * @author Lyle T Harris
 */
@Path(ContainerResource.PATH)
public class ContainerResource {

    public static final String PATH = "/container";

    private final static String[] CURRENCY
            = {MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML};

    final static private org.slf4j.Logger LOG
            = LoggerFactory.getLogger(ContainerResource.class);

    @Inject
    private PersistenceManager pm;

    @Inject
    private ContainerDriver driver;

    @Context
    private SecurityContext sc;


    @POST
    @Path("create")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Container create(Container c) throws IOException {
        LOG.info(c.getName() + " => " + c.getTemplate());
        pm.addContainer(c);
        return c;
    }


    /**
     * Users can only list the containers they created whereas the Tenant Admin
     * can list containers for all users in the tenancy.
     *
     * @return
     */
    @GET
    @Path("list")
    public Containers list() {

        // Principal name is tenant/user
        String principalName = sc.getUserPrincipal().getName();
        LOG.trace("Principal Name: " + principalName);

        final String tenantName = principalName.split("/")[0];
        LOG.debug(tenantName);

        Containers containers = new Containers();

        return containers;
    }


}
