package org.teamtators.bbt8r.subsystems;

import com.kauailabs.navx.frc.AHRS;
import edu.wpi.first.wpilibj.Encoder;
import edu.wpi.first.wpilibj.I2C;
import edu.wpi.first.wpilibj.SPI;
import edu.wpi.first.wpilibj.Solenoid;
import org.teamtators.bbt8r.DriveInputSupplier;
import org.teamtators.bbt8r.TatorRobot;
import org.teamtators.common.config.helpers.EncoderConfig;
import org.teamtators.common.config.helpers.SolenoidConfig;
import org.teamtators.common.config.helpers.SparkMaxControllerGroupConfig;
import org.teamtators.common.control.*;
import org.teamtators.common.controllers.LogitechF310;
import org.teamtators.common.datalogging.DataCollector;
import org.teamtators.common.datalogging.LogDataProvider;
import org.teamtators.common.drive.*;
import org.teamtators.common.hw.NEOEncoder;
import org.teamtators.common.hw.SparkMaxControllerGroup;
import org.teamtators.common.hw.TatorADXRS450;
import org.teamtators.common.hw.TatorSparkMax;
import org.teamtators.common.math.Pose2d;
import org.teamtators.common.scheduler.Subsystem;
import org.teamtators.common.scheduler.TrajectoryStore;
import org.teamtators.common.tester.ManualTest;
import org.teamtators.common.tester.ManualTestGroup;
import org.teamtators.common.tester.components.*;
import org.teamtators.common.util.Tuple;

import java.util.Arrays;
import java.util.List;

public class Drive extends Subsystem implements TankDrive {

    private static final String TRAJECTORIES_FILE = "Trajectories.yaml";

    private AHRS gyro;
    private Encoder leftEncoder;
    private Encoder rightEncoder;
    public NEOEncoder leftTransmissionEncoder;
    private NEOEncoder rightTransmissionEncoder;

    private Solenoid shifterA;
    private Solenoid shifterB;
    private SparkMaxControllerGroup leftMotor;
    private SparkMaxControllerGroup rightMotor;
    private Config config;

    private Timer shiftTimer = new Timer();
    private Timer shiftSyncTimer = new Timer();

    public double leftTransmissionSpeed = 0;
    public double rightTransmissionSpeed = 0;
    public double leftTransmissionTarget = 0;
    public double rightTransmissionTarget = 0;

    boolean lastRightBump = false ;
    private boolean localDebug = false;

    // private int latestPoseIndex;

    public  DriveTrainState driveTrainState_current = DriveTrainState.INITIALIZE;
    public  DriveTrainState driveTrainState_new = DriveTrainState.HIGH_GEAR;

    private DriveTrainShiftState driveTrainShiftState_current = DriveTrainShiftState.INITIALIZE;
    private DriveTrainShiftState driveTrainShiftState_new     = DriveTrainShiftState.INITIALIZE;

    public enum DriveTrainState {
        LOW_GEAR,
        SHIFTING_LOW_TO_HIGH,
        SHIFTING_HIGH_TO_LOW,
        HIGH_GEAR,
        INITIALIZE
    }

    public enum DriveTrainShiftState {
        GO_TO_NEUTRAL, // state for preparing to shift into neutral
        SYNC_SPEED, // state for changing wheel speeds in neutral
        WAIT_TO_SHIFT, // state for preparing to shift out of neutral
        SHIFTED,
        INITIALIZE
    }

    /*
   Profiles
   */
    private TankKinematics tankKinematics;
    //private IndexedCircularBuffer<Pose2d> recentPoses = new IndexedCircularBuffer<>(32);
    private PoseEstimator poseEstimator = new PoseEstimator(this/*, recentPoses, this::setLatestPoseIndex*/);
    private PidController leftController = new PidController("Drive.leftController");
    private PidController rightController = new PidController("Drive.rightController");
    private TrapezoidalProfileFollower straightMotionFollower = new TrapezoidalProfileFollower("Drive.straightMotionFollower");
    private OutputController outputController = new OutputController();

    /**
     * operates on controlling *power difference* between wheels
     */
    private PidController rotationController = new PidController("Drive.rotationController");

    /**
     * operates on controlling speeds of wheels
     */
    private PidController rotationControllerSpeeds = new PidController("Drive.rotationControllerSpeeds");

    private SparkMaxPIDController leftTransmissionController1;
    private SparkMaxPIDController leftTransmissionController2;
    private SparkMaxPIDController rightTransmissionController1;
    private SparkMaxPIDController rightTransmissionController2;
    private TrapezoidalProfileFollower kfCalibrationShim = new TrapezoidalProfileFollower("kFCalibration");

    private DriveTrajectoryFollower driveTrajectoryFollower = new DriveTrajectoryFollower(this);
    private TrajectoryStore trajectoryStore;

    private DriveMode driveMode = DriveMode.DRIVER;
    private Gear gear = Gear.HIGH;

    private TatorRobot robot;

    private boolean shiftButtonState;

    public DriveInputSupplier inputSupplier;

    private final double WHEEL_DIAMETER = 7.75;
    private final double WHEEL_CIRCUMFERENCE = WHEEL_DIAMETER * Math.PI;
    private final double HIGH_GEAR_RATIO = 1 / 20.83333;
    private final double LOW_GEAR_RATIO = 1 / 8.5;

    public Drive(TatorRobot robot) {
        super("Drive");
        leftController.setInputProvider(this::getLeftRate);
        leftController.setOutputConsumer(this::setLeftPower);
        rightController.setInputProvider(this::getRightRate);
        rightController.setOutputConsumer(this::setRightPower);

        straightMotionFollower.setPositionProvider(this::getCenterDistance);
        straightMotionFollower.setVelocityProvider(this::getCenterRate);
        straightMotionFollower.setOutputConsumer((double o) -> outputController.setStraightOutput(o, false));
        straightMotionFollower.setOnTargetPredicate(ControllerPredicates.finished());

        rotationControllerSpeeds.setInputProvider(this::getYawAngle);
        rotationControllerSpeeds.setOutputConsumer(output -> setSpeeds(output, -output));

        rotationController.setInputProvider(this::getYawAngle);
        rotationController.setOutputConsumer((double output) -> outputController.setRotationOutput(output));

        shiftButtonState = false;

        inputSupplier = new DriveInputSupplier();

        this.robot = robot;
    }

    @Override
    public void update(double delta) {
    }

    /**
     * gets the left distance
     *
     * @return
     */
    public double getLeftDistance() {
        return leftEncoder.getDistance();
    }

    /**
     * gets the right distance
     *
     * @return
     */
    public double getRightDistance() {
        return rightEncoder.getDistance();
    }

    @Override
    public void resetDistances() {
        rightEncoder.reset();
        leftEncoder.reset();
    }

    @Override
    public double getLeftRate() {
        return leftEncoder.getRate();
    }

    @Override
    public double getRightRate() {
        return rightEncoder.getRate();
    }

    public double getLeftTransmissionRate() {
        return leftTransmissionEncoder.getRate();
    }

    public double getRightTransmissionRate() {
        return rightTransmissionEncoder.getRate();
    }

    @Override
    public void setLeftPower(double power) {
        leftMotor.set(power);
    }

    @Override
    public void setRightPower(double power) {
        rightMotor.set(power);
    }

    @Override
    public void setRightSpeed(double rightSpeed) {
        rightController.setSetpoint(rightSpeed);
    }

    @Override
    public void setLeftSpeed(double leftSpeed) {
        leftController.setSetpoint(leftSpeed);
    }

    private double leftTargetSpeed = 0;

    public void setLeftTransmissionSpeed(double speed) {
        leftTargetSpeed = speed;
        leftTransmissionController1.setVelocitySetpoint(speed);
        leftTransmissionController2.setVelocitySetpoint(speed);
    }

    private double rightTargetSpeed = 0;

    public void setRightTransmissionSpeed(double speed) {
        rightTargetSpeed = speed;
        rightTransmissionController1.setVelocitySetpoint(speed);
        rightTransmissionController2.setVelocitySetpoint(speed);
    }

    public DriveTrainState getCurrentDriveTrainState() {
        return driveTrainState_current ;
    }

