package com.hiinoono.rest.container;

import com.hiinoono.jaxb.Container;
import com.hiinoono.jaxb.Containers;
import com.hiinoono.jaxb.State;
import com.hiinoono.jaxb.User;
import com.hiinoono.os.ContainerDriver;
import com.hiinoono.persistence.PersistenceManager;
import com.hiinoono.rest.auth.HiinoonoRolesAllowed;
import com.hiinoono.rest.auth.Roles;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
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
//
//    @Inject
//    private ContainerDriver driver;

    @Context
    private SecurityContext sc;


    @POST
    @Path("create")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
//    @HiinoonoRolesAllowed(roles = {Roles.USER},
//            message = "You are not permitted to create containers.  " +
//                    "Only Users can create containers.")
    public Container create(Container c) throws IOException {
        LOG.info(c.getName() + " => " + c.getTemplate());

        // Principal name is tenant/user
        String principalName = sc.getUserPrincipal().getName();
        LOG.trace("Principal Name: " + principalName);

        final String tenantName = principalName.split("/")[0];
        final String userName = principalName.split("/")[1];

        User user = new User();
        user.setTenant(tenantName);
        user.setName(userName);

        c.setOwner(user);

        pm.addContainer(c);
        return c;
    }


    /**
     * Users can only list the containers they created whereas the Tenant Admin
     * can list containers for all users in the tenancy and Hiinoono Admin can
     * list all containers for all tenants.
     *
     * @return
     */
    @GET
    @Path("list")
    @HiinoonoRolesAllowed(roles = {Roles.H_ADMIN, Roles.T_ADMIN, Roles.USER},
            message = "You are not permitted to list containers.")
    public Containers list() {

        // Principal name is tenant/user
        String principalName = sc.getUserPrincipal().getName();
        LOG.trace("Principal Name: " + principalName);

        final String tenantName = principalName.split("/")[0];
        final String userName = principalName.split("/")[1];

        LOG.debug(tenantName);

        List<Container> list;

        if (sc.isUserInRole(Roles.H_ADMIN)) {
            list = pm.getContainers().collect(Collectors.toList());

        } else if (sc.isUserInRole(Roles.T_ADMIN)) {
            list = pm.getContainers().filter(
                    n -> n.getOwner().getTenant().equals(tenantName)
            ).collect(Collectors.toList());

        } else {
            list = pm.getContainers().filter(
                    n -> n.getOwner().getTenant().equals(tenantName)
                    && n.getOwner().getName().equals(userName)
            ).collect(Collectors.toList());

        }

        Containers containers = new Containers();
        containers.getContainers().addAll(list);

        return containers;
    }


}
