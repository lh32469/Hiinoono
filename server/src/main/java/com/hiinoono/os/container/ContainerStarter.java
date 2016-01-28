package com.hiinoono.os.container;

import com.hiinoono.Utils;
import com.hiinoono.jaxb.Container;
import com.hiinoono.jaxb.State;
import com.hiinoono.os.ShellCommand;
import com.hiinoono.persistence.zk.ZKUtils;
import com.hiinoono.persistence.zk.ZooKeeperConstants;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommand.Setter;
import com.netflix.hystrix.HystrixCommandProperties;
import java.security.GeneralSecurityException;
import java.util.LinkedList;
import javax.xml.bind.JAXBException;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.LoggerFactory;


/**
 * HystrixCommand for the starting of the Container specified in the
 * constructor.
 *
 * @author Lyle T Harris
 */
public class ContainerStarter extends HystrixCommand<Container> {

    private final Container container;

    private final ZooKeeper zk;

    private static final HystrixCommandProperties.Setter COMMAND_PROPS
            = HystrixCommandProperties.Setter()
            .withExecutionTimeoutInMilliseconds(60000);

    final static private org.slf4j.Logger LOG
            = LoggerFactory.getLogger(ContainerStarter.class);


    public ContainerStarter(Container container, ZooKeeper zk) {
        super(Setter
                .withGroupKey(ContainerConstants.GROUP_KEY)
                .andCommandPropertiesDefaults(COMMAND_PROPS));

        this.container = container;
        this.zk = zk;
    }


    @Override
    protected Container run() throws Exception {
        LOG.info("Starting: " + container.getName());

        // lxc name (cn-name.user.tenant)
        final String containerName = ContainerUtils.getZKname(container);

        try {

            container.setState(State.STARTING);
            // Add to /containers/{nodeId}/STARTING
            ZKUtils.saveToState(zk, container);

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
                        ZooKeeperConstants.ACL,
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

                String externalIP = addresses.split(" ")[0];

                container.setSsh("ssh -p " + port + " ubuntu@" + externalIP);
                container.getPortForwardingPairs().add(port + ":" + 22);

                LinkedList<String> command = new LinkedList<>();
                command.add("lxc-cgroup");
                command.add("-n");
                command.add(containerName);
                command.add("memory.limit_in_bytes");
                command.add("1024M");
                new ShellCommand(command).queue();

                command = new LinkedList<>();
                command.add("lxc-cgroup");
                command.add("-n");
                command.add(containerName);
                command.add("cpuset.cpus");
                command.add("0");
                new ShellCommand(command).queue();

            } else {
                LOG.info("Simulate starting: " + containerName);
                Thread.sleep(5000);
            }

            // Delete Starting state
            ZKUtils.deleteCurrentState(zk, container);

            // Move to /containers/{nodeId}/RUNNING
            container.setState(State.RUNNING);
            container.setLastStarted(Utils.now());
            ZKUtils.saveToState(zk, container);

            LOG.info("Started " + containerName);
            return container;

        } catch (Exception ex) {
            LOG.error(ex.toString(), ex);
            throw ex;
        }

    }


    @Override
    protected Container getFallback() {

        LOG.error("Error Creating: " + container.getName());

        try {

            // Delete Starting state
            ZKUtils.deleteCurrentState(zk, container);

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
