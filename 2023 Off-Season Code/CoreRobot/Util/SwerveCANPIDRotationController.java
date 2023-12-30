package common.Util;


import com.revrobotics.CANSparkMax;
import com.revrobotics.RelativeEncoder;
import com.revrobotics.SparkMaxPIDController;
import com.revrobotics.CANSparkMax.ControlType;


public class SwerveCANPIDRotationController {
    private final double MOTOR_ROTATIONS_PER_MODULE_ROTATION;
    private CANSparkMax rotationMotor;
    private RelativeEncoder rotationEncoder;

    private double targetAngle;
    private double currentAngle;
    private double desiredModuleAngle;
    private double desiredComplimentModuleAngle;
    private double desiredDeltaAngle;
    private double complimentDeltaAngle;
    private double deltaAngle;
    private double PI = Math.PI;
    private double TAU = 2 * PI;
    private SparkMaxPIDController pidController;

    private boolean complimentSelected;

    public SwerveCANPIDRotationController(CANSparkMax device, double motorRotationsPerCircle) {
        rotationMotor = device;
        pidController = device.getPIDController();
        rotationEncoder = device.getEncoder();
        this.MOTOR_ROTATIONS_PER_MODULE_ROTATION = motorRotationsPerCircle;
    }

    /**
     * @return the distance from the original target and the current position before movement at this slice of time
     * possible use case Swerve modules flipping the motor power depending on whether the return is 1 or -1
     * @author Ibrahim Ahmad
     */

    public double getSteerAngle() {
        return motorRotationsToRadians(rotationEncoder.getPosition());
    }

    public double setOptimizedPositionNew(double steerAngle) {
        double driveVoltage = 1;
        steerAngle %= (2.0 * Math.PI);
        if (steerAngle < 0.0) {
            steerAngle += 2.0 * Math.PI;
        }

        double difference = steerAngle - getSteerAngle();
        // Change the target angle so the difference is in the range [-pi, pi) instead of [0, 2pi)
        if (difference >= Math.PI) {
            steerAngle -= 2.0 * Math.PI;
        } else if (difference < -Math.PI) {
            steerAngle += 2.0 * Math.PI;
        }
        difference = steerAngle - getSteerAngle(); // Recalculate difference

        // If the difference is greater than 90 deg or less than -90 deg the drive can be inverted so the total
        // movement of the module is less than 90 deg
        if (difference > Math.PI / 2.0 || difference < -Math.PI / 2.0) {
            // Only need to add 180 deg here because the target angle will be put back into the range [0, 2pi)
            steerAngle += Math.PI;
            driveVoltage *= -1.0;
        }

        // Put the target angle back into the range [0, 2pi)
        steerAngle %= (2.0 * Math.PI);
        if (steerAngle < 0.0) {
            steerAngle += 2.0 * Math.PI;
        }


        setNewReference(steerAngle);

        return driveVoltage;
    }

    private void setNewReference(double position) {
        double currentAngleRadiansMod = getSteerAngle() % (2.0 * Math.PI);
        if (currentAngleRadiansMod < 0.0) {
            currentAngleRadiansMod += 2.0 * Math.PI;
        }

        // The reference angle has the range [0, 2pi) but the Falcon's encoder can go above that
        double adjustedReferenceAngleRadians = position + getSteerAngle() - currentAngleRadiansMod;
        if (position - currentAngleRadiansMod > Math.PI) {
            adjustedReferenceAngleRadians -= 2.0 * Math.PI;
        } else if (position - currentAngleRadiansMod < -Math.PI) {
            adjustedReferenceAngleRadians += 2.0 * Math.PI;
        }

        pidController.setReference(radiansToMotorRotations(adjustedReferenceAngleRadians), ControlType.kPosition);
    }

    public void setRelativePosition(double change) {
        pidController.setReference(rotationEncoder.getPosition() * MOTOR_ROTATIONS_PER_MODULE_ROTATION + change, ControlType.kPosition);
    }

    public void setOptimizedPosition(double pos) {
        currentAngle = motorRotationsToRadians(rotationEncoder.getPosition() % MOTOR_ROTATIONS_PER_MODULE_ROTATION);
        currentAngle += TAU;
        currentAngle %= TAU;
        desiredModuleAngle = pos;
        desiredComplimentModuleAngle = (desiredModuleAngle + PI) % (TAU);

        // Find the delta angles
        desiredDeltaAngle = findDeltaAngle(currentAngle, desiredModuleAngle);
        complimentDeltaAngle = findDeltaAngle(currentAngle, desiredComplimentModuleAngle);

        // Find the best angle
        if (Math.abs(desiredDeltaAngle) < Math.abs(complimentDeltaAngle)) {
            deltaAngle = desiredDeltaAngle;
            targetAngle = desiredModuleAngle;
            complimentSelected = false;
        } else {
            deltaAngle = complimentDeltaAngle;
            targetAngle = desiredComplimentModuleAngle;
            complimentSelected = true;
        }

        // We should never be moving more than pi/2 -> print out all our information
        if (Math.abs(deltaAngle) > (Math.PI / 2.0)) {
            System.out.println("DELTA ANGLE IS LARGER THAN PI/2!!!!!!!!!!!");
            System.out.println("\tEncoder Rotations of Rotation Motor:                 " + rotationEncoder.getPosition());
            System.out.println("\tcurrentAngle:                                        " + currentAngle);
            System.out.println("\tdesiredModuleAngle:                                  " + desiredModuleAngle);
            System.out.println("\tdesiredComplimentModuleAngle:                        " + desiredComplimentModuleAngle);
            System.out.println("\tdesiredDeltaAngle:                                   " + desiredDeltaAngle);
            System.out.println("\tcomplimentDeltaAngle:                                " + complimentDeltaAngle);
            System.out.println("\tdeltaAngle:                                          " + deltaAngle);
            System.out.println("\tFinal Rotations:                                     " + radiansToMotorRotations(deltaAngle));
        }

        // Go to the best angle
        setRelativePosition(radiansToMotorRotations(deltaAngle));
    }

    public double motorRotationsToRadians(double rotations) {
        return (rotations / MOTOR_ROTATIONS_PER_MODULE_ROTATION) * TAU;
    }

    public double getDesiredModuleAngle() {
        return desiredModuleAngle;
    }

    public double getTargetAngle() {
        return targetAngle;
    }

    public double radiansToMotorRotations(double radians) {
        return (radians / TAU) * MOTOR_ROTATIONS_PER_MODULE_ROTATION;
    }

    public double findDeltaAngle(double currentAngle, double desiredAngle) {
        double delta1 = desiredAngle - currentAngle;
        double delta2 = (2 * PI) - Math.abs(delta1);
        delta2 = (delta1 > 0) ? -delta2 : delta2;
        return (Math.abs(delta1) < Math.abs(delta2)) ? delta1 : delta2;
    }

    public double circleCalculate() {
        return Math.floor(rotationEncoder.getPosition() / MOTOR_ROTATIONS_PER_MODULE_ROTATION);
    }

    public boolean complimentSelected() {
        return complimentSelected;
    }

    public void setP(double kP) {
        pidController.setP(kP);
    }

    public void setD(double kD) {
        pidController.setD(kD);
    }

    public double getP() {
        return pidController.getP();
    }

    public void setReference(double position, ControlType controlType) {
        pidController.setReference(position, controlType);
    }

}
