package org.teamtators.Util;

import com.ctre.phoenix.sensors.Pigeon2;

import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

public class TatorPigeon extends Pigeon2 implements SwerveGyro {
    private Timer timer;
    private double last;
    private double offset = 0;
    public NetworkTableEntry gyroOffset;

    public TatorPigeon(int deviceNumber, String canbus) {
        super(deviceNumber, canbus);
        timer = new Timer();
    }

    public TatorPigeon(int deviceNumber) {
        super(deviceNumber);
        timer = new Timer();
    }

    @Override
    public void zero() {
        offset = getYaw() + 90;
    }

    @Override
    public double getYawD() {
        return 270 - getYaw() + offset;
    }

    private void updateYaw() {
        last = getYawD();
    }

    @Override
    public double getYawContinuous() {
        updateYaw();
        return last;
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public void setCurrentAngle(double angle) {
        zero();
        offset += angle;
    }
}
