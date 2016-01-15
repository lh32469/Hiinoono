package com.hiinoono.managers;

import com.hiinoono.Utils;
import com.hiinoono.persistence.zk.ZooKeeperClient;
import java.util.ArrayList;
import java.util.List;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.ACL;
import org.slf4j.LoggerFactory;


/**
 *
 * @author Lyle T Harris
 */
public class ContainerManager implements Watcher {

    public static final String CONTAINERS = "/containers";

    public static final String MGR_NODE = CONTAINERS + "/manager";

    /**
     * ACL to create nodes with.
     *
     * For private: Ids.CREATOR_ALL_ACL
     *
     * For development: Ids.OPEN_ACL_UNSAFE
     */
    private final ArrayList<ACL> acl = ZooDefs.Ids.OPEN_ACL_UNSAFE;

    /**
     * ZooKeeper (zk) cannot be a field in case of session timeouts so should be
     * fetched from ZooKeeperClient whenever needed. Session reconnecting is
     * handled in ZooKeeperClient.
     */
    private final ZooKeeperClient zooKeeperClient;

    final static private org.slf4j.Logger LOG
            = LoggerFactory.getLogger(ContainerManager.class);


    public ContainerManager(ZooKeeperClient zkc) throws
            InterruptedException, KeeperException {

        this.zooKeeperClient = zkc;
        LOG.info(this.toString());
        System.out.println("\n\n******** Constructing *********\n\n");
        System.out.println("ZK: " + zkc);

        ZooKeeper zk = zooKeeperClient.getZookeeper();
        if (zk.exists(CONTAINERS, null) == null) {
            LOG.info("Creating: " + CONTAINERS);
            zk.create(CONTAINERS, "Initialized".getBytes(),
                    acl, CreateMode.PERSISTENT);
        }

        takeover();
    }


    /**
     * Takeover as ContainerManager for the Cluster.
     *
     * @throws InterruptedException
     * @throws KeeperException
     */
    final void takeover() throws
            InterruptedException, KeeperException {

        final String nodeId = Utils.getNodeId();
        final ZooKeeper zk = zooKeeperClient.getZookeeper();

        try {
            // Try to become Manager
            zk.create(MGR_NODE,
                    nodeId.getBytes(), acl, CreateMode.EPHEMERAL);
            LOG.info(nodeId + " is ContainerManager");
            System.out.println("Current ContainerManager");
            zk.getChildren(CONTAINERS, this);
        } catch (KeeperException ex) {
            LOG.info(ex.getLocalizedMessage());
            byte[] containerMangerNode = zk.getData(MGR_NODE, this, null);
            LOG.info(new String(containerMangerNode) + " is ContainerManager");
            zk.exists(MGR_NODE, this);
        }
    }


    @Override
    public void process(WatchedEvent event) {
        System.out.println("Event: " + event);

        if (event.getType().equals(Event.EventType.NodeDeleted)
                && event.getPath().equals(MGR_NODE)) {
            System.out.println("Current Manager Quit");
            try {
                takeover();
            } catch (InterruptedException | KeeperException ex) {
                LOG.error("Unable to takeover as ContainerManager ("
                        + Utils.getNodeId() + ")", ex);
            }

        } else {
            final ZooKeeper zk = zooKeeperClient.getZookeeper();
            try {
                List<String> containers = zk.getChildren(event.getPath(), this);
                System.out.println(containers);
            } catch (KeeperException | InterruptedException ex) {
                LOG.error("Problem getting containers ("
                        + Utils.getNodeId() + ")", ex);
            }
        }

    }


    void processContainers() {

    }


}
