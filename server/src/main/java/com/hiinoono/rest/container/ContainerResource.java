package com.hiinoono.rest.container;

import com.hiinoono.jaxb.Container;
import com.hiinoono.jaxb.DiskOption;
import com.hiinoono.jaxb.MemOption;
import com.hiinoono.jaxb.State;
import com.hiinoono.jaxb.Template;
import com.hiinoono.jaxb.Tenant;
import com.hiinoono.jaxb.User;
import com.hiinoono.persistence.PersistenceManager;
import com.hiinoono.rest.auth.HiinoonoRolesAllowed;
import com.hiinoono.rest.auth.Roles;
import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
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
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.validation.SchemaFactory;
import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


/**
 *
 * @author Lyle T Harris
 */
@Path(ContainerResource.PATH)
public class ContainerResource {

    public static final String PATH = "/container";

    private final static String[] CURRENCY
            = {MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML};

    private static final Class[] JAXB_CLASSES = {Container.class};

    final static private org.slf4j.Logger LOG
            = LoggerFactory.getLogger(ContainerResource.class);

    @Inject
    private PersistenceManager pm;
//
//    @Inject
//    private ContainerDriver driver;

    @Context
    private SecurityContext sc;


    @GET
    @Path("sample")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Container getSample() {

        Container c = new Container();
        c.setName("cn-01");
        c.setTemplate(Template.UBUNTU);
        c.setMemory(MemOption.MEG_512);
        c.setDisk(DiskOption.GIG_5);
        c.setCpuLimit(2);

        NumberFormat nf = new DecimalFormat("'GIG_'#");

        System.out.println("Value: " + DiskOption.GIG_5);
        try {
            System.out.println("Parsed: "
                    + nf.parse(DiskOption.GIG_5.toString()));
            System.out.println("Parsed: "
                    + nf.parse(DiskOption.GIG_40.toString()));
        } catch (ParseException ex) {
            LOG.error(ex.toString());
        }

        return c;
    }


    @POST
    @Path("create")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @HiinoonoRolesAllowed(roles = {Roles.USER},
            message = "You are not permitted to create containers.  "
            + "Only Users can create containers.")
    public Container create(Container c) throws IOException {
        LOG.info(c.getName() + " => " + c.getTemplate());

        // Principal name is tenant/user
        String principalName = sc.getUserPrincipal().getName();
        LOG.trace("Principal Name: " + principalName);

        final String tenantName = principalName.split("/")[0];
        final String userName = principalName.split("/")[1];

        Tenant tenant = pm.getTenantByName(tenantName).get();

        User owner = tenant.getUsers().stream().filter((user)
                -> (user.getName().equals(userName))).findFirst().get();

        if (owner.getEmail() == null) {
            // TODO: Make sure this is set when user added.
            owner.setEmail("foo@foo.com");
        }

        owner.setPassword("******");
        c.setOwner(owner);
        c.setState(State.CREATE_REQUESTED);

        try {

            String language = XMLConstants.W3C_XML_SCHEMA_NS_URI;
            Map props = Collections.EMPTY_MAP;
            SchemaFactory sf = SchemaFactory.newInstance(language);
            URL schemaURL
                    = ClassLoader.getSystemResource("xsd/Container.xsd");

            JAXBContext jc
                    = JAXBContextFactory.createContext(JAXB_CLASSES, props);

            Marshaller m = jc.createMarshaller();
            m.setSchema(sf.newSchema(schemaURL));

            // Validate...
            m.marshal(c, new DefaultHandler());

        } catch (SAXException | JAXBException ex) {
            throw new NotAcceptableException(ex.toString());
        }

        pm.addContainer(c);
        return c;
    }


    @GET
    @Path("delete/{tenant}/{user}/{name}")
    @HiinoonoRolesAllowed(roles = {Roles.H_ADMIN, Roles.T_ADMIN, Roles.USER},
            message = "You are not permitted to delete a container")
    public Response delete(@PathParam("tenant") String tenantParam,
            @PathParam("user") String userParam,
            @PathParam("name") String containerName) {

        // Principal name is tenant/user
        String principalName = sc.getUserPrincipal().getName();
        LOG.trace("Principal Name: " + principalName);

        final String tenantName = principalName.split("/")[0];
        final String userName = principalName.split("/")[1];

        Container toDelete;

        if (sc.isUserInRole(Roles.H_ADMIN)) {
            // H_Admin can delete any container
            toDelete = get(tenantParam, userParam, containerName);
        } else if (sc.isUserInRole(Roles.T_ADMIN)) {
            // Ignore Tenant PathParam, can only delete in own tenancy
            toDelete = get(tenantName, userParam, containerName);
        } else {
            // Ignore Tenant and User PathParams, can only view own containers.
            toDelete = get(tenantName, userName, containerName);
        }

        if (!toDelete.getState().equals(State.STOPPED)
                && !toDelete.getState().equals(State.ERROR)) {
            throw new NotAcceptableException("Container " + containerName
                    + " is currently in state: " + toDelete.getState()
                    + " and needs to be stopped first.");
        }

        pm.deleteContainer(toDelete);

        return Response.ok().build();
    }


