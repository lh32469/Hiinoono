package com.hiinoono.os.container;

import com.netflix.hystrix.HystrixCommandGroupKey;


/**
 * Specific constants associated with Container administration.
 *
 * @author Lyle T Harris
 */
public interface ContainerConstants {

    /**
     * Common GroupKey for Container related HystrixCommands.
     */
    HystrixCommandGroupKey GROUP_KEY
            = HystrixCommandGroupKey.Factory.asKey("Container");

    /**
     * Top level path/dir for Nodes to register and get Containers assigned.
     */
    String CONTAINERS = "/containers";

    /**
     * Sub path/dir for each node where containers are placed while in the
     * process of moving from one state to another. Total path is
     * /containers/{nodeId}/transition
     */
    String TRANSITIONING = "/transition";

    /**
     * Name of ZK Ephemeral node to indicate whom is the current, cluster-wide
     * ContainerManager.
     */
    String MGR_NODE = "/ContainerManager";

}
