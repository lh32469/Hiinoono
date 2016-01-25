package com.hiinoono.managers;

import com.hiinoono.Utils;
import com.hiinoono.jaxb.Container;
import com.hiinoono.os.container.ContainerConstants;
import com.hiinoono.os.container.ContainerCreator;
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
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.ACL;
import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.slf4j.LoggerFactory;


/**
 *
 * @author Lyle T Harris
 */
public class ContainerManager implements Watcher, ContainerConstants {

    private static JAXBContext jc;

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


    public ContainerManager(ZooKeeperClient zkc) throws
            InterruptedException, KeeperException {

        this.zooKeeperClient = zkc;
        ZooKeeper zk = zooKeeperClient.getZookeeper();

        if (zk.exists(CONTAINERS, null) == null) {
            LOG.info("Creating: " + CONTAINERS);
            zk.create(CONTAINERS, "Initialized".getBytes(),
                    acl, CreateMode.PERSISTENT);
        }
//
//        for (String state : STATES) {
//            if (zk.exists(state, null) == null) {
//                LOG.info("Creating: " + state);
//                zk.create(state, "Initialized".getBytes(),
//                        acl, CreateMode.PERSISTENT);
//            }
//        }

        // Should eventually move elsewhere
        if (zk.exists(MANAGERS, null) == null) {
            LOG.info("Creating: " + MANAGERS);
            zk.create(MANAGERS, "Initialized".getBytes(),
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
            LOG.info(nodeId + " (Me) is ContainerManager");
            System.out.println("Current ContainerManager");

//            // Watch the various states
//            for (String state : STATES) {
//                zk.getChildren(state, this);
//            }

        } catch (KeeperException ex) {
            LOG.info(ex.getLocalizedMessage());
            byte[] containerMangerNode = zk.getData(MGR_NODE, this, null);
            LOG.info(new String(containerMangerNode) + " is ContainerManager");
            zk.exists(MGR_NODE, this);
        }
    }


    @Override
    public void process(WatchedEvent event) {
        LOG.info(event.toString());
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

        } 
//        else if (event.getType().equals(EventType.NodeChildrenChanged)
//                && event.getPath().equals(NEW)) {
//            // New -> Created or Error
//            try {
//                startContainer(event);
//            } catch (JAXBException | KeeperException | InterruptedException ex) {
//                throw new IllegalStateException(ex.getLocalizedMessage());
//            }
//
//        }

    }


    void startContainer(WatchedEvent event) throws
            JAXBException, KeeperException, InterruptedException {

        final ZooKeeper zk = zooKeeperClient.getZookeeper();
        List<String> _containers = zk.getChildren(event.getPath(), this);

        for (String c : _containers) {
            Unmarshaller um = jc.createUnmarshaller();
            final String path = event.getPath() + "/" + c;
            byte[] data
                    = zk.getData(path, false, null);
            String json = new String(data);
            Container container
                    = (Container) um.unmarshal(new StringReader(json));

            LOG.info(container.getName());
            System.out.println("Starting: " + container.getName());

            zk.delete(path, -1);

            ContainerCreator cc = new ContainerCreator(container, zk);
            cc.queue();
        }

    }


//
//    private void processContainers(WatchedEvent event) {
//
//        final ZooKeeper zk = zooKeeperClient.getZookeeper();
//
//        try {
//
//            List<String> children = zk.getChildren(event.getPath(), this);
//            Set<String> latest = new HashSet<>(children);
//
//            if (latest.size() > containers.size()) {
//                // Added a Container
//                latest.removeAll(containers);
//                System.out.println("Added " + latest);
//            }
//
//            containers = new HashSet<>(children);
//            System.out.println(containers);
//        } catch (KeeperException | InterruptedException ex) {
//            LOG.error("Problem getting containers ("
//                    + Utils.getNodeId() + ")", ex);
//        }
//
//    }
}
