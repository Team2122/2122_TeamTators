package org.teamtators.bbt8r.subsystems;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.revrobotics.ControlType;
import org.teamtators.bbt8r.TatorTalonFX;
import org.teamtators.bbt8r.TatorTalonFXConfig;
import org.teamtators.common.Robot;
import org.teamtators.common.config.helpers.AnalogPotentiometerConfig;
import org.teamtators.common.config.helpers.DigitalSensorConfig;
import org.teamtators.common.config.helpers.SparkMaxConfig;
import org.teamtators.common.control.SparkMaxPIDController;
import org.teamtators.common.hw.AnalogPotentiometer;
import org.teamtators.common.hw.DigitalSensor;
import org.teamtators.common.hw.NEOEncoder;
import org.teamtators.common.hw.TatorSparkMax;
import org.teamtators.common.scheduler.Subsystem;
import org.teamtators.common.tester.AutomatedTest;
import org.teamtators.common.tester.ManualTestGroup;
import org.teamtators.common.tester.automated.MotorEncoderTest;
import org.teamtators.common.tester.components.DigitalSensorTest;
import org.teamtators.common.tester.components.SpeedControllerTest;
import org.teamtators.common.tester.components.TalonFXPowerTest;
import org.teamtators.common.util.Tuple;

import java.util.ArrayList;
import java.util.List;

public class Turret extends Subsystem {

    private TatorSparkMax rotationMotor;
    private NEOEncoder rotationEncoder;
    public SparkMaxPIDController rotationController;

    public TatorTalonFX flywheelMotor;

    public TatorSparkMax hoodMotor;
    public NEOEncoder hoodEncoder;
    public SparkMaxPIDController hoodController;
    private DigitalSensor hoodSensor;

    private AnalogPotentiometer rotationPot;

    public Config config;

    // State Machine to keep track of the Turret State
    private TurretState turretState_next;
    private TurretState turretState_current;

    private double targetExtension;
    public double targetAngle = 140, targetRotations;
    private double targetSpeed = 0;

    private Tuple <Double, Double> baseRotationPoint; // Format: <Angle, Rotations>

    public boolean homing = false;
    private boolean localDebug = false;

    public enum TurretState {
        IDLING, // IDLING is the default state when nothing is happening with turret
        SHOOTING, // SHOOTING is for firing balls out of the flywheel, not for running the flywheel
        ROTATING // ROTATING is for turning the turret, the flywheel can be run during this state through the variable beReadyToShoot
    }

    public Turret() {
        super("Turret");
        setTurretState(TurretState.IDLING);
    }

    public Config getConfig() {
        return config;
    }

    private void setRotationMotorPower(double power) {
        rotationController.setReference(power, ControlType.kDutyCycle);
    }

    public double getRotationEncoder() {
        return rotationEncoder.getCounts();
    }

    public double getFlywheelSpeed() {
        return flywheelMotor.get();
    }

    private void setFlywheelPower(double power) {
        flywheelMotor.set(ControlMode.PercentOutput, power);
    }

    public void setTargetFlywheelSpeed(double speed) {
        targetSpeed = speed;
    }

    private void setFlywheelSpeed() {
        setFlywheelSpeed(targetSpeed);
    }

    private void setFlywheelSpeed(double speed) 
    { 
        // Position Change / 100ms
        flywheelMotor.set(speed);
    }

    // Uses maxFlywheelError to check if the flyWheelSpeed isn't too much
    public boolean isFlywheelOnTarget()
    {
        if (Math.abs(getFlywheelSpeed() - targetSpeed) < config.maxFlywheelError) 
        {
            if (localDebug) {
                logger.info("Flywheel is on target");
            }
            return true;
        }
        else
        {
            if (localDebug) {
                logger.info("Flywheel not on target");
            }
            return false;
        }
    }

    // sets targetAngle and targetRotations if the set angle is between upper and lower limit.
    public void setTargetTurretAngle(double angle) 
    {
        if (angle > config.upperLimit) 
        {
            if (localDebug) {
                logger.warn("Tried to Move Turret Too Far, resetting \n\tTarget Angle: " + angle + "\n\tUpper Limit: " + config.upperLimit);
            }

            angle = config.upperLimit;
        }
        else if (angle < config.lowerLimit) 
        {
            if (localDebug) {
                logger.warn("Tried to Move Turret Too Far!, resetting \n\tTarget Angle: " + angle + "\n\tLower Limit: " + config.lowerLimit);
            }

            angle = config.lowerLimit;
        }

        targetAngle = angle;

        double deltaAngle = (baseRotationPoint.getA() - targetAngle);
        targetRotations = baseRotationPoint.getB() + (deltaAngle * config.rotationsPerDegree);
    }

    public double getTargetTurretAngle() {
        return targetAngle;
    }

    /**
     * @return targetSpeed
     */
    public double getTargetSpeed() {
        return targetSpeed;
    }

    /**
     * Used to get the absolute angle of the potentiometer.
     *
     * @return Angle of rotation
     */
    public double getTurretAngle() 
    {
        return rotationPot.get(); // CHANGE THIS TO ENCODER TICKS
    }

    /**
     * rotates turret with rotationController based off targetRotations
     */
    public void turretToAngle() 
    {
        rotationController.setReference(targetRotations, ControlType.kSmartMotion);
    }

    /**
     * @return if turret is within a certain range of targetAngle
     */
    public boolean isTurretAtAngle() 
    {
        // Calculate if we are withing range of the target angle
        boolean result = (Math.abs(targetRotations - rotationEncoder.getRotations()) < (config.minimumTurretMove * config.rotationsPerDegree));

        if (!result && localDebug) 
        {
            logger.info( "Turret Not at Correct Angle\nCurrentAngle: " + getTurretAngle() + "\nTargetAngle: " + targetAngle);
        }

        return result;
    }

    /**
     * sets targetExtension if..
     * 
     * @param extension is not above max or below min percentage if extension is
     *                  less than 0, then set to 0 if extension is greater than
     *                  maxPercentage then extension is set to maxPercentage
     */
    public void setHoodTargetExtension(double extension) {

        if (extension > config.maximumPercentage) 
        {
            if (localDebug) {
                logger.info("Tried to Extend Hood Too Far, resetting to maximum extension");
            }
            extension = config.maximumPercentage;
        } 
        else if (extension < 0) 
        {
            if (localDebug) {
                logger.info("Tried to Retract Hood Too Far, resetting to minimum extension");
            }

            extension = 0;
        }

        targetExtension = extension;
    }

    public double getHoodTargetExtension() 
    {
        return targetExtension;
    }

    public boolean getHoodSensor() 
    {
        return hoodSensor.get();
    }

    // extension is a percentage
    public double getHoodExtension() 
    {
        // Initialize hood to home
        double result = this.config.maximumPercentage;

        if (Robot.isReal())
            result = getHoodEncoder() / config.upperHoodTickCount;

        return result;
    }

    /**
     * @return if hood is at targetExtension or very close to targetExtension
     */
    public boolean isHoodAtExtension() 
    {
        // Check to see if the hood is close to the target extension, within
        // (maxHoodPositionError) range.
        boolean result = Math.abs(targetExtension - getHoodExtension()) <= config.maxHoodPositionError;

        if (!result && localDebug) 
        {
            logger.info("Hood not yet at extension\nTarget Extension: " + targetExtension + "\nCurrent Extension: " + getHoodExtension());
        }

        return result;
    }

    public double getHoodEncoder() {

        // Initialize hood to minimum extension
        double result = this.config.lowerLimit;

        if (Robot.isReal())
            result = hoodEncoder.getCounts();

        return result;
    }

    private void setHoodMotorPower(double power) {
        hoodController.setReference(power, ControlType.kDutyCycle);
    }

    public void setHoodMotorExtension()
    {
        // IDEAS: Use this math with kPosition, take down travel velocity to not
        // overshoot, try with pure encoder ticks
        hoodController.setReference((getHoodTargetExtension() * config.upperHoodTickCount) / 42, ControlType.kSmartMotion);

        if (getHoodEncoder() > (config.maximumPercentage * config.upperHoodTickCount) || getHoodEncoder() < 0) {
            hoodMotor.stopMotor();
        }
    }

    public boolean isHoodOverExtended() 
    {
        // Check to see if the hood is greater than macximum percentage OR less than 0
        boolean result = (getHoodEncoder() > (config.maximumPercentage * config.upperHoodTickCount) || getHoodEncoder() < 0);

        if (result) {
            return true;
        } else {
            return false;
        }
    }

    public void updateTurretState( )
    {   

        if (localDebug )
            logger.info( "Setting Turret State : " + turretState_next ) ; 
        

        switch (turretState_next) {
            case SHOOTING: // powers on flywheel, stops rotation motor

                if (turretState_next != turretState_current) 
                {
                    // Only execute this code if we are changing state!!!
                    setFlywheelSpeed();
                    stopRotation();
                    setTurretState(TurretState.SHOOTING);
                }
                break;

            case ROTATING: // moves turretToAngle

                // Check on the turret angle, reset target
                turretToAngle();

                if (isOverRotated()) 
                {
                    if (localDebug) {
                        logger.info("Turret Over Rotated");
                    }
                    setTurretState(TurretState.IDLING);
                    stopRotation();
                } 
                else 
                {
                    setTurretState(TurretState.ROTATING);
                }

                break;

            case IDLING:

                if (turretState_next != turretState_current) {
                    // Only execute IF we are changing state
                    // not shooting or rotating
                    // flywheelMotor.set(ControlMode.PercentOutput, 0);
                    stopRotation();
                    setTurretState(TurretState.IDLING);
                }

                break;
        }

        // Check to see if hood is over extended
        if (isHoodOverExtended() && !homing) {
            if (localDebug) {
                logger.info("Hood Over Extended");
            }

            hoodMotor.stopMotor();
        }

        // Update the current state
        turretState_current = turretState_next ;
    }

    /**
     * used in SPITTING state from SuperStructure based off the wanted state,
     * argument can represent the setter for targetSpeed or turretAngle
     */

    public void setTurretState( TurretState turretState_to_be_set, double argument )
    {
        if (turretState_to_be_set == TurretState.SHOOTING) 
        {
            targetSpeed = argument;
        } 
        else if (turretState_to_be_set == TurretState.ROTATING) 
        {
            targetAngle = argument;
        }

        setTurretState( turretState_to_be_set );
    }

    /**
     * pretty sure there are methods in here that do the same thing??
     * 
     * @return true if turretAngle is over or under the limit
     */

     public boolean isOverRotated() 
    {
        double angle = getTurretAngle();

        if (angle >= config.upperLimit || angle <= config.lowerLimit) 
        {
            return true;
        } else 
        {
            return false;
        }
    }

    public void stopRotation() 
    {
        rotationMotor.stopMotor();
    }

    public void updateBasePoint() 
    {
        updateBasePoint(rotationPot.get(), rotationEncoder.getRotations());
    }

    public void updateBasePoint(double angle, double rotations) 
    {
        // Updates the basePoint that the turret rotations are based off of
        if (localDebug) {
            logger.info("Updated Angle: " + angle + "\tUpdated Rotations: " + rotations);
        }

        baseRotationPoint = new Tuple<>(angle, rotations);
    }

    public TurretState getCurrentTurretState() 
    {
        return turretState_current;
    }

    public void setTurretState( TurretState new_turretState ) 
    {
        turretState_next = new_turretState;
    }

    /**
     * if the flyWheelSpeed is less than 1750 (probably should change to a yaml
     * value though) then return false also flyWheel must be on target and
     * isHoodAtExtension must be true to be ready to shoot
     */
    public boolean readyToShoot() 
    {
        // If we are not shooting OR flywheel is not up to a minimum 1750 rpm, we are
        // not ready to shoot
        if (getCurrentTurretState() != TurretState.SHOOTING || getFlywheelSpeed() <= 1750) 
        {
            if (localDebug) {
                logger.info("Not in shooting state OR up to speed");
            }
            return false;
        }

        // Second check, are we on target (RPM) and is the hood extended correctly
        boolean result = /* isTurretAtAngle() && */ isFlywheelOnTarget() && isHoodAtExtension();

        return result;
    }

    public void printTurretState() {
        if (localDebug) {
            logger.info("Current Turret State: " + turretState_next);
        }
    }

    public void printTurretAngle() {
        if (localDebug) {
            logger.info("Current Turret Angle: " + getTurretAngle());
        }
    }

    public void printTurretTargetAngle() {
        if (localDebug) {
            logger.info("Current Turret Target Angle: " + targetAngle);
        }
    }

    public void printTurretTargetRotations() {
        if (localDebug) {
            logger.info("Current Turret Target Rotations: " + targetRotations);
        }
    }

    public void printTurretRotations() {
        if (localDebug) {
            logger.info("Turret Rotations: " + rotationEncoder.getRotations());
        }
    }

