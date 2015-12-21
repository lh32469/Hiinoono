package com.hiinoono.rest.node;

import com.hiinoono.jaxb.Status;
import com.hiinoono.jaxb.Value;
import com.hiinoono.persistence.PersistenceManager;
import com.hiinoono.persistence.ZooKeeperPersistenceManager;
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


}
