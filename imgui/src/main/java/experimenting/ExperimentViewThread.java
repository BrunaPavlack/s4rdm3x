package experimenting;

import glm_.vec2.Vec2;
import glm_.vec4.Vec4;
import gui.ImGuiWrapper;
import gui.JavaProperty;
import imgui.HoveredFlag;
import imgui.WindowFlag;
import imgui.internal.Window;
import se.lnu.siq.s4rdm3x.experiments.*;
import se.lnu.siq.s4rdm3x.experiments.metric.*;
import se.lnu.siq.s4rdm3x.experiments.metric.aggregated.RelativeLineCount;
import se.lnu.siq.s4rdm3x.experiments.system.FileBased;
import se.lnu.siq.s4rdm3x.model.CGraph;
import se.lnu.siq.s4rdm3x.model.cmd.mapper.ArchDef;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.BiPredicate;

import static imgui.ImguiKt.COL32;

class ExperimentViewThread extends Thread {
    String m_id;
    ExperimentRunner m_experiment;

    private double m_avgPerformance = 0;
    private int m_avgCount = 0;

    private String m_name = "";

    private Vec4 m_currentColor = new Vec4(0.75, 0.75, 0.75, 1);

    static final int g_nbmapper_ex = 0;
    static final int g_hugmemapper_ex = 1;
    static final int g_irattract_ex = 2;
    int m_experimentIx = 0;

    // ir experiment parameters
    IRExperimentRunnerBase.Data m_irData = new IRExperimentRunnerBase.Data();

    // nbmapper experiment parameters
    ExperimentRunner.RandomBoolVariable m_doWordCount = new ExperimentRunner.RandomBoolVariable();
    ExperimentRunner.RandomDoubleVariable m_threshold = new ExperimentRunner.RandomDoubleVariable(0.9, 0);

    // hugme experiment parameters
    ExperimentRunner.RandomDoubleVariable m_omega = new ExperimentRunner.RandomDoubleVariable(0.5, 0.5);
    ExperimentRunner.RandomDoubleVariable m_phi = new ExperimentRunner.RandomDoubleVariable(0.5, 0.5);

    // generic experiment parameters
    ExperimentRunner.RandomDoubleVariable m_initialSetSize = new ExperimentRunner.RandomDoubleVariable(0.1, 0.1);
    SystemSelection m_selectedSystem = new SystemSelection();
    boolean m_useManualmapping = false;

    public ExperimentViewThread(ExperimentViewThread a_toBeCopied, int a_id) {
        m_id = "ExThread_" + a_id;
        m_name = a_toBeCopied.m_name;
        m_currentColor = new Vec4(a_toBeCopied.m_currentColor);
        m_experimentIx = a_toBeCopied.m_experimentIx;
        m_irData = new IRExperimentRunnerBase.Data(a_toBeCopied.m_irData);
        m_doWordCount = new ExperimentRunner.RandomBoolVariable(a_toBeCopied.m_doWordCount);
        m_threshold = new ExperimentRunner.RandomDoubleVariable(a_toBeCopied.m_threshold);
        m_omega = new ExperimentRunner.RandomDoubleVariable(a_toBeCopied.m_omega);
        m_phi = new ExperimentRunner.RandomDoubleVariable(a_toBeCopied.m_phi);
        m_initialSetSize = new ExperimentRunner.RandomDoubleVariable(a_toBeCopied.m_initialSetSize);
        m_selectedSystem = new SystemSelection(a_toBeCopied.m_selectedSystem);
        m_useManualmapping = a_toBeCopied.m_useManualmapping;
        for (Metric exrMetric : a_toBeCopied.m_selectedMetrics.getSelected()) {
            m_selectedMetrics.select(exrMetric);
        }
    }

    ExperimentViewThread(int a_id) {
        m_id = "ExThread_" + a_id;
    }

    ExperimentViewThread(int a_id, ExperimentRunner a_runner) {
        m_id = "ExThread_" + a_id;
        setExperiment(a_runner);
    }


