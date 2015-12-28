package com.hiinoono.rest.site;

import com.hiinoono.jaxb.SiteInfo;
import com.hiinoono.persistence.PersistenceManager;
import com.hiinoono.persistence.zk.ZooKeeperPersistenceManager;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;
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
    private PersistenceManager pm;


    @GET
    @Path("info")
    public SiteInfo info(@Context UriInfo ui) {

        SiteInfo info = new SiteInfo();
        info.setName("HN-1");
        String version = "Unknown";

        try {
            version = getVersion();
        } catch (IOException ex) {
            LOG.error(ex.getLocalizedMessage());
        }

        info.setVersion(version);
        info.setUri(ui.getAbsolutePath().toString());

        return info;

    }


    String getVersion() throws IOException {
        Properties props = new Properties();

        Enumeration<URL> manifest
                = ClassLoader.getSystemResources("META-INF/MANIFEST.MF");

        while (manifest.hasMoreElements()) {
            URL nextElement = manifest.nextElement();
            if (nextElement.getFile().contains("Hs.jar")) {
                props.load(nextElement.openStream());
                break;
            }
        }

        return props.getProperty("version")
                + " (" + props.getProperty("date") + ")";
    }


}
