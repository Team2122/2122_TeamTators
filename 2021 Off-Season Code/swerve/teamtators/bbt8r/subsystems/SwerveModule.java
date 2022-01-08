package org.teamtators.bbt8r.subsystems;

import com.ctre.phoenix.motorcontrol.NeutralMode;
import com.revrobotics.ControlType;
import edu.wpi.first.wpilibj.geometry.Rotation2d;
import edu.wpi.first.wpilibj.kinematics.SwerveModuleState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamtators.bbt8r.Module;
import org.teamtators.bbt8r.TatorTalonFX;
import org.teamtators.bbt8r.TatorTalonFXConfig;
import org.teamtators.bbt8r.subsystems.SwerveInputProxy.SwerveInput;
import org.teamtators.bbt8r.staging.TatorAbsoluteEncoder;
import org.teamtators.bbt8r.staging.TatorCANPIDRotationController;
import org.teamtators.bbt8r.staging.Vector;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.config.helpers.SparkMaxConfig;
import org.teamtators.common.hw.NEOEncoder;
import org.teamtators.common.hw.TatorSparkMax;

import java.util.Arrays;

public class SwerveModule implements Module, Configurable<SwerveModule.Config> {

    private Config config;

    // Vector each module needs to apply to only rotate.
    // Proper method of accessing is rotationVector[moduleNumber][0] is the X and [moduleNumber][1] is y.
    private final double[][] rotationVectors = {{1, 1}, {-1, 1}, {-1, -1}, {1, -1}};

    // Hardware
    private TatorAbsoluteEncoder absoluteEncoder;       // Encoder used for checking rotation angles
    private TatorSparkMax rotationMotor;     // NEO550 is rotation motor
    private TatorTalonFX movementMotor;      // Talon FX is the movement motor
    private NEOEncoder rotationEncoder;

    // Controllers
    private TatorCANPIDRotationController canPID;

    // Vectors
    private Vector endVector = new Vector();    // Vector used for moving the motors
    private Vector rotationVector = new Vector();
    private Vector tempRotate = new Vector();

    // Logger
    private Logger logger;

    // Constants
    private double PI = Math.PI;
    private double TAU = 2 * PI;
    private double MAX_ERROR = .01;
    private double me = 0;

    private SwerveModule() {

    }

    public void initialize() {
        rotationVector.setXY(rotationVectors[config.moduleNumber][0], rotationVectors[config.moduleNumber][1]);
        tempRotate.setXY(rotationVectors[config.moduleNumber][0], rotationVectors[config.moduleNumber][1]);
        rotationEncoder = rotationMotor.getNeoEncoder();
        movementMotor.setNeutralMode(NeutralMode.Brake);
        canPID = new TatorCANPIDRotationController(rotationMotor, config.MOTOR_ROTATIONS_PER_MODULE_ROTATION);
        canPID.setP(.8);
//        canPID.setD(.05);
        absoluteEncoder.setDistancePerRotation(config.MOTOR_ROTATIONS_PER_MODULE_ROTATION);
        calibrate();
    }

    public void calibrate() {
        absoluteEncoder.setInverted();
        rotationEncoder.setPosition(absoluteEncoder.getDistance());
    }

    public void stop() {
        canPID.setReference(rotationEncoder.getRotations(), ControlType.kPosition);
        movementMotor.stopMotor();
        rotationMotor.stopMotor();
    }

    public void reset() {
        canPID.setReference(0, ControlType.kPosition);
    }

    /**
     * This is effectively the main method of the swerve modules it calls all the proper methods to rotate the motors
     *
     * @param input The input vector for the motion of the module. The desired angle is the angle of the vector.
     *              The magnitude of the rotation vector is the rotationScalar. The speed of the motor is
     *              the magnitude of the vector, this unit is in IPM and must be converted to RPM
     * @author Ibrahim Ahmad
     */
    public void setMotion(SwerveInput input) { // Effectively "main method" of this code, updates the motor values and power
        if( !(input.vector.getMagnitude() ==0 && input.rotationScalar == 0)){
            endVector.setXY(input.vector.getX(), input.vector.getY());

            // Add the rotation vector to the vector
            tempRotate.setXY(rotationVector.getX(), rotationVector.getY());
            tempRotate.scale(input.rotationScalar);
            endVector.add(tempRotate);

            // Set movement
//            System.out.println(rotationEncoder.getRotations()/55 * 2 * PI + " " + config.moduleNumber);
            canPID.setOptimizedPosition(endVector.getTheta());
            movementMotor.set(IPMtoRPM(endVector.getMagnitude() * Math.cos(getAngleError())));
        } else{
            canPID.setReference(rotationEncoder.getRotations(),ControlType.kPosition);
            movementMotor.stopMotor();
        }
//        System.out.println(config.moduleNumber + ": " + getCurrentAngle());

//        logger.info("Module " + config.moduleNumber + " After Code: " + input);
    }


