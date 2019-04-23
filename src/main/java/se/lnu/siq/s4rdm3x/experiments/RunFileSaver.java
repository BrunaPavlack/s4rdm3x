package se.lnu.siq.s4rdm3x.experiments;

import se.lnu.siq.s4rdm3x.model.cmd.mapper.ArchDef;
import se.lnu.siq.s4rdm3x.model.CGraph;
import se.lnu.siq.s4rdm3x.model.CNode;

import java.io.File;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

import static java.nio.file.Files.exists;
import static java.nio.file.Files.write;

public class RunFileSaver implements ExperimentRunner.RunListener {

    Path m_filePath;
    Path m_mappingsFilePath;
    int m_errorCounter;
    int m_runCount;

    public static class Mapping {
        public int m_runId;
        public int m_runFile;
        public String m_node;
        public String m_mapping;
        public String m_clustering;
        public String m_clusteringType;
    }

    public int getRunCount() {
        return m_runCount;
    }

    public RunFileSaver(String a_system, String a_metric, boolean a_doSaveMappings) {
        m_errorCounter = 0;
        m_runCount = 0;
        m_filePath = createFile(a_system, a_metric, a_system + "_" + a_metric);


        ArrayList<String> row = new ArrayList<>();
        row.add("date");
        row.add("time");
        row.add("localId");
        row.add("initialClustered");
        row.add("totalMapped");
        row.add("initialDistribution");
        row.add("iterations");
        row.add("totalManuallyClustered");
        row.add("totalAutoClustered");
        row.add("totalAutoWrong");
        row.add("totalFailedClusterings");
        row.add("mappingPercent");
        row.add("metric");
        row.add("system");

        row.add("omega");
        row.add("phi");

        writeRow(m_filePath, row);

        if (a_doSaveMappings) {
            m_mappingsFilePath = createFile(a_system,   "mappings" + File.separator + a_metric, a_system + "_" + a_metric + "_mappings");
            row = new ArrayList<>();
            row.add("runId");
            row.add("nodeId");
            row.add("runFile");
            row.add("mapping" );
            row.add("clustering");
            row.add("clusteringType");
            row.add("metric");
            row.add("metricValue");

            writeRow(m_mappingsFilePath, row);
        } else {
            m_mappingsFilePath = null;
        }
    }

    private Path initSaveFile(ExperimentRunData.BasicRunData a_rd) {
        Path p = createFile(a_rd.m_system.getName(), a_rd.m_metric.getName(), a_rd.m_system + "_" + a_rd.m_metric.getName());


        ArrayList<String> row = new ArrayList<>();
        row.add("date");
        row.add("time");
        row.add("localId");
        row.add("initialClustered");
        row.add("totalMapped");
        row.add("initialDistribution");
        row.add("iterations");
        row.add("totalManuallyClustered");
        row.add("totalAutoClustered");
        row.add("totalAutoWrong");
        row.add("totalFailedClusterings");
        row.add("mappingPercent");
        row.add("metric");
        row.add("system");

        row.add("omega");
        row.add("phi");

        writeRow(p, row);

        return p;
    }


     public ExperimentRunData.BasicRunData OnRunInit(ExperimentRunData.BasicRunData a_rd, CGraph a_g, ArchDef a_arch) {
        a_rd.m_initialDistribution = getInitialClusterDistributionString(a_g, a_arch);
        return a_rd;
    }

    public void OnRunCompleted(ExperimentRunData.BasicRunData a_rd, CGraph a_g, ArchDef a_arch) {

        if (m_filePath == null) {
            m_filePath = initSaveFile(a_rd);
        }

        ArrayList<String> row = new ArrayList<>();
        row.add(a_rd.m_date);
        row.add("" + a_rd.m_time);
        row.add("" + a_rd.m_id);
        row.add("" + a_rd.getInitialClusteringNodeCount());
        row.add("" + a_rd.m_totalMapped);
        row.add(a_rd.m_initialDistribution);
        row.add("" + a_rd.m_iterations);
        row.add("" + a_rd.m_totalManuallyClustered);
        row.add("" + a_rd.getAutoClusteredNodeCount());
        row.add("" + a_rd.m_totalAutoWrong);
        row.add("" + a_rd.m_totalFailedClusterings);
        row.add("" + a_rd.m_initialClusteringPercent);
        row.add(a_rd.m_metric.getName());
        row.add(a_rd.m_system.getName());

        if (a_rd instanceof ExperimentRunData.HuGMEData){
            ExperimentRunData.HuGMEData rd = (ExperimentRunData.HuGMEData)a_rd;

            row.add("" + rd.m_omega);
            row.add("" + rd.m_phi);
        }

        writeRow(m_filePath, row);

        if (m_mappingsFilePath != null) {
            for(CNode n: a_g.getNodes()) {
                ArchDef.Component mapped;

                mapped = a_arch.getMappedComponent(n);

                if (mapped != null)  {
                    ArchDef.Component clustered;
                    clustered = a_arch.getClusteredComponent(n);

                    row = new ArrayList<>();
                    row.add("" + a_rd.m_id);
                    row.add(m_filePath.toString());
                    row.add(n.getName());
                    row.add(mapped.getName());
                    if (clustered !=  null) {
                        row.add(clustered.getName());
                    } else {
                        row.add("n/a");
                    }
                    row.add(mapped.getClusteringType(n).toString());
                    row.add(a_rd.m_metric.getName());
                    row.add("" + a_rd.m_metric.getMetric(n));

                    writeRow(m_mappingsFilePath, row);
                }
            }
        }

        m_runCount++;
    }

    private void writeRow(Path a_filePath, String a_row) {
        try {
            write(a_filePath, a_row.getBytes(), StandardOpenOption.APPEND);
            m_errorCounter = 0;
        } catch (Exception e) {
            if (m_errorCounter > 100) {
                System.out.println("Could not write row " + e.getMessage() + e.getStackTrace());
                System.out.println("Exiting!");
                System.exit(-1);
            } else {
                m_errorCounter++;
                Random r = new Random();
                try{Thread.sleep((long)(r.nextDouble() * 1717));} catch (Exception e2) {};
                writeRow(a_filePath, a_row);
            }
        }
    }

    private void writeRow(Path a_filePath, Iterable<String> m_strings) {

        String txtRow = "";
        for (String s : m_strings) {
            txtRow += s + "\t";
        }

        txtRow = txtRow.substring(0, txtRow.length() - 1);
        txtRow += "\r\n";
        writeRow(a_filePath, txtRow);

    }

    private void handleError(String a_errorText, Exception a_e) {
        if (m_errorCounter > 100) {
            System.out.println(a_errorText);
            System.out.println(a_e.getMessage());
            System.out.println(a_e.getStackTrace());
            System.out.println("Exiting!");
            System.exit(-1);
        } else {
            m_errorCounter++;
        }
    }

    private void createDir(Path a_p) {
        if (a_p != null) {
            try {
                Files.createDirectories(a_p);
                m_errorCounter = 0;
            } catch (FileAlreadyExistsException a_faee) {
                // this is fine
            } catch (Exception a_e) {
                handleError("Could not create Dir: " + a_p.toString(), a_e);
                createDir(a_p);
            }
        }
    }

    private Path createFile(String a_dir1, String a_dir2, String a_fileName) {
        String fileName = a_dir1 + File.separator + a_dir2 + File.separator + a_fileName;
        java.nio.file.Path fp = Paths.get(fileName + "_0.csv");

        createDir(fp.getParent());

        int i = 1;
        while (exists(fp)) {
            fp = Paths.get(fileName + "_" + i + ".csv");
            i++;
        }

        try {
            Files.createFile(fp);
            m_errorCounter = 0;
        } catch (Exception e) {
            if (m_errorCounter > 100) {
                System.out.println("Could not Create file: " + fp.toString());
                System.out.println(e.getMessage());
                System.out.println(e.getStackTrace());
                System.out.println("Exiting!");
                System.exit(-1);
                return null;
            } else {
                m_errorCounter++;
                Random r = new Random();
                try {
                    Thread.sleep((long) (r.nextDouble() * 1717));
                } catch (Exception e2) {
                }
                ;
                return createFile(a_dir1, a_dir2, a_fileName);
            }
        }

        return fp;
    }

    private String getInitialClusterDistributionString(CGraph a_g, ArchDef a_arch) {
        String ret="";

        for (ArchDef.Component c : a_arch.getComponents()) {
            int count = 0;
            for (CNode a_n : a_g.getNodes()) {
                if (c == a_arch.getClusteredComponent(a_n)) {
                    count++;
                }
            }
            ret += "," + c.getName() + ":" + count;
        }

        ret = ret.substring(1); // removes first ","
        ret = "[" + ret + "]";

        return ret;
    }
}
