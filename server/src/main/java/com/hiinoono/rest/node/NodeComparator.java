package com.hiinoono.rest.node;

import com.hiinoono.jaxb.Container;
import com.hiinoono.jaxb.Node;
import java.util.Comparator;


/**
 * Used for sorting Nodes for Container placement. Container to be placed is
 * passed in to be used for possibly placing on the same or different Node as
 * other Containers according to placement requirements. 
 *
 * @author Lyle T Harris
 */
public class NodeComparator implements Comparator<Node> {

    private final Container container;


    public NodeComparator(Container container) {
        this.container = container;
    }


    @Override
    public int compare(Node n1, Node n2) {

        // Initially just go with most available memory.
        // TODO: Include previously allocated memory to containers.
        Long n1Mem = n1.getMemAvailable();
        Long n2Mem = n2.getMemAvailable();

        return (int) (n2Mem - n1Mem);

    }


}
