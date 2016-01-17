package com.hiinoono.os.container;

import com.hiinoono.Utils;
import com.hiinoono.jaxb.Container;
import com.hiinoono.jaxb.State;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandProperties;
import java.util.ArrayList;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.ACL;
import org.slf4j.LoggerFactory;


/**
 *
 * @author Lyle T Harris
 */
public class ContainerCreator extends HystrixCommand<Container> {

    private static JAXBContext jc;

    private final Container container;

    private final ZooKeeper zk;

    /**
     * Path to transition state for this Container.
     */
    private final String transitionState;

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
            = LoggerFactory.getLogger(ContainerCreator.class);


    public ContainerCreator(Container container, ZooKeeper zk) {
        super(Setter
                .withGroupKey(GROUP_KEY)
                .andCommandPropertiesDefaults(COMMAND_PROPS));
        
        this.container = container;
        this.zk = zk;

        transitionState = ContainerConstants.CONTAINERS
                + "/" + Utils.getNodeId()
                + ContainerConstants.TRANSITIONING
                + "/" + container.getName();

    }


    @Override
    protected Container run() throws Exception {
        LOG.info("Creating: " + container.getName());

        container.setState(State.CREATING);
        // Add to /containers/{nodeId}/transition
        ContainerUtils.marshall(zk, container, transitionState);

        // Simulate creation
        Thread.sleep(15000);
        container.setState(State.CREATED);

        // Delete transition state
        zk.delete(transitionState, -1);

        // Create and move to /containers/{nodeId}/created
        final String created = ContainerConstants.CONTAINERS
                + "/" + Utils.getNodeId()
                + ContainerConstants.CREATED
                + "/" + container.getName();

        ContainerUtils.marshall(zk, container, created);

        LOG.info("Created " + container.getName());
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

            // Delete transition state
            zk.delete(transitionState, -1);

            ContainerUtils.marshall(zk, container, path);

        } catch (JAXBException | KeeperException | InterruptedException ex) {
            LOG.error(ex.getLocalizedMessage(), ex);
        }

        return container;
    }


}
