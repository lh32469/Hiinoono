package com.hiinoono.os.mock;

import com.hiinoono.jaxb.Container;
import com.hiinoono.os.ContainerDriver;
import java.io.IOException;
import org.slf4j.LoggerFactory;


/**
 *
 * @author Lyle T Harris
 */
public class MockContainerDriver implements ContainerDriver {

    final static private org.slf4j.Logger LOG
            = LoggerFactory.getLogger(MockContainerDriver.class);


    @Override
    public Container create(Container container) throws IOException {
        LOG.info(container.getName() + " -> " + container.getTemplate());
        return container;
    }


}
