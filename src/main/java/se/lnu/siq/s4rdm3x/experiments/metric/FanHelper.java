package se.lnu.siq.s4rdm3x.experiments.metric;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import se.lnu.siq.s4rdm3x.cmd.HuGMe;
import se.lnu.siq.s4rdm3x.cmd.util.AttributeUtil;
import se.lnu.siq.s4rdm3x.cmd.util.FanInCache;
import se.lnu.siq.s4rdm3x.dmodel.dmClass;
import se.lnu.siq.s4rdm3x.dmodel.dmDependency;

public class FanHelper {
    FanInCache m_fic;
    AttributeUtil m_au;
    public FanHelper(Iterable<Node> a_nodes) {
        m_fic = new FanInCache(a_nodes);
        m_au = new AttributeUtil();
    }

    double getFanIn(Node a_n) {
        return m_fic.getFanIn(a_n);
    }

    public double getFanOut(Node a_n) {
        double fanOut = 0;
        for (dmClass from : m_au.getClasses(a_n)) {
            fanOut += countDependenciesFrom(a_n, from);
        }

        return fanOut;
    }

    private double countDependenciesFrom(Node a_source, dmClass a_c) {
        double count = 0;
        // TODO: we should have some weight here

        for(dmDependency d : a_c.getDependencies()) {
            if (!m_au.hasClass(a_source, d.getTarget())) {
                count += d.getCount();
            }
        }

        return count;
    }

    public boolean hasDirectDependency(Node a_from, Node a_to) {
        for (dmClass c : m_au.getClasses(a_from)) {
            for (dmDependency d : c.getDependencies()) {
                if (m_au.hasClass(a_to, d.getTarget())) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean hasDirectDependency(Node a_from, Node a_to, dmDependency.Type a_type) {
        for (dmClass c : m_au.getClasses(a_from)) {
            for (dmDependency d : c.getDependencies()) {
                if (d.getType() == a_type) {
                    if (m_au.hasClass(a_to, d.getTarget())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
