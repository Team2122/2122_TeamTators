package org.teamtators.common.config.helpers;

import org.teamtators.common.hw.PressureSensor;

public class PressureSensorConfig {
    private int channel;
    private double supplyVoltage;
    private double slope;
    private double intercept;

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public double getSupplyVoltage() {
        return supplyVoltage;
    }

    public void setSupplyVoltage(double supplyVoltage) {
        this.supplyVoltage = supplyVoltage;
    }

    public double getSlope() {
        return slope;
    }

    public void setSlope(double slope) {
        this.slope = slope;
    }

    public double getIntercept() {
        return intercept;
    }

    public void setIntercept(double intercept) {
        this.intercept = intercept;
    }

    public PressureSensor create() {
        return new PressureSensor(channel, supplyVoltage, slope, intercept);
    }
}