    public void initalizeDriveTrainState( ) {
        driveTrainState_current = DriveTrainState.LOW_GEAR ;
    }

    
    public String setDriveTrainState_new( double left, double right, boolean rightBump )
    {
        String shift_mode_str = "" ;

        // don't let the driver drive speeds while we are shifting
        if ( driveTrainState_current == DriveTrainState.HIGH_GEAR || 
             driveTrainState_current == DriveTrainState.LOW_GEAR) 
        {
            if (getDriveMode() == Drive.DriveMode.DRIVER) 
            {
                drivePowers( left, right );
            }
        }

        boolean bumpShiftState = rightBump && !lastRightBump ;

        // First State machine to determine which state we are in for the drive train
        // And if the bump-shift has been triggered, which shift-state to go to.
		
        switch ( driveTrainState_current )
        {
            case LOW_GEAR :
                // this is stable, low gear
                shift_mode_str = "LOW GEAR";
                if ( bumpShiftState )
                {
                    driveTrainState_new = DriveTrainState.SHIFTING_LOW_TO_HIGH ;
                    if (localDebug) {
                        logger.info("Entering SHIFTING_LOW_TO_HIGH");
                    }
                }
                break ;

            case HIGH_GEAR :
                shift_mode_str = "HIGH GEAR";
                double absLeftSpeed = Math.abs( getLeftRate() );
                double absRightSpeed = Math.abs( getRightRate() );

                // Make sure we are at a safe speed to downshift
                if ( bumpShiftState &&  
                    ( absLeftSpeed <= config.maxDownshiftSpeed ) &&
                        ( absRightSpeed <= config.maxDownshiftSpeed ) )
                {
                    driveTrainState_new = DriveTrainState.SHIFTING_HIGH_TO_LOW ;
                    if (localDebug) {
                        logger.info("Entering SHIFTING_HIGH_TO_LOW");
                    }
                }
                break ;

            case SHIFTING_LOW_TO_HIGH :
                shift_mode_str = "SHIFTING LOW to HIGH";
                break;

            case SHIFTING_HIGH_TO_LOW :
                shift_mode_str = "SHIFTING HIGH to LOW";
                break ;
        }

        double syncSpeedControlFactor = 0 ;

        // Since the "new" drive train state is different from the 'current' state, 
        // we need to change the motor powers, etc.
        // We also set the 'shiftstate', based on the criteria fo changing states
            
        if ( driveTrainState_current != driveTrainState_new )
        {
            driveTrainState_current = driveTrainState_new ;
            
            if (localDebug) {
                logger.info("Setting Drive State " + driveTrainState_current );		
            }
            
            switch ( driveTrainState_current )
            {
                case SHIFTING_HIGH_TO_LOW :
                    driveTrainShiftState_new = DriveTrainShiftState.GO_TO_NEUTRAL;
                    syncSpeedControlFactor = 1000*(10/6) ;
                    shiftTimer.restart();
                    break ;

                case SHIFTING_LOW_TO_HIGH :
                    driveTrainShiftState_new = DriveTrainShiftState.GO_TO_NEUTRAL;
                    syncSpeedControlFactor = 1000*(6/10) ;
                    shiftTimer.restart();
                    break ;

                case HIGH_GEAR :
                    // hold solenoids in high gear
                    shiftGear(Drive.Gear.HIGH); 
                    setLeftCurrentLimit(config.currentLimitHigh);

                    setRightCurrentLimit(config.currentLimitHigh);
                    if (localDebug)
                        logger.info( "Setting Current Limit High Gear : " + config.currentLimitHigh ) ;
                    break;

                case LOW_GEAR :
                    // hold solenoids in low gear
                    shiftGear(Drive.Gear.LOW); 
                    setLeftCurrentLimit(config.currentLimitLow);
                    setRightCurrentLimit(config.currentLimitLow);
                    if (localDebug)
                        logger.info( "Setting Current Limit Low Gear : " + config.currentLimitLow ) ;
                    break ;
            }			
	    }

        // Here we are starting to look at the Drive Train Shift sub-state
        // We are determining if we are ready to move on to the next state.

        switch ( driveTrainShiftState_current )
        {
            case GO_TO_NEUTRAL :
                if ( readyForNeutralShift() )
                {
                    // Wait before for the shift into Neutral 
                    driveTrainShiftState_new = DriveTrainShiftState.SYNC_SPEED ;
                    // restarts a timer that has to do with syncing
                    shiftTimer.restart(); 
                    
                    if (localDebug) {
                        logger.info("Finished GO_TO_NEUTRAL");
                    }
                }
                break ;

            case SYNC_SPEED :

                // Here we are purposefully dropping the target RPM each itteration 
                // until we hit the break condition 

                setLeftTransmissionSpeed(getLeftRateRPS() * syncSpeedControlFactor);
                setRightTransmissionSpeed(getRightRateRPS() * syncSpeedControlFactor);				

                // exit when on-target
                if (syncTimedOut() ||
                        ( Math.abs(getLeftTransError()) <= config.MAX_SYNC_ERROR
                            && Math.abs(getRightTransError()) <= config.MAX_SYNC_ERROR) )				
                {
                    // Either taken too long, or we are within speed range for shifting					
                    driveTrainShiftState_new = DriveTrainShiftState.WAIT_TO_SHIFT ;

                    // restarts a timer that has to do with syncing
                    shiftTimer.restart(); 					

                    if (localDebug) {
                        logger.info("Finished SYNC_SPEED");
                    }				
                }
                break;

            case WAIT_TO_SHIFT :
                if ( readyForShift() )
                {
                    // Wait after the shift out of Neutral 
                    driveTrainShiftState_new = DriveTrainShiftState.SHIFTED ;
                    if (localDebug) {
                        logger.info("Finished WAIT_TO_SHIFT");
                    }				
                }
                break;

            case SHIFTED :
                // Dont do anything
        }

        // These are the action commands for each of the state changes
        // only performed when we actually change state

        if ( driveTrainShiftState_current != driveTrainShiftState_new )
        {
            driveTrainShiftState_current = driveTrainShiftState_new ;
            
            if (localDebug) {
                logger.info("Current Drive Shift-State " + driveTrainShiftState_current );
            }

            switch ( driveTrainShiftState_current )
            {
                case GO_TO_NEUTRAL :
                    // set solenoids to neutral position
                    shiftGear(Gear.NEUTRAL);
                    if (localDebug) {
                        logger.info("Setting drive train in Neutral!");
                    }				
                    break ;			

                case SYNC_SPEED :
                    if (localDebug) {
                        logger.info("Waiting for transmission to come to speed!");
                    }				
                    break ;

                case WAIT_TO_SHIFT :
                    switch( driveTrainState_current )
                    {
                        case SHIFTING_LOW_TO_HIGH :							
                            driveTrainState_new = DriveTrainState.HIGH_GEAR ;
                            break ;

                        case SHIFTING_HIGH_TO_LOW :
                            driveTrainState_new = DriveTrainState.LOW_GEAR ;
                            break ;						
                    }
                    if (localDebug) {
                        logger.info("At Speed, now changing gears!");
                    }				
                    break;

                case SHIFTED :
                    if (localDebug) {
                        logger.info("Finished shifting gears!");
                    }				
                    break ;

            }
        }

    	// keep the last button value so that we can know which instant it has been pressed or de-pressed
        lastRightBump = rightBump; 
		
        return shift_mode_str ;
    }


    private boolean readyForNeutralShift() {
        return shiftTimer.get() >= config.shiftToNeutralDelay;
    }

    private boolean readyForShift() {
        return shiftTimer.get() >= config.shiftFromNeutralDelay;
    }

    private boolean syncTimedOut() {
        return shiftSyncTimer.get() >= config.transmissionSyncTimeout;
    }

    /**
     * @param yawAngle degrees
     */
    @Override
    public void setYawAngle(double yawAngle) {
        if (gyro == null) {
            if (localDebug) {
                logger.info("gyro cannot be configured: gyro is null");
            }
            return;
        }
        if (!gyro.isCalibrating()) {
            if (localDebug) {
                logger.info("successfully set gyro angle");
            }
            gyro.zeroYaw();
            gyro.setAngleAdjustment(yawAngle);
        } else {
            if (localDebug) {
                logger.info("gyro is currently calibrating! cannot set gyro angle");
            }
        }
    }

