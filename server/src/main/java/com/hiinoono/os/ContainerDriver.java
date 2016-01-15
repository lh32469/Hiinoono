package com.hiinoono.os;

import com.hiinoono.jaxb.Container;
import java.io.IOException;


/**
 * Methods for creating, starting, stopping and deleting containers.
 *
 * @author Lyle T Harris
 */
public interface ContainerDriver {

    /**
     * Creates the container and returns a Container with additional information
     * filled in.
     *
     * @param container
     * @return a Container with additional information filled in.
     * @throws java.io.IOException
     */
    Container create(Container container) throws IOException;


}
