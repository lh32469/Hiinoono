package com.hiinoono.persistence.zk;

import java.util.ArrayList;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;


/**
 * ZooKeeper path constants.
 *
 * @author Lyle T Harris
 */
public interface ZooKeeperConstants {

    /**
     * Top-level ZK path/dir for Node registration.
     */
    String NODES = "/nodes";

    /**
     * Top level path/dir for various cluster-wide managers to register
     * themselves. Need to find a better home for this at some point.
     */
    String MANAGERS = "/managers";

    /**
     * ACL to create nodes with.
     *
     * For private: Ids.CREATOR_ALL_ACL
     *
     * For development: Ids.OPEN_ACL_UNSAFE
     */
    ArrayList<ACL> ACL = ZooDefs.Ids.OPEN_ACL_UNSAFE;

}
