package com.hiinoono.rest;

import com.hiinoono.rest.node.NodeResource;
import com.hiinoono.rest.site.SiteResource;
import com.hiinoono.rest.zk.ZooKeeperResource;
import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.message.MessageProperties;
import org.glassfish.jersey.moxy.json.MoxyJsonConfig;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.wadl.WadlFeature;


/**
 *
 * @author Lyle T Harris
 */
public class API extends ResourceConfig {

    public API() {
       
        register(SiteResource.class);
        register(NodeResource.class);
        register(ZooKeeperResource.class);

        if (System.getProperty("LOG") != null) {
            register(LoggingFilter.class);
        }
        register(WadlFeature.class);

        MoxyJsonConfig config = new MoxyJsonConfig();
        config.setFormattedOutput(true);
        config.setIncludeRoot(false);
        register(config.resolver());

        property(MessageProperties.XML_FORMAT_OUTPUT, true);
        property(MessageProperties.JAXB_PROCESS_XML_ROOT_ELEMENT, false);
    }


}