    @Override
    public double getYawAngle() {
        return gyro.getAngle();
    }

    @Override
    public double getYawRate() {
        return gyro.getRate();
    }

    @Override
    public Pose2d getPose() {
        return poseEstimator.getPose();
    }

    @Override
    public TankKinematics getTankKinematics() {
        return tankKinematics;
    }

    @Override
    public double getMaxSpeed() {
        return config.maxSpeed;
    }

    public DriveTrajectoryFollower getDriveTrajectoryFollower() {
        return driveTrajectoryFollower;
    }

    public DriveMode getDriveMode() {
        return driveMode;
    }

    public void setDriveMode(DriveMode driveMode) {
        this.driveMode = driveMode;
    }

    @Override
    public void stop() {
        leftController.stop();
        rightController.stop();
        leftTransmissionController1.stop();
        leftTransmissionController2.stop();
        rightTransmissionController1.stop();
        rightTransmissionController2.stop();
        straightMotionFollower.stop();
        outputController.stop();
        driveTrajectoryFollower.stop();
    }

    public void drivePowers(double left, double right) {
        setLeftPower(left);
        setRightPower(right);
    }

    private void stopPowers() {
        setLeftPower(0);
        setRightPower(0);
    }

    public void driveTrajectory(TrajectoryPath path) {
        driveTrajectoryFollower.setTrajectoryPath(path);
        driveTrajectoryFollower.start();
        leftController.start();
        rightController.start();
    }

    public void driveTrajectory(String pathName) {
        driveTrajectory(getPathByStringName(pathName));
    }

    public void stopTrajectory() {
        driveTrajectoryFollower.stop();
        leftController.stop();
        rightController.stop();
//        outputController.stop();
        rotationControllerSpeeds.stop();
        stopPowers();
        stop();
    }

    public boolean isStraightProfileOnTarget() {
        return straightMotionFollower.isOnTarget();
    }

    public boolean straightFinished() {
        return straightMotionFollower.isFinished();
    }

    public PoseEstimator getPoseEstimator() {
        return poseEstimator;
    }

    public TrajectoryPath getPathByStringName(String pathName) {
        return trajectoryStore.getPath(pathName);
    }

    public AHRS getGyro() {
        return gyro;
    }

    public void calibrateGyro() {
        logger.info("Starting Calibration");
        gyro.calibrate();
        logger.info("Calibration Completed");
    }

    public Gear getGear() {
        return gear;
    }

    public void setLeftCurrentLimit( int currentLimit ) {
        leftMotor.getSparkMaxes()[0].setSmartCurrentLimit( currentLimit );
        leftMotor.getSparkMaxes()[0].burnFlash();
        leftMotor.getSparkMaxes()[1].setSmartCurrentLimit( currentLimit );
        leftMotor.getSparkMaxes()[1].burnFlash();
        if (localDebug) {
            logger.info("finished setting left current");
        }
    }

    public void setOpenLoopRamp(double openLoopRampTime){
        leftMotor.getSparkMaxes()[0].setOpenLoopRampRate(openLoopRampTime);
        leftMotor.getSparkMaxes()[0].burnFlash();
        leftMotor.getSparkMaxes()[1].setOpenLoopRampRate(openLoopRampTime);
        leftMotor.getSparkMaxes()[1].burnFlash();

        rightMotor.getSparkMaxes()[0].setOpenLoopRampRate(openLoopRampTime);
        rightMotor.getSparkMaxes()[0].burnFlash();
        rightMotor.getSparkMaxes()[1].setOpenLoopRampRate(openLoopRampTime);
        rightMotor.getSparkMaxes()[1].burnFlash();
    }

    public void setCloseLoopRamp(double closeLoopRampTime){
        leftMotor.getSparkMaxes()[0].setClosedLoopRampRate(closeLoopRampTime);
        leftMotor.getSparkMaxes()[0].burnFlash();
        leftMotor.getSparkMaxes()[1].setClosedLoopRampRate(closeLoopRampTime);
        leftMotor.getSparkMaxes()[1].burnFlash();
        rightMotor.getSparkMaxes()[0].setClosedLoopRampRate(closeLoopRampTime);
        rightMotor.getSparkMaxes()[0].burnFlash();
        rightMotor.getSparkMaxes()[1].setClosedLoopRampRate(closeLoopRampTime);
        rightMotor.getSparkMaxes()[1].burnFlash();
    }



    public void setRightCurrentLimit( int currentLimit ) {
        rightMotor.getSparkMaxes()[0].setSmartCurrentLimit( currentLimit ) ;
        rightMotor.getSparkMaxes()[0].burnFlash();
        rightMotor.getSparkMaxes()[1].setSmartCurrentLimit( currentLimit );
        rightMotor.getSparkMaxes()[1].burnFlash();
        if (localDebug) {
            logger.info("finished setting right current");
        }
    }

    public void getRightMotorOutput(){
        logger.info("right motor 1 output" + rightMotor.getSparkMaxes()[0].getOutputCurrent());
        logger.info("right motor 2 output" + rightMotor.getSparkMaxes()[1].getOutputCurrent());
    }
    public void getLeftMotorOutput(){
        logger.info("left motor output" + leftMotor.getSparkMaxes()[0].getOutputCurrent());
        logger.info("left motor output" + leftMotor.getSparkMaxes()[1].getOutputCurrent());
    }




