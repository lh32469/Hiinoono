/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hiinoono.rest.node;

import com.hiinoono.jaxb.Container;
import com.hiinoono.jaxb.DiskOption;
import com.hiinoono.jaxb.MemOption;
import com.hiinoono.jaxb.Node;
import java.text.ParseException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;


/**
 *
 * @author ltharris
 */
public class NodeComparatorTest {

    public NodeComparatorTest() {
    }


    @BeforeClass
    public static void setUpClass() {
    }


    @AfterClass
    public static void tearDownClass() {
    }


    @Before
    public void setUp() {
    }


    @After
    public void tearDown() {
    }


    /**
     *
     */
    @Test
    public void smallMemoryDifference() {
        System.out.println("smallMemoryDifference");

        Node n1 = new Node();
        n1.setHostname("node1");
        n1.setMemAvailable(16000000l);

        Node n2 = new Node();
        n2.setHostname("node2");
        n2.setMemAvailable(16005000l);

        Node n3 = new Node();
        n3.setHostname("node3");
        n3.setMemAvailable(16000000l);

        List<Node> nodes = new LinkedList<>();
        nodes.add(n1);
        nodes.add(n2);
        nodes.add(n3);

        Collections.sort(nodes, new NodeComparator(new Container()));
        System.out.println("1st: " + nodes.get(0).getHostname());

        for (Node node : nodes) {
            System.out.println(node.getHostname());
        }

        assertEquals(n1, nodes.get(0));
        assertEquals(n2, nodes.get(1));
        assertEquals(n3, nodes.get(2));
    }


    /**
     *
     */
    @Test
    public void largeMemoryDifference() {
        System.out.println("largeMemoryDifference");

        Node n1 = new Node();
        n1.setHostname("node1");
        n1.setMemAvailable(16000000l);

        Node n2 = new Node();
        n2.setHostname("node2");
        n2.setMemAvailable(24000000l);

        Node n3 = new Node();
        n3.setHostname("node3");
        n3.setMemAvailable(8000000l);

        List<Node> nodes = new LinkedList<>();
        nodes.add(n1);
        nodes.add(n2);
        nodes.add(n3);

        Collections.sort(nodes, new NodeComparator(new Container()));
        System.out.println("1st: " + nodes.get(0).getHostname());

        for (Node node : nodes) {
            System.out.println(node.getHostname());
        }
        assertEquals(n2, nodes.get(0));
        assertEquals(n1, nodes.get(1));
        assertEquals(n3, nodes.get(2));
    }


    /**
     * Test that small differences in VG size doesn't affect sorting.
     */
    @Test
    public void smallVolumeGroupDifference() {
        System.out.println("smallVolumeGroupDifference");

        Node n1 = new Node();
        n1.setHostname("node1");
        n1.setMemAvailable(128000l);
        n1.setVgFree(127950.0f);

        Node n2 = new Node();
        n2.setHostname("node2");
        n2.setMemAvailable(128000l);
        n2.setVgFree(128000.0f);

        Node n3 = new Node();
        n3.setHostname("node3");
        n3.setMemAvailable(128000l);
        n3.setVgFree(128050.0f);

        List<Node> nodes = new LinkedList<>();
        nodes.add(n1);
        nodes.add(n2);
        nodes.add(n3);

        Collections.sort(nodes, new NodeComparator(new Container()));
        System.out.println("1st: " + nodes.get(0).getHostname());

        for (Node node : nodes) {
            System.out.println(node.getHostname());
        }

        assertEquals(n1, nodes.get(0));
        assertEquals(n2, nodes.get(1));
        assertEquals(n3, nodes.get(2));
    }


    /**
     * Test that large differences in VG size does affect sorting.
     */
    @Test
    public void largeVolumeGroupDifference() {
        System.out.println("largeVolumeGroupDifference");

        Node n1 = new Node();
        n1.setHostname("node1");
        n1.setVgFree(125000.0f);

        Node n2 = new Node();
        n2.setHostname("node2");
        n2.setVgFree(128000.0f);

        Node n3 = new Node();
        n3.setHostname("node3");
        n3.setVgFree(26000.0f);

        List<Node> nodes = new LinkedList<>();
        nodes.add(n1);
        nodes.add(n2);
        nodes.add(n3);

        Collections.sort(nodes, new NodeComparator(new Container()));
        System.out.println("1st: " + nodes.get(0).getHostname());

        for (Node node : nodes) {
            System.out.println(node.getHostname());
        }

        assertEquals(n2, nodes.get(0));
        assertEquals(n1, nodes.get(1));
        assertEquals(n3, nodes.get(2));
    }


    /**
     * Test that all MemOptions are parsable.
     */
    @Test
    public void testMemOptions() throws ParseException {
        System.out.println("testMemOptions");

        for (MemOption option : MemOption.values()) {
            long value = NodeComparator.MEM_FORMAT
                    .parse(option.toString()).longValue();
            System.out.println(option + " = " + value);
        }
    }


    /**
     * Test that all DiskOptions are parsable.
     */
    @Test
    public void testDiskOptions() throws ParseException {
        System.out.println("testDiskOptions");

        for (DiskOption option : DiskOption.values()) {
            long value = NodeComparator.DISK_FORMAT
                    .parse(option.toString()).longValue();
            System.out.println(option + " = " + value);
        }

    }


}
