package com.hiinoono.os.container;

import com.hiinoono.Utils;
import com.hiinoono.jaxb.Container;
import com.hiinoono.jaxb.State;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandProperties;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.ACL;
import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.slf4j.LoggerFactory;


/**
 *
 * @author Lyle T Harris
 */
public class ContainerStarter extends HystrixCommand<Container> {

    private static JAXBContext jc;

    private final Container container;

    private final ZooKeeper zk;

    /**
     * ACL to create nodes with.
     *
     * For private: Ids.CREATOR_ALL_ACL
     *
     * For development: Ids.OPEN_ACL_UNSAFE
     */
    private final ArrayList<ACL> acl = ZooDefs.Ids.OPEN_ACL_UNSAFE;

    private static final HystrixCommandGroupKey GROUP_KEY
            = HystrixCommandGroupKey.Factory.asKey("Container");

    private static final HystrixCommandProperties.Setter COMMAND_PROPS
            = HystrixCommandProperties.Setter()
            .withExecutionTimeoutInMilliseconds(60000);

    final static private org.slf4j.Logger LOG
            = LoggerFactory.getLogger(ContainerStarter.class);


    static {

        Class[] classes = {Container.class};

        try {
            Map<String, Object> properties = new HashMap<>();
            properties.put("eclipselink.media-type", "application/json");
            jc = JAXBContextFactory.createContext(classes, properties);

        } catch (JAXBException ex) {
            LOG.error(ex.getErrorCode(), ex);
        }

    }


    public ContainerStarter(Container container, ZooKeeper zk) {
        super(Setter
                .withGroupKey(GROUP_KEY)
                .andCommandPropertiesDefaults(COMMAND_PROPS));
        this.container = container;
        this.zk = zk;
    }


    @Override
    protected Container run() throws Exception {
        LOG.info("Starting: " + container.getName());
        // Start and move to /containers/running
        Thread.sleep(15000);  // Simulate starting
        container.setState(State.RUNNING);
        container.setLastStarted(Utils.now());

        // Start and move to /containers/{nodeId}/running
        final String path = ContainerConstants.CONTAINERS
                + "/" + Utils.getNodeId()
                + ContainerConstants.RUNNING
                + "/" + container.getName();

        ContainerUtils.marshall(zk, container, path);

        LOG.info("Started " + container.getName());
        return container;
    }


    @Override
    protected Container getFallback() {

        LOG.error("Error Creating: " + container.getName());

        container.setState(State.ERROR);
        // Move to /containers/{nodeId}/errors
        final String path = ContainerConstants.CONTAINERS
                + "/" + Utils.getNodeId()
                + ContainerConstants.ERRORS
                + "/" + container.getName();

        try {
            ContainerUtils.marshall(zk, container, path);
        } catch (JAXBException | KeeperException | InterruptedException ex) {
            LOG.error(ex.getLocalizedMessage(), ex);
        }

        return container;
    }


}
