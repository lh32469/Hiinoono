package com.hiinoono.rest.node;

import com.hiinoono.Utils;
import com.hiinoono.jaxb.Node;
import com.hiinoono.os.ShellCommand;
import com.hiinoono.persistence.zk.ZKUtils;
import com.hiinoono.persistence.zk.ZooKeeperClient;
import static com.hiinoono.persistence.zk.ZooKeeperConstants.NODES;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.TimerTask;
import javax.xml.bind.JAXBException;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.ACL;
import org.slf4j.LoggerFactory;


/**
 * Updates ZooKeeper with statistics for current node.
 *
 * @author Lyle T Harris
 */
public class NodeStatTimerTask extends TimerTask {

    private final ZooKeeperClient zooKeeperClient;

    private final static Path MEM_INFO = Paths.get("/proc/meminfo");

    private final static ArrayList<ACL> acl = ZooDefs.Ids.OPEN_ACL_UNSAFE;

    private final static org.slf4j.Logger LOG
            = LoggerFactory.getLogger(NodeStatTimerTask.class);


    public NodeStatTimerTask() {
        this.zooKeeperClient = null;
    }


    public NodeStatTimerTask(ZooKeeperClient zooKeeperClient) throws
            KeeperException, InterruptedException,
            JAXBException, GeneralSecurityException {

        this.zooKeeperClient = zooKeeperClient;

        ZooKeeper zk = zooKeeperClient.getZookeeper();

        if (zk.exists(NODES, null) == null) {
            LOG.info("Creating: " + NODES);
            zk.create(NODES, "Initialized".getBytes(),
                    acl, CreateMode.PERSISTENT);
        }

        Node node = new Node();
        node.setId(Utils.getNodeId());
        node.setJoined(Utils.now());
        ShellCommand hostname = new ShellCommand("hostname");
        node.setHostname(hostname.execute());

        final String nodeStatus = NODES + "/" + Utils.getNodeId();
        if (zk.exists(nodeStatus, null) != null) {
            zk.delete(nodeStatus, -1);
        }

        LOG.info("Started: " + nodeStatus);
        ZKUtils.saveEphemeral(zk, node, nodeStatus);
    }


    @Override
    public void run() {

        try {

            ZooKeeper zk = zooKeeperClient.getZookeeper();

            final String nodeStatus = NODES + "/" + Utils.getNodeId();
            Node node = ZKUtils.loadNode(zk, nodeStatus);
            node.setUpdated(Utils.now());

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

            ZKUtils.updateData(zk, node, nodeStatus);

        } catch (Exception ex) {
            LOG.error(ex.toString(), ex);
        }

    }


}