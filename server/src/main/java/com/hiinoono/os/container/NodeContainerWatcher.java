package com.hiinoono.os.container;

import com.hiinoono.Utils;
import com.hiinoono.jaxb.Container;
import com.hiinoono.jaxb.Node;
import com.hiinoono.os.ShellCommand;
import static com.hiinoono.os.container.ContainerConstants.CONTAINERS;
import static com.hiinoono.os.container.ContainerConstants.NEW;
import static com.hiinoono.os.container.ContainerConstants.STATES;
import com.hiinoono.persistence.zk.ZKUtils;
import com.hiinoono.persistence.zk.ZooKeeperClient;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.ZKUtil;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.ACL;
import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.slf4j.LoggerFactory;


/**
 * Watches the paths associated with the Node for container related events.
 * Handles creating new containers, starting, stopping etc..
 *
 * @author Lyle T Harris
 */
public class NodeContainerWatcher implements Watcher, ContainerConstants {

    /**
     * Top-level ZK path/dir for Node registration.
     */
    public static final String NODES = "/nodes";

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

        Node node = new Node();
        node.setId(Utils.getNodeId());
        node.setJoined(Utils.now());
        ShellCommand hostname = new ShellCommand("hostname");
        node.setHostname(hostname.execute());

        final String nodeStatus = NODES + "/" + Utils.getNodeId();
        if (zk.exists(nodeStatus, null) != null) {
            zk.delete(nodeStatus, -1);
        }
        ZKUtils.saveEphemeral(zk, node, nodeStatus);

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

        // Make sure the state path/dirs exist
        for (String state : STATES) {
            final String path = containerNodePath + state;
            if (zk.exists(path, null) == null) {
                LOG.info("Creating: " + path);
                zk.create(path, "Initialized".getBytes(),
                        acl, CreateMode.PERSISTENT);
            }
        }

        // Start watching the state paths/dirs
        for (String state : STATES) {
            zk.getChildren(containerNodePath + state, this);
        }

    }


    @Override
    public void process(WatchedEvent event) {
        LOG.debug(event.toString());

        final EventType type = event.getType();
        final String path = event.getPath();

        try {
            if (EventType.NodeChildrenChanged.equals(type)) {

                ZooKeeper zk = zooKeeperClient.getZookeeper();

                // Reset this Watcher
                List<String> containers = zk.getChildren(path, this);
                LOG.debug("Containers: " + containers);

                for (String c : containers) {

                    final String cPath = event.getPath() + "/" + c;
                    Container container = ZKUtils.loadContainer(zk, cPath);

                    if (path.equals(containerNodePath + NEW)) {
                        new ContainerCreator(container, zk).queue();
                        zk.delete(cPath, -1);
                    } else if (path.equals(containerNodePath + CREATED)) {
                        new ContainerStarter(container, zk).queue();
                        zk.delete(cPath, -1);
                    }

                }

            }

        } catch (JAXBException |
                KeeperException |
                GeneralSecurityException |
                InterruptedException ex) {
            LOG.error(ex.toString(), ex);
        }

    }


}
