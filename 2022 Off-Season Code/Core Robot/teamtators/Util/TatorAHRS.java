package org.teamtators.Util;

import com.kauailabs.navx.frc.AHRS;
import edu.wpi.first.wpilibj.I2C;
import edu.wpi.first.wpilibj.SPI;
import edu.wpi.first.wpilibj.SerialPort;

public class TatorAHRS extends AHRS implements SwerveGyro{
    private float offsetYaw = 0;
    private double offsetAngle = 0;
    //    private double ratio = 392.4 / 360.0;
//    private double ratio = 1.0;
    private double ratio;

    public TatorAHRS(I2C.Port i2c_port_id) {
        super(i2c_port_id);
    }

    public TatorAHRS(SerialPort.Port port) {
        super(port);
        ratio = 1;
    }

    public TatorAHRS(SPI.Port port) {
        super(port);
//        ratio = 392.4 / 360.0;
        ratio = 1;
    }

    public void zeroYawNAngle() {
        offsetYaw = super.getYaw();
        offsetAngle = super.getAngle();
        System.out.println("Software Yaw Reset | Yaw -->  " + getYawWithOffset() + "  " + getAngleWithOffset() + "  <-- Angle");
    }

    public float getYawWithOffset() {
//        System.out.println((float) ((super.getYaw() / ratio) - offsetYaw));
        return (float) ((super.getYaw() / ratio) - offsetYaw);
    }

    public double superGetAngle() {
        return super.getAngle();
    }

    public double getAngleWithOffset() {
        return ((super.getAngle() / ratio) - offsetAngle);
    }

    public double getCurrentOffset() {
        return offsetAngle;
    }

    /**
     * @param angle The new angle in Radians
     */
    public void setAngle(double angle) {
        angle = Math.toDegrees(angle);
        double currentAngle = super.getAngle();
        offsetAngle = -(angle - currentAngle);
//        offsetAngle = -currentAngle + angle;

        double newYaw = ((angle % (360)) + 360) % 360;
        if (newYaw > 180) {
            //newYaw = -180 + (newYaw % 180);
            newYaw = newYaw - 360;
        }

    }

    @Override
    public void zero() {
        zeroYawNAngle();

    }

    @Override
    public double getYawD() {
        return getYaw();
    }

    @Override
    public double getYawContinuous() {
        return getAngleWithOffset();
    }

    @Override
    public void setCurrentAngle(double angle) {
        // TODO Auto-generated method stub
        
    }
}
