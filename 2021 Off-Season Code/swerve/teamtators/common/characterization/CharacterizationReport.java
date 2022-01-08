package org.teamtators.common.characterization;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class CharacterizationReport {
    private static final Logger logger = LoggerFactory.getLogger(CharacterizationReport.class);
    private Map<String, String> characterizationOutput = new HashMap<>();
    private ArrayList<String[]> regressionPoints = new ArrayList<>();
    public void addCharacterizationOutput(String key, Object val) {
        characterizationOutput.put(key, String.valueOf(val));
    }
    public void addRegressionPoint (double stableVelocity, double inputPower) {
        regressionPoints.add(new String[] {String.valueOf(stableVelocity), String.valueOf(inputPower)});
    }
    public void outToCSV (String filepath) {
        try {
            FileWriter data = new FileWriter(filepath);
            CSVPrinter printer = new CSVPrinter(data, CSVFormat.EXCEL);
            printer.printRecord(Arrays.asList("velocity", "power"));
            printer.printRecords(regressionPoints);
            printer.println();
            printer.println();
            printer.printRecord(characterizationOutput.keySet());
            printer.printRecord(characterizationOutput.values());
            printer.flush();
            printer.close();
        } catch (Exception e) {
            logger.error("Error while writing characterization report", e);
        }
    }
    public String printReport() {
        StringBuilder report = new StringBuilder();
        report.append("========================================\n");
        report.append("        CHARACTERIZATION REPORT         \n");
        report.append("\n");
        characterizationOutput.forEach((k, v) -> report.append("==> ").append(k).append(" = ").append(v).append("\n"));
        report.append("\n");
        report.append("========================================\n");
        return report.toString();
    }
}
