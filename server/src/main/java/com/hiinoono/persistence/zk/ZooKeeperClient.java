package com.hiinoono.persistence.zk;

import java.io.IOException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.LoggerFactory;


/**
 *
 * @author Lyle T Harris
 */
public class ZooKeeperClient implements Watcher {

    final static private org.slf4j.Logger LOG
            = LoggerFactory.getLogger(ZooKeeperClient.class);

    private final ZooKeeper zk;


    public ZooKeeperClient(String connectString) throws
            IOException {
        zk = new ZooKeeper(connectString, 60000, this);
    }


    public ZooKeeperClient(String connectString, String authInfo) throws
            IOException {
        zk = new ZooKeeper(connectString, 60000, this);
        zk.addAuthInfo("digest", authInfo.getBytes());
    }


    @Override
    public void process(WatchedEvent event) {
        LOG.info(event.toString());
    }


    public ZooKeeper getZookeeper() {
        return zk;
    }


}