    public void shiftGear(Gear gear) {
        switch (gear) {
            case HIGH:
                shifterA.set(false);
                shifterB.set(false);
                break;
            case NEUTRAL:
                shifterA.set(true);
                shifterB.set(false);
                break;
            case LOW:
                shifterA.set(true);
                shifterB.set(true);
                break;
        }
    }

    public void setShiftButtonState(boolean state) {
        shiftButtonState = state;
    }

    public boolean getShiftButtonState() {
        return shiftButtonState;
    }

    public Gear readShiftedState() {
        if (shifterA.get() && shifterB.get()) {
            if (localDebug) {
                logger.info("We are in High");
            }
            return Gear.HIGH;
        } else if (!shifterA.get() && !shifterB.get()) {
            if (localDebug) {
                logger.info("We are in Low");
            }
            return Gear.LOW;
        } else {
            if (localDebug) {
                logger.info("We are in Neutral");
            }
            return Gear.NEUTRAL;
        }
    }

    public double getLeftRateRPS() {
        return leftEncoder.getRate() * config.leftEncoder.getRotationsPerDistance();
    }

    public double getRightRateRPS() {
        return rightEncoder.getRate() * config.rightEncoder.getRotationsPerDistance();
    }

    public List<Updatable> getUpdatables() {
        return Arrays.asList(
                poseEstimator,
                driveTrajectoryFollower,
                leftController,
                rightController,
                outputController
        );
    }

    public List<TatorSparkMax> getSparkMaxes() {
        TatorSparkMax[] leftSparks = (TatorSparkMax[]) leftMotor.getSpeedControllers();
        TatorSparkMax[] rightSparks = (TatorSparkMax[]) rightMotor.getSpeedControllers();
        return Arrays.asList(leftSparks[0], leftSparks[1], rightSparks[0], rightSparks[1]);
    }

    public double getLeftTransError() {
        return Math.abs(leftTargetSpeed) - Math.abs(getLeftTransmissionRate());
    }

    public double getRightTransError() {
        return Math.abs(rightTargetSpeed) - Math.abs(getRightTransmissionRate());
    }

    public enum DriveMode {
        DRIVER,
        VISION,
        FOLLOW_TRAJECTORY
    }

    public enum Gear {
        HIGH,
        NEUTRAL,
        LOW
    }

    public void configure(Config config) {
        super.configure();
        this.config = config;

//        gyro = new TatorADXRS450(SPI.Port.kOnboardCS0);
        gyro = new AHRS(I2C.Port.kOnboard);

        this.rightMotor = config.rightMotor.create();
        this.rightTransmissionEncoder = this.rightMotor.getMaster().getNeoEncoder();
        this.rightTransmissionEncoder.configure(this.rightMotor.getMaster().getEncoderConfig());
        this.leftMotor = config.leftMotor.create();
        this.leftTransmissionEncoder = this.leftMotor.getMaster().getNeoEncoder();
        this.leftTransmissionEncoder.configure(this.leftMotor.getMaster().getEncoderConfig());
        this.rightEncoder = config.rightEncoder.create();
        this.leftEncoder = config.leftEncoder.create();
        this.shifterA = config.shifterA.create();
        this.shifterB = config.shifterB.create();
        localDebug = config.debug;

        leftTransmissionController1 = new SparkMaxPIDController((TatorSparkMax) leftMotor.getSpeedControllers()[0],
                "Driver.leftTransmissionController1");
        leftTransmissionController2 = new SparkMaxPIDController((TatorSparkMax) leftMotor.getSpeedControllers()[1],
                "Driver.leftTransmissionController2");
        rightTransmissionController1 = new SparkMaxPIDController((TatorSparkMax) rightMotor.getSpeedControllers()[0],
                "Driver.rightTransmissionController1");
        rightTransmissionController2 = new SparkMaxPIDController((TatorSparkMax) rightMotor.getSpeedControllers()[1],
                "Driver.rightTransmissionController2");

        leftTransmissionController1.configure(config.leftTransmissionController);
        leftTransmissionController2.configure(config.leftTransmissionController);
        rightTransmissionController1.configure(config.rightTransmissionController);
        rightTransmissionController2.configure(config.rightTransmissionController);

        this.tankKinematics = config.tankKinematics;

        poseEstimator.setKinematics(config.tankKinematics);
        poseEstimator.start();

        double time = Timer.getTimestamp();

        trajectoryStore = new TrajectoryStore(robot.getConfigLoader(), tankKinematics.getDifferentialDriveKinematics());

        driveTrajectoryFollower.setDriveKinematics(tankKinematics.getDifferentialDriveKinematics());
        driveTrajectoryFollower.configure(this.config.driveTrajectoryFollower);

        this.rotationController.configure(config.rotationController);
        rotationControllerSpeeds.configure(config.rotationControllerSpeeds);

        trajectoryStore.loadPathsFromConfig(TRAJECTORIES_FILE);
        if (localDebug) {
            logger.info("Time elapsed loading paths: {}", Timer.getTimestamp() - time);
        }

        leftController.configure(config.speedController);
        rightController.configure(config.speedController);

        driveTrajectoryFollower.configure(this.config.driveTrajectoryFollower);
        trajectoryStore.loadPathsFromConfig(TRAJECTORIES_FILE);
    }

    public static class Config {
        public EncoderConfig leftEncoder;
        public EncoderConfig rightEncoder;
        public SparkMaxControllerGroupConfig leftMotor;
        public SparkMaxControllerGroupConfig rightMotor;
        public SolenoidConfig shifterA, shifterB;
        public double maxSpeed;
        public SparkMaxPIDController.Config leftTransmissionController;
        public SparkMaxPIDController.Config rightTransmissionController;
        public TankKinematics tankKinematics;

        public PidController.Config rotationControllerSpeeds;
        public PidController.Config rotationController;
        public PidController.Config speedController;
        public DriveTrajectoryFollower.Config driveTrajectoryFollower;
        public int currentLimitLow, currentLimitHigh;

        public double shiftToNeutralDelay;
        public double shiftFromNeutralDelay;
        public double transmissionSyncTimeout;
        public double maxDownshiftSpeed;

        public double shiftDownSpeedThreshold;
        public double shiftUpSpeedThreshold;
        public double driveTurnSpeedThreshold;
        public double kShiftingToHigh;
        public double kShiftingToLow;
        public double MAX_SYNC_ERROR;

        public boolean debug;

    }

    /**
     * Class that can control the wheels in a more abstract way (ex. rotationOutput)
     */
    private class OutputController extends AbstractUpdatable {
        double leftOutput;
        double rightOutput;
        double straightOutput;
        boolean clearOutputs = false; // unused functionality
        double rotationOutput = 0;

        OutputController() {
            super("Drive.OutputController");
        }

        void setLeftOutput(double leftOutput) {
            this.leftOutput = leftOutput;
        }

        void setRightOutput(double rightOutput) {
            this.rightOutput = rightOutput;
        }

        void setStraightOutput(double straightOutput, boolean clearOutputs) {
            this.straightOutput = straightOutput;
            this.clearOutputs = clearOutputs;
        }

        void setStraightOutput(double straightOutput) {
            setStraightOutput(straightOutput, true);
        }

        void setRotationOutput(double rotationOutput) {
            this.rotationOutput = rotationOutput;
        }

        @Override
        public synchronized void stop() {
            if (isRunning()) {
                setPowers(0, 0);
            }
            rotationOutput = 0;
            straightOutput = 0;
            super.stop();
        }

        @Override
        protected void doUpdate(double delta) {
            double left = leftOutput;
            double right = rightOutput;
            left += straightOutput;
            right += straightOutput;
            left += rotationOutput;
            right -= rotationOutput;
            setPowers(left, right);
        }
    }

    public double rotationsToInches(double rotations) {
        return rotationsToInches(rotations, readShiftedState());
    }

    public double rotationsToInches(double rotations, Gear gear) {
        switch (gear) {
            case HIGH:
                return rotations * HIGH_GEAR_RATIO * WHEEL_CIRCUMFERENCE;
            case LOW:
                return rotations * LOW_GEAR_RATIO * WHEEL_CIRCUMFERENCE;
            case NEUTRAL:
            default:
                if (localDebug) {
                    logger.info("Cannot Calculate Distance In Neutral Mode");
                }
                return 0;
        }
    }

///////////////////////////////////////////////////////////////////////////////////////////

    /**************************************   TESTING   **************************************/
///////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public ManualTestGroup createManualTests() {
        ManualTestGroup tests = super.createManualTests();
        //tests.addTest(new ADXRS450Test(gyro));
        tests.addTest(new EncoderTest("leftEncoder", leftEncoder));
        tests.addTest(new MotorControllerGroupTest("leftMotor", leftMotor));
        tests.addTest(new SpeedControllerTest(
                "motor 0 in left motors id: " + leftMotor.getSparkMaxes()[0].getDeviceId(),
                leftMotor.getSpeedControllers()[0]));
        tests.addTest(new SpeedControllerTest(
                "motor 1 in left motors id: " + leftMotor.getSparkMaxes()[1].getDeviceId(),
                leftMotor.getSpeedControllers()[1]));
        tests.addTest(new EncoderTest("rightEncoder", rightEncoder));
        tests.addTest(new SpeedControllerTest(
                "motor 0 in right motors id: " + rightMotor.getSparkMaxes()[0].getDeviceId(),
                rightMotor.getSpeedControllers()[0]));
        tests.addTest(new SpeedControllerTest(
                "motor 1 in right motors id: " + rightMotor.getSparkMaxes()[1].getDeviceId(),
                rightMotor.getSpeedControllers()[1]));
        tests.addTest(new MotorControllerGroupTest("rightMotor", rightMotor));
        tests.addTest(new SolenoidTest("shifterA", shifterA));
        tests.addTest(new SolenoidTest("shifterB", shifterB));
        tests.addTest(new ShiftTest());
        tests.addTest(new TransmissionTest());
        tests.addTest(new DriveSpeedTest());
        tests.addTest(new SparkMaxAutomatedVelocityCalibrationTest("driveCalib", this::getCenterRate, (a) -> {
            setLeftPower(a);
            setRightPower(a);
        }, 2, 0.5, 2,
                new double[]{0.2, -0.2, 0.3, -0.3, 0.4, -0.4, 0.5, -0.5}));

//        tests.addTest(new SparkMaxAutomatedVelocityCalibrationTest("leftTransAutoCalib", leftTransmissionEncoder::getRateRPM, leftMotor::set, 2.5, 0.15, 2,
//                new double[]{0.1, 0.2, 0.3, 0.4, -0.1, -0.2, -0.3, -0.4, 0.5, -0.5}));
//        tests.addTest(new SparkMaxAutomatedVelocityCalibrationTest("rightTransAutoCalib", rightTransmissionEncoder::getRateRPM, rightMotor::set, 2.5, 0.15, 2,
//                new double[]{0.1, 0.2, 0.3, 0.4, -0.1, -0.2, -0.3, -0.4, 0.5, -0.5}));

