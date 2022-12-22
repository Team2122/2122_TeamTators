package frc.robot.subsystems;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.NeutralMode;
import com.ctre.phoenix.motorcontrol.can.TalonFX;
import com.ctre.phoenix.sensors.CANCoderStatusFrame;
import com.revrobotics.CANSparkMax;
import com.revrobotics.RelativeEncoder;
import com.revrobotics.CANSparkMax.ControlType;

import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.wpilibj.DutyCycleEncoder;
import edu.wpi.first.wpilibj.motorcontrol.Talon;
import frc.robot.constants.SwerveConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamtators.Util.*;


public class SwerveModule {

    /**
     * An enumeration for the control state of the module
     */
    public enum ModuleState {
        /**
         * The rotation PID is running and the module will try and move to the correct angle
         */
        Enabled,
        /**
         * The rotation PID is not running and the module can be moved by hand
         */
        Disabled
    }

    private ModuleState currentState = ModuleState.Enabled;

    //Config Values
    private final int moduleNumber;
    private final double MOTOR_ROTATIONS_PER_MODULE_ROTATION;
    private final double MOTOR_ROTATIONS_PER_WHEEL_ROTATION;
    private final double WHEEL_CIRCUMFERENCE;
//    double x = 0;

    // Vector each module needs to apply to only rotate.
    // Proper method of accessing is rotationVector[moduleNumber][0] is the X and [moduleNumber][1] is y.
    private final double[][] mainBotModulePositions = {{-1,-1}, {-1, 1}, {1, 1}, {1, -1}};
    private final double[][] swerveBotModulePositions = {{-16, -13.5}, {16, -13.5}, {16, 13.5}, {-16, 13.5}};

    private final double[] directionFixOffset = {1, 1, 1, 1};
    private final double[] rotationFixOffset = {0, 0, 0, 0};

    private double[] mainBotEncoderOffsets = {0, 0, 0, 0};

    // These are the starting values
    private final double[] compBotEncoderOffsets = SwerveConstants.SwerveModule.SWERVE_MODULE_OFFSETS.clone();

    // Hardware
    private volatile CANCoderWrapper absoluteEncoder;     // Encoder used for checking rotation angles
    private CANSparkMax rotationMotor;          // NEO550 is rotation motor
    private TalonFXWrapper movementMotor;      // Talon FX is the movement motor
    private RelativeEncoder rotationEncoder;
    private double direction;

    // Controllers
    private SwerveCANPIDRotationController canPID;

    // Vectors
    private Vector endVector = new Vector();    // Vector used for moving the motors
    private Vector rotationVector = new Vector();
    private Vector tempRotate = new Vector();

    // Logger

    // Constants
    private double PI = Math.PI;
    private double TAU = 2 * PI;
    private double MAX_ERROR = .01;

    boolean mainBot = true;

    public SwerveModule(int module, TalonFXWrapper driveMotor, CANSparkMax rotationMotor, CANCoderWrapper canCoder) {
        this.moduleNumber = module;
        driveMotor.setNeutralMode(NeutralMode.Brake);
        movementMotor = driveMotor;
        this.rotationMotor = rotationMotor;
        // if (moduleNumber != 0) {
            rotationMotor.setInverted(false);
        // }
        // else{
            // rotationMotor.setInverted(false);
        // }
        this.absoluteEncoder = canCoder;
        this.MOTOR_ROTATIONS_PER_WHEEL_ROTATION = SwerveConstants.SwerveModule.MOTOR_ROTATIONS_PER_WHEEL_ROTATION;
        this.MOTOR_ROTATIONS_PER_MODULE_ROTATION = SwerveConstants.SwerveModule.MOTOR_ROTATIONS_PER_MODULE_ROTATION;
        this.WHEEL_CIRCUMFERENCE = SwerveConstants.SwerveModule.WHEEL_CIRCUMFERENCE;
        initialize();
        canCoder = null;
    }

