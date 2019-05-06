package se.lnu.siq.s4rdm3x.experiments;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Random;

import static java.nio.file.Files.write;

public class RundDataCSVFileSaver {

    public void writeHeader(Path a_filePath) throws IOException {
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
        row.add("algorithm");

        // parameters for HuGMe
        row.add("omega");
        row.add("phi");

        // parameters for NBMapper
        row.add("threshold");
        row.add("stemming");
        row.add("wordcount");


        writeRow(a_filePath, row);
    }

    public void writeData(Path a_filePath, Iterable<? extends ExperimentRunData.BasicRunData> a_data) throws IOException {
        for (ExperimentRunData.BasicRunData d : a_data) {
            writeData(a_filePath, d);
        }
    }

    public void writeData(Path a_filePath, ExperimentRunData.BasicRunData a_rd) throws IOException {
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

            row.add("HuGMe");
            row.add("" + rd.m_omega);
            row.add("" + rd.m_phi);
        } else {
            row.add("");row.add("");
        }

        if (a_rd instanceof ExperimentRunData.NBMapperData){
            ExperimentRunData.NBMapperData rd = (ExperimentRunData.NBMapperData)a_rd;
            row.add("NaiveBayes");
            row.add("" + rd.m_threshold);
            row.add("" + (rd.m_doStemming ? "Y" : "N"));
            row.add("" + (rd.m_doWordCount ? "Y" : "N"));
        } else {
            row.add("");row.add("");row.add("");
        }

        writeRow(a_filePath, row);
    }

    private void writeRow(Path a_filePath, Iterable<String> m_strings) throws IOException {

        String txtRow = "";
        for (String s : m_strings) {
            txtRow += s + "\t";
        }

        txtRow = txtRow.substring(0, txtRow.length() - 1);
        txtRow += "\r\n";

        write(a_filePath, txtRow.getBytes(), StandardOpenOption.APPEND);

    }
}