package org.teamtators.common.characterization;

import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamtators.common.datalogging.DataCollector;

import java.util.ArrayList;
import java.util.List;

public class SmartMotionCharacterizationRoutine extends MotorCharacterizationRoutine {
    private static final Logger logger = LoggerFactory.getLogger(SmartMotionCharacterizationRoutine.class);
    @Override
    protected void doCharacterization(List<List<MotorDataRecord>> data) {
        SimpleRegression kVRegression = new SimpleRegression();
        CharacterizationReport report = new CharacterizationReport();
        for(List<MotorDataRecord> records : data) {
            double stableVelocity = findStableVelocity(records);
            double inputPower = records.get(0).powerApplied;
            logger.info("({}, {})", stableVelocity, inputPower);
            kVRegression.addData(stableVelocity, inputPower); //assume constant power because that's how this works!
            report.addRegressionPoint(stableVelocity, inputPower);
        }
        report.addCharacterizationOutput("kV", kVRegression.getSlope());
        report.addCharacterizationOutput("kS", kVRegression.getIntercept());
        report.addCharacterizationOutput("R^2", kVRegression.getRSquare());
        System.out.println(report.printReport());
        report.outToCSV(DataCollector.getDataCollector().getFilename(DataCollector.getDataCollector().getFormattedNow(),
                "CharacterizationReport"));
    }






    private double findStableVelocity(List<MotorDataRecord> records) {
        double trackingVal = Double.NEGATIVE_INFINITY;

        double stableThreshold = 25.0;

        ArrayList<MotorDataRecord> recordsInThreshold = new ArrayList<>();

        for(MotorDataRecord record : records) {
            if(Math.abs(record.velocity - trackingVal) > stableThreshold) {
                trackingVal = record.velocity;
                recordsInThreshold.clear();
            }
            recordsInThreshold.add(record);
        }

        return averageVelocity(recordsInThreshold);
    }


    private double averageVelocity(List<MotorDataRecord> records) {
        return records.stream().mapToDouble(r -> r.velocity).average().orElse(Double.NaN);
    }
}
