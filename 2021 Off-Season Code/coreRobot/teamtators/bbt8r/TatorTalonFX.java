package org.teamtators.bbt8r;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.TalonFXControlMode;
import com.ctre.phoenix.motorcontrol.can.TalonFX;
import edu.wpi.first.wpilibj.SpeedController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TatorTalonFX extends TalonFX implements SpeedController {

    public final double ENCODER_TICKS_PER_REVOLUTION = 2048.0;
    private static final Logger logger = LoggerFactory.getLogger(TatorTalonFX.class);

    /**
     * Constructor
     *
     * @param deviceID [0,62]
     */
    public TatorTalonFX(int deviceID) {
        super(deviceID);
    }

    @Override
    public void set(double speed) {
        set(TalonFXControlMode.Velocity, RPMtoETPDS(speed));
    } // 600

    @Override
    public void setVoltage(double voltage) {
        set(ControlMode.Current, voltage);
    }

    /**
     *
     * @return returns the velocity of the motor in RPM
     */
    @Override
    public double get() {
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
     * I have no idea what the difference between this and set() is.
     *
     * @param output
     */
    @Override
    public void pidWrite(double output) {

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

    public void setPercentOutput(double percentOutput) {
        set(ControlMode.PercentOutput, percentOutput);
    }

}
