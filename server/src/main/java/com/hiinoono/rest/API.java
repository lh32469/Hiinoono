package com.hiinoono.rest;

import com.hiinoono.os.ContainerDriver;
import com.hiinoono.os.VirtualMachineDriver;
import com.hiinoono.os.container.NodeContainerWatcher;
import com.hiinoono.os.mock.MockContainerDriver;
import com.hiinoono.os.mock.MockVirtualMachineDriver;
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
import com.hiinoono.rest.site.SiteResource;
import com.hiinoono.rest.tenant.TenantResource;
import com.hiinoono.rest.user.UserResource;
import com.hiinoono.rest.vm.VirtualMachineResource;
import java.io.IOException;
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
        register(VirtualMachineResource.class);
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
        config.setIncludeRoot(false);
        register(config.resolver());

        register(new Binder());

        property(MessageProperties.XML_FORMAT_OUTPUT, true);
        property(MessageProperties.JAXB_PROCESS_XML_ROOT_ELEMENT, false);
    }


    class Binder extends AbstractBinder {

        @Override
        protected void configure() {

            String zooKeepers = System.getProperty("zooKeepers");

            // Only Mock availble currently
            bind(MockVirtualMachineDriver.class)
                    .to(VirtualMachineDriver.class);

            bind(MockContainerDriver.class)
                    .to(ContainerDriver.class);

            if (zooKeepers == null) {
                bind(new UnitTestPersistenceManager())
                        .to(PersistenceManager.class);
            } else {
                try {
                    ZooKeeperClient zkc
                            = new ZooKeeperClient(zooKeepers, "Welcome1");
                    bind(zkc);
                    bind(new NodeContainerWatcher(zkc));
                    bind(ZooKeeperPersistenceManager.class)
                            .to(PersistenceManager.class);
                } catch (IOException |
                        KeeperException |
                        InterruptedException ex) {
                    LOG.error(ex.getLocalizedMessage());
                    System.err.println(ex.getLocalizedMessage());
                    System.exit(1);
                }
            }

        }


    }

}
