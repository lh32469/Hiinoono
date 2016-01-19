package com.hiinoono.os.container;

import com.hiinoono.Utils;
import com.hiinoono.jaxb.Container;
import com.hiinoono.jaxb.User;
import com.hiinoono.persistence.zk.ZKUtils;
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
public class ContainerUtils {

    final static private org.slf4j.Logger LOG
            = LoggerFactory.getLogger(ContainerUtils.class);


    /**
     * Get a ZooKeeper friendly, unique Container name made up of Tenant name
     * plus User name plus actual container name.
     *
     * @param container The Container to get name for.
     * @return ZK friendly name base on Container name and Owner.
     */
    public static String getZKname(Container container) {
        StringBuilder zkName = new StringBuilder();
        User owner = container.getOwner();
        zkName.append(container.getName());
        zkName.append(".");
        zkName.append(owner.getTenant().replaceAll(" ", "_"));
        zkName.append(".");
        zkName.append(owner.getName().replaceAll(" ", "_"));
        return zkName.toString();
    }


}
