package org.teamtators.common.hw;

import edu.wpi.first.wpilibj.AnalogInput;

public class PressureSensor {
    private AnalogInput pressureSensor;
    private double supplyVoltage;
    private double slope;
    private double intercept;

    /**
     * @param pressureSensor the input that represents the pressureSensor
     * @param supplyVoltage  the supply voltage in VDC
     * @param slope          (outputVoltage / supplyVoltage) * slope
     */
    public PressureSensor(AnalogInput pressureSensor, double supplyVoltage, double slope, double intercept) {
        this.pressureSensor = pressureSensor;
        this.supplyVoltage = supplyVoltage;
        this.slope = slope;
        this.intercept = intercept;
    }

    /**
     * @param supplyVoltage the supply voltage in VDC
     */
    public PressureSensor(int channel, double supplyVoltage, double slope, double intercept) {
        this(new AnalogInput(channel), supplyVoltage, slope, intercept);
    }

    public double getPressure() {
        double outputVoltage = getVoltage();
        return slope * (outputVoltage / supplyVoltage) + intercept;
    }

    public double getVoltage() {
        return pressureSensor.getVoltage();
    }
}
