package com.hiinoono.managers;

import com.hiinoono.Utils;
import com.hiinoono.persistence.zk.ZooKeeperClient;
import com.hiinoono.persistence.zk.ZooKeeperConstants;
import static com.hiinoono.persistence.zk.ZooKeeperConstants.ACL;
import java.security.GeneralSecurityException;
import java.util.logging.Logger;
import javax.xml.bind.JAXBException;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.LoggerFactory;


/**
 * Implements common Manager functionality.
 *
 * @author Lyle T Harris
 */
public abstract class Manager implements Watcher, ZooKeeperConstants {

    /**
     * ZooKeeper (zk) cannot be a field in case of session timeouts so should be
     * fetched from ZooKeeperClient whenever needed. Session reconnecting is
     * handled in ZooKeeperClient.
     */
    private final ZooKeeperClient zooKeeperClient;

    private final String managerName;

    private final String managerNodePath;

    final static private org.slf4j.Logger LOG
            = LoggerFactory.getLogger(Manager.class);


    public Manager(ZooKeeperClient zooKeeperClient) throws
            KeeperException, InterruptedException,
            JAXBException, GeneralSecurityException {

        this.zooKeeperClient = zooKeeperClient;
        this.managerName = this.getClass().getSimpleName();
        this.managerNodePath = MANAGERS + "/" + managerName;

        ZooKeeper zk = zooKeeperClient.getZookeeper();

        if (zk.exists(MANAGERS, null) == null) {
            LOG.info("Creating: " + MANAGERS);
            zk.create(MANAGERS, "Initialized".getBytes(),
                    ACL, CreateMode.PERSISTENT);
        }

        LOG.info(managerName);

        takeover();
    }


    /**
     * Takeover as Manager for this service for the Cluster.
     */
    private void takeover() throws
            InterruptedException, KeeperException,
            JAXBException, GeneralSecurityException {

        final String nodeId = Utils.getNodeId();
        final ZooKeeper zk = zooKeeperClient.getZookeeper();

        try {
            // Try to become Manager
            zk.create(managerNodePath,
                    nodeId.getBytes(), ACL, CreateMode.EPHEMERAL);
            String node = nodeId.split("-")[0] + "..";
            LOG.info(node + " (Me) is " + managerName);
            checkPath();

        } catch (KeeperException ex) {
            LOG.debug(ex.toString());
            byte[] containerMangerNode
                    = zk.getData(managerNodePath, this, null);
            String node = new String(containerMangerNode);
            node = node.split("-")[0] + "..";
            LOG.info(node + " is " + managerName);
            zk.exists(managerNodePath, this);
        }
    }


    @Override
    public void process(WatchedEvent event) {
        LOG.debug(event.toString());

        final Event.EventType type = event.getType();
        final String path = event.getPath();

        try {

            if (type.equals(Event.EventType.NodeDeleted)
                    && path.equals(managerNodePath)) {
                LOG.info("Current " + managerName + " Quit");
                takeover();

            } else if (Event.EventType.NodeChildrenChanged.equals(type)) {
                // Handle Event
                _checkPath();
            }

        } catch (JAXBException |
                KeeperException |
                GeneralSecurityException |
                InterruptedException ex) {
            LOG.error(ex.toString(), ex);
        }

    }


    /**
     * Finish processing one Event before handling any others.
     */
    private synchronized void _checkPath() throws
            KeeperException, InterruptedException,
            JAXBException, GeneralSecurityException {
        checkPath();
    }


    protected ZooKeeper getZooKeeper() {
        return this.zooKeeperClient.getZookeeper();
    }


    /**
     * Perform steps associated with this Manager.
     */
    public abstract void checkPath() throws
            KeeperException, InterruptedException,
            JAXBException, GeneralSecurityException;


}
