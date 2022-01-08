package org.teamtators.bbt8r.subsystems;

import edu.wpi.first.wpilibj.Solenoid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamtators.bbt8r.TatorRobot;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.config.helpers.DigitalSensorConfig;
import org.teamtators.common.config.helpers.SolenoidConfig;
import org.teamtators.common.config.helpers.SparkMaxConfig;
import org.teamtators.common.control.SparkMaxPIDController;
import org.teamtators.common.hw.DigitalSensor;
import org.teamtators.common.hw.NEOEncoder;
import org.teamtators.common.hw.TatorSparkMax;
import org.teamtators.common.scheduler.Subsystem;
import org.teamtators.common.tester.ManualTestGroup;
import org.teamtators.common.tester.components.*;

/**
 * this week's In The Words of the Mechanical Team notable quotes from Grace:
 * "the turret needs to be turned to face the back of the robot before the
 * climber can go vertical. After that, nothing else should be in the way."
 *
 * "The climber has a solenoid for the rachet, a solenoid to make it vertical,
 * and a NEO to lift."
 */
public class Climber extends Subsystem implements Configurable<Climber.Config> 
{
    private static final Logger logger = LoggerFactory.getLogger(Climber.class);
    private static final boolean RATCHET_ACTIVE_STATE = false; // maybe?
    private boolean localDebug = false;

    private enum LiftArmState {
        LIFT_ARM_UP,
        LIFT_ARM_DOWN,
        INITIALIZE
    }

    public LiftArmState liftArmState = LiftArmState.INITIALIZE;

    /**
     * on/off ratchet for climber motor
     */
    private Solenoid ratchet;
    /**
     * makes the arm vertical
     */
    public Solenoid verticalIn;
    public Solenoid verticalOut;
    /**
     * climber motor
     */
    public TatorSparkMax lift;
    /**
     * encoder for the climber motor
     */
    public NEOEncoder liftEncoder;
    private DigitalSensor liftSensor;
    /**
     * speed controller for lift motor "pid"
     */
    public SparkMaxPIDController liftController; // how we track speed AND position

    private Config config;

    public Climber(TatorRobot robot) {
        super("Climber");
    }

  /*  public void moveToPosition (double position) {
        if (config.liftLowerBound < position && position < config.liftUpperBound)
            liftController.moveToPosition(position);
        else
            logger.info("Cannot move lift to position {} because it is outside of the bound [{},{}]",
                    position, config.liftLowerBound, config.liftUpperBound);
    }
    /*
    public boolean getLiftControllerFinished () {
        return liftController.isFinished;
    }
     */

    public boolean isSafeToMoveArm(Turret turret) {
        double angle = turret.getTurretAngle();
        boolean safe = config.lowerBoundTurretAngleForRaising < angle && angle < config.upperBoundTurretAngleForRaising;
        if (localDebug)
            logger.info("isSafeToMoveArm: " + safe);
        return safe;
    }

    // raises arm of turret if safe
    public void setLiftArmState(LiftArmState liftArmState) {
        switch (liftArmState) {
            case LIFT_ARM_UP:
                verticalIn.set(true);
                verticalOut.set(false);
                liftArmState = liftArmState.LIFT_ARM_UP;
                break;
            case LIFT_ARM_DOWN:
                verticalIn.set(false);
                verticalOut.set(true);
                liftArmState = liftArmState.LIFT_ARM_DOWN;
                break;
            case INITIALIZE:
                liftArmState = liftArmState.LIFT_ARM_DOWN;
                break;
        }
    }

    public LiftArmState getLiftArmState() {
        return liftArmState;
    }

    public boolean raiseArm()
    {
        if (localDebug) {
            logger.info("Raising Arm");
        }
        setLiftArmState(liftArmState.LIFT_ARM_UP);

        return true;
    }

    // lowers arm of turret if safe
    public boolean lowerArm() 
    {
        if (localDebug) {
            logger.info("Lowering Arm");
        }
        setLiftArmState(liftArmState.LIFT_ARM_DOWN);

        return true;
    }

    // prints left encoder rotations
    public void printLiftEncoderRotations() {
        if (localDebug) {
            logger.info("lift rotations counts: " + liftEncoder.getRotations());
        }
    }

    // sets lift motor power
    public void setLiftMotorPower(double power) {
        lift.set(power);
    }

    // stops lift motor
    public void stopLiftMotor() {
        lift.stopMotor();
    }

    // gets lift sensor
    public boolean getLiftSensor() {
        return liftSensor.get();
    }

    // get the life encoder rotations
    public double getLiftEncoderRotations() {
        return liftEncoder.getRotations();
    }

    // activates ratchet
    public void activateRatchet() {
        if (localDebug) {
            logger.info("Activating Ratchet");
        }
        ratchet.set(RATCHET_ACTIVE_STATE);
    }

    // deactivates ratchet
    public void deactivateRatchet() {
        if (localDebug) {
            logger.info("Deactivating Ratchet");
        }
        ratchet.set(!RATCHET_ACTIVE_STATE);
    }

    /*
    public boolean reachedUpperLimit(double position){
        if(config.liftDesiredHeight == position || position == config.liftUpperBound){
            return true;
        } else {
            return false;
        }
    }
     */

    @Override
    public ManualTestGroup createManualTests() {
        var tests = new ManualTestGroup("Climber tests");
        tests.addTests(new SolenoidTest("verticalIn solenoid test", verticalIn));
        tests.addTests(new SolenoidTest(" verticalOut solenoid test", verticalOut));
        tests.addTests(new SolenoidTest("ratchet test", ratchet));
        tests.addTests(new SpeedControllerTest("lift", lift));
        tests.addTests(new NEOEncoderTest("lift encoder", liftEncoder));
        tests.addTests(new DigitalSensorTest("lift sensor", liftSensor));
        return tests;
    }

    @Override
    public void configure(Config config) {
        verticalIn = config.verticalIn.create();
        verticalOut = config.verticalOut.create();
        ratchet = config.ratchet.create();
        lift = config.lift.create();
        // lift.setEncoderConfig(config.liftEncoder);
        liftEncoder = new NEOEncoder(lift);
        // liftEncoder.encoder.setMeasurementPeriod(5);
        liftEncoder.configure(config.liftEncoder);
        liftSensor = config.liftSensor.create();
        liftController = new SparkMaxPIDController(lift, "liftController");
        liftController.configure(config.liftController);
        // liftController.setPositionProvider(liftEncoder::getDistance);
        // liftController.setVelocityProvider(liftEncoder::getRate);
        // liftController.setOutputConsumer(lift::set);
        this.config = config;
        localDebug = config.debug;
    }

    public static class Config 
    {
        public SolenoidConfig ratchet;
        public SolenoidConfig verticalIn;
        public SolenoidConfig verticalOut;
        public SparkMaxConfig lift;
        public NEOEncoder.Config liftEncoder;
        public DigitalSensorConfig liftSensor;
        public SparkMaxPIDController.Config liftController;

        // configuration for turret angles
        // where it's safe to raise the arm
        public double lowerBoundTurretAngleForRaising;
        public double upperBoundTurretAngleForRaising;

        // public double liftUpperBound;
        // public double liftDesiredHeight;
        // public double liftLowerBound;

        public boolean debug;

    }
}