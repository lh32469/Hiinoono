package com.hiinoono.rest.node;

import com.hiinoono.jaxb.Node;
import java.util.Comparator;


/**
 * Used for sorting Nodes for Container placement.
 *
 * @author Lyle T Harris
 */
public class NodeComparator implements Comparator<Node> {

    @Override
    public int compare(Node n1, Node n2) {

        // Initially just go with most available memory.
        // TODO: Include previously allocated memory to containers.
        Long n1Mem = n1.getMemAvailable();
        Long n2Mem = n2.getMemAvailable();

        return (int) (n2Mem - n1Mem);

    }


}