    public void initialize() {

        // First, we need to apply the offset corrections
        mainBotEncoderOffsets[moduleNumber] = (rotationFixOffset[moduleNumber] + compBotEncoderOffsets[moduleNumber]) * directionFixOffset[moduleNumber];
        rotationVector.setXY(mainBotModulePositions[moduleNumber][0], mainBotModulePositions[moduleNumber][1]);
        tempRotate.setXY(mainBotModulePositions[moduleNumber][0], mainBotModulePositions[moduleNumber][1]);
        rotationEncoder = rotationMotor.getEncoder();
        canPID = new SwerveCANPIDRotationController(rotationMotor, MOTOR_ROTATIONS_PER_MODULE_ROTATION);
        canPID.setP(1);
        canPID.setD(.05);
        movementMotor.configurePID(.05, 0.0, 0.0, 1.0, 0.0 );
        calibrate();
    }

    public void calibrate() {
        // double[] offsets;
            // offsets = mainBotEncoderOffsets;
            // rotationVector.setXY(mainBotModulePositions[moduleNumber][0], mainBotModulePositions[moduleNumber][1]);
            // rotationVector.setTheta(rotationVector.getTheta() + (Math.PI / 2));
            // rotationVector.setMagnitude(1);

        // double currentValue = absoluteEncoder.getConvertedAbsolute() + offsets[moduleNumber];
        // System.out.println(currentValue);
        absoluteEncoder.setPosition(absoluteEncoder.getAbsolutePosition());
        System.out.println((absoluteEncoder.getAbsolutePosition()/360.0) * MOTOR_ROTATIONS_PER_MODULE_ROTATION);
        rotationEncoder.setPosition((absoluteEncoder.getAbsolutePosition()/360.0) * MOTOR_ROTATIONS_PER_MODULE_ROTATION);
    }

    public void stop() {
        canPID.setReference(rotationEncoder.getPosition(), CANSparkMax.ControlType.kPosition);
        movementMotor.stop();
        rotationMotor.stopMotor();
    }

    public void reset() {
        canPID.setReference(0, CANSparkMax.ControlType.kPosition);
    }

    /**
     * This is effectively the main method of the swerve modules it calls all the proper methods to rotate the motors
     *
     * @param input The input vector for the motion of the module. The desired angle is the angle of the vector.
     *              The magnitude of the rotation vector is the rotationScalar. The speed of the motor is
     *              the magnitude of the vector, this unit is in IPM and must be converted to RPM
     * @author Ibrahim Ahmad
     */
    public void setMotion(SwerveInputProxy.SwerveInput input) { // Effectively "main method" of this code, updates the motor values and power
    //    rotationMotor.set(.1);
        // System.out.println(absoluteEncoder.getAbsolutePosition());
        // if(moduleNumber == 0){
            // System.out.println(absoluteEncoder.getAbsolutePosition() +" " +absoluteEncoder.getDeviceID() + " left side encoder: " + ((absoluteEncoder.getAbsolutePosition()/360.0) * MOTOR_ROTATIONS_PER_MODULE_ROTATION) + " right side " + rotationEncoder.getPosition());
            // rotationMotor.set(.1);

            // movementMotor.setPercentOutput(.1);
        // }
        if (currentState == ModuleState.Enabled) {
            if (input.vector.getMagnitude() != 0 || input.rotationScalar != 0) {
                endVector.setXY(input.vector.getX(), input.vector.getY());

                // Add the rotation vector to the vector
                tempRotate.setXY(rotationVector.getX(), rotationVector.getY());
                tempRotate.scale(input.rotationScalar);
                tempRotate.addTheta(Math.PI/2.0);
                // tempRotate.setMagnitude(.01);
                endVector.add(tempRotate);

                 direction = canPID.setOptimizedPositionNew(endVector.getTheta());;
               movementMotor.setVelocity(endVector.getMagnitude()  * direction * baseSpeed());

            } else {
               canPID.setReference(rotationEncoder.getPosition(), CANSparkMax.ControlType.kPosition);
                // rotationMotor.stopMotor();
               movementMotor.stop();
            }
        } else if (currentState == ModuleState.Disabled) {
        }

    }

