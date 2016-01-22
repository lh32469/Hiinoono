package com.hiinoono.os.container;

import com.hiinoono.jaxb.Container;
import com.hiinoono.jaxb.User;
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
     * name.user.tenant
     *
     * @param container The Container to get name for.
     * @return ZK friendly name base on Container name and Owner.
     */
    public static String getZKname(Container container) {
        StringBuilder zkName = new StringBuilder();
        User owner = container.getOwner();
        zkName.append(container.getName());
        zkName.append(".");
        zkName.append(owner.getName().replaceAll(" ", "_"));
        zkName.append(".");
        zkName.append(owner.getTenant().replaceAll(" ", "_"));

        return zkName.toString();
    }


}