    public void runExperiment(DataListener a_newDataListener) {
        try {
            if (m_experiment != null) {
                m_experiment.stop();
            }

            m_experiment = createExperiment();

            ExperimentViewThread source = this;

            m_experiment.setRunListener(new ExperimentRunner.RunListener() {
                public ExperimentRunData.BasicRunData OnRunInit(ExperimentRunData.BasicRunData a_rd, CGraph a_g, ArchDef a_arch) {

                    return a_rd;
                }

                public void OnRunCompleted(ExperimentRunData.BasicRunData a_rd, CGraph a_g, ArchDef a_arch) {

                    a_newDataListener.onNewData(a_rd, source);

                    if (m_avgCount == 0) {
                        m_avgCount = 1;
                        m_avgPerformance = a_rd.calcAutoPerformance();
                    } else {
                        m_avgPerformance = m_avgPerformance * (m_avgCount / (double) (m_avgCount + 1)) + a_rd.calcAutoPerformance() / (double) (m_avgCount + 1);
                        m_avgCount++;
                    }

                }
            });


            Thread t = new Thread(this);
            t.start();

        } catch (IOException e) {
            System.out.println(e);
            System.out.println(e.getStackTrace());
        }
    }

    public void stopExperiment() {
        if (m_experiment != null) {
            m_experiment.stop();
            m_experiment = null;
        }
    }

    public Vec4 getColor() {
        return m_currentColor;
    }


    static class MetricPair {

        MetricPair(String a_name, Metric a_abs) {
            m_name = a_name;
            m_absMetric = a_abs;
            m_relMetric = new RelativeLineCount(a_abs);
        }

        MetricPair(String a_name, Metric a_abs, Metric a_rel) {
            m_name = a_name;
            m_absMetric = a_abs;
            m_relMetric = a_rel;
        }

        String m_name;
        Metric m_absMetric;
        Metric m_relMetric;

        public String getName() {
            return m_name;
        }
    }

    static class MetricSelection {

        private MetricPair[] m_metrics = { new MetricPair("Random", new Rand(), null), new MetricPair("BC. Cyclo. Cpl.", new ByteCodeCyclomaticComplexity()), new MetricPair("BC. Instr. Count", new ByteCodeInstructions()),
                new MetricPair("Coupling In", new CouplingIn()), new MetricPair("Coupling Out", new CouplingOut()), new MetricPair("Fan In", new FanIn()), new MetricPair("Fan Out", new FanOut()),
                new MetricPair("LCOM HS", new LCOMHS()), new MetricPair("Line Count", new LineCount(), null), new MetricPair("Method Count", new NumberOfMethods()), new MetricPair("Child Count", new NumberOfChildren()),
                new MetricPair("Child Level Count", new NumberOfChildLevels()), new MetricPair("Child Count Lvl 0", new NumberOfChildrenLevel0()), new MetricPair("Field Count", new NumberOfFields()), new MetricPair("Parent Count", new NumberOfParents()),
                new MetricPair("Rank", new Rank()), new MetricPair("Class Count", new NumberOfClasses())
        };

        /*private static Metric[] g_metrics = {new NumberOfMethods(), new NumberOfChildren(), new NumberOfChildLevels(), new NumberOfChildrenLevel0(), new NumberOfFields(), new NumberOfParents(), new Rank(), new NumberOfClasses()};*/
        private ArrayList<Metric> m_selectedMetrics = new ArrayList<>();

        void select(Metric a_metric) {
            if (!isSelected(a_metric)) {
                m_selectedMetrics.add(a_metric);
            }
        }


        boolean isSelected(Metric a_metric) {
            return getSelected(a_metric) != null;
        }

        Metric getSelected(Metric a_metric) {
            for (Metric m : m_selectedMetrics) {
                if (m.getName().equals(a_metric.getName())) {
                    return m;
                }
            }

            return null;
        }

        Iterable<MetricPair> getMetricPairs() {
            return Arrays.asList(m_metrics);
        }

        public void toogle(Metric a_m) {
            Metric m = getSelected(a_m);
            if (m != null) {
                m_selectedMetrics.remove(m);
            } else {
                m_selectedMetrics.add(a_m);
            }
        }