    @GET
    @Path("stop/{tenant}/{user}/{name}")
    @HiinoonoRolesAllowed(roles = {Roles.H_ADMIN, Roles.T_ADMIN, Roles.USER},
            message = "You are not permitted to stop a container")
    public Response stop(@PathParam("tenant") String tenantParam,
            @PathParam("user") String userParam,
            @PathParam("name") String containerName) {

        // Principal name is tenant/user
        String principalName = sc.getUserPrincipal().getName();
        LOG.trace("Principal Name: " + principalName);

        final String tenantName = principalName.split("/")[0];
        final String userName = principalName.split("/")[1];

        Container toStop;

        if (sc.isUserInRole(Roles.H_ADMIN)) {
            // H_Admin can delete any container
            toStop = get(tenantParam, userParam, containerName);
        } else if (sc.isUserInRole(Roles.T_ADMIN)) {
            // Ignore Tenant PathParam, can only delete in own tenancy
            toStop = get(tenantName, userParam, containerName);
        } else {
            // Ignore Tenant and User PathParams, can only start own containers.
            toStop = get(tenantName, userName, containerName);
        }

        if (toStop.getState().equals(State.STOPPED)) {
            throw new NotAcceptableException("Container " + containerName
                    + " is already stopped.");
        }

        pm.stopContainer(toStop);

        return Response.ok().build();
    }


    @GET
    @Path("start/{tenant}/{user}/{name}")
    @HiinoonoRolesAllowed(roles = {Roles.H_ADMIN, Roles.T_ADMIN, Roles.USER},
            message = "You are not permitted to start a container")
    public Response start(@PathParam("tenant") String tenantParam,
            @PathParam("user") String userParam,
            @PathParam("name") String containerName) {

        // Principal name is tenant/user
        String principalName = sc.getUserPrincipal().getName();
        LOG.trace("Principal Name: " + principalName);

        final String tenantName = principalName.split("/")[0];
        final String userName = principalName.split("/")[1];

        Container toStart;

        if (sc.isUserInRole(Roles.H_ADMIN)) {
            // H_Admin can start any container
            toStart = get(tenantParam, userParam, containerName);
        } else if (sc.isUserInRole(Roles.T_ADMIN)) {
            // Ignore Tenant PathParam, can only start in own tenancy
            toStart = get(tenantName, userParam, containerName);
        } else {
            // Ignore Tenant and User PathParams, can only start own containers.
            toStart = get(tenantName, userName, containerName);
        }

        if (toStart.getState().equals(State.RUNNING)) {
            throw new NotAcceptableException("Container " + containerName
                    + " is already running.");
        }

        pm.startContainer(toStart);

        return Response.ok().build();
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
    public List<Container> list() {

        // Principal name is tenant/user
        String principalName = sc.getUserPrincipal().getName();
        LOG.debug("Principal Name: " + principalName);

        final String tenantName = principalName.split("/")[0];
        final String userName = principalName.split("/")[1];

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

        return list;
    }


    /**
     * Users can only get the containers they created whereas the Tenant Admin
     * can get containers for all users in the tenancy and Hiinoono Admin can
     * get any container.
     *
     * @param tenantParam Name of the Tenant who owns the Container.
     * @param userParam Name of the User who owns the Container.
     * @param containerName Name of the Container to get.
     *
     * @return
     */
    @GET
    @Path("get/{tenant}/{user}/{name}")
    @HiinoonoRolesAllowed(roles = {Roles.H_ADMIN, Roles.T_ADMIN, Roles.USER},
            message = "You are not permitted to get a container")
    public Container get(@PathParam("tenant") String tenantParam,
            @PathParam("user") String userParam,
            @PathParam("name") String containerName) {

        // Principal name is tenant/user
        String principalName = sc.getUserPrincipal().getName();
        LOG.trace("Principal Name: " + principalName);

        final String tenantName = principalName.split("/")[0];
        final String userName = principalName.split("/")[1];

        Optional<Container> optional;

        if (sc.isUserInRole(Roles.H_ADMIN)) {
            optional = pm.getContainers().filter(
                    n -> n.getOwner().getTenant().equals(tenantParam)
                    && n.getOwner().getName().equals(userParam)
                    && n.getName().equals(containerName)
            ).findFirst();

        } else if (sc.isUserInRole(Roles.T_ADMIN)) {
            // Ignore Tenant PathParam, can only view in own tenancy
            optional = pm.getContainers().filter(
                    n -> n.getOwner().getTenant().equals(tenantName)
                    && n.getOwner().getName().equals(userParam)
                    && n.getName().equals(containerName)
            ).findFirst();

        } else {
            // Ignore Tenant and User PathParams, can only view own containers.
            optional = pm.getContainers().filter(
                    n -> n.getOwner().getTenant().equals(tenantName)
                    && n.getOwner().getName().equals(userName)
                    && n.getName().equals(containerName)
            ).findFirst();

        }

        if (!optional.isPresent()) {
            throw new NotAcceptableException("Container " + containerName
                    + " doesn't exist.");
        }

        return optional.get();
    }


}
