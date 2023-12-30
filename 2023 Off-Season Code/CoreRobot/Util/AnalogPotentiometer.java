package common.Util;

import edu.wpi.first.util.sendable.Sendable;
import edu.wpi.first.util.sendable.SendableBuilder;
import edu.wpi.first.util.sendable.SendableRegistry;
import edu.wpi.first.wpilibj.AnalogInput;
import edu.wpi.first.wpilibj.RobotController;

public class AnalogPotentiometer implements Sendable {
    public static final double DEFAULT_FULL_RANGE = 360.0;
    public static final double DEFAULT_OFFSET = 0.0;
    public static final boolean DEFAULT_CONTINUOUS = false;
    private AnalogInput analogInput;
    private double fullRange = DEFAULT_FULL_RANGE;
    private double offset = DEFAULT_OFFSET;
    private double minValue = 0.0;
    private boolean continuous = DEFAULT_CONTINUOUS;
    private boolean inverted = false;

    public AnalogPotentiometer(int channel) {
        SendableRegistry.add(this, "AnalogPotentiometer");
        analogInput = new AnalogInput(channel);

        SendableRegistry.addChild(this, analogInput);
    }

    public double getRawVoltage() {
        return analogInput.getVoltage();
    }

    public void close() {
        SendableRegistry.remove(this);
        analogInput.close();
    }

    public double get() {
        double p = analogInput.getAverageVoltage() / RobotController.getVoltage5V();
        if (inverted) {
            p = 1 - p;
        }
        double value = p * fullRange;
        double absFullRange = Math.abs(fullRange);
        value += offset;
        if (continuous) {
            while (value < minValue) {
                value += absFullRange;
            }
            while (value > (minValue + fullRange)) {
                value -= absFullRange;
            }
        }
        return value;
    }

    public double getFullRange() {
        return fullRange;
    }

    public void setFullRange(double fullRange) {
        this.fullRange = fullRange;
    }

    public double getOffset() {
        return offset;
    }

    public void setOffset(double offset) {
        this.offset = offset;
    }

    public boolean isContinuous() {
        return continuous;
    }

    public void setContinuous(boolean continuous) {
        this.continuous = continuous;
    }

    public double getMinValue() {
        return minValue;
    }

    public void setMinValue(double minValue) {
        this.minValue = minValue;
    }

    public void setAverageBits(int averageBits) {
        analogInput.setAverageBits(averageBits);
    }

    public int getAverageBits() {
        return analogInput.getAverageBits();
    }

    public void setOversampleBits(int oversampleBits) {
        analogInput.setOversampleBits(oversampleBits);
    }

    public int getOversampleBits() {
        return analogInput.getOversampleBits();
    }

    public AnalogInput getAnalogInput() {
        return analogInput;
    }

    @Override
    public void initSendable(SendableBuilder builder) {
        builder.setSmartDashboardType("Analog Input");
        builder.addDoubleProperty("Value", this::get, null);
        builder.addDoubleProperty("Offset", this::getOffset, this::setOffset);
    }

    public void setInverted(boolean inverted) {
        this.inverted = inverted;
    }
    
}
