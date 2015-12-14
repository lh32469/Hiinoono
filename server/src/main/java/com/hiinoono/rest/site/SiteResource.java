package com.hiinoono.rest.site;

import com.hiinoono.jaxb.SiteInfo;
import com.hiinoono.rest.zk.ZooKeeperResource;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import org.slf4j.LoggerFactory;


/**
 *
 * @author Lyle T Harris
 */
@Path(SiteResource.PATH)
public class SiteResource {

    public static final String PATH = "/site";

    final static private org.slf4j.Logger LOG
            = LoggerFactory.getLogger(SiteResource.class);

    @Inject
    private ZooKeeperResource zkr;


    @GET
    @Path("info")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public SiteInfo info(@Context UriInfo ui) {

        SiteInfo info = new SiteInfo();
        info.setName("HN-1");
        info.setVersion("1.0");
        info.setUri(ui.getAbsolutePath().toString());

        return info;

    }


}