    public void printTurretSpeed() {
        if (localDebug) {
            logger.info("Current Turret Flywheel Speed: " + flywheelMotor.get());
        }
    }

    public void printTurretTargetSpeed() {
        if (localDebug) {
            logger.info("Current Turret Target Flywheel Speed: " + targetSpeed);
        }
    }

    public void printTurretEncoderCount() {
        if (localDebug) {
            logger.info("Current Turret Rotation Motor Encoder Count: " + getRotationEncoder());
        }
    }

    public void printHoodEncoderCount() {
        if (localDebug) {
            logger.info("Current Hood Motor Encoder Count: " + getHoodEncoder());
        }
    }

    public void printHoodExtension() {
        if (localDebug) {
            logger.info("Hood Extension: " + getHoodExtension());
        }
    }

    public void printHoodTargetExtension() {
        if (localDebug) {
            logger.info("Target Hood Extension: " + getHoodTargetExtension());
        }
    }

    public void printIsHoodAtExtension() {
        if (localDebug) {
            logger.info("Is Hood At Extension: " + isHoodAtExtension());
        }
    }

    @Override
    public ManualTestGroup createManualTests() 
    {
        ManualTestGroup tests = super.createManualTests();
        tests.addTest(new SpeedControllerTest("rotationMotor", rotationMotor));
        tests.addTest(new TalonFXPowerTest("flywheelMotor", flywheelMotor));
        tests.addTest(new SpeedControllerTest("hoodMotor", hoodMotor));
        tests.addTest(new DigitalSensorTest("hoodSensor", hoodSensor));
        return tests;
    }

    public List<AutomatedTest> createAutomatedTests() 
    {
        ArrayList<AutomatedTest> tests = new ArrayList<>();
        tests.add(new MotorEncoderTest("pickMotorEncoderTest", this::setRotationMotorPower, this::getRotationEncoder));
        tests.add(new MotorEncoderTest("flipMotorEncoderTest", this::setFlywheelPower, this::getFlywheelSpeed));
        tests.add(new MotorEncoderTest("hoodMotorEncoderTest", this::setHoodMotorPower, this::getHoodEncoder));
        return tests;
    }

    public void configure(Config config) {
        this.config = config;

        flywheelMotor = config.tatorTalonFXConfig.create();
        flywheelMotor.set(ControlMode.Velocity, 0);

        rotationMotor = config.rotationMotor.create();
        rotationEncoder = rotationMotor.getNeoEncoder();
        rotationController = new SparkMaxPIDController(rotationMotor, "Turret.rotationController");
        rotationController.configure(config.rotationController);
        rotationPot = config.rotationPot.create();

        // Initalize with the values from the Robot
        if (Robot.isReal())
            baseRotationPoint = new Tuple<>(rotationPot.get(), rotationEncoder.getRotations());
        else
            baseRotationPoint = new Tuple<>(0.0, 0.0);

        hoodMotor = config.hoodMotor.create();
        hoodEncoder = hoodMotor.getNeoEncoder();
        hoodController = new SparkMaxPIDController(hoodMotor, "Turret.hoodController");
        hoodController.configure(config.hoodController);
        hoodSensor = config.hoodSensor.create();
        localDebug = config.debug;
    }

    public static class Config 
    {
        // Falcon500 Motor
        public TatorTalonFXConfig tatorTalonFXConfig;

        // Turret Settings
        public double clearingPower;
        public double spittingSpeed;
        public double maxFlywheelError;
        public double encoderTicksPerRevolution; // http://www.ctr-electronics.com/downloads/pdf/Falcon%20500%20User%20Guide.pdf
                                                 // page 8 = 2048

        // Rotation Motor
        public double minimumTurretMove; // The minimum distance the turret can rotate in degrees.
        public double rotationsPerDegree;
        public double upperLimit;
        public double lowerLimit;

        public SparkMaxConfig rotationMotor; // Spins the turret itself

        public SparkMaxPIDController.Config rotationController;

        // Hood Solenoid
        public SparkMaxConfig hoodMotor;
        public SparkMaxPIDController.Config hoodController;
        public DigitalSensorConfig hoodSensor;
        public double upperHoodTickCount;
        public double maximumPercentage;
        public double maxHoodPositionError;

        // Potentiometer
        public AnalogPotentiometerConfig rotationPot;

        // Local Debugging
        public boolean debug;

    }
}