        public void clear() {
            m_selectedMetrics.clear();
        }

        public Iterable<Metric> getSelected() {
            return m_selectedMetrics;
        }
    }

    MetricSelection m_selectedMetrics = new MetricSelection();


    static class SystemNameFile {
        public String m_name;
        public String m_file;

        SystemNameFile(String a_name, String a_file) {
            m_name = a_name;
            m_file = a_file;
        }
    }
    static class SystemSelection {
        static final SystemNameFile[] g_systems = {
                new SystemNameFile("Ant", "data/systems/ant/ant-system_model.txt"),
                new SystemNameFile("Argouml", "data/systems/argouml/argouml-system_model.txt"),
                new SystemNameFile("JabRef", "data/systems/JabRef/3.7/jabref-3_7-system_model_1.txt"),
                new SystemNameFile("Lucene", "data/systems/lucene/lucene-system_model.txt"),
                new SystemNameFile("Sweethome 3d", "data/systems/sweethome3d/sweethome3d-system_model.txt"),
                new SystemNameFile("Teammates", "data/systems/teammates/teammates-system_model.txt")};

        ArrayList<SystemNameFile> m_selectedSystems = new ArrayList<>();

        public SystemSelection(SystemSelection a_cpy) {
            a_cpy.m_selectedSystems.forEach(sf -> m_selectedSystems.add(sf));
        }

        public SystemSelection() {

        }

        ArrayList<String> getSystemNames() {
            ArrayList<String> ret = new ArrayList<>();

            for (SystemNameFile snf : g_systems) {
                ret.add(snf.m_name);
            }
            return ret;
        }

        Iterable<SystemNameFile> getSystems() {
            return Arrays.asList(g_systems);
        }

        Iterable<SystemNameFile> getSelectedSystems() {
            return m_selectedSystems;
        }

        boolean isSelected(SystemNameFile a_snf) {
            return m_selectedSystems.contains(a_snf);
        }

        void toogleSelection(SystemNameFile a_snf) {
            if (isSelected(a_snf)) {
                m_selectedSystems.remove(a_snf);
            } else {
                m_selectedSystems.add(a_snf);
            }
        }


        public int getSystemCount() {
            return g_systems.length;
        }

        public boolean isLastSystem(SystemNameFile a_snf) {
            return g_systems[g_systems.length - 1] == a_snf;
        }

        public void selectFileName(String a_fileName) {
            for (SystemNameFile snf : g_systems) {
                if (snf.m_file.equals(a_fileName)) {
                    if (!isSelected(snf)) {
                        m_selectedSystems.add(snf);
                    }
                }
            }
        }

        public void clearSelection() {
            m_selectedSystems.clear();
        }
    }










    private ExperimentRunner.RandomBoolVariable doRandomBoolVariable(ImGuiWrapper a_imgui, String a_label, ExperimentRunner.RandomBoolVariable a_var) {
        String [] randomBoolLabels = {"Yes", "No", "Random"};

        int[] currentItem = {a_var.isRandom() ? 2 : a_var.getValue() ? 0 : 1};
        if (a_imgui.imgui().combo(a_label + "##" + m_id, currentItem, Arrays.asList(randomBoolLabels), 3)) {
            switch (currentItem[0]) {
                case 0:
                    a_var = new ExperimentRunner.RandomBoolVariable(true);
                    break;
                case 1:
                    a_var = new ExperimentRunner.RandomBoolVariable(false);
                    break;
                case 2:
                    a_var = new ExperimentRunner.RandomBoolVariable();
                    break;
                default:
                    System.out.println("Unhandled Case in Switch: " + currentItem[0]);
                    assert (false);
                    break;
            }
        }

        return a_var;
    }



    interface DataListener {
        void onNewData(ExperimentRunData.BasicRunData a_rd, ExperimentViewThread a_source);
    }

    public enum DoExperimentAction {
        None,
        Delete,
        Copy
    }

    public DoExperimentAction doExperiment(ImGuiWrapper a_imgui, DataListener a_newDataListener) {
        DoExperimentAction ret = DoExperimentAction.None;
        if (a_imgui.imgui().collapsingHeader("Experiment " + m_name + "###Header" + m_id, 0)) {
            //Vec2 size = new Vec2(a_imgui.imgui().getContentRegionAvailWidth(), a_imgui.getTextLineHeightWithSpacing() * 2 + a_imgui.imgui().getContentRegionAvailWidth() / 3);

            if (isRunningExperiment()) {
                a_imgui.pushDisableWidgets();
            }

            m_name = a_imgui.inputTextSingleLine("Name###Name" + m_id, m_name);



            //a_imgui.imgui().beginChild(m_id, size, true, 0);




            {
                String[] experiments = {"Naive Bayes Mapping", "HuGMe", "IRAttract"};
                int[] exIx = {m_experimentIx};
                if (a_imgui.imgui().combo("Experiment Type" + "##" + m_id, exIx, Arrays.asList(experiments), 2)) {
                    m_experimentIx = exIx[0];
                }
            }



            if (m_experimentIx == g_nbmapper_ex || m_experimentIx == g_irattract_ex) {

                m_irData.doStemming(doRandomBoolVariable(a_imgui, "Use Stemming", m_irData.doStemming()));
                m_irData.doUseCDA(doRandomBoolVariable(a_imgui, "Use CDA", m_irData.doUseCDA()));
                m_irData.doUseNodeText(doRandomBoolVariable(a_imgui, "Use Code Text", m_irData.doUseNodeText()));
                m_irData.doUseNodeName(doRandomBoolVariable(a_imgui, "Use Code Name", m_irData.doUseNodeName()));
                m_irData.doUseArchComponentName(doRandomBoolVariable(a_imgui, "Use Architecture Name", m_irData.doUseArchComponentName()));
                m_irData.minWordSize(doRandomIntVariable(a_imgui, "Min Word Length", m_irData.minWordSize()));

                if (m_experimentIx == g_nbmapper_ex) {
                    m_doWordCount = doRandomBoolVariable(a_imgui, "Use Word Counts", m_doWordCount);
                    m_threshold = doRandomDoubleVariable(a_imgui, "Threshold", m_threshold);
                }
            } else if (m_experimentIx == g_hugmemapper_ex) {
                m_omega = doRandomDoubleVariable(a_imgui, "Omega Threshold", m_omega);
                m_phi = doRandomDoubleVariable(a_imgui, "Phi", m_phi);
            } else if (m_experimentIx == g_irattract_ex) {
                // add parameters here
            }

            a_imgui.imgui().separator();
            {
                    /*int [] currentSystem = {m_selectedSystem.getCurrentSystemIx()};
                    if (a_imgui.imgui().combo("System##" + m_id, currentSystem, m_selectedSystem.getSystemNames(), m_selectedSystem.getSystemCount())) {
                        m_selectedSystem.setCurrentSystem(currentSystem[0]);
                    }*/
                for (SystemNameFile snf : m_selectedSystem.getSystems()) {
                    boolean isSelected[] = {m_selectedSystem.isSelected(snf)};
                    if (a_imgui.imgui().checkbox(snf.m_name + "##" + m_id, isSelected)) {
                        m_selectedSystem.toogleSelection(snf);
                    }
                    if (!m_selectedSystem.isLastSystem(snf)) {
                        a_imgui.imgui().sameLine(0, 10);
                    }

                }
            }
            m_initialSetSize = doRandomDoubleVariable(a_imgui, "Initial Set Size", m_initialSetSize);


            if (a_imgui.collapsingHeader("Metrics##" + m_id, 0)) {

                final float boxWidth = a_imgui.imgui().getTextLineHeightWithSpacing() + 5;
                final float colWidth = a_imgui.calcTextSize("Child Count Lvl 0", false).getX() + 10 + 2 * boxWidth;

                int count = 0;
                for (MetricPair m : m_selectedMetrics.getMetricPairs()) {

                    if (m.m_relMetric != null) {
                        boolean[] isSelected = {m_selectedMetrics.isSelected(m.m_relMetric)};
                        if (a_imgui.imgui().checkbox("##" + m.getName() + m_id, isSelected)) {
                            m_selectedMetrics.toogle(m.m_relMetric);
                        }
                        if (a_imgui.imgui().isItemHovered(0) && a_imgui.beginTooltip()) {
                            a_imgui.text("Relative Linecount");
                            a_imgui.endTooltip();
                        }
                        // for some reason the first column gets a wierd offset
                        a_imgui.imgui().sameLine((count % 4) * colWidth + boxWidth, (count % 4) == 0 ? 7 : 0);
                    }

                    boolean[] isSelected = {m_selectedMetrics.isSelected(m.m_absMetric)};
                    if (a_imgui.imgui().checkbox(m.getName() + "##" + m_id, isSelected)) {
                        m_selectedMetrics.toogle(m.m_absMetric);
                    }
                    if (a_imgui.imgui().isItemHovered(0) && a_imgui.beginTooltip()) {
                        a_imgui.text("Absolute");
                        a_imgui.endTooltip();
                    }
                    count++;
                    if (count % 4 != 0) {
                        a_imgui.imgui().sameLine((count % 4) * colWidth, 0);
                    }

                }
                if (count % 4 != 0) {
                    a_imgui.imgui().newLine();
                }
                a_imgui.imgui().separator();
            }


            {
                boolean[] manualMappnig = {m_useManualmapping};
                if (a_imgui.imgui().checkbox("Use Manual Mapping##" + m_id, manualMappnig)) {
                    m_useManualmapping = manualMappnig[0];
                }
            }


            a_imgui.imgui().colorEdit3("Plot Color##" + m_id, m_currentColor, 0);

            if (isRunningExperiment()) {
                a_imgui.popDisableWidgets();
            }


            if (!isRunningExperiment()) {


                if (a_imgui.button("Run Experiment##" + m_id, 0)) {
                    runExperiment(a_newDataListener);
                }

            } else {
                if (a_imgui.button("Stop Experiment##" + m_id, 0)) {
                    m_avgPerformance = 0;
                    m_avgCount = 0;
                    m_experiment.stop();
                    halt();
                }
            }

            a_imgui.imgui().sameLine(0);
            if (a_imgui.button("Copy Experiment##" + m_id, 0)) {
                ret = DoExperimentAction.Copy;
            }
            a_imgui.imgui().sameLine(0);
            if (a_imgui.button("Delete Experiment##" + m_id, 0)) {
                ret = DoExperimentAction.Delete;
            }


            a_imgui.text(String.format("Average Performance: %.2f", getAvgPerformance() * 100));



                /*for (ExperimentRunData.BasicRunData exd : m_selectedDataPoints) {
                    a_imgui.text(exd.m_system.getName());
                }*/

                /*if (beginPopupContextItem(a_imgui,"test_popup", 1)) {
                    a_imgui.menuItem("test_item", "", false, true);
                    a_imgui.endPopup();
                }*/


            //a_imgui.imgui().endChild();
            a_imgui.imgui().separator();
            a_imgui.imgui().separator();
        }

        return ret;
    }

    private boolean isRunningExperiment() {
        return !(m_experiment == null || m_experiment.getState() == ExperimentRunner.State.Idle);
    }

    private void setExperiment(ExperimentRunner a_exr) {

        if (m_experiment != null && m_experiment.getState() == ExperimentRunner.State.Running) {
            m_experiment.stop();
            m_experiment = null;
        }

        m_selectedMetrics.clear();
        for (Metric exrMetric : a_exr.getMetrics()) {
            m_selectedMetrics.select(exrMetric);
        }
        m_selectedSystem.clearSelection();
        a_exr.getSystems().forEach(s -> m_selectedSystem.selectFileName(((FileBased)s).getFile()));

        m_useManualmapping = a_exr.doUseManualmapping();
        m_initialSetSize = a_exr.getInitialSetSize();

        if (a_exr instanceof NBMapperExperimentRunner) {
            NBMapperExperimentRunner nbexr = (NBMapperExperimentRunner)a_exr;
            m_threshold = nbexr.getThreshold();
            m_irData = nbexr.getIRDataClone();
            m_doWordCount = nbexr.getWordCount();
            m_experimentIx = g_nbmapper_ex;
        } else if (a_exr instanceof HuGMeExperimentRunner) {
            HuGMeExperimentRunner hugme = (HuGMeExperimentRunner)a_exr;
            m_omega = hugme.getOmega();
            m_phi = hugme.getPhi();
            m_experimentIx = g_hugmemapper_ex;
        } else if (a_exr instanceof IRAttractExperimentRunner) {
            m_experimentIx = g_irattract_ex;
            // initialize experiments here
        }

        m_name = a_exr.getName();
    }

