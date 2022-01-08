package org.teamtators.common.hw;

import com.ctre.phoenix.motorcontrol.can.WPI_TalonSRX;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.config.Deconfigurable;

public class SRXEncoder implements Configurable<SRXEncoder.Config>, Deconfigurable {
    private Logger logger = LoggerFactory.getLogger(getClass());
    private WPI_TalonSRX motor;
    private double distancePerRotation;
    private boolean inverted;

    public SRXEncoder(WPI_TalonSRX motor) {
        this.motor = motor;
        reset(); //SRX encoder values persist between code restarts - we have to manually reset this here.
        //testHealth(); should work but doesn't.
        motor.getSelectedSensorPosition(0); //dummy read
        motor.getSelectedSensorVelocity(0); //dummy read
    }

    private void testHealth() {
        int us = motor.getSensorCollection().getPulseWidthRiseToRiseUs();
        if(us == 0) {
            logger.error("Sensor on a TalonSRX [canid = {}] not plugged in.", motor.getDeviceID());
        }
    }

    public double getDistance() {
        return (inverted ? -1 : 1) * (motor.getSelectedSensorPosition(0) / 4096.0) * distancePerRotation;
    }

    public double getVelocity() {
        return 10 * (inverted ? -1 : 1) * (motor.getSelectedSensorVelocity(0) / 4096.0) * distancePerRotation;
    }

    public void configure(Config config) {
        this.distancePerRotation = config.distancePerRotation;
        this.inverted = config.inverted;
    }

    @Override
    public void deconfigure() {

    }

    public void reset() {
        motor.setSelectedSensorPosition(0, 0, 0); //todo is 0ms okay?
    }

    public int get() {
        return (int)motor.getSelectedSensorPosition(0);
    }

    public WPI_TalonSRX getMotor() {
        return motor;
    }

    public static class Config {
        public double distancePerRotation = 1.0;
        public boolean inverted = false;
    }
}
