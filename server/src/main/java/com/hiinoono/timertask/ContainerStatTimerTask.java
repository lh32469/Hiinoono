package com.hiinoono.os.container;

import com.hiinoono.jaxb.Container;
import com.hiinoono.jaxb.Node;
import com.hiinoono.jaxb.State;
import com.hiinoono.jaxb.Tenant;
import com.hiinoono.os.ShellCommand;
import com.hiinoono.persistence.zk.ZKUtils;
import com.hiinoono.persistence.zk.ZooKeeperClient;
import static com.hiinoono.persistence.zk.ZooKeeperConstants.ACL;
import static com.hiinoono.persistence.zk.ZooKeeperConstants.STATS;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.slf4j.LoggerFactory;


/**
 * Updates ZooKeeper with statistics for containers assigned to the current
 * node.
 *
 * @author Lyle T Harris
 */
public class ContainerStatTimerTask extends TimerTask {

    static final Pattern STATE
            = Pattern.compile("State:\\s+(.*)");

    static final Pattern CPU_USAGE
            = Pattern.compile("CPU use:\\s+(.*)");

    static final Pattern MEMORY_USAGE
            = Pattern.compile("Memory use:\\s+(.*)");

    static final Pattern BLKIO_USAGE
            = Pattern.compile("BlkIO use:\\s+(.*)");

    static final Pattern TX_BYTES
            = Pattern.compile("TX bytes:\\s+(.*)");

    static final Pattern RX_BYTES
            = Pattern.compile("RX bytes:\\s+(.*)");

    static final Pattern LINK
            = Pattern.compile("Link:\\s+(.*)");

    private final ZooKeeperClient zooKeeperClient;

    private final static org.slf4j.Logger LOG
            = LoggerFactory.getLogger(ContainerStatTimerTask.class);


    public ContainerStatTimerTask() {
        this.zooKeeperClient = null;
    }


    public ContainerStatTimerTask(ZooKeeperClient zooKeeperClient) throws
            KeeperException, InterruptedException,
            JAXBException, GeneralSecurityException {

        this.zooKeeperClient = zooKeeperClient;

        ZooKeeper zk = zooKeeperClient.getZookeeper();

        if (zk.exists(STATS, null) == null) {
            LOG.info("Creating: " + STATS);
            zk.create(STATS, "Initialized".getBytes(),
                    ACL, CreateMode.PERSISTENT);
        }

    }


    @Override
    public void run() {

        ZooKeeper zk = zooKeeperClient.getZookeeper();
        String lsContainers;

        boolean mock = System.getProperty("MOCK") != null;
        final String mockDir = "src/test/resources/containers";

        try {

            if (mock) {
                String cmd = "ls " + mockDir;
                lsContainers = new ShellCommand(cmd).execute();
                LOG.info(lsContainers);
            } else {
                lsContainers = new ShellCommand("lxc-ls").execute();
            }

            lsContainers = lsContainers.trim();
            
            for (String name : lsContainers.split("\\s+")) {
                
                if(name.trim().isEmpty()) {
                    LOG.info("Empty [" + name + "]");
                    continue;
                }
                
                LOG.info("[" + name + "]");

                Container container = new Container();
                container.setName(name);

                String info;

                if (mock) {
                    Path path = Paths.get(mockDir, name);
                    byte[] bytes = Files.readAllBytes(path);
                    info = new String(bytes);
                    LOG.info(info);

                } else {
                    String cmd = "lxc-info -n " + name + " -H";
                    info = new ShellCommand(cmd).execute();
                   // LOG.info(info);
                }

                Matcher m = STATE.matcher(info);
                while (m.find()) {
                    container.setState(State.valueOf(m.group(1)));
                }

                m = CPU_USAGE.matcher(info);
                while (m.find()) {
                    container.setCpuUsage(Long.parseLong(m.group(1)));
                }

                m = BLKIO_USAGE.matcher(info);
                while (m.find()) {
                    container.setBlkIO(Long.parseLong(m.group(1)));
                }

                m = MEMORY_USAGE.matcher(info);
                while (m.find()) {
                    container.setMemUsage(Long.parseLong(m.group(1)));
                }

                m = LINK.matcher(info);
                while (m.find()) {
                    container.setLink(m.group(1));
                }

                m = TX_BYTES.matcher(info);
                while (m.find()) {
                    container.setTxBytes(Long.parseLong(m.group(1)));
                }

                m = RX_BYTES.matcher(info);
                while (m.find()) {
                    container.setRxBytes(Long.parseLong(m.group(1)));
                }

                final String path = STATS + "/" + name;

                if (zk.exists(path, null) == null) {
                    ZKUtils.saveEphemeral(zk, container, path);
                } else {
                    ZKUtils.updateData(zk, container, path);
                }

            }

        } catch (Exception ex) {
            LOG.error(ex.toString(), ex);
        }

    }


}
