package com.hiinoono.os.container;

import com.hiinoono.Utils;
import com.hiinoono.jaxb.Container;
import com.hiinoono.jaxb.State;
import com.hiinoono.persistence.zk.ZKUtils;
import com.hiinoono.persistence.zk.ZooKeeperClient;
import static com.hiinoono.persistence.zk.ZooKeeperConstants.NODES;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import javax.xml.bind.JAXBException;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import static org.apache.zookeeper.Watcher.Event.EventType.NodeChildrenChanged;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.ACL;
import org.slf4j.LoggerFactory;


/**
 * Watches the paths associated with the Node for container related events.
 * Handles creating new containers, starting, stopping etc..
 *
 * @author Lyle T Harris
 */
public class NodeContainerWatcher implements Watcher, ContainerConstants {

    /**
     * ZK path associated with this Node in the /container path/dir.
     */
    private final String containerNodePath;

    /**
     * ACL to create nodes with.
     *
     * For private: Ids.CREATOR_ALL_ACL
     *
     * For development: Ids.OPEN_ACL_UNSAFE
     */
    private final static ArrayList<ACL> acl = ZooDefs.Ids.OPEN_ACL_UNSAFE;

    /**
     * ZooKeeper (zk) cannot be a field in case of session timeouts so should be
     * fetched from ZooKeeperClient whenever needed. Session reconnecting is
     * handled in ZooKeeperClient.
     */
    private final ZooKeeperClient zooKeeperClient;

    final static private org.slf4j.Logger LOG
            = LoggerFactory.getLogger(NodeContainerWatcher.class);


    public NodeContainerWatcher(ZooKeeperClient zkc) throws
            InterruptedException, KeeperException,
            IOException, JAXBException, GeneralSecurityException {

        this.zooKeeperClient = zkc;
        ZooKeeper zk = zooKeeperClient.getZookeeper();

        if (zk.exists(NODES, null) == null) {
            LOG.info("Creating: " + NODES);
            zk.create(NODES, "Initialized".getBytes(),
                    acl, CreateMode.PERSISTENT);
        }

        if (zk.exists(CONTAINERS, null) == null) {
            LOG.info("Creating: " + CONTAINERS);
            zk.create(CONTAINERS, "Initialized".getBytes(),
                    acl, CreateMode.PERSISTENT);
        }

        // Create path for this Node
        containerNodePath = CONTAINERS + "/" + Utils.getNodeId();
        if (zk.exists(containerNodePath, null) == null) {
            LOG.info("Creating: " + containerNodePath);
            zk.create(containerNodePath, "Initialized".getBytes(),
                    acl, CreateMode.PERSISTENT);
        }

        List<String> statePaths = new LinkedList<>();

        // Get subset of States for state paths
        for (State state : State.values()) {
            String value = state.toString();
            if (!value.contains("REQUESTED")) {
                statePaths.add("/" + value);
            }
        }

        // Add Transitioning "State"
        statePaths.add(ContainerConstants.TRANSITIONING);

        // Make sure the state path/dirs exist
        for (String state : statePaths) {
            final String path = containerNodePath + state;
            if (zk.exists(path, null) == null) {
                LOG.info("Creating: " + path);
                zk.create(path, "Initialized".getBytes(),
                        acl, CreateMode.PERSISTENT);
            }
        }

        // Start watching the state paths/dirs
        for (String state : statePaths) {
            zk.getChildren(containerNodePath + state, this);
        }

    }


    @Override
    public void process(WatchedEvent event) {
        LOG.debug(event.toString());

        final EventType type = event.getType();
        final String path = event.getPath();

        ZooKeeper zk = zooKeeperClient.getZookeeper();

        try {

            if (NodeChildrenChanged.equals(type)
                    && path.equals(containerNodePath + TRANSITIONING)) {
                transition(zk, path, event);
            } else {
                // Reset this Watcher
                zk.getChildren(path, this);
            }

        } catch (JAXBException |
                KeeperException |
                GeneralSecurityException |
                InterruptedException ex) {
            LOG.error(ex.toString(), ex);
        }

    }


    /**
     * Check the TRANSITIONING node path to see if there are any Containers that
     * need to be handled.
     */
    private void transition(ZooKeeper zk,
            final String path, WatchedEvent event) throws
            InterruptedException, KeeperException,
            GeneralSecurityException, JAXBException {

        // Get container names and reset this Watcher
        List<String> containers = zk.getChildren(path, this);
        LOG.debug("Containers: " + containers);

        for (String containerName : containers) {

            final String cPath = event.getPath() + "/" + containerName;
            Container container = ZKUtils.loadContainer(zk, cPath);

            // Get Desired State
            State state = container.getState();
            LOG.info(state + ": " + ContainerUtils.getZKname(container));
            if (state.equals(State.CREATE_REQUESTED)) {
                zk.delete(cPath, -1);
                new ContainerCreator(container, zk).queue();
            } else if (state.equals(State.START_REQUESTED)) {
                zk.delete(cPath, -1);
                new ContainerStarter(container, zk).queue();
            } else if (state.equals(State.STOP_REQUESTED)) {
                zk.delete(cPath, -1);
                new ContainerStopper(container, zk).queue();
            } else if (state.equals(State.DELETE_REQUESTED)) {
                zk.delete(cPath, -1);
                new ContainerDeleter(container, zk).queue();
            }

        }
    }


}
