package com.hiinoono.os.container;

import com.hiinoono.jaxb.Container;
import com.hiinoono.jaxb.State;
import com.hiinoono.os.ShellCommand;
import com.hiinoono.persistence.zk.ZKUtils;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandProperties;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import javax.xml.bind.JAXBException;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.LoggerFactory;


/**
 * HystrixCommand for the creation of the Container specified in the
 * constructor.
 *
 * @author Lyle T Harris
 */
public class ContainerCreator extends HystrixCommand<Container> {

    private final Container container;

    private final ZooKeeper zk;

    private static final HystrixCommandProperties.Setter COMMAND_PROPS
            = HystrixCommandProperties.Setter()
            .withExecutionTimeoutInMilliseconds(300000);

    final static private org.slf4j.Logger LOG
            = LoggerFactory.getLogger(ContainerCreator.class);


    public ContainerCreator(Container container, ZooKeeper zk) {
        super(Setter
                .withGroupKey(ContainerConstants.GROUP_KEY)
                .andCommandPropertiesDefaults(COMMAND_PROPS));

        this.container = container;
        this.zk = zk;
    }


    @Override
    protected Container run() throws Exception {
        LOG.info("Creating: " + container.getName());

        // lxc name (cn-name.user.tenant)
        final String containerName = ContainerUtils.getZKname(container);

        try {

            container.setState(State.CREATING);
            // Add to /containers/{nodeId}/CREATING
            ZKUtils.saveToState(zk, container);

            final Charset set = StandardCharsets.UTF_8;
            final Base64.Encoder encoder = Base64.getEncoder();

            if (System.getProperty("MOCK") == null) {

                // TODO: Create user and then container under that user.
                List<String> command = new LinkedList<>();
                command.add("lxc-create");
                command.add("-t");
                command.add(container.getTemplate().value());
                command.add("-n");
                command.add(containerName);
                command.add("-B");
                command.add("lvm");
                command.add("--vgname");
                command.add("hiinoono");
                command.add("--fssize");
                command.add("5G");
                ShellCommand shell = new ShellCommand(command);

                String result = shell.execute();

                if (result.equals(ShellCommand.ERROR)) {
                    LOG.error(command.toString());
                    String stdout = shell.getStdout();
                    String encoded
                            = encoder.encodeToString(stdout.getBytes(set));
                    ZKUtils.saveInstallLog(zk, container, encoded);
                    // So Hystrix will register failure.
                    throw new IllegalStateException(shell.getStdout());
                }

                String encoded = encoder.encodeToString(result.getBytes(set));
                ZKUtils.saveInstallLog(zk, container, encoded);

            } else {

                String log = "Simulate creating: " + containerName;
                String encoded = encoder.encodeToString(log.getBytes(set));
                ZKUtils.saveInstallLog(zk, container, encoded);
                LOG.info(log);
                Thread.sleep(5000);
            }

            // Delete Creating state
            ZKUtils.deleteCurrentState(zk, container);
            // Request a start
            container.setState(State.START_REQUESTED);
            ZKUtils.saveToTransitioning(zk, container);

            LOG.info("Created " + containerName);
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

            // Delete Creating state
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
