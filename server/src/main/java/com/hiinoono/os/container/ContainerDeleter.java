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
import javax.xml.bind.JAXBException;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.LoggerFactory;


/**
 *
 * @author Lyle T Harris
 */
public class ContainerDeleter extends HystrixCommand<Container> {

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


    public ContainerDeleter(Container container, ZooKeeper zk) {
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
        LOG.info("Deleting: " + containerName);

        try {

            container.setState(State.STOPPING);

            if (System.getProperty("MOCK") == null) {

                new ShellCommand("lxc-destroy -n " + containerName).execute();

                String ip = container.getIpAddress();

                // 
                for (String pair : container.getPortForwardingPairs()) {
                    String[] array = pair.split(":");

                    String iptables = new ShellCommand("iptables -t nat "
                            + "-D PREROUTING -p tcp -i eth0 --dport "
                            + array[0] + " -j DNAT --to-destination "
                            + ip + ":" + array[1]
                    ).execute();

                    LOG.info(iptables);
                }

            } else {
                // Simulate stopping
                LOG.info("Simulated deleting...");
                Thread.sleep(5000);
            }

            LOG.info("Deleted " + containerName);
            return container;

        } catch (Exception ex) {
            LOG.error(ex.toString());
            throw ex;
        }

    }


    @Override
    protected Container getFallback() {

        LOG.error("Error Deleting: " + container.getName());

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
