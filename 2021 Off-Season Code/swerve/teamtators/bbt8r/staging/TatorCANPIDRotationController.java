package org.teamtators.bbt8r.staging;

import com.revrobotics.CANPIDController;
import com.revrobotics.ControlType;
import org.teamtators.common.hw.NEOEncoder;
import org.teamtators.common.hw.TatorSparkMax;

public class TatorCANPIDRotationController extends CANPIDController {
    private final double MOTOR_ROTATIONS_PER_MODULE_ROTATION;
    private TatorSparkMax rotationMotor;
    private NEOEncoder rotationEncoder;

    private double targetAngle;
    private double currentAngle;
    private double desiredModuleAngle;
    private double desiredComplimentModuleAngle;
    private double desiredDeltaAngle;
    private double complimentDeltaAngle;
    private double deltaAngle;
    private double PI = Math.PI;
    private double TAU = 2 * PI;

    private boolean complimentSelected;

    public TatorCANPIDRotationController(TatorSparkMax device, double motorRotationsPerCircle){
        super(device);
        rotationMotor = device;
        rotationEncoder = device.getNeoEncoder();
        this.MOTOR_ROTATIONS_PER_MODULE_ROTATION = motorRotationsPerCircle;
    }

    /**
     * @author Ibrahim Ahmad
     * @return the distance from the original target and the current position before movement at this slice of time
     * possible use case Swerve modules flipping the motor power depending on whether the return is 1 or -1
     *
     * */
    public void setRelativePosition(double change){
        setReference(rotationEncoder.getDistance() + change, ControlType.kPosition);
    }

    public void setOptimizedPosition(double pos){
        currentAngle = motorRotationsToRadians(rotationEncoder.getRotations() % MOTOR_ROTATIONS_PER_MODULE_ROTATION);
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
            System.out.println("\tEncoder Rotations of Rotation Motor:                 " + rotationEncoder.getRotations());
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

    public double getDesiredModuleAngle(){
        return desiredModuleAngle;
    }

    public double getTargetAngle(){
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

    public double circleCalculate(){
        return Math.floor(rotationEncoder.getDistance() / MOTOR_ROTATIONS_PER_MODULE_ROTATION);
    }

    public boolean complimentSelected() {
        return complimentSelected;
    }


}