    public SwerveModuleState getModuleState() {
        return new SwerveModuleState(rotationsToMeters(movementMotor.getPosition()), new Rotation2d(getCurrentAngle()));
    }

    public void printPosition() {
    }

    public double motorRotationsToRadians(double rotations) {
        return (rotations / MOTOR_ROTATIONS_PER_MODULE_ROTATION) * TAU;
    }

    public double rotationsToInches(double rotations) {
        return rotations / MOTOR_ROTATIONS_PER_WHEEL_ROTATION * WHEEL_CIRCUMFERENCE;
    }

    public static double degreesToModuleRotations(double degrees) {
        return (degrees / 360 * SwerveConstants.SwerveModule.MOTOR_ROTATIONS_PER_MODULE_ROTATION);
    }

    public double rotationsToMeters(double rotations) {
        double speed = (rotations / MOTOR_ROTATIONS_PER_WHEEL_ROTATION) * WHEEL_CIRCUMFERENCE;
        return speed;
    }

    public double getCurrentAngle() {
        return (motorRotationsToRadians(rotationEncoder.getPosition()));
    }

    public void setP(double p) {
        canPID.setP(p);
    }

    public double getP() {
        return canPID.getP();
    }

    public double getDesiredAngleError() {
        return canPID.getDesiredModuleAngle() - getCurrentAngle();
    }

    public double getAngleError() {
        double var = Math.abs(canPID.getTargetAngle() % (2 * PI) - getCurrentAngle() % (2 * PI)) % (2 * PI);
        return Math.min(var, Math.abs(TAU - var));
    }

    public Vector getTotalVector() { // Meters per Second
        Vector out = new Vector();
        out.setPolar(getCurrentAngle(), rotationsToMeters(movementMotor.getVelocity()));
        out.scale((1.0 / 60) * 1.025 / 1.06451612903);
        return out;
    }

    public double IPMtoRPM(double IPM) {
        double wheelRotations = IPM / WHEEL_CIRCUMFERENCE;
        return wheelRotations * MOTOR_ROTATIONS_PER_WHEEL_ROTATION;
    }

    public boolean atTargetAngle(double maxError) {
        return getDesiredAngleError() < maxError;
    }

    public double getVelocity() {
        return rotationsToInches(movementMotor.getVelocity());
    }

    public ModuleState getState() {
        return currentState;
    }

    public void setState(ModuleState state) {
        currentState = state;
    }

    public TatorMotor getDriveMotor() {
        return movementMotor;
    }

    public CANSparkMax getRotationMotor() {
        return rotationMotor;
    }

    public CANCoderWrapper getAbsoluteEncoder() {
        return absoluteEncoder;
    }

    public double getAbsoluteEncoderAngle() {
        return absoluteEncoder.getConvertedAbsolute();
    }

    public boolean isRotationMotorInverted() {
        boolean inversion = rotationMotor.getInverted();
        return inversion;
    }

    public void setMovementMotor(double percentOutput) {
        movementMotor.setPercentOutput(percentOutput);
    }

    public double getMovementMotorVelocity() {
        return movementMotor.getVelocity();
    }

    public void setRotationMotor(double percentOutput) {
        rotationMotor.set(percentOutput);
    }

    public double getRotationMotorVelocity() {
        return rotationEncoder.getVelocity();
    }

    public static double baseSpeed(){
        return ( 3200 / 5.0 * 1.98030888031);
    }

    @Override
    public String toString() {
        return "SwerveModule{" +
                ", rotationVectors=" + mainBotModulePositions[moduleNumber] +
                ", movementMotor=" + movementMotor.getVelocity() +
                ", rotationEncoder=" + rotationEncoder.getPosition() +
                ", endVector=" + endVector +
                ", rotationVector=" + rotationVector +
                ", tempRotate=" + tempRotate +
                ", MAX_ERROR=" + MAX_ERROR +
                '}';
    }
}