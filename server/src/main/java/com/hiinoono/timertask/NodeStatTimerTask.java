package com.hiinoono.timertask;

import com.hiinoono.Utils;
import com.hiinoono.jaxb.Node;
import com.hiinoono.os.ShellCommand;
import com.hiinoono.persistence.zk.ZKUtils;
import com.hiinoono.persistence.zk.ZooKeeperClient;
import static com.hiinoono.persistence.zk.ZooKeeperConstants.ACL;
import static com.hiinoono.persistence.zk.ZooKeeperConstants.NODES;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.Scanner;
import java.util.TimerTask;
import javax.xml.bind.JAXBException;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.LoggerFactory;


/**
 * Updates ZooKeeper with statistics for current node.
 *
 * @author Lyle T Harris
 */
public class NodeStatTimerTask extends TimerTask {

    private final ZooKeeperClient zooKeeperClient;

    private final static Path MEM_INFO = Paths.get("/proc/meminfo");

    private final Node node;

    /**
     * ZK path to node to update with statistics for this H-Node.
     */
    private final String nodeStatusPath;

    private final static org.slf4j.Logger LOG
            = LoggerFactory.getLogger(NodeStatTimerTask.class);


    public NodeStatTimerTask(ZooKeeperClient zooKeeperClient) throws
            KeeperException, InterruptedException,
            JAXBException, GeneralSecurityException {

        this.zooKeeperClient = zooKeeperClient;

        ZooKeeper zk = zooKeeperClient.getZookeeper();

        if (zk.exists(NODES, null) == null) {
            LOG.info("Creating: " + NODES);
            zk.create(NODES, "Initialized".getBytes(),
                    ACL, CreateMode.PERSISTENT);
        }

        node = new Node();
        node.setId(Utils.getNodeId());
        node.setJoined(Utils.now());
        ShellCommand hostname = new ShellCommand("hostname");
        node.setHostname(hostname.execute());

        nodeStatusPath = NODES + "/" + Utils.getNodeId();
        LOG.info("Started: " + nodeStatusPath);

        if (zk.exists(nodeStatusPath, false) == null) {
            ZKUtils.saveEphemeral(zk, node, nodeStatusPath);
        } else {
            ZKUtils.updateData(zk, node, nodeStatusPath);
        }

    }


    @Override
    public void run() {

        try {

            ZooKeeper zk = zooKeeperClient.getZookeeper();

            if (Files.exists(MEM_INFO, LinkOption.NOFOLLOW_LINKS)) {

                for (String line : Files.readAllLines(MEM_INFO)) {
                    LOG.trace(line);
                    if (line.startsWith("MemTotal")) {
                        Scanner sc = new Scanner(line);
                        sc.next();
                        node.setMemTotal(sc.nextLong());
                    } else if (line.startsWith("MemAvailable")) {
                        Scanner sc = new Scanner(line);
                        sc.next();
                        node.setMemAvailable(sc.nextLong());
                    } else if (line.startsWith("SwapTotal")) {
                        Scanner sc = new Scanner(line);
                        sc.next();
                        node.setSwapTotal(sc.nextLong());
                    } else if (line.startsWith("SwapFree")) {
                        Scanner sc = new Scanner(line);
                        sc.next();
                        node.setSwapFree(sc.nextLong());
                    }
                }

            } else {
                LOG.info("No memory stats, " + MEM_INFO + " not found");
            }

            if (System.getProperty("MOCK") == null) {

                // Get the available space in the hiinoono volume group
                String cmnd = "vgs -o vg_free --units M --noheadings hiinoono";
                ShellCommand command = new ShellCommand(cmnd);
                String vgFree = command.execute();
                // Strip off trailing 'M'
                vgFree = vgFree.replaceAll("M", "");
                node.setVgFree(Float.parseFloat(vgFree));

                // TODO: get what managers are running on this Node.
                // TODO: get containers assigned to this Node and 
                // Update  node.setMemAllocated(value) with the
                // total allocate to each container.
            }

            node.setUpdated(Utils.now());

            if (zk.exists(nodeStatusPath, false) == null) {
                ZKUtils.saveEphemeral(zk, node, nodeStatusPath);
            } else {
                ZKUtils.updateData(zk, node, nodeStatusPath);
            }

        } catch (Exception ex) {
            LOG.error(ex.toString(), ex);
        }

    }


}
