package org.teamtators.common.control;

import org.teamtators.common.hw.TatorSparkMax;

import java.util.List;

public class FaultChecker implements Updatable {
    private List<TatorSparkMax> sparks;
    public FaultChecker() {

    }

    public void setSparks(List<TatorSparkMax> sparks) {
        this.sparks = sparks;
    }
    @Override
    public void update(double delta) {
        if(sparks == null) {
            return;
        }
        for(var spark : sparks) {
            short faults = spark.getFaults();
            if(faults != 0) {
                spark.clearFaults();
                System.out.println("CAN ID " + spark.getDeviceId() + " reported faults: "+faults);
            }
        }
    }
}
