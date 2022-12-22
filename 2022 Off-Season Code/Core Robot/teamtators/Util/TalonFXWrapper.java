package org.teamtators.Util;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.TalonFXControlMode;
import com.ctre.phoenix.motorcontrol.can.WPI_TalonFX;

import edu.wpi.first.wpilibj.motorcontrol.MotorController;

public class TalonFXWrapper extends WPI_TalonFX implements TatorMotor{

    public final double ENCODER_TICKS_PER_REVOLUTION = 2048.0;
    private double offset = 0;
    /**
     * Constructor
     *
     * @param deviceID [0,62]
     */
    public TalonFXWrapper(int deviceID) {
        super(deviceID);
    }

    public TalonFXWrapper(int deviceID, String canbus){
        super(deviceID,canbus);
    }

    @Override
    public MotorController getMotorController() {
        return this;
    }

    /**
     * A method for setting the speed of the motor
     *
     * @param speed The speed in RPM of the motor
     */
    @Override
    public void setVelocity(double speed) {
        set(TalonFXControlMode.Velocity, RPMtoETPDS(speed));
    }

    @Override
    public void setVoltage(double voltage) {
        set(ControlMode.Current, voltage);
    }

    /**
     * @return returns the velocity of the motor in RPM
     */
    @Override
    public double getVelocity() {
        return ETPDStoRPM(getSelectedSensorVelocity());
    }

    public void setInverted(boolean isInverted) {
        super.setInverted(isInverted);
    }

    public boolean getInverted() {
        return super.getInverted();
    }

    @Override
    public void disable() {
        set(ControlMode.PercentOutput, 0);
    }

    @Override
    public void stopMotor() {
        set(ControlMode.PercentOutput, 0);
    }

    /**
     * Converts RPM to the equivalent Revolutions Per Decisecond (RPDS)
     *
     * @param RPM RPM input
     * @return the equivalent of the RPM input in RPDS
     */
    public double RPMtoRPDS(double RPM) {
        return RPM / (60 * 10);
    }

    /**
     * Converts Revolutions per Decisecond (RPDS) to the equivalent RPM
     *
     * @param RPDS Revolutions Per Decisecond input
     * @return the equivalent of the RPDS input in RPM
     */
    public double RPDStoRPM(double RPDS) {
        return RPDS * (60 * 10);
    }

    /**
     * Converts RPM to the equivalent Encoder Ticks Per Decisecond (ETPDS)
     *
     * @param RPM RPM input
     * @return the equivalent of the RPM input in ETPDS
     */
    public double RPMtoETPDS(double RPM) {
        return (RPM * ENCODER_TICKS_PER_REVOLUTION * (1.0 / 60.0) * (1.0 / 10.0));
    }

    /**
     * Converts Encoder Ticks Per Decisecond (ETPDS) to the equivalent RPM
     *
     * @param ETPDS Encoder Ticks Per Decisecond input
     * @return the equivalent of the ETPDS input in RPM
     */
    public double ETPDStoRPM(double ETPDS) {
        return (ETPDS * (1.0 / ENCODER_TICKS_PER_REVOLUTION) * 60 * 10);
    }

    @Override
    public void setEncoderPosition(double value){
        offset = value - getPosition();
    }

    @Override
    public void configurePID(double p, double i, double izone, double d, double f) {
        config_kP(0, p);
        config_kI(0, i);
        config_kD(0, d);
        config_IntegralZone(0, izone);
        config_kF(0, f);
    }

    public void setPercentOutput(double percentOutput) {
        set(ControlMode.PercentOutput, percentOutput);
    }

    @Override
    public void setPosition(double value) {
        set(ControlMode.Position, value);
    }

    @Override
    public void stop() {
        stopMotor();
    }

    @Override
    public double getPosition() {
        return  getSelectedSensorPosition()* (1.0 / ENCODER_TICKS_PER_REVOLUTION) + offset;
    }
}