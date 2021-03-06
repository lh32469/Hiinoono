package com.hiinoono.persistence.zk;

import com.hiinoono.Utils;
import com.hiinoono.jaxb.Container;
import com.hiinoono.jaxb.Node;
import com.hiinoono.jaxb.Tenant;
import com.hiinoono.os.container.ContainerConstants;
import com.hiinoono.os.container.ContainerUtils;
import com.hiinoono.os.container.GetContainersForNode;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Stream;
import javax.ws.rs.NotAcceptableException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.slf4j.LoggerFactory;


/**
 *
 * @author Lyle T Harris
 */
public class ZKUtils implements ZooKeeperConstants {

    private static JAXBContext jc;

    final static private org.slf4j.Logger LOG
            = LoggerFactory.getLogger(ZKUtils.class);


    static {

        Class[] classes = {Container.class, Node.class, Tenant.class};

        try {
            Map<String, Object> properties = new HashMap<>();
            properties.put("eclipselink.media-type", "application/json");
            jc = JAXBContextFactory.createContext(classes, properties);

        } catch (JAXBException ex) {
            LOG.error(ex.getErrorCode(), ex);
        }

    }


    /**
     * Loads the Container from ZK at the path provided.
     */
    public static Container loadContainer(ZooKeeper zk, String path) throws
            JAXBException, KeeperException,
            InterruptedException, GeneralSecurityException {
        return (Container) load(zk, path);
    }


    /**
     * Loads the Node from ZK at the path provided.
     */
    public static Node loadNode(ZooKeeper zk, String path) throws
            JAXBException, KeeperException,
            InterruptedException, GeneralSecurityException {
        return (Node) load(zk, path);
    }


    /**
     * Loads the Tenant from ZK at the path provided.
     */
    public static Tenant loadTenant(ZooKeeper zk, String path) throws
            JAXBException, KeeperException,
            InterruptedException, GeneralSecurityException {
        return (Tenant) load(zk, path);
    }


    /**
     * Loads the Object from ZK at the path provided.
     */
    private static Object load(ZooKeeper zk, String path) throws
            JAXBException, KeeperException,
            InterruptedException, GeneralSecurityException {

        LOG.debug(path);

        Unmarshaller um = jc.createUnmarshaller();
        String json = new String(Utils.decrypt(zk.getData(path, false, null)));
        LOG.trace(json);
        return um.unmarshal(new StringReader(json));
    }


    /**
     * Store the Object in the given path in a Persistent mode.
     */
    public static void savePersistent(ZooKeeper zk, Object obj, String path)
            throws JAXBException, KeeperException,
            InterruptedException, GeneralSecurityException {

        ByteArrayOutputStream mem = new ByteArrayOutputStream();
        Marshaller m = jc.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        m.marshal(obj, mem);
        zk.create(path, Utils.encrypt(mem.toByteArray()),
                ACL, CreateMode.PERSISTENT);
    }


    /**
     * Store the Object in the given path in an Ephemeral mode.
     */
    public static void saveEphemeral(ZooKeeper zk, Object obj, String path)
            throws JAXBException, KeeperException,
            InterruptedException, GeneralSecurityException {

        ByteArrayOutputStream mem = new ByteArrayOutputStream();
        Marshaller m = jc.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        m.marshal(obj, mem);
        zk.create(path, Utils.encrypt(mem.toByteArray()),
                ACL, CreateMode.EPHEMERAL);
    }


    /**
     * Update the date in the given path with the Object provided.
     */
    public static void updateData(ZooKeeper zk, Object obj, String path)
            throws JAXBException, KeeperException,
            InterruptedException, GeneralSecurityException {

        ByteArrayOutputStream mem = new ByteArrayOutputStream();
        Marshaller m = jc.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        m.marshal(obj, mem);
        zk.setData(path, Utils.encrypt(mem.toByteArray()), -1);
    }


    /**
     * Store the Container in the ContainerConstants.TRANSITIONING path for the
     * Node it is currently assigned to.
     */
    public static void saveToTransitioning(ZooKeeper zk, Container container)
            throws JAXBException, KeeperException,
            InterruptedException, GeneralSecurityException {

        final String transition = ContainerConstants.CONTAINERS
                + "/" + container.getNodeId()
                + ContainerConstants.TRANSITIONING
                + "/" + ContainerUtils.getZKname(container);

        ZKUtils.savePersistent(zk, container, transition);
    }


