package com.hiinoono.rest.node;

import com.hiinoono.common.jaxb.Status;
import com.hiinoono.common.jaxb.Value;
import com.hiinoono.rest.zk.ZooKeeperResource;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
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
    private ZooKeeperResource zkr;


    @GET
    @Path("status")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Status status() {
        
        Status status = new Status();
        status.setValue(Value.OK);
        status.getMessages().add("Tutto Bene!");
        return status;

    }


}
