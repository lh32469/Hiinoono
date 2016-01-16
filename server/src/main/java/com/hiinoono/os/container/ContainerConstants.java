package com.hiinoono.os.container;

/**
 * Specific constants associated with Container administration.
 *
 * @author Lyle T Harris
 */
public interface ContainerConstants {

    /**
     * Top level path/dir for various cluster-wide managers to register
     * themselves. Need to find a better home for this at some point.
     */
    public static final String MANAGERS = "/managers";

    /**
     * Top level path/dir for Nodes to register and get Containers assigned.
     */
    public static final String CONTAINERS = "/containers";

    /**
     * Sub path/dir for each node where newly assigned containers are placed for
     * a given node. Total path is /containers/{nodeId}/new
     */
    public static final String NEW = "/new";

    /**
     * Sub path/dir for each node where newly created containers are placed for
     * a given node. Total path is /containers/{nodeId}/created
     */
    public static final String CREATED = "/created";

    public static final String STARTING = "/starting";

    /**
     * Sub path/dir for each node where running containers are placed for a
     * given node. Total path is /containers/{nodeId}/running.
     */
    public static final String RUNNING = "/running";

    /**
     * Sub path/dir for each node where containers that had errors are placed
     * for a given node. Total path is /containers/{nodeId}/errors.
     */
    public static final String ERRORS = "/errors";

    /**
     * Name of ZK Ephemeral node to indicate whom is the current, cluster-wide
     * ContainerManager.
     */
    public static final String MGR_NODE = "/ContainerManager";

    public static final String[] STATES
            = {NEW, CREATED, STARTING, RUNNING};

}