    /**
     * Store the install log for the container.
     */
    public static void saveInstallLog(ZooKeeper zk, Container c, String log)
            throws JAXBException, KeeperException,
            InterruptedException, GeneralSecurityException {

        // Cleanup prior runs if any.
        deleteInstallLog(zk, c);

        final String logPath = ZooKeeperConstants.LOGS
                + "/" + ContainerUtils.getZKname(c)
                + "-install.log";

        zk.create(logPath, Utils.encrypt(log.getBytes()),
                ACL, CreateMode.PERSISTENT);
    }


    /**
     * Delete the install log for the container. Used for cleanup when deleting
     * container.
     */
    public static void deleteInstallLog(ZooKeeper zk, Container c)
            throws KeeperException, InterruptedException {

        final String logPath = ZooKeeperConstants.LOGS
                + "/" + ContainerUtils.getZKname(c)
                + "-install.log";

        if (zk.exists(logPath, false) != null) {
            zk.delete(logPath, -1);
        }

    }


    /**
     * Store the Container in the State path associated with the current State
     * of the Container for the Node it is currently assigned to. Example:
     * /containers/{nodeId}/STOPPED
     */
    public static void saveToState(ZooKeeper zk, Container container)
            throws JAXBException, KeeperException,
            InterruptedException, GeneralSecurityException {

        final String transition = ContainerConstants.CONTAINERS
                + "/" + container.getNodeId()
                + "/" + container.getState()
                + "/" + ContainerUtils.getZKname(container);

        ZKUtils.savePersistent(zk, container, transition);
    }


    /**
     * Delete the Container in the State path associated with the current State
     * of the Container for the Node it is currently assigned to. Example:
     * /containers/{nodeId}/STOPPED
     */
    public static void deleteCurrentState(ZooKeeper zk, Container container)
            throws JAXBException, KeeperException,
            InterruptedException, GeneralSecurityException {

        final String path = ContainerConstants.CONTAINERS
                + "/" + container.getNodeId()
                + "/" + container.getState()
                + "/" + ContainerUtils.getZKname(container);

        zk.delete(path, -1);
    }


    /**
     * Get Stream of all Nodes.
     *
     * @return
     */
    public static Stream<Node> getNodes(ZooKeeper zk) {

        try {

            List<Node> nodes = new LinkedList<>();
            List<String> nodeIds = zk.getChildren(NODES, false);
            LOG.info(nodeIds.toString());

            for (String nodeId : nodeIds) {

                // Submit request for Containers for this node.
                String path = ContainerConstants.CONTAINERS + "/" + nodeId;
                Future<List<Container>> future = 
                        new GetContainersForNode(zk, path).queue();
                // Load the Node info
                Node node = ZKUtils.loadNode(zk, NODES + "/" + nodeId);
                // Add all the containers for this Node
                node.getContainers().addAll(future.get());
                nodes.add(node);

            }

            return nodes.stream();

        } catch (KeeperException |
                JAXBException |
                GeneralSecurityException |
                ExecutionException |
                InterruptedException ex) {
            LOG.error(ex.toString(), ex);
            return Collections.EMPTY_LIST.stream();
        }
    }


    public static Stream<Container> getContainers(ZooKeeper zk) {

        List<Container> containers = new LinkedList<>();

        try {
            // Get list of Nodes holding Containers.
            List<String> nodes
                    = zk.getChildren(ContainerConstants.CONTAINERS, null);

            List<Future<List<Container>>> futures = new LinkedList<>();

            for (String node : nodes) {
                String path = ContainerConstants.CONTAINERS + "/" + node;
                futures.add(new GetContainersForNode(zk, path).queue());
            }

            for (Future<List<Container>> future : futures) {
                containers.addAll(future.get());
            }

        } catch (KeeperException |
                InterruptedException |
                ExecutionException ex) {
            LOG.error(ex.toString(), ex);
            throw new NotAcceptableException(ex.toString());
        }

        return containers.stream();
    }


}
