package com.hiinoono.os.container;

import com.netflix.hystrix.HystrixCommand;
import java.util.List;
import com.hiinoono.jaxb.Container;
import com.hiinoono.persistence.zk.ZKUtils;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixThreadPoolProperties;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.LinkedList;
import javax.xml.bind.JAXBException;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZKUtil;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.LoggerFactory;


/**
 * Get the Containers for a particular Node from ZooKeeper.
 *
 * @author Lyle T Harris
 */
public class GetContainersForNode extends HystrixCommand<List<Container>> {

    private final ZooKeeper zk;

    private final String path;

    private static final HystrixCommandGroupKey GROUP_KEY
            = HystrixCommandGroupKey.Factory.asKey("ZK-Persistence");

    /**
     * Monitor failures but don't currently open CircuitBreaker
     */
    private static final HystrixCommandProperties.Setter CB_DISABLED
            = HystrixCommandProperties.Setter()
            .withExecutionIsolationSemaphoreMaxConcurrentRequests(100)
            .withExecutionTimeoutInMilliseconds(5000)
            .withCircuitBreakerEnabled(false);

    private static final HystrixThreadPoolProperties.Setter THREAD_PROPERTIES
            = HystrixThreadPoolProperties.Setter()
            .withQueueSizeRejectionThreshold(10000)
            .withMaxQueueSize(10000);

    final static private org.slf4j.Logger LOG
            = LoggerFactory.getLogger(GetContainersForNode.class);


    /**
     * Get the Containers from ZooKeeper under the node path provided.
     *
     * @param zk
     * @param path ZK /container/{nodeId} path.
     *
     */
    public GetContainersForNode(ZooKeeper zk, String path) {
        super(Setter
                .withGroupKey(GROUP_KEY)
                .andThreadPoolPropertiesDefaults(THREAD_PROPERTIES)
                .andCommandPropertiesDefaults(CB_DISABLED)
        );

        this.zk = zk;
        this.path = path;

    }


    @Override
    protected List<Container> run() throws Exception {
        try {
            LOG.info(path);

            List<Container> containers = new LinkedList<>();
            List<String> states = zk.getChildren(path, null);

            List<String> tree = ZKUtil.listSubTreeBFS(zk, path);

            // Remove directory-only paths
            tree.remove(path);
            for (String state : states) {
                tree.remove(path + "/" + state);
            }

            for (String leaf : tree) {
                LOG.debug(leaf);
                Container container = ZKUtils.loadContainer(zk, leaf);
                containers.add(container);

                final String statFile = "/statistics/" +
                        ContainerUtils.getZKname(container);
                           
                if (zk.exists(statFile, false) != null) {
                    Container stats = ZKUtils.loadContainer(zk, statFile);
                    container.setCpuUsage(stats.getCpuUsage());
                    container.setMemUsage(stats.getMemUsage());
                    container.setBlkIO(stats.getBlkIO());
                    container.setLink(stats.getLink());
                    container.setTxBytes(stats.getTxBytes());
                    container.setRxBytes(stats.getRxBytes());
                    container.setCpuLimit(stats.getCpuLimit());
                }
            }

            return containers;

        } catch (KeeperException |
                InterruptedException |
                GeneralSecurityException |
                JAXBException ex) {
            LOG.error(ex.toString(), ex);
            throw ex;
        }
    }


    @Override
    protected List<Container> getFallback() {
        LOG.error("Failed");
        return Collections.EMPTY_LIST;
    }


}
