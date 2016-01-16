package com.hiinoono.os.container;

import com.hiinoono.jaxb.Container;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
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
public class ContainerUtils {

    private static JAXBContext jc;

    /**
     * ACL to create nodes with.
     *
     * For private: Ids.CREATOR_ALL_ACL
     *
     * For development: Ids.OPEN_ACL_UNSAFE
     */
    private final static ArrayList<ACL> acl = ZooDefs.Ids.OPEN_ACL_UNSAFE;

    final static private org.slf4j.Logger LOG
            = LoggerFactory.getLogger(ContainerUtils.class);


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


    /**
     * Store the Container in the given path.
     *
     * @param c
     * @param path
     * @throws JAXBException
     */
    public static void marshall(ZooKeeper zk, Container c, String path) throws
            JAXBException, KeeperException, InterruptedException {

        ByteArrayOutputStream mem = new ByteArrayOutputStream();
        Marshaller m = jc.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        m.marshal(c, mem);
        zk.create(path, mem.toByteArray(),
                acl, CreateMode.PERSISTENT);
    }


}
