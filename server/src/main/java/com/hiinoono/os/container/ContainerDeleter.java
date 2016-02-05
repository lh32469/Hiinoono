package com.hiinoono.os.container;

import com.hiinoono.jaxb.Container;
import com.hiinoono.jaxb.State;
import com.hiinoono.os.ShellCommand;
import com.hiinoono.persistence.zk.ZKUtils;
import com.hiinoono.persistence.zk.ZooKeeperConstants;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommand.Setter;
import com.netflix.hystrix.HystrixCommandProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import javax.xml.bind.JAXBException;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.LoggerFactory;


/**
 * HystrixCommand for the starting of the Container specified in the
 * constructor.
 *
 * @author Lyle T Harris
 */
public class ContainerDeleter extends HystrixCommand<Container> {

    private final Container container;

    private final ZooKeeper zk;

    private static final HystrixCommandProperties.Setter COMMAND_PROPS
            = HystrixCommandProperties.Setter()
            .withExecutionTimeoutInMilliseconds(60000);

    final static private org.slf4j.Logger LOG
            = LoggerFactory.getLogger(ContainerStarter.class);


    public ContainerDeleter(Container container, ZooKeeper zk) {
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
        LOG.info("Deleting: " + containerName);

        try {

            container.setState(State.DELETING);
            // Add to /containers/{nodeId}/DELETING
            ZKUtils.saveToState(zk, container);

            if (System.getProperty("MOCK") == null) {

                new ShellCommand("lxc-destroy -n " + containerName).execute();

                String ip = container.getIpAddress();

                // Cleanup iptables port forwarding entries for this Container.
                for (String pair : container.getPortForwardingPairs()) {
                    String[] array = pair.split(":");

                    String iptables = new ShellCommand("iptables -t nat "
                            + "-D PREROUTING -p tcp -i eth0 --dport "
                            + array[0] + " -j DNAT --to-destination "
                            + ip + ":" + array[1]
                    ).execute();

                    LOG.debug("iptables output: " + iptables);
                }

                // Update rules for iptables-persistent
                String stdout = new ShellCommand("iptables-save").execute();
                Path rules = Paths.get("/etc/iptables/rules.v4");
                Files.write(rules, stdout.getBytes());

                final String statsPath = ZooKeeperConstants.STATS
                        + "/" + containerName;

                if (zk.exists(statsPath, false) != null) {
                    zk.delete(statsPath, -1);
                }

                // Cleanup install log.
                ZKUtils.deleteInstallLog(zk, container);

            } else {
                LOG.info("Simulate deleting: " + containerName);
                Thread.sleep(5000);
            }

            // Delete current state
            ZKUtils.deleteCurrentState(zk, container);

            LOG.info("Deleted " + containerName);
            return container;

        } catch (Exception ex) {
            LOG.error(ex.toString(), ex);
            throw ex;
        }

    }


    @Override
    protected Container getFallback() {

        // lxc name (cn-name.user.tenant)
        final String containerName = ContainerUtils.getZKname(container);

        LOG.error("Error Deleting: " + containerName);

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
