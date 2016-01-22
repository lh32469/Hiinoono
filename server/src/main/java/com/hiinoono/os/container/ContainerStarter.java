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
public class ContainerStarter extends HystrixCommand<Container> {

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

        transitionState = ContainerConstants.CONTAINERS
                + "/" + Utils.getNodeId()
                + ContainerConstants.TRANSITIONING
                + "/" + ContainerUtils.getZKname(container);
    }


    @Override
    protected Container run() throws Exception {
        LOG.info("Starting: " + container.getName());

        try {

            container.setState(State.STARTING);
            ZKUtils.savePersistent(zk, container, transitionState);

            // lxc name (cn-name.user.tenant)
            final String containerName = ContainerUtils.getZKname(container);

            if (System.getProperty("MOCK") == null) {

                new ShellCommand("lxc-start -n " + containerName).execute();

                // Wait for IP address
                String ip = "";

                while (ip == null || ip.isEmpty()) {
                    Thread.sleep(2000);
                    ip = new ShellCommand("lxc-info -i -H -n "
                            + containerName).execute();
                }

                container.setIpAddress(ip);

                /// Setup iptables for NAT
                // Get port
                String sequence = zk.create("/port",
                        containerName.getBytes(),
                        ZKUtils.acl,
                        CreateMode.PERSISTENT_SEQUENTIAL);

                LOG.info(sequence);

                int port = Integer.parseInt(sequence.replace("/port", ""));
                port += 12000; // Change this to offset property.

                String iptables = new ShellCommand("iptables -t nat "
                        + "-A PREROUTING -p tcp -i eth0 --dport "
                        + port + " -j DNAT --to-destination " + ip + ":22"
                ).execute();

                LOG.info(iptables);

                // Get external IP
                String addresses = new ShellCommand("hostname -I").execute();

                ip = addresses.split(" ")[0];

                container.setSsh("ssh -p " + port + " ubuntu@" + ip);
                container.setState(State.RUNNING);
                container.setLastStarted(Utils.now());

            } else {
                // Simulate starting
                Thread.sleep(15000);
                container.setState(State.RUNNING);
                container.setLastStarted(Utils.now());
            }

            // Delete transition state
            zk.delete(transitionState, -1);

            // Start and move to /containers/{nodeId}/running
            final String path = ContainerConstants.CONTAINERS
                    + "/" + Utils.getNodeId()
                    + ContainerConstants.RUNNING
                    + "/" + containerName;

            ZKUtils.savePersistent(zk, container, path);

            LOG.info("Started " + containerName);
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
