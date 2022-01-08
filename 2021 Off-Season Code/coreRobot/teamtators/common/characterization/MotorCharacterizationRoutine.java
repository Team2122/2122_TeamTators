package org.teamtators.common.characterization;

import org.apache.commons.csv.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public abstract class MotorCharacterizationRoutine implements CharacterizationRoutine {
    private static Logger logger = LoggerFactory.getLogger(MotorCharacterizationRoutine.class);
    @Override
    public void characterize(List<CharacterizationDataset> data) {
        List<List<MotorDataRecord>> out = new ArrayList<>();
        for (CharacterizationDataset set : data) {
            logger.trace("Building motor data from {}", set.getDatasetName());
            List<CSVRecord> records = set.getRecords();
            ArrayList<MotorDataRecord> datasets = new ArrayList<>();
            for (CSVRecord record : records) {
                if (records.get(0) == record) {
                    continue; //skip names row
                }
                Optional<Double> timestamp = readDoubleChecked(record, 0);
                Optional<Double> applied = readDoubleChecked(record, 1);
                Optional<Double> position = readDoubleChecked(record, 2);
                Optional<Double> velocity = readDoubleChecked(record, 3);
                if (timestamp.isEmpty() || applied.isEmpty() || position.isEmpty() || velocity.isEmpty()) {
                    logger.info("Bad row!");
                    continue;
                }
                MotorDataRecord motorRecord = new MotorDataRecord(timestamp.get(), applied.get(), position.get(), velocity.get());
                logger.trace("Read record: {}", motorRecord);
                datasets.add(motorRecord);
            }
            out.add(datasets);
        }
        doCharacterization(out);
    }

    protected void outCSV (String filepath) {

    }

    protected Optional<Double> readDoubleChecked(CSVRecord record, int idx) {
        if (record.size() <= idx) {
            return Optional.empty(); //bad
        }
        String val = record.get(idx);
        if (val.isEmpty()) {
            return Optional.empty();
        }
        double v;
        try {
            v = Double.parseDouble(val);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
        return Optional.of(v);
    }

    protected abstract void doCharacterization(List<List<MotorDataRecord>> data);

    public static class MotorDataRecord {
        public final double timestamp;
        public final double powerApplied;
        public final double position;
        public final double velocity;

        public MotorDataRecord(double timestamp, double powerApplied, double position, double velocity) {
            this.timestamp = timestamp;
            this.powerApplied = powerApplied;
            this.position = position;
            this.velocity = velocity;
        }

        @Override
        public String toString() {
            return String.format("[ts=%.2f, pa=%.4f, s=%.2f, v=%.4f]", timestamp, powerApplied, position, velocity);
        }
    }
}
