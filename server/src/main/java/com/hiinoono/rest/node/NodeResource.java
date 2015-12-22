package com.hiinoono.rest.node;

import com.hiinoono.jaxb.Nodes;
import com.hiinoono.jaxb.Status;
import com.hiinoono.jaxb.Value;
import com.hiinoono.persistence.PersistenceManager;
import com.hiinoono.rest.auth.Roles;
import java.util.stream.Collectors;
import javax.annotation.security.RolesAllowed;
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


    @GET
    @RolesAllowed({Roles.H_ADMIN})
    public Nodes getNodes() {
        Nodes nodes = new Nodes();
        nodes.getNodes().addAll(pm.getNodes().collect(Collectors.toList()));
        return nodes;
    }


}
