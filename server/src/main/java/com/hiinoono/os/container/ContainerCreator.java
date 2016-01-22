package com.hiinoono.os.container;

import com.hiinoono.Utils;
import com.hiinoono.jaxb.Container;
import com.hiinoono.jaxb.State;
import com.hiinoono.os.ShellCommand;
import com.hiinoono.persistence.zk.ZKUtils;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandProperties;
import java.security.GeneralSecurityException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
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
                + "/" + ContainerUtils.getZKname(container);
    }


    @Override
    protected Container run() throws Exception {
        LOG.info("Creating: " + container.getName());

        // lxc name (cn-name.user.tenant)
        final String containerName = ContainerUtils.getZKname(container);

        try {

            container.setState(State.CREATING);
            // Add to /containers/{nodeId}/transition
            ZKUtils.savePersistent(zk, container, transitionState);

            if (System.getProperty("MOCK") == null) {

                List<String> command = new LinkedList<>();
                command.add("lxc-create");
                command.add("-t");
                command.add(container.getTemplate());
                command.add("-n");
                command.add(containerName);
                ShellCommand shell = new ShellCommand(command);
                LOG.info(shell.execute());

                container.setState(State.CREATED);

            } else {
                // Simulate creation
                Thread.sleep(15000);
                container.setState(State.CREATED);
            }

            // Delete transition state
            zk.delete(transitionState, -1);

            // Create and move to /containers/{nodeId}/created
            final String created = ContainerConstants.CONTAINERS
                    + "/" + Utils.getNodeId()
                    + ContainerConstants.CREATED
                    + "/" + containerName;

            ZKUtils.savePersistent(zk, container, created);

            LOG.info("Created " + containerName);
            return container;

        } catch (Exception ex) {
            LOG.error(ex.toString());
            throw ex;
        }
    }


    @Override
    protected Container getFallback() {

        LOG.error("Error Creating: " + container.getName());

        container.setState(State.ERROR);
        // Move to /containers/{nodeId}/errors
        final String path = ContainerConstants.CONTAINERS
                + "/" + Utils.getNodeId()
                + ContainerConstants.ERRORS
                + "/" + ContainerUtils.getZKname(container);

        try {

            // Delete transition state
            zk.delete(transitionState, -1);

            ZKUtils.savePersistent(zk, container, path);

        } catch (JAXBException | KeeperException |
                GeneralSecurityException | InterruptedException ex) {
            LOG.error(ex.toString(), ex);
        }

        return container;
    }


}
