/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hiinoono.rest.node;

import com.hiinoono.jaxb.Container;
import com.hiinoono.jaxb.Node;
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
     * Test of compare method, of class NodeComparator.
     */
    @Test
    public void testCompare() {
        System.out.println("compare");

        Node n1 = new Node();
        n1.setHostname("node1");
        n1.setMemAvailable(16000l);

        Node n2 = new Node();
        n2.setHostname("node2");
        n2.setMemAvailable(24000l);

        Node n3 = new Node();
        n3.setHostname("node3");
        n3.setMemAvailable(8000l);

        List<Node> nodes = new LinkedList<>();
        nodes.add(n1);
        nodes.add(n2);
        nodes.add(n3);

        Collections.sort(nodes, new NodeComparator(new Container()));
        System.out.println("1st: " + nodes.get(0).getHostname());

        // Most memory wins for now.
        assertEquals(n2, nodes.get(0));
    }


}