        // no autos for now, but keep these around
//        tests.addTests(new TrajectoryFollowerTest("straightTest", "straight"),
//                new TrajectoryFollowerTest("reverseStraightTest",
//                        "reverseStraight",
//                        new Pose2d(Translation2d.zero(), new Rotation(0.0, 1.0))),
//                new TrajectoryFollowerTest("forwardRightTest", "forwardRight"),
//                new TrajectoryFollowerTest("snakeTest", "snake"),
//                new TrajectoryFollowerTest("cubicTest", "cubicTest")
//                        .withMessages("D5 facing D6", "D5 facing D6"),
//                new TrajectoryFollowerTest("backwardRightTest", "backwardRight"),
//                new TrajectoryFollowerTest("circleTest", "circleTest"),
//                new TrajectoryFollowerTest("barrelQuintic", "barrelQuintic")
//        );
        tests.addTest(new OutputControllerTest(rotationController, 0, 180));
        tests.addTest(new SpinTest(rotationControllerSpeeds, 180));
        return tests;
    }

    /**
     * Test to control the goal position (using an axis) of a controller, using the outputController as well
     */
    public class OutputControllerTest extends ManualTest {
        double minSetpoint;
        AbstractController controller;
        double maxSetpoint;
        boolean running = false;

        public OutputControllerTest(AbstractController controller, double maxSetpoint, double minSetpoint) {
            super(controller.getName());
            this.controller = controller;
            this.maxSetpoint = maxSetpoint;
            this.minSetpoint = minSetpoint;
        }

        @Override
        public void start() {
            printTestInstructions("Testing {} {} (setpoint range = [{}, {}])",
                    controller.getClass().getSimpleName(), controller.getName(), minSetpoint, maxSetpoint);
            printTestInstructions("Press A to enable, B to disable, X to get information and joystick to set setpoint");
        }

        protected void enableController() {
            controller.start();
        }

        protected void disableController() {
            controller.stop();
        }

        @Override
        public void update(double delta) {
            if (running)
                controller.update(delta);
        }

        @Override
        public void onButtonDown(LogitechF310.Button button) {
            switch (button) {
                case A:
                    enableController();
                    outputController.start();
                    running = true;
                    break;
                case B:
                    stop();
                    running = false;
                    break;
                case X:
                    printTestInstructions("current input: {}, current error: {}", controller.getInput(), controller.getError());
                    break;
            }
        }

        @Override
        public void updateAxis(double value) {
            double x = (1 - value) / 2.0;
            double output = minSetpoint + x * (maxSetpoint - minSetpoint);
            controller.setSetpoint(output);
        }

        @Override
        public void stop() {
            printTestInstructions("stopping outputControllerTest");
            disableController();
            outputController.stop();
            running = false;
        }
    }

    /**
     * Test to spin the robot a certain amount of degrees
     */
    public class SpinTest extends ManualTest {
        double goal;
        PidController controller;
        boolean running = false;
        Tuple<Double, Double> startWheels = new Tuple<>(0.0, 0.0);
        Tuple<Double, Double> endWheels = new Tuple<>(0.0, 0.0);
        Tuple<Double, Double> deltaWheels = new Tuple<>(0.0, 0.0);

        /**
         * @param goal: angle in degrees
         */
        public SpinTest(PidController controller, double goal) {
            super("SpinTest");
            this.goal = goal;
            this.controller = controller;
        }

        public void start() {
            printTestInstructions("To angle {}", goal);
            printTestInstructions("Press A to enable, B to disable, X to get information and joystick to set setpoint");
        }

        @Override
        public void update(double delta) {
            if (!running) return;
            leftController.update(delta);
            rightController.update(delta);
            controller.update(delta);
        }

        @Override
        public void onButtonDown(LogitechF310.Button button) {
            switch (button) {
                case A:
                    running = true;
                    leftController.start();
                    rightController.start();
                    controller.start();

                    controller.setSetpoint(goal);
                    startWheels = new Tuple<>(0.0, 0.0);
                    break;
                case B:
                    running = false;
                    stop();
                    break;
                case X:
                    printTestInstructions("current error: {}", controller.getError());
                    break;
            }
        }

        @Override
        public void stop() {
            controller.stop();
            leftController.stop();
            rightController.stop();
            endWheels = new Tuple<>(getLeftDistance(), getRightDistance());
            deltaWheels = new Tuple<>(
                    endWheels.getA() - startWheels.getA(),
                    endWheels.getB() - startWheels.getB()
            );
            var leftEffectiveTrackWidth = 360.0 * Math.abs(deltaWheels.getA()) / (Math.PI * goal);
            var rightEffectiveTrackWidth = 360.0 * Math.abs(deltaWheels.getB()) / (Math.PI * goal);
            printTestInfo("effective track width is left: {}, right {}", leftEffectiveTrackWidth, rightEffectiveTrackWidth);
        }
    }

    /**
     * Easy way to play-test auto driving paths
     */
    public class TrajectoryFollowerTest extends ManualTest {
        String trajectoryName;
        TrajectoryPath trajectory;
        Pose2d startPose;
        String startPoseMessage;
        String endPoseMessage;
        boolean invalid = false;

        public TrajectoryFollowerTest(String name, String trajectoryName) {
            super(name);
            this.trajectoryName = trajectoryName;
            trajectory = getTrajectory(trajectoryName);
            if (invalid) return;
            startPose = trajectory.getStart().getPose();
            if (trajectory.getReversed())
                startPose = startPose.invertYaw();
        }

        public TrajectoryFollowerTest(String name, String trajectoryName, Pose2d startPose) {
            super(name);
            this.trajectoryName = trajectoryName;
            this.startPose = startPose;
            this.trajectory = getTrajectory(trajectoryName);
        }

        private TrajectoryPath getTrajectory(String name) {
            try {
                var traj = getPathByStringName(name);
                return traj;
            } catch (IllegalArgumentException e) {
                invalid = true;
                return null;
            }
        }

        public TrajectoryFollowerTest withMessages(String startPoseMessage, String endPoseMessage) {
            this.startPoseMessage = startPoseMessage;
            this.endPoseMessage = endPoseMessage;
            return this;
        }

        @Override
        public void start() {
            if (invalid) {
                printTestInfo("invalid trajectory");
                return;
            }
            if (localDebug) {
                logger.info("Press A to start, X to stop");
            }
            poseEstimator.setPose(startPose); // reset to start pose
            if (localDebug) {
                logger.info("Set pose to " + startPose.toString());
            }
            if (startPoseMessage != null)
                printTestInfo("should start at {}", startPoseMessage);
            if (endPoseMessage != null)
                printTestInfo("should end at {}", endPoseMessage);
        }

        @Override
        public void onButtonDown(LogitechF310.Button button) {
            switch (button) {
                case A:
                    if (invalid) {
                        if (localDebug) {
                            logger.info("trajectory {} may not exist, or was not generated correctly", trajectoryName);
                        }
                        break;
                    }
                    if (localDebug) {
                        logger.info("Driving trajectory!");
                    }
                    driveTrajectory(trajectoryName);
                    break;
                case X:
                    stopTrajectory();
            }
        }
    }

    /**
     * Easy way to play-test shifting
     */
    public class ShiftTest extends ManualTest {
        public ShiftTest() {
            super("ShiftTest");
        }

        @Override
        public void start() {
            printTestInfo("Press X to shift low. Press B to shift neutral. Press A to shift high.");
        }

        @Override
        public void onButtonDown(LogitechF310.Button button) {
            switch (button) {
                case X:
                    shiftGear(Gear.LOW);
                    break;
                case B:
                    shiftGear(Gear.NEUTRAL);
                    break;
                case A:
                    shiftGear(Gear.HIGH);
                    break;
            }
        }
    }

    /**
     * Looks like controlled speed driving test
     * Controlled meaning PID
     * but also it is driven using the controller
     */
    public class TransmissionTest extends ManualTest {
        private double desiredV;
        private double axisVal;
        private double leftV;
        private double rightV;
        private boolean run = false;
        private LogDataProvider logDataProvider = new LogDataProvider() {
            @Override
            public String getName() {
                return "TransmissionTest";
            }

            @Override
            public List<Object> getKeys() {
                return Arrays.asList("desiredVelocity", "leftV", "rightV");
            }

            @Override
            public List<Object> getValues() {
                return Arrays.asList(desiredV, leftV, rightV);
            }
        };
        private DataCollector dataCollector = DataCollector.getDataCollector();

        public TransmissionTest() {
            super("TransmissionTest");
        }

        @Override
        public void updateAxis(double value) {
            axisVal = value;
        }

        public void start() {
            run = false;
            printTestInfo("Press A to set transmission speed from joystick. Hold B to run.");
        }

        @Override
        public void onButtonDown(LogitechF310.Button button) {
            switch (button) {
                case A:
                    run = true;
                    dataCollector.startProvider(logDataProvider);
                    break;
                case B:
                    desiredV = (axisVal + 1) / 2.0 * 5800;
                    if (localDebug) {
                        logger.info("Set desired velocity to {}", desiredV);
                    }
                    break;
            }
        }

        @Override
        public void onButtonUp(LogitechF310.Button button) {
            switch (button) {
                case A:
                    run = false;
                    dataCollector.stopProvider(logDataProvider);
                    break;
            }
        }

        @Override
        public void update(double delta) {
            leftV = leftTransmissionEncoder.getRateRPM();
            rightV = rightTransmissionEncoder.getRateRPM();
            if (run) {
                setLeftTransmissionSpeed(desiredV);
                setRightTransmissionSpeed(desiredV);
            } else {
                setLeftPower(0);
                setRightPower(0);
            }
        }
    }

    /**
     * Doesn't work afaik. Needs work, testing.
     */
    public class DriveSpeedTest extends ManualTest {
        private double value;

        public DriveSpeedTest() {
            super("DriveSpeedTest");
        }

        @Override
        public void start() {
            // the output controller controls the robot using powers so
            //   since we want to control it with speeds, we need to
            //   disable the output controller
            outputController.stop();
            printTestInfo("Hold A to enable controller. Press X to set setpoint.");
        }

        @Override
        public void stop() {
            leftController.stop();
            rightController.stop();
        }

        @Override
        public void updateAxis(double value) {
            this.value = value;
        }

        @Override
        public void onButtonDown(LogitechF310.Button button) {
            switch (button) {
                case A:
                    leftController.start();
                    rightController.start();
                    break;
                case X:
                    // presumably, -1 <= value <= 1
                    var speed = (value + 1.0) / 2.0 * 80.0;
                    if (localDebug) {
                        logger.info("{}", speed);
                    }
                    setSpeeds(speed, speed);
                    break;
            }
        }

        @Override
        public void onButtonUp(LogitechF310.Button button) {
            switch (button) {
                case A:
                    value = 0;
                    leftController.stop();
                    rightController.stop();
                    break;
            }
        }
    }

}
