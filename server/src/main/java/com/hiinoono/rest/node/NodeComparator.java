package com.hiinoono.rest.node;

import com.hiinoono.jaxb.Container;
import com.hiinoono.jaxb.DiskOption;
import com.hiinoono.jaxb.MemOption;
import com.hiinoono.jaxb.Node;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Comparator;
import org.slf4j.LoggerFactory;


/**
 * Used for sorting Nodes for Container placement. Container to be placed is
 * passed in to be used for possibly placing on the same or different Node as
 * other Containers according to placement requirements.
 *
 * @author Lyle T Harris
 */
public class NodeComparator implements Comparator<Node> {

    private final Container container;

    final static private org.slf4j.Logger LOG
            = LoggerFactory.getLogger(NodeComparator.class);


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

        // Check Container assignments.
        long memAssigned1 = n1.getContainers().parallelStream().mapToLong(
                (c) -> convert(c.getMemory())).sum();
        memAssigned1 *= 1024; // Convert to kBytes

        int node1PercentMemSubscribed
                = (int) (((100 * memAssigned1) / n1.getMemTotal()));

        long memAssigned2 = n2.getContainers().parallelStream().mapToLong(
                (c) -> convert(c.getMemory())).sum();
        memAssigned2 *= 1024;  // Convert to kBytes

        int node2PercentMemSubscribed
                = (int) (((100 * memAssigned2) / n2.getMemTotal()));

        int percentMemSubscribedDiff
                = node2PercentMemSubscribed - node1PercentMemSubscribed;

        // Only care about 5% memory subcription rate difference.
        if (percentMemSubscribedDiff < -5) {
            result++;
        } else if (percentMemSubscribedDiff > 5) {
            result--;
        }
        return result;

    }


    /**
     * Convert the DiskOption to a long value so that GIG_10 gets becomes 10.
     * Needs to be thread safe.
     *
     * @param disk
     * @return
     */
    long convert(DiskOption disk) {

        /**
         * Format of the enum for requested container disk space. Create one
         * each time for thread safety.
         */
        final NumberFormat formatter = new DecimalFormat("'GIG_'#");

        if (disk == null) {
            return Long.MAX_VALUE;
        }

        try {
            return formatter.parse(disk.toString()).longValue();
        } catch (ParseException | NumberFormatException ex) {
            // NodeComparatorTest ensures all DiskOptions are parsable.
            LOG.error(ex.toString() + "  " + disk);
            return Long.MAX_VALUE;
        }
    }


    /**
     * Convert the MemoryOption to a long value so that MEG_2048 gets becomes
     * 2048. Needs to be thread safe.
     *
     * @param memory
     * @return
     */
    long convert(MemOption memory) {

        /**
         * Format of the enum for requested container memory. Create one each
         * time for thread safety.
         */
        final NumberFormat formatter = new DecimalFormat("'MEG_'#");

        if (memory == null) {
            return Long.MAX_VALUE;
        }

        try {
            return formatter.parse(memory.toString()).longValue();
        } catch (ParseException | NumberFormatException ex) {
            // NodeComparatorTest ensures all MemOptions are parsable.
            LOG.error(ex.toString() + "  " + memory);
            return Long.MAX_VALUE;
        }
    }


}
