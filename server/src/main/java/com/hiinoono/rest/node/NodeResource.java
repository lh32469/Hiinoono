package com.hiinoono.rest.node;

import com.hiinoono.jaxb.Container;
import com.hiinoono.jaxb.Node;
import com.hiinoono.jaxb.Nodes;
import com.hiinoono.jaxb.Status;
import com.hiinoono.jaxb.Value;
import com.hiinoono.jaxb.Manager;
import com.hiinoono.jaxb.Managers;
import com.hiinoono.persistence.PersistenceManager;
import com.hiinoono.rest.auth.HiinoonoRolesAllowed;
import com.hiinoono.rest.auth.Roles;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import org.slf4j.LoggerFactory;


/**
 *
 * @author Lyle T Harris
 */
@Path(NodeResource.PATH)
public class NodeResource {

    public static final String PATH = "/node";

    final static private org.slf4j.Logger LOG
            = LoggerFactory.getLogger(NodeResource.class);

    @Inject
    private PersistenceManager pm;


    @GET
    @Path("status")
    public Status status() {

        Status status = new Status();
        status.setValue(Value.OK);
        status.getMessages().add("Tutto Bene!");
        status.getMessages().add("All good...");
        return status;

    }


    /**
     * Get all available Nodes in the order of current placement preference.
     */
    @GET
    @HiinoonoRolesAllowed(roles = {Roles.H_ADMIN},
            message = "You are not permitted to list nodes.")
    public Nodes getNodes() {
        Nodes nodes = new Nodes();
        List<Node> available = pm.getNodes().collect(Collectors.toList());
        Collections.sort(available, new NodeComparator(new Container()));
        nodes.getNodes().addAll(available);
        return nodes;
    }


    /**
     * Create list of cluster-wide managers and what node is master for each
     * service/task/etc..
     */
    @GET
    @Path("managers")
    public Managers managers() {

        List<Manager> _managers = pm.getManagers().collect(Collectors.toList());

        Managers managers = new Managers();
        managers.getManagers().addAll(_managers);

        return managers;

    }


}
