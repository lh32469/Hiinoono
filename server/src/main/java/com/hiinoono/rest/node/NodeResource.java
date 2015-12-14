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
@Singleton
public class NodeResource {

    public static final String PATH = "/node";

    private final String id;

    final static private org.slf4j.Logger LOG
            = LoggerFactory.getLogger(NodeResource.class);


    public NodeResource() {
        this.id = UUID.randomUUID().toString();
    }

    @Inject
    private ZooKeeperResource zkr;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getIt() {
        LOG.info("");
        return id + "\n" + zkr + "\n";
    }


    @GET
    @Path("status")
    @Produces({MediaType.TEXT_PLAIN + ";qs=1"})
    public String getStatusAsText() {
        LOG.debug("");
        return getStatus().getValue().value();
    }


    @GET
    @Path("status")
    @Produces({
        MediaType.APPLICATION_JSON + ";qs=.75",
        MediaType.APPLICATION_XML + ";qs=.5"
    })
    public Status getStatus() {
        Status status = new Status();
        status.setValue(Value.OK);
        status.getMessages().add("Tutto Bene!");
        return status;

    }


}
