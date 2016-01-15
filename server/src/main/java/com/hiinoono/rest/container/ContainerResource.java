package com.hiinoono.rest.container;

import com.hiinoono.jaxb.Container;
import com.hiinoono.os.ContainerDriver;
import com.hiinoono.persistence.PersistenceManager;
import java.io.IOException;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
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
        return driver.create(c);
    }


}
