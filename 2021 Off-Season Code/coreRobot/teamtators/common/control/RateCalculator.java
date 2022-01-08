package org.teamtators.common.control;

import java.util.function.DoubleSupplier;

public class RateCalculator implements Updatable {
    private DoubleSupplier input;
    private volatile double last = 0;
    private double[] pastSamples;
    private int sampleIdx = 0;
    private volatile double avgSampled = 0;

    public RateCalculator(DoubleSupplier input, int sampleCount) {
        this.input = input;
        pastSamples = new double[sampleCount];
    }

    @Override
    public void update(double delta) {
        double current = input.getAsDouble();
        double rate_inst = (current - last) / delta;
//        System.out.println(rate_inst);
        pastSamples[sampleIdx] = rate_inst;
        double accum = 0;
        for (int i = 0; i < pastSamples.length; i++) {
            accum += pastSamples[i];
        }
        avgSampled = accum / pastSamples.length;
        sampleIdx = sampleIdx == pastSamples.length - 1 ? 0 : sampleIdx + 1;
        last = current;
    }

    public double getRate() {
        return avgSampled;
    }
}
