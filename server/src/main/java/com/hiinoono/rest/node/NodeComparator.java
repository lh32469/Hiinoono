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


    /**
     * Compares its two arguments for order. Returns a negative integer, zero,
     * or a positive integer as the first argument is less than, equal to, or
     * greater than the second.<p>
     *
     * Compares two Nodes for order. Returns a negative integer if the first
     * Node is better suited for placement than the second Node.
     */
    @Override
    public int compare(Node n1, Node n2) {

        int result = 0;

        // Memory available is reported in KBytes so only
        // care if one node has 100Meg more than the other.
        long memDiffInMeg
                = (n2.getMemAvailable() - n1.getMemAvailable()) / 1024;

        if (memDiffInMeg < -100) {
            result--;
        } else if (memDiffInMeg > 100) {
            result++;
        }

        // Gets another point if the memory difference is greater than 1Gig
        // so will outweigh disk differences.
        if (memDiffInMeg < -1000) {
            result--;
        } else if (memDiffInMeg > 1000) {
            result++;
        }

        // Memory available is reported in Megabytes so only
        // care if one node has 1Gig more than the other.
        float diskDiffInGig = (n2.getVgFree() - n1.getVgFree()) / 1024;

        if (diskDiffInGig < -1) {
            result--;
        } else if (diskDiffInGig > 1) {
            result++;
        }

        return result;

    }


}
