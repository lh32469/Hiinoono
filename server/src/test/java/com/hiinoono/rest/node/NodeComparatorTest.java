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

    private Node n1, n2, n3;

    private List<Node> nodes;

    private NodeComparator nc;


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

        nc = new NodeComparator(new Container());

        n1 = new Node();
        n1.setHostname("node1");

        n2 = new Node();
        n2.setHostname("node2");

        n3 = new Node();
        n3.setHostname("node3");

        nodes = new LinkedList<>();
        nodes.add(n1);
        nodes.add(n2);
        nodes.add(n3);

        for (Node node : nodes) {
            node.setMemTotal(16328372);      // kB
            node.setMemAvailable(14922516);  // kB
        }
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

        n1.setMemAvailable(16000000l);
        n2.setMemAvailable(16005000l);
        n3.setMemAvailable(16000000l);

        Collections.sort(nodes, nc);

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

        n1.setMemAvailable(16000000l);
        n2.setMemAvailable(24000000l);
        n3.setMemAvailable(8000000l);

        Collections.sort(nodes, nc);

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

        n1.setVgFree(127950.0f);
        n2.setVgFree(128000.0f);
        n3.setVgFree(128050.0f);

        Collections.sort(nodes, nc);

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
    public void containerAssign1() {
        System.out.println("containerAssign1");

        Container c = new Container();
        c.setMemory(MemOption.MEG_1024);
        n1.getContainers().add(c);

        c = new Container();
        c.setMemory(MemOption.MEG_8192);
        n2.getContainers().add(c);

        Collections.sort(nodes, nc);

        for (Node node : nodes) {
            System.out.println(node.getHostname());
        }

        assertEquals(n3, nodes.get(0));
        assertEquals(n1, nodes.get(1));
        assertEquals(n2, nodes.get(2));
    }


    /**
     * Test that large differences in VG size does affect sorting.
     */
    @Test
    public void largeVolumeGroupDifference() {
        System.out.println("largeVolumeGroupDifference");

        n1.setVgFree(125000.0f);
        n2.setVgFree(128000.0f);
        n3.setVgFree(26000.0f);

        Collections.sort(nodes, nc);

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
            long value = nc.convert(option);
            System.out.println(option + " = " + value);
            assertTrue(value > 0);
        }
    }


    /**
     * Test that all DiskOptions are parsable.
     */
    @Test
    public void testDiskOptions() throws ParseException {
        System.out.println("testDiskOptions");

        for (DiskOption option : DiskOption.values()) {
            long value = nc.convert(option);
            System.out.println(option + " = " + value);
            assertTrue(value > 0);
        }

    }


}