    public double motorRotationsToRadians(double rotations) {
        return (rotations / config.MOTOR_ROTATIONS_PER_MODULE_ROTATION) * TAU;
    }

    public double rotationsToInches(double rotations) {
        return rotations / config.MOTOR_ROTATIONS_PER_WHEEL_ROTATION * config.WHEEL_CIRCUMFERENCE;
    }

    public double rotationsToMeters(double rotations) {
        return rotations / config.MOTOR_ROTATIONS_PER_WHEEL_ROTATION * config.WHEEL_CIRCUMFERENCE * 0.000254;
    }

    public double scaleMagnitudeToVelocity(double magnitude) {
        return magnitude * config.MAXIMUM_VELOCITY;
    }

    public double getCurrentAngle() {
        return (motorRotationsToRadians(rotationEncoder.getRotations()));
    }

    public void setP(double p){
        canPID.setP(p);
    }

    public double getP(){
        return canPID.getP();
    }

    public double getAngleError() {
        return canPID.getDesiredModuleAngle() - getCurrentAngle();
    }

    public Vector getTotalVector() {
        Vector out = new Vector();
        return out.setPolar(PI -getCurrentAngle(), rotationsToMeters(movementMotor.get()));
    }


    public double IPMtoRPM(double IPM) {
        double wheelRotations = IPM / config.WHEEL_CIRCUMFERENCE;
        return wheelRotations * config.MOTOR_ROTATIONS_PER_WHEEL_ROTATION;
    }

//    public Vector getTotalVector() {
//        Vector vector = new Vector();
//        vector.setPolar(getCurrentAngle(), rotationsToInches(movementMotor.get()));
//        System.out.println("Module Number: " + config.moduleNumber + " " + vector);
//        return vector;

    public boolean atTargetAngle(double maxError) {
        return getAngleError() < maxError;
    }

    public double getVelocity() {
        return rotationsToInches(movementMotor.get());
    }

    public Rotation2d getRotation2d() {
        return new Rotation2d(getCurrentAngle());
    }

    public SwerveModuleState getSwerveModuleState() {
        return getSwerveModuleState(new SwerveModuleState());
    }

    public SwerveModuleState getSwerveModuleState(SwerveModuleState moduleState) {
        moduleState.angle = getRotation2d();
        moduleState.speedMetersPerSecond = rotationsToMeters(movementMotor.get());
        return moduleState;
    }

    @Override
    public String toString() {
        return "SwerveModule{" +
                ", rotationVectors=" + rotationVectors[config.moduleNumber] +
                ", absoluteEncoder=" + absoluteEncoder.getDistance() +
                ", movementMotor=" + movementMotor.get() +
                ", rotationEncoder=" + rotationEncoder.getDistance() +
                ", endVector=" + endVector +
                ", rotationVector=" + rotationVector +
                ", tempRotate=" + tempRotate +
                ", MAX_ERROR=" + MAX_ERROR +
                '}';
    }

    @Override
    public void configure(Config config) {
        this.config = config;
        config.WHEEL_CIRCUMFERENCE = 2 * PI * config.WHEEL_RADIUS;
        movementMotor = config.movementMotor.create();
        rotationMotor = config.rotationMotor.create();
        absoluteEncoder = config.absoluteEncoder.create();
        logger = LoggerFactory.getLogger("SwerveDrive.Module " + config.moduleNumber);
        initialize();
    }

    public static class Config {
        public int moduleNumber;
        public TatorTalonFXConfig movementMotor;
        public SparkMaxConfig rotationMotor;
        public TatorAbsoluteEncoder.Config absoluteEncoder;

        public double MOTOR_ROTATIONS_PER_MODULE_ROTATION = 55;
        public double MAXIMUM_VELOCITY = 2000;
        public double rotationMultiplier = 1;

        public double MOTOR_ROTATIONS_PER_WHEEL_ROTATION = 6.8571428571;
        public double WHEEL_RADIUS = 4.5; // Inches

        public double WHEEL_CIRCUMFERENCE; // Inches

        public SwerveModule create() {
            SwerveModule module = new SwerveModule();
            module.configure(this);
            return module;
        }
    }
}
