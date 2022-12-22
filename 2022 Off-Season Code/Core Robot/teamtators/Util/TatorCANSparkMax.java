package org.teamtators.Util;

import com.revrobotics.CANSparkMax;
import com.revrobotics.CANSparkMaxLowLevel;
import com.revrobotics.RelativeEncoder;

import edu.wpi.first.wpilibj.motorcontrol.MotorController;

public class TatorCANSparkMax implements TatorMotor{

    private CANSparkMax canSparkMax;
    private TatorCANPID canPID;
    private RelativeEncoder canEncoder;
    public TatorCANSparkMax(int deviceId, CANSparkMaxLowLevel.MotorType type) {
        canSparkMax = new CANSparkMax(deviceId, type);
        this.canPID = new TatorCANPID(canSparkMax);
        this.canEncoder = canSparkMax.getEncoder();
    }


    @Override
    public MotorController getMotorController() {
        return canSparkMax;
    }

    public TatorCANPID getCanPID() {
        return canPID;
    }

    @Override
    public void setVelocity(double value) {
        canPID.setReference(value, CANSparkMax.ControlType.kVelocity);
    }

    @Override
    public void setPercentOutput(double value) {
        canSparkMax.set(value);
    }

    public void setPosition(double value){
        canPID.setPosition(value);
    }

    @Override
    public void setEncoderPosition(double value){
        System.out.println(value);
        canEncoder.setPosition(value);
    }

    // units must be checked
    @Override
    public double getVelocity() {
        return canEncoder.getVelocity();
    }

    @Override
    public double getPosition() {
        return canEncoder.getPosition();
    }

    @Override
    public void stop() {
        canSparkMax.stopMotor();
    }

    @Override
    public void setInverted(boolean inverted) {
        canSparkMax.setInverted(inverted);
    }

    public RelativeEncoder getEncoder(){
        return canEncoder;
    }
    public void configurePID(double p, double i, double d, double iZone, double f) {
        canPID.setPIDF(p, i, d, f);
        canPID.setIZone(iZone);
    }

}