    ExperimentRunner createExperiment() throws IOException {
        ExperimentRunner ret = null;
        ArrayList<se.lnu.siq.s4rdm3x.experiments.system.System> systems = new ArrayList<>();
        for (SystemNameFile snf : m_selectedSystem.getSelectedSystems()) {
            if (snf.m_file != null) {
                systems.add(new se.lnu.siq.s4rdm3x.experiments.system.FileBased(snf.m_file));
            }
        }

        if (m_experimentIx == g_nbmapper_ex) {
            ret = new NBMapperExperimentRunner(systems, m_selectedMetrics.getSelected(), m_useManualmapping, m_initialSetSize, m_irData, m_doWordCount, m_threshold);
        } else if (m_experimentIx == g_hugmemapper_ex) {
            ret = new HuGMeExperimentRunner(systems, m_selectedMetrics.getSelected(), m_useManualmapping, m_initialSetSize, m_omega, m_phi);
        } else if (m_experimentIx == g_irattract_ex) {
            ret = new IRAttractExperimentRunner(systems, m_selectedMetrics.getSelected(), m_useManualmapping, m_initialSetSize, m_irData);
        }

        ret.setName(m_name);

        return ret;
    }

    private ExperimentRunner.RandomDoubleVariable doRandomDoubleVariable(ImGuiWrapper a_imgui, String a_label, ExperimentRunner.RandomDoubleVariable a_threshold) {
        Float[] minArray = new Float[1]; minArray[0] = (float)a_threshold.getMin();
        Float[] maxArray = new Float[1]; maxArray[0] = (float)a_threshold.getMax();

        if (a_imgui.imgui().dragFloatRange2(a_label+"##"+m_id, new JavaProperty<>(minArray), new JavaProperty<>(maxArray), 0.01f, 0f, 1f, "%.2f", "%.2f", 1)) {
            double scale = (maxArray[0] - minArray[0]) / 2.0;
            a_threshold = new ExperimentRunner.RandomDoubleVariable(minArray[0] + scale, scale);
        }
        return a_threshold;
    }

    private ExperimentRunner.RandomIntVariable doRandomIntVariable(ImGuiWrapper a_imgui, String a_label, ExperimentRunner.RandomIntVariable a_threshold) {
        Integer[] minArray = new Integer[1]; minArray[0] = a_threshold.getMin();
        Integer[] maxArray = new Integer[1]; maxArray[0] = a_threshold.getMax();

        if (a_imgui.imgui().dragIntRange2(a_label+"##"+m_id, new JavaProperty<>(minArray), new JavaProperty<>(maxArray), 1.0f, 0, 255, "min:%d", "max:%d")) {
            a_threshold = new ExperimentRunner.RandomIntVariable(minArray[0], maxArray[0]);
        }
        return a_threshold;
    }

    public void run() {
        //java.lang.System.out.print("" + m_ix + ", ");
        CGraph graph = new CGraph();
        //m_fs = new RunFileSaver(m_sua.getName(), m_metric.getName(), m_doSaveMappings);

        //m_exr.setRunListener(m_fs);
        m_experiment.run(graph);
    }

    public ExperimentRunner.State getExState() {
        if (m_experiment != null) {
            return m_experiment.getState();
        } else {
            return ExperimentRunner.State.Idle;
        }
    }

    public void halt() {
        if (m_experiment != null) {
            m_experiment.stop();
        }
    }

    public double getAvgPerformance() {
        return m_avgPerformance;
    }
}