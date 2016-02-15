package com.hiinoono.rest;

import com.hiinoono.managers.PlacementManager;
import com.hiinoono.managers.PortsManager;
import com.hiinoono.timertask.ContainerStatTimerTask;
import com.hiinoono.os.container.NodeContainerWatcher;
import com.hiinoono.persistence.PersistenceManager;
import com.hiinoono.persistence.UnitTestPersistenceManager;
import com.hiinoono.persistence.zk.ZooKeeperPersistenceManager;
import com.hiinoono.persistence.zk.ZooKeeperClient;
import com.hiinoono.rest.auth.AuthorizationFilter;
import com.hiinoono.rest.auth.HiinoonoRolesFeature;
import com.hiinoono.rest.container.ContainerResource;
import com.hiinoono.rest.exceptions.ClientErrorExceptionMapper;
import com.hiinoono.rest.exceptions.DefaultExceptionMapper;
import com.hiinoono.rest.node.NodeResource;
import com.hiinoono.timertask.NodeStatTimerTask;
import com.hiinoono.rest.site.SiteResource;
import com.hiinoono.rest.tenant.TenantResource;
import com.hiinoono.rest.user.UserResource;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Random;
import java.util.Timer;
import javax.xml.bind.JAXBException;
import org.apache.zookeeper.KeeperException;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.message.MessageProperties;
import org.glassfish.jersey.moxy.json.MoxyJsonConfig;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.EncodingFilter;
import org.glassfish.jersey.server.wadl.WadlFeature;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;


/**
 *
 * @author Lyle T Harris
 */
public class API extends ResourceConfig {

    final static private org.slf4j.Logger LOG
            = LoggerFactory.getLogger(API.class);


    public API() {

        register(SiteResource.class);
        register(NodeResource.class);
        register(TenantResource.class);
        register(UserResource.class);
        register(ContainerResource.class);
        //register(RolesAllowedDynamicFeature.class);
        register(HiinoonoRolesFeature.class);
        register(AuthorizationFilter.class);

        // Exception Mappers
        //register(HiinoonoExceptionMapper.class);
        register(DefaultExceptionMapper.class);
        register(ClientErrorExceptionMapper.class);

        if (System.getProperty("LOG") != null) {
            register(LoggingFilter.class);
        }
        register(WadlFeature.class);
        register(TimingFilter.class);
        register(GZipEncoder.class);
        register(EncodingFilter.class);

        MoxyJsonConfig config = new MoxyJsonConfig();
        config.setFormattedOutput(true);
        // Don't change includeRoot unless you know ALL 
        // the client side effects.
        config.setIncludeRoot(false);
        register(config.resolver());

        register(new Binder());

        property(MessageProperties.XML_FORMAT_OUTPUT, true);
        // Don't change process root unless you know ALL 
        // the client side effects.
        property(MessageProperties.JAXB_PROCESS_XML_ROOT_ELEMENT, false);
    }


    class Binder extends AbstractBinder {

        @Override
        protected void configure() {

            String zooKeepers = System.getProperty("zooKeepers");

            if (zooKeepers == null) {
                bind(new UnitTestPersistenceManager())
                        .to(PersistenceManager.class);
            } else {
                try {
                    ZooKeeperClient zkc
                            = new ZooKeeperClient(zooKeepers, "Welcome1");
                    bind(zkc);

                    Timer timer = new Timer();
                    NodeStatTimerTask stats = new NodeStatTimerTask(zkc);
                    // Need to run at least once before PlacementManager
                    timer.scheduleAtFixedRate(stats, 0, 30000);

                    ContainerStatTimerTask containerStats
                            = new ContainerStatTimerTask(zkc);
                    timer.schedule(containerStats, 0, 30000);

                    new NodeContainerWatcher(zkc);
                    new PlacementManager(zkc);
                    new PortsManager(zkc);

                    bind(ZooKeeperPersistenceManager.class)
                            .to(PersistenceManager.class);

                } catch (IOException |
                        KeeperException |
                        JAXBException |
                        GeneralSecurityException |
                        InterruptedException ex) {
                    LOG.error(ex.toString());
                    System.err.println(ex.toString());
                    System.exit(1);
                }
            }

        }


    }

}
