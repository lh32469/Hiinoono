package com.hiinoono.os.container;

import com.hiinoono.Utils;
import com.hiinoono.jaxb.Container;
import com.hiinoono.jaxb.State;
import com.hiinoono.os.ShellCommand;
import com.hiinoono.persistence.zk.ZKUtils;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommand.Setter;
import com.netflix.hystrix.HystrixCommandProperties;
import java.security.GeneralSecurityException;
import javax.xml.bind.JAXBException;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.LoggerFactory;


/**
 * HystrixCommand for the stopping of the Container specified in the
 * constructor.
 *
 * @author Lyle T Harris
 */
public class ContainerStopper extends HystrixCommand<Container> {

    private final Container container;

    private final ZooKeeper zk;

    private static final HystrixCommandProperties.Setter COMMAND_PROPS
            = HystrixCommandProperties.Setter()
            .withExecutionTimeoutInMilliseconds(60000);

    final static private org.slf4j.Logger LOG
            = LoggerFactory.getLogger(ContainerStarter.class);


    public ContainerStopper(Container container, ZooKeeper zk) {
        super(Setter
                .withGroupKey(ContainerConstants.GROUP_KEY)
                .andCommandPropertiesDefaults(COMMAND_PROPS));

        this.container = container;
        this.zk = zk;
    }


    @Override
    protected Container run() throws Exception {

        // lxc name (cn-name.user.tenant)
        final String containerName = ContainerUtils.getZKname(container);
        LOG.info("Stopping: " + containerName);

        try {

            container.setState(State.STOPPING);
            // Add to /containers/{nodeId}/STOPPING
            ZKUtils.saveToState(zk, container);

            if (System.getProperty("MOCK") == null) {
                new ShellCommand("lxc-stop -n " + containerName).execute();
            } else {
                LOG.info("Simulate stopping: " + containerName);
                Thread.sleep(5000);
            }

            // Delete Stopping state
            ZKUtils.deleteCurrentState(zk, container);

            container.setState(State.STOPPED);
            container.setLastStopped(Utils.now());
            ZKUtils.saveToState(zk, container);

            LOG.info("Stopped " + containerName);
            return container;

        } catch (Exception ex) {
            LOG.error(ex.toString(), ex);
            throw ex;
        }

    }


    @Override
    protected Container getFallback() {

        LOG.error("Error Stopping: " + container.getName());

        try {

            // Move to /containers/{nodeId}/ERROR
            container.setState(State.ERROR);
            ZKUtils.saveToState(zk, container);

        } catch (JAXBException | KeeperException |
                GeneralSecurityException | InterruptedException ex) {
            LOG.error(ex.toString(), ex);
        }

        return container;
    }


}
