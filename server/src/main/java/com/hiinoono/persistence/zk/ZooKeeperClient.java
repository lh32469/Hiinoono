package com.hiinoono.persistence.zk;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
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

    private ZooKeeper zk;

    private final String connectString;

    private String authInfo;


    public ZooKeeperClient(String connectString) throws
            IOException {
        this.connectString = connectString;
        zk = new ZooKeeper(connectString, 60000, this);
    }


    public ZooKeeperClient(String connectString, String authInfo) throws
            IOException {
        this(connectString);
        if (authInfo != null) {
            this.authInfo = authInfo;
            zk.addAuthInfo("digest", authInfo.getBytes());
        }
    }


    @Override
    public void process(WatchedEvent event) {
        LOG.info(event.toString());
        if (event.getState().equals(Event.KeeperState.Expired)) {
            try {
                LOG.info("Reconnecting: " + connectString);
                zk = new ZooKeeper(connectString, 60000, this);
                if (this.authInfo != null) {
                    zk.addAuthInfo("digest", authInfo.getBytes());
                }
            } catch (IOException ex) {
                LOG.error(ex.getLocalizedMessage(), ex);
            }
        }
    }


    public ZooKeeper getZookeeper() {
        return zk;
    }


}
