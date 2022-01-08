package org.teamtators.common.hw;

import com.revrobotics.CANEncoder;
import com.revrobotics.EncoderType;
import org.teamtators.bbt8r.TatorNEOEncoder;
import org.teamtators.common.config.Configurable;

public class NEOEncoder implements Configurable<NEOEncoder.Config> {
    private TatorNEOEncoder encoder;


    private double conversionFactor = 1;

    public NEOEncoder(TatorSparkMax sparkMax) {
//        this.encoder = sparkMax.getEncoder();
        this.encoder = new TatorNEOEncoder(sparkMax, EncoderType.kHallSensor, 0);
    }

    public double getDistance() {

        return encoder.getPosition() * conversionFactor;
    }

    public double getRotations() {
        //System.out.println("In Rotations Rotations: " + encoder.getPosition());
        return encoder.getPosition();
    }

    public int getCounts() {
        //System.out.println("encoder ticks per rev: " + encoder.getCountsPerRevolution());
        return (int) encoder.getPosition() * encoder.getCountsPerRevolution();
    }

    public double getRate() {
        return encoder.getVelocity() * conversionFactor;
    }

    public double getRateRPM() {
        return encoder.getVelocity();
    }

    public double getRateCPM() {
        return encoder.getVelocity() * encoder.getCountsPerRevolution();
    }

    public void reset() {
        setPositionRotations(0);
    }

    public void setPosition(double positionConvertedUnits) {
        encoder.setPosition(positionConvertedUnits / conversionFactor);
    }

    public void setPositionRotations(double rotations) {
        encoder.setPosition(rotations);
    }

    public boolean getInverted() {
//        return encoder.getInverted();
        return false;
    }

    public void setInverted(boolean inverted) {
//        encoder.setInverted(inverted);
    }

    public void configure(Config config) {
        this.conversionFactor = config.distancePerRevolution;
        setInverted(config.inverted);
    }

    public static class Config {
        public double distancePerRevolution;
        public boolean inverted;
    }
}
