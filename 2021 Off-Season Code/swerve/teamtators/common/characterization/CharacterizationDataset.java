package org.teamtators.common.characterization;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

public class CharacterizationDataset {
    private String datasetName;
    private List<CSVRecord> records;

    private CharacterizationDataset() {}

    public String getDatasetName() {
        return datasetName;
    }

    public List<CSVRecord> getRecords() {
        return records;
    }

    public static CharacterizationDataset fromCSV(File file) throws IOException {
        CSVParser parser = CSVParser.parse(file, Charset.defaultCharset(), CSVFormat.EXCEL);
        CharacterizationDataset dataset = new CharacterizationDataset();
        dataset.datasetName = file.getName();
        dataset.records = parser.getRecords();
        return dataset;
    }
}
