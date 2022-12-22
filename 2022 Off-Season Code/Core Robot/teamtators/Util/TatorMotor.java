package org.teamtators.Util;

import edu.wpi.first.wpilibj.motorcontrol.MotorController;

public interface TatorMotor {
    public MotorController getMotorController();
    public void setVelocity(double value);
    public void setPercentOutput(double value);
    public void setPosition(double value);
    public double getVelocity();
    public double getPosition();
    public void stop();
    public void setInverted(boolean inverted);
    public void configurePID(double p, double i, double d, double iZone, double f);
    public void setEncoderPosition(double value);
}
