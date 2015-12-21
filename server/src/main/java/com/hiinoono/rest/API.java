package com.hiinoono.rest;

import com.hiinoono.persistence.PersistenceManager;
import com.hiinoono.persistence.UnitTestPersistenceManager;
import com.hiinoono.rest.auth.AuthorizationFilter;
import com.hiinoono.rest.node.NodeResource;
import com.hiinoono.rest.site.SiteResource;
import com.hiinoono.rest.tenant.TenantResource;
import com.hiinoono.persistence.ZooKeeperPersistenceManager;
import java.io.IOException;
import org.apache.zookeeper.KeeperException;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.message.MessageProperties;
import org.glassfish.jersey.moxy.json.MoxyJsonConfig;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.glassfish.jersey.server.wadl.WadlFeature;


/**
 *
 * @author Lyle T Harris
 */
public class API extends ResourceConfig {

    public API() {

        register(SiteResource.class);
        register(NodeResource.class);
        register(TenantResource.class);
        register(RolesAllowedDynamicFeature.class);
        register(AuthorizationFilter.class);

        if (System.getProperty("LOG") != null) {
            register(LoggingFilter.class);
        }
        register(WadlFeature.class);

        MoxyJsonConfig config = new MoxyJsonConfig();
        config.setFormattedOutput(true);
        config.setIncludeRoot(false);
        register(config.resolver());

        register(new Binder());

        property(MessageProperties.XML_FORMAT_OUTPUT, true);
        property(MessageProperties.JAXB_PROCESS_XML_ROOT_ELEMENT, false);
    }


    class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bind(new UnitTestPersistenceManager()).to(PersistenceManager.class);
        }


    }

}
