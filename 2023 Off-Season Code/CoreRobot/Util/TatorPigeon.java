package common.Util;

import com.ctre.phoenix6.hardware.Pigeon2;

import edu.wpi.first.math.geometry.Rotation2d;

public class TatorPigeon extends Pigeon2 {
    private double offset = 0;

    public TatorPigeon(int deviceNumber, String canbus) {
        super(deviceNumber, canbus);
        System.out.println("Boot gyro angle: " + this.getYaw());
    }

    public TatorPigeon(int deviceNumber) {
        super(deviceNumber);
    }

    public void zero() {
        offset = -getYaw().getValue() + 180;
    }


    public double getYawContinuous() {
        return Math.toRadians(getYaw().getValue() + offset);
    }

    public Rotation2d getRotation2d(){
        return Rotation2d.fromRadians(getYawContinuous());
    }

    public void setCurrentAngle(double angle) {
        offset = angle;
    }
}
