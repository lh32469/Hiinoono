package com.hiinoono.managers;

import com.hiinoono.Utils;
import com.hiinoono.jaxb.Container;
import com.hiinoono.jaxb.Node;
import com.hiinoono.os.container.ContainerUtils;
import com.hiinoono.persistence.zk.ZKUtils;
import com.hiinoono.persistence.zk.ZooKeeperClient;
import com.hiinoono.persistence.zk.ZooKeeperConstants;
import com.hiinoono.rest.node.NodeComparator;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import javax.xml.bind.JAXBException;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.LoggerFactory;


/**
 * Manages placement of Containers in the cluster. Only one instance of this is
 * active at any time.
 *
 * @author Lyle T Harris
 */
@Resource
public class PlacementManager implements Watcher, ZooKeeperConstants {

    /**
     * ZooKeeper Node watched for new child Containers that need to be placed.
     */
    public static final String TO_BE_PLACED_NODEPATH = "/placement";

    /**
     * ZooKeeper (zk) cannot be a field in case of session timeouts so should be
     * fetched from ZooKeeperClient whenever needed. Session reconnecting is
     * handled in ZooKeeperClient.
     */
    private final ZooKeeperClient zooKeeperClient;

    private final String managerName;

    private final String managerNodePath;

    final static private org.slf4j.Logger LOG
            = LoggerFactory.getLogger(PlacementManager.class);

    final static private org.slf4j.Logger CONTAINER_LOG
            = LoggerFactory.getLogger(ContainerUtils.class);


    public PlacementManager(ZooKeeperClient zkc) throws
            InterruptedException, KeeperException,
            JAXBException, GeneralSecurityException {

        this.zooKeeperClient = zkc;
        ZooKeeper zk = zooKeeperClient.getZookeeper();

        if (zk.exists(MANAGERS, null) == null) {
            LOG.info("Creating: " + MANAGERS);
            zk.create(MANAGERS, "Initialized".getBytes(),
                    ACL, CreateMode.PERSISTENT);
        }

        if (zk.exists(TO_BE_PLACED_NODEPATH, null) == null) {
            LOG.info("Creating: " + TO_BE_PLACED_NODEPATH);
            zk.create(TO_BE_PLACED_NODEPATH, "Initialized".getBytes(),
                    ACL, CreateMode.PERSISTENT);
        }

        managerName = getClass().getSimpleName();
        managerNodePath = MANAGERS + "/" + managerName;

        takeover();
    }


    /**
     * Takeover as PlacementManager for the Cluster.
     *
     * @throws InterruptedException
     * @throws KeeperException
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
            LOG.info(nodeId + " (Me) is " + managerName);
            checkPath();

        } catch (KeeperException ex) {
            LOG.info(ex.toString());
            byte[] containerMangerNode
                    = zk.getData(managerNodePath, this, null);
            LOG.info(new String(containerMangerNode) + " is " + managerName);
            zk.exists(managerNodePath, this);
        }
    }


    @Override
    public void process(WatchedEvent event) {
        LOG.info(event.toString());

        final EventType type = event.getType();
        final String path = event.getPath();

        try {

            if (type.equals(EventType.NodeDeleted)
                    && path.equals(managerNodePath)) {
                LOG.info("Current Manager Quit");
                takeover();

            } else if (EventType.NodeChildrenChanged.equals(type)) {
                // Container placement requested.
                checkPath();
            }

        } catch (JAXBException |
                KeeperException |
                GeneralSecurityException |
                InterruptedException ex) {
            LOG.error(ex.toString(), ex);
        }

    }


    /**
     * Checks the TO_BE_PLACED_NODEPATH to see if there are any Containers that
     * need to be place. Called on takeover() and when this instance is acting
     * manager and receives ZK event.
     *
     * @throws KeeperException
     * @throws InterruptedException
     */
    synchronized void checkPath() throws
            KeeperException, InterruptedException,
            JAXBException, GeneralSecurityException {

        final ZooKeeper zk = zooKeeperClient.getZookeeper();

        List<String> containerNames
                = zk.getChildren(TO_BE_PLACED_NODEPATH, this);

        if (containerNames.isEmpty()) {
            LOG.info("Nothing to place.");
            return;
        }

        // Get currently available Nodes and their statistics.
        List<Node> nodes
                = ZKUtils.getNodes(zk).collect(Collectors.toList());

        for (String containerName : containerNames) {
            final String cPath = TO_BE_PLACED_NODEPATH + "/" + containerName;
            Container container = ZKUtils.loadContainer(zk, cPath);
            zk.delete(cPath, -1);
            place(container, nodes);
        }

    }


    synchronized void place(Container container, List<Node> nodes) throws
            JAXBException, KeeperException,
            InterruptedException, GeneralSecurityException {

        LOG.info(ContainerUtils.getZKname(container));
        final ZooKeeper zk = zooKeeperClient.getZookeeper();

        // Sort the Nodes based on the best fit for this Container.
        // All things being equal, select at random.
        Collections.shuffle(nodes);
        Collections.sort(nodes, new NodeComparator(container));
        final Node assignedNode = nodes.get(0);
        CONTAINER_LOG.info(ContainerUtils.getZKname(container)
                + " => " + assignedNode.getHostname());
        container.setNodeId(assignedNode.getId());

        /*
         * Container is stored in ZK in the /containers/{nodeId}/transition for
         * the Node the container is assigned to and then NodeContainerWatcher
         * for that nodes picks it up and creates it.
         */
        ZKUtils.saveToTransitioning(zk, container);

        // Update in-memory Node with new assignment for next placement.
        assignedNode.getContainers().add(container);

    }


}
