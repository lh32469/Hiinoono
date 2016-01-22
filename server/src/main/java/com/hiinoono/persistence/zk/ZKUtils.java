package com.hiinoono.persistence.zk;

import com.hiinoono.Utils;
import com.hiinoono.jaxb.Container;
import com.hiinoono.jaxb.Node;
import com.hiinoono.jaxb.Tenant;
import com.hiinoono.os.container.ContainerConstants;
import com.hiinoono.os.container.ContainerUtils;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.ACL;
import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.slf4j.LoggerFactory;


/**
 *
 * @author Lyle T Harris
 */
public class ZKUtils {

    private static JAXBContext jc;

    /**
     * ACL to create nodes with.
     *
     * For private: Ids.CREATOR_ALL_ACL
     *
     * For development: Ids.OPEN_ACL_UNSAFE
     */
    public final static ArrayList<ACL> acl = ZooDefs.Ids.OPEN_ACL_UNSAFE;

    final static private org.slf4j.Logger LOG
            = LoggerFactory.getLogger(ContainerUtils.class);


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

        LOG.info(path);

        Unmarshaller um = jc.createUnmarshaller();
        String json = new String(Utils.decrypt2(zk.getData(path, false, null)));
        LOG.debug(json);
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
        zk.create(path, Utils.encrypt2(mem.toByteArray()),
                acl, CreateMode.PERSISTENT);
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
        zk.create(path, Utils.encrypt2(mem.toByteArray()),
                acl, CreateMode.EPHEMERAL);
    }


    /**
     * Store the Container in the ContainerConstants.TRANSITIONING path for
     * the Node it is currently assigned to.
     */
    public static void saveToTransitioning(ZooKeeper zk, Container container)
            throws JAXBException, KeeperException,
            InterruptedException, GeneralSecurityException {

        final String transition = ContainerConstants.CONTAINERS
                + "/" + container.getNode().getId()
                + ContainerConstants.TRANSITIONING
                + "/" + ContainerUtils.getZKname(container);

        ZKUtils.savePersistent(zk, container, transition);
    }


}
