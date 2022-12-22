package org.teamtators.Util;

public interface SwerveGyro {
    public void zero();
    public double getYawD();
    public  double getYawContinuous();

    public boolean isConnected();
    public void setCurrentAngle(double angle);
}
