package se.lnu.siq.s4rdm3x.experiments.metric;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.junit.jupiter.api.Test;
import se.lnu.siq.s4rdm3x.dmodel.NodeGenerator;
import se.lnu.siq.s4rdm3x.dmodel.dmDependency;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CouplingTest {

    @Test
    void test1() {
        CouplingIn sutIn = new CouplingIn();
        CouplingOut sutOut = new CouplingOut();
        NodeGenerator ng = new NodeGenerator();
        Graph g = ng.generateGraph(dmDependency.Type.Extends, new String [] {"AB", "AC"});
        Node a = g.getNode("A");
        Node b = g.getNode("B");
        Node c = g.getNode("C");

        sutIn.assignMetric(g.getNodeSet());
        assertEquals(0, sutIn.getMetric(a));
        assertEquals(1, sutIn.getMetric(b));
        assertEquals(1, sutIn.getMetric(c));

        sutOut.assignMetric(g.getNodeSet());
        assertEquals(2, sutIn.getMetric(a));
        assertEquals(0, sutIn.getMetric(b));
        assertEquals(0, sutIn.getMetric(c));
    }

    @Test
    void test2() {
        CouplingIn sutIn = new CouplingIn();
        CouplingOut sutOut = new CouplingOut();
        NodeGenerator ng = new NodeGenerator();
        Graph g = ng.generateGraph(dmDependency.Type.Extends, new String [] {"AB", "AC"});
        ng.addToGraph(g, dmDependency.Type.MethodCall, new String [] {"AB", "AC"});
        Node a = g.getNode("A");
        Node b = g.getNode("B");
        Node c = g.getNode("C");

        sutIn.assignMetric(g.getNodeSet());
        assertEquals(0, sutIn.getMetric(a));
        assertEquals(1, sutIn.getMetric(b));
        assertEquals(1, sutIn.getMetric(c));

        sutOut.assignMetric(g.getNodeSet());
        assertEquals(2, sutIn.getMetric(a));
        assertEquals(0, sutIn.getMetric(b));
        assertEquals(0, sutIn.getMetric(c));
    }

    @Test
    void testCircular() {
        CouplingIn sutIn = new CouplingIn();
        CouplingOut sutOut = new CouplingOut();
        NodeGenerator ng = new NodeGenerator();
        Graph g = ng.generateGraph(dmDependency.Type.Extends, new String [] {"AB", "AC", "AA", "BB", "CC"});
        Node a = g.getNode("A");
        Node b = g.getNode("B");
        Node c = g.getNode("C");

        sutIn.assignMetric(g.getNodeSet());
        assertEquals(0, sutIn.getMetric(a));
        assertEquals(1, sutIn.getMetric(b));
        assertEquals(1, sutIn.getMetric(c));

        sutOut.assignMetric(g.getNodeSet());
        assertEquals(2, sutIn.getMetric(a));
        assertEquals(0, sutIn.getMetric(b));
        assertEquals(0, sutIn.getMetric(c));
    }
}
