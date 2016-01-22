package com.hiinoono.os.container;

import com.hiinoono.Utils;
import com.hiinoono.jaxb.Container;
import com.hiinoono.jaxb.State;
import com.hiinoono.os.ShellCommand;
import com.hiinoono.persistence.zk.ZKUtils;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommand.Setter;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandProperties;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.slf4j.LoggerFactory;


/**
 *
 * @author Lyle T Harris
 */
public class ContainerStopper extends HystrixCommand<Container> {

    private final Container container;

    private final ZooKeeper zk;

    /**
     * Path to transition state for this Container.
     */
    private final String transitionState;

    private static final HystrixCommandGroupKey GROUP_KEY
            = HystrixCommandGroupKey.Factory.asKey("Container");

    private static final HystrixCommandProperties.Setter COMMAND_PROPS
            = HystrixCommandProperties.Setter()
            .withExecutionTimeoutInMilliseconds(60000);

    final static private org.slf4j.Logger LOG
            = LoggerFactory.getLogger(ContainerStarter.class);


    public ContainerStopper(Container container, ZooKeeper zk) {
        super(Setter
                .withGroupKey(GROUP_KEY)
                .andCommandPropertiesDefaults(COMMAND_PROPS));

        this.container = container;
        this.zk = zk;

        transitionState = ContainerConstants.CONTAINERS
                + "/" + Utils.getNodeId()
                + ContainerConstants.TRANSITIONING
                + "/" + ContainerUtils.getZKname(container);
    }


    @Override
    protected Container run() throws Exception {

        // lxc name (cn-name.user.tenant)
        final String containerName = ContainerUtils.getZKname(container);
        LOG.info("Stopping: " + containerName);

        try {

            container.setState(State.STOPPING);
           
            if (System.getProperty("MOCK") == null) {

                new ShellCommand("lxc-stop -n " + containerName).execute();

            } else {
                // Simulate stopping
                LOG.info("Simulated stopping...");
                Thread.sleep(5000);
            }

            container.setState(State.STOPPED);
            container.setLastStopped(Utils.now());

            // Start and move to /containers/{nodeId}/stopped
            final String stoppedPath = ContainerConstants.CONTAINERS
                    + "/" + Utils.getNodeId()
                    + ContainerConstants.STOPPED
                    + "/" + containerName;

            ZKUtils.savePersistent(zk, container, stoppedPath);

            LOG.info("Stopped " + containerName);
            return container;

        } catch (Exception ex) {
            LOG.error(ex.toString());
            throw ex;
        }

    }


    @Override
    protected Container getFallback() {

        LOG.error("Error Stopping: " + container.getName());

        container.setState(State.ERROR);
        // Move to /containers/{nodeId}/errors
        final String errorPath = ContainerConstants.CONTAINERS
                + "/" + Utils.getNodeId()
                + ContainerConstants.ERRORS
                + "/" + ContainerUtils.getZKname(container);

        try {

            ZKUtils.savePersistent(zk, container, errorPath);

        } catch (JAXBException | KeeperException |
                GeneralSecurityException | InterruptedException ex) {
            LOG.error(ex.toString(), ex);
        }

        return container;
    }


}
