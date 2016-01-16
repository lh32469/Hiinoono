package com.hiinoono.os.container;

import com.hiinoono.Utils;
import com.hiinoono.jaxb.Container;
import static com.hiinoono.os.container.ContainerConstants.CONTAINERS;
import static com.hiinoono.os.container.ContainerConstants.NEW;
import static com.hiinoono.os.container.ContainerConstants.STATES;
import com.hiinoono.persistence.zk.ZooKeeperClient;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
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

    private static JAXBContext jc;

    /**
     * ZK path associated with this Node.
     */
    private final String nodePath;

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
            = LoggerFactory.getLogger(NodeContainerWatcher.class);


    static {

        Class[] classes = {Container.class};

        try {
            Map<String, Object> properties = new HashMap<>();
            properties.put("eclipselink.media-type", "application/json");
            jc = JAXBContextFactory.createContext(classes, properties);

        } catch (JAXBException ex) {
            LOG.error(ex.getErrorCode(), ex);
        }

    }


    public NodeContainerWatcher(ZooKeeperClient zkc) throws
            InterruptedException, KeeperException {

        this.zooKeeperClient = zkc;
        ZooKeeper zk = zooKeeperClient.getZookeeper();

        this.nodePath = CONTAINERS + "/" + Utils.getNodeId();

        if (zk.exists(CONTAINERS, null) == null) {
            LOG.info("Creating: " + CONTAINERS);
            zk.create(CONTAINERS, "Initialized".getBytes(),
                    acl, CreateMode.PERSISTENT);
        }

        // Create path for this Node
        if (zk.exists(nodePath, null) == null) {
            LOG.info("Creating: " + nodePath);
            zk.create(nodePath, "Initialized".getBytes(),
                    acl, CreateMode.PERSISTENT);
        }

        // Make sure the state path/dirs exist
        for (String state : STATES) {
            final String path = nodePath + state;
            if (zk.exists(path, null) == null) {
                LOG.info("Creating: " + path);
                zk.create(path, "Initialized".getBytes(),
                        acl, CreateMode.PERSISTENT);
                zk.getChildren(path, this);
            }
        }

        // Start watching the state paths/dirs
        for (String state : STATES) {
            zk.getChildren(nodePath + state, this);
        }

    }


    @Override
    public void process(WatchedEvent event) {
        LOG.info(event.toString());
        System.out.println("Event: " + event);

        final EventType type = event.getType();
        final String path = event.getPath();

        try {
            if (EventType.NodeChildrenChanged.equals(type)) {
                // Reset Watcher
                zooKeeperClient.getZookeeper().getChildren(path, this);

                if (path.equals(nodePath + NEW)) {
                    // New -> Created or Error
                    createContainer(event);
                } else if (path.equals(nodePath + CREATED)) {
                    // Created -> Running or Error
                    startContainer(event);
                }

            }
        } catch (JAXBException |
                KeeperException |
                InterruptedException ex) {
            LOG.error(ex.getLocalizedMessage(), ex);
        }

    }


    void createContainer(WatchedEvent event) throws
            JAXBException, KeeperException, InterruptedException {

        final ZooKeeper zk = zooKeeperClient.getZookeeper();
        List<String> _containers = zk.getChildren(event.getPath(), null);

        for (String c : _containers) {
            Unmarshaller um = jc.createUnmarshaller();
            final String path = event.getPath() + "/" + c;
            byte[] data = zk.getData(path, false, null);
            String json = new String(data);
            Container container
                    = (Container) um.unmarshal(new StringReader(json));

            LOG.info(container.getName());
            System.out.println("Starting: " + container.getName());

            zk.delete(path, -1);

            ContainerCreator hystrix = new ContainerCreator(container, zk);
            hystrix.queue();
        }

    }


    void startContainer(WatchedEvent event) throws
            JAXBException, KeeperException, InterruptedException {

        final ZooKeeper zk = zooKeeperClient.getZookeeper();
        List<String> _containers = zk.getChildren(event.getPath(), null);

        for (String c : _containers) {
            Unmarshaller um = jc.createUnmarshaller();
            final String path = event.getPath() + "/" + c;
            byte[] data = zk.getData(path, false, null);
            String json = new String(data);
            Container container
                    = (Container) um.unmarshal(new StringReader(json));

            LOG.info(container.getName());
            System.out.println("Starting: " + container.getName());

            zk.delete(path, -1);

            ContainerStarter hystrix = new ContainerStarter(container, zk);
            hystrix.queue();
        }

    }


}
