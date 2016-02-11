package com.hiinoono.managers;

import com.hiinoono.persistence.zk.ZooKeeperClient;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import javax.xml.bind.JAXBException;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.LoggerFactory;


/**
 *
 * Manages the /ports ZooKeeper path by cleaning up old sequence nodes.
 *
 * @author Lyle T Harris
 */
public class PortsManager extends Manager {

    final static private org.slf4j.Logger LOG
            = LoggerFactory.getLogger(PortsManager.class);


    public PortsManager(ZooKeeperClient zooKeeperClient) throws
            InterruptedException, KeeperException,
            JAXBException, GeneralSecurityException {
        super(zooKeeperClient);
    }


    @Override
    public void checkPath() throws
            KeeperException, InterruptedException,
            JAXBException, GeneralSecurityException {

        final ZooKeeper zk = getZooKeeper();

        List<String> ports = zk.getChildren(PORTS, this);

        if (ports.isEmpty() || ports.size() < 30) {
            LOG.debug("Nothing to do.. (" + ports.size() + ")");
            return;
        }

        // Ports are sequential so once there are over 30, delete the 1st 15.
        Collections.sort(ports);

        for (int i = 0; i < 15; i++) {
            final String path = PORTS + "/" + ports.get(i);
            LOG.debug("Deleting: " + path);
            zk.delete(path, -1);
        }

    }


}
