package org.teamtators.bbt8r.subsystems;

import org.teamtators.common.config.helpers.DigitalSensorConfig;
import org.teamtators.common.config.helpers.SparkMaxConfig;
import org.teamtators.common.control.SparkMaxPIDController;
import org.teamtators.common.control.Timer;
import org.teamtators.common.hw.DigitalSensor;
import org.teamtators.common.hw.NEOEncoder;
import org.teamtators.common.hw.TatorSparkMax;
import org.teamtators.common.scheduler.Subsystem;
import org.teamtators.common.tester.AutomatedTest;
import org.teamtators.common.tester.ManualTestGroup;
import org.teamtators.common.tester.automated.MotorEncoderTest;
import org.teamtators.common.tester.components.DigitalSensorTest;
import org.teamtators.common.tester.components.SpeedControllerTest;

import org.teamtators.bbt8r.subsystems.SuperStructure.*;
import org.teamtators.bbt8r.TatorRobot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BallChannel extends Subsystem {

    private TatorSparkMax horizontalRollers;
    private TatorSparkMax verticalRollers;
    private SparkMaxPIDController verticalController;
    private TatorSparkMax kingRoller;
    private NEOEncoder kingEncoder;
    private NEOEncoder verticalEncoder;
    private DigitalSensor kingEntranceSensor;
    private DigitalSensor topVerticalSensor;
    private DigitalSensor lastBallSensor;
    private NEOEncoder horizontalEncoder;

    private double verticalRollerPower;
    private double horizontalRollerPower;
    private double kingRollerPower;
    private double verticalkV;

    private double spitVerticalPower;
    private double spitHorizontalPower;
    private double spitKingPower;

    private double verticalRollerQueuingPower;
    private double horizontalRollerQueuingPower;
    private double kingRollerQueuingPower;

    private double currentVerticalPosition;
    // private double currentKingPosition;

    private Turret turret;
    private SuperStructure superStructure;

    private Config config;

    private boolean firstBall = false;
    private boolean localDebug = false;

    private boolean lastLastBallSensorState = false;
    private boolean specialCaseEnabled = false;
    private boolean lastSpecialCaseActive = false;
    private double specialCaseVerticalStartingTicks;

    Timer timer = new Timer();

    // Initalize the states
    private BallChannelFeedingSubState ballFeedingSubState_current = BallChannelFeedingSubState.COLLECTING_FEED;
    private BallChannelFeedingSubState ballFeedingSubState_next = BallChannelFeedingSubState.INITIALIZE;

    private FiringSubState firingSubState_current = FiringSubState.INITIALIZE;
    private FiringSubState firingSubState_next = FiringSubState.INITIALIZE;

    private SuperStructureState lastSuperStructureState = SuperStructureState.IDLING;

    public enum ballChannelState {
        PICKING,
        IDLING,
        SHOOTING,
        CLEARING
    }

    public enum BallChannelFeedingSubState {
        COLLECTING_FEED,
        BRIDGE_COLLECT,
        BRIDGE_HANDOFF,
        BRIDGE_CLEAR,
        STOP_VERTICAL_FEED,
        STOP_ALL_FEED,
        ADVANCE_VERTICAL,
        INITIALIZE
    }

    public enum FiringSubState {
        QUEUING,
        KES_LBS_QUEUING,
        WAITING,
        FIRING,
        INITIALIZE
    }

    public BallChannel(TatorRobot robot) 
    {
        super("BallChannel");
        timer.start();
        // constructor, starts a timer
    }

    public void setRobotSubSystemPointers(  SuperStructure superStructure_ptr, Turret turret_ptr )
    {
        superStructure = superStructure_ptr ;
        turret = turret_ptr ;    
    }

    public void updateBallChannelState()
    {
        // Pull the current superstructure state
        SuperStructureState newSuperStructureState = superStructure.getCurrentSuperStructureState() ;

        switch (newSuperStructureState)
        {
            case SHOOTING:
                updateShootingStateMachine() ;
                break;

            case PICKING:
                updatePickerStateMachine() ;
                break;

            case IDLING:
                if (lastSuperStructureState != newSuperStructureState)
                    stopMotors();
                break;

            case SPITTING:
                if (lastSuperStructureState != newSuperStructureState) {
                    setVerticalRollersPower(spitVerticalPower);
                    setHorizontalRollersPower(spitHorizontalPower);
                    setKingRollerPower(spitKingPower);
                }
                break;

            case CLEARING:
                if (lastSuperStructureState != newSuperStructureState) {
                    setVerticalRollersPower(-spitVerticalPower/2.0);
                    setHorizontalRollersPower(-spitHorizontalPower);
                    setKingRollerPower(-0.25);
                }
                break;
        }

        lastSuperStructureState = newSuperStructureState;

    }

    public void setFirstBalltrue() {
        firstBall = true ;
        // sets the flag to indicate the first ball
    }

    public void setHorizontalRollersPower(double power) {
        horizontalRollers.set(power);
        // sets the power for horizontal rollers
    }

    public void setVerticalRollersPower(double power) {
        verticalRollers.set(power);
        // sets the power for vertical rollers
    }

    public void setKingRollerPower(double power) {
        kingRoller.set(power);
        // sets the power for the king roller
    }

    public boolean getKingEntranceSensor() {
        return kingEntranceSensor.get();
        // returns the value of the king roller sensor
    }

    public boolean getTopVerticalSensor() {
        return topVerticalSensor.get();
        // returns the value of the top vertical sensor
    }

    public boolean getLastBallSensor() {
        return lastBallSensor.get();
        // returns the value of the las ball sensor
    }

    public double getVerticalEncoder() {
        return verticalEncoder.getCounts();
        // returns the value of the vertical roller encoder
    }

    public double getKingEncoder() {
        return kingEncoder.getCounts();
        // returns the value of the king roller encoder
    }

    public double getHorizontalEncoder() {
        return horizontalEncoder.getCounts();
        // returns the value of the horizontal roller encoder
    }

    public void setMotorsPower(double horizontalPower, double verticalPower, double kingRollerPower) {
        horizontalRollers.set(horizontalPower);
        verticalRollers.set(verticalPower);
        kingRoller.set(kingRollerPower);
        // sets the power for all the rollers (horizontal, vertical, and king)
    }

    public void stopMotors() {
        setMotorsPower(0, 0, 0);
        // stops the motors.
    }

    public double getVerticalRPM() {
        return verticalEncoder.getRateRPM();
        // returns the RPM for the vertical roller encoder
    }

    public double getKingRollerRPM() {
        return kingEncoder.getRateRPM();
        // returns the RPM for the king roller encoder
    }

    public double getSyncedVerticalVelocity() {
        double kingOmega = getKingRollerRPM();
        return Math.abs(kingOmega) * 2 * config.verticalKingRatio;
        // calculates and returns the velocity the vertical roller has to be at to match
        // the horizontal roller
    }

    public void setVerticalVelocity(double rps) {
        verticalController.setVelocitySetpoint(rps);
        // sets the vertical velocity
    }

    private void updatePickerStateMachine( ) {

        if (getTopVerticalSensor()) {
            // Check to see if a ball is at the top of the vertical ball channel
            // ready to fire, in which case we need to stop the feed.
            ballFeedingSubState_next = BallChannelFeedingSubState.STOP_ALL_FEED;

            if ( localDebug ) {
                logger.info("Stopping Horizontal and Vertical Feed");
            }
        }

        switch (ballFeedingSubState_current) 
        {
            case COLLECTING_FEED:
                // Feed the ball along the horizontals until it hits the 
                // King Entrance Sensor
                if (getKingEntranceSensor() == true) {
                    ballFeedingSubState_next = BallChannelFeedingSubState.BRIDGE_COLLECT;
                    if (localDebug){
                        logger.info("Exiting " + ballFeedingSubState_current + " State");
                    }
                }
                break;

            case BRIDGE_COLLECT:
                // Move the ball using the King roller, until the Last Ball 
                // Sensor is true, then go to handoff
                if (getLastBallSensor() == true) {

                    ballFeedingSubState_next = BallChannelFeedingSubState.BRIDGE_HANDOFF;

                    if ( localDebug ) {
                        logger.info("Exiting " + ballFeedingSubState_current + " State");
                    }
                }
                break;

            case BRIDGE_HANDOFF:
                // Start keeping track of the vertical ball channel ticks, as we move the ball 
                // up to the vertical channel
                if (getLastBallSensor() == false) {

                    currentVerticalPosition = verticalEncoder.getRotations();
                    // currentKingPosition = kingEncoder.getRotations();

                    // Update for the special case, keeping track of the last ball past the LBS sensor
                    specialCaseVerticalStartingTicks = currentVerticalPosition ;
                    // Setting the Old LBS sensor state to true - IMPORTANT
                    lastLastBallSensorState = true ;
                    
                    if (localDebug) {
                        // logger.info( "Starting Vertical Ticks : " + currentVerticalPosition );
                        // logger.info( "Starting King Ticks : " + currentKingPosition );
                    }

                    ballFeedingSubState_next = BallChannelFeedingSubState.ADVANCE_VERTICAL;
                    if ( localDebug ){
                        logger.info("Exiting " + ballFeedingSubState_current + " State" );
                    }
                }
                break;

            case BRIDGE_CLEAR:
                // Start keeping track of the vertical ball channel ticks, as we move the ball 
                // up to the vertical channel

                if (getLastBallSensor() == false) {
                    currentVerticalPosition = verticalEncoder.getRotations();                    
                    // currentKingPosition = kingEncoder.getRotations();

                    // Update for the special case, keeping track of the last ball past the LBS sensor
                    specialCaseVerticalStartingTicks = currentVerticalPosition ;
                    // Setting the Old LBS sensor state to true - IMPORTANT
                    lastLastBallSensorState = true ;

                    if (localDebug){
                        // logger.info( "Starting Vertical Ticks : " + currentVerticalPosition );
                        // logger.info( "Starting King Ticks : " + currentKingPosition );
                    }

                    ballFeedingSubState_next = BallChannelFeedingSubState.ADVANCE_VERTICAL;
                    if ( localDebug ){
                        logger.info("Exiting " + ballFeedingSubState_current + " State" );
                    }
                }
                break;

            case ADVANCE_VERTICAL:
                // Continue advancing the vertical channel until the minimum 
                // number of ticks has been counted 
                
                double new_VerticalPosition = verticalEncoder.getRotations() ;
                // double new_KingPosition = kingEncoder.getRotations();
                
                if (localDebug) {
                    // logger.info( "Current Vertical Ticks : " + new_VerticalPosition );
                    // logger.info( "Current King Ticks : " + new_KingPosition );
                }

                if ( Math.abs( new_VerticalPosition - currentVerticalPosition ) >= config.verticalBallQueuingSpacing) {
                    if ( localDebug ) {
                        logger.info("Exiting " + ballFeedingSubState_current + " State, Vertical Position : " + new_VerticalPosition );
                        // logger.info("Exiting " + ballFeedingSubState_current + " State, King Position : " + new_KingPosition );
                    }
                    ballFeedingSubState_next = BallChannelFeedingSubState.STOP_VERTICAL_FEED;
                }
                break;

            case STOP_VERTICAL_FEED:
                // Stop the vertical feed, and go back to Collecting
                ballFeedingSubState_next = BallChannelFeedingSubState.COLLECTING_FEED;
                if ( localDebug ){
                    logger.info("Exiting " + ballFeedingSubState_current + " State");
                }
                break;

            case STOP_ALL_FEED:
                // Stop all motors
                break;

            case INITIALIZE:
                // Check the state, make sure that we are entering the right state based 
                // on the Last Ball Sensor
                if (getLastBallSensor() == true ) {
                    ballFeedingSubState_next = BallChannelFeedingSubState.BRIDGE_CLEAR;
                }
                else {
                    ballFeedingSubState_next = BallChannelFeedingSubState.COLLECTING_FEED;
                }
                break ;

            default:
                // Fallback case
                ballFeedingSubState_next = BallChannelFeedingSubState.COLLECTING_FEED;
                break;
        }

        if (ballFeedingSubState_next != ballFeedingSubState_current)
        {
            // Set the current state
            ballFeedingSubState_current = ballFeedingSubState_next;
            logger.info("Entering " + ballFeedingSubState_current + " State");

            switch ( ballFeedingSubState_current)
            {
                case COLLECTING_FEED:
                    // Turn on the horizontals and King roller
                    setHorizontalRollersPower(horizontalRollerPower);
                    setKingRollerPower(kingRollerPower);
                    break;

                case BRIDGE_COLLECT:
                    // Turn off the horizontals and leave on the King roller
                    setHorizontalRollersPower(0);
                    setKingRollerPower(kingRollerPower);

                    // Turn on the verticals 
                    double verticalPow = 0;
                    verticalPow = getSyncedVerticalVelocity() * verticalkV;
                    setVerticalRollersPower(verticalPow * 0.25);
                    break;

                case BRIDGE_HANDOFF:
                    if (getKingEntranceSensor() == false) {
                        // Turn on the horizontals 
                        setHorizontalRollersPower(horizontalRollerPower);
                    }
                    else {
                        // Turn off the horizontals
                        setHorizontalRollersPower(0);
                    }
                    
                    // King roller should be running
                    setKingRollerPower(kingRollerPower);

                    // Turn on the verticals
                    verticalPow = getSyncedVerticalVelocity() * verticalkV;
                    setVerticalRollersPower(verticalPow);
                    break;

                case BRIDGE_CLEAR:
                    if(getTopVerticalSensor() == false) {
                        setHorizontalRollersPower(0);
                        setKingRollerPower(kingRollerPower);
                        setVerticalRollersPower(3*verticalRollerPower/4.0);
                    }
                    break;

                case STOP_VERTICAL_FEED:
                    // Stop verticals, but leave the King roller
                    // and horizontals running
                    setVerticalRollersPower(0);
                    setKingRollerPower(kingRollerPower);
                    setHorizontalRollersPower(horizontalRollerPower);
                    break;

                case STOP_ALL_FEED:
                    // Stop all motors
                    stopMotors();
                    break;
            }
        }
    }

    private void updateShootingStateMachine() 
    {
        // This code gets executed every iteration
        switch (firingSubState_current) 
        {
            // raise balls to top of lift

            case QUEUING:

                if ( ( getKingEntranceSensor() == true ) || 
                     ( getLastBallSensor() == true ) )
                {
                    logger.info("QUEUING interrupted, ball detected on KES or LBS, moving to KES LBS QUEUING");
                    setFiringSubState( FiringSubState.KES_LBS_QUEUING );
                }

                if (getTopVerticalSensor()== true) {
                    // Another ball has arrived at the top sensor
                    // need to wait before firing
                    logger.info("QUEUING completed, moving onto WAITING");
                    setFiringSubState( FiringSubState.WAITING );
                }

                break;

                
            case KES_LBS_QUEUING:

                if ( ( getKingEntranceSensor() == false ) && 
                     ( getLastBallSensor() == false ) )
                {
                    logger.info("KES LBS QUEUING interrupted, moving back to QUEUING");
                    setFiringSubState( FiringSubState.QUEUING );
                }

                if (getTopVerticalSensor()== true) {
                    // Another ball has arrived at the top sensor
                    // need to wait before firing
                    logger.info("KES LBS QUEUING completed, moving onto WAITING");
                    setFiringSubState( FiringSubState.WAITING );
                }

                break;

            // wait for ready to fire
            case WAITING:
                if ( (timer.hasPeriodElapsed(config.shootingWaitTime) || firstBall)
                        && turret.isFlywheelOnTarget() 
                        && ( turret.getFlywheelSpeed() > 1500 ) )
                {
                    logger.info("WAITING Conditions met, moving to FIRING. First ball state: " + firstBall);

                    // Reset the first ball flag now that we have shot the first ball
                    if ( firstBall ) {
                        firstBall = false ;
                    }

                    setFiringSubState( FiringSubState.FIRING );
                }
                break;

            // firing
            case FIRING:
                if (getTopVerticalSensor()==false) 
                {
                    // The ball has left the turret, go back to queuing
                    logger.info("FIRING Completed, moving to QUEUING"); 
                    setFiringSubState( FiringSubState.QUEUING ) ;
                }
                break;

            // Setting up the initialization
            case INITIALIZE:
                firstBall = true;

                // Determine if there is a ball already at the top of the ball channel
                if ( getTopVerticalSensor() == true ) {
                   setFiringSubState(FiringSubState.WAITING);
                } else {
                   setFiringSubState(FiringSubState.QUEUING);
                }
                break;
        }

        // SPECIAL CASE triggers
        // SPECIAL CASE START: clear last ball sensor

        if (getLastBallSensor() == false && lastLastBallSensorState == true) {
            // This is specific to the firing state machine, it updates the vertical ticks
            // when the last ball exits the LBS sensor.  We do not turn this on until we have seen 
            // the LBS sensor go from true (ball ) to false (no-ball)
            specialCaseEnabled = true;
            specialCaseVerticalStartingTicks = verticalEncoder.getRotations();
        }

        // get the LBS sensor for the next time around
        lastLastBallSensorState = getLastBallSensor();

        // Only run this code if the firing sub-state changes
        if ( ( firingSubState_current != firingSubState_next ) || 
             ( lastSpecialCaseActive != specialCaseActive() ) )
        {
            // Set the current state
            firingSubState_current = firingSubState_next ;
            logger.info("Entering Firing Sub State : " + firingSubState_next ) ; 

            switch ( firingSubState_next ) 
            {
                case QUEUING:                    
                    // Restart the timer to make sure that we have the right
                    // time gap between balls
                    timer.restart();

                    // Set the Roller Powers for QUEUING
                    setKingRollerPower(kingRollerQueuingPower);
                    setVerticalRollersPower(verticalRollerQueuingPower);
                    setHorizontalRollersPower(horizontalRollerQueuingPower);
                    break;

                case KES_LBS_QUEUING:
                    // Set the Roller Powers for QUEUING
                    setKingRollerPower(kingRollerQueuingPower);
                    setVerticalRollersPower(verticalRollerQueuingPower);
                    setHorizontalRollersPower(0);
                    break;

                case WAITING:
                    // Set the Roller Powers for WAITING
                    if (specialCaseActive()) {
                        setHorizontalRollersPower(horizontalRollerQueuingPower);
                        setKingRollerPower(0);
                    } else {
                        setHorizontalRollersPower(0);
                        setKingRollerPower(0);
                    }
                    setVerticalRollersPower(0);
                    break;

                case FIRING:
                    // Set the Roller Powers for firing
                    /*SPECIAL CASE :: firing, waiting
                    * start counting when clear lastballsensor
                    * start horizontal if count big enough
                    * stop horizontal when we block kingentrance?? (this condition we might need to change)
                    * */
                    // Assuming nobody randomly clears the vertical encoder.
                    if (specialCaseActive()) {
                        setHorizontalRollersPower(horizontalRollerQueuingPower);
                        setKingRollerPower(kingRollerQueuingPower);

                        // reset the state 
                        specialCaseEnabled = false;
                    } else {
                        setHorizontalRollersPower(0);
                        setKingRollerPower(0);
                    }
                    setVerticalRollersPower(verticalRollerPower);
                    break;
            }        
        }

        // Update the tracking of the special case
        lastSpecialCaseActive = specialCaseActive();
    }

    private boolean specialCaseActive() {
        // This case checks the position of the last ball in the vertical channel
        // If greater than threshold, and LBS, then returns true.
        double currentBallSpacing = verticalEncoder.getRotations() - specialCaseVerticalStartingTicks ;
        boolean return_val = specialCaseEnabled && ( currentBallSpacing >= ( config.verticalBallQueuingSpacing * 4 ) ) ;
        return return_val ;
    }

    public ManualTestGroup createManualTests() {
        ManualTestGroup tests = super.createManualTests();
        tests.addTest(new SpeedControllerTest("horizontalRollers", horizontalRollers));
        tests.addTest(new SpeedControllerTest("verticalRollers", verticalRollers));
        tests.addTest(new SpeedControllerTest("kingRoller", kingRoller));
        tests.addTest(new DigitalSensorTest("kingEntranceSensor", kingEntranceSensor));
        tests.addTest(new DigitalSensorTest("topVerticalSensor", topVerticalSensor));
        tests.addTest(new DigitalSensorTest("lastBallSensor", lastBallSensor));
        return tests;
    }

    public void resetBallChannelStateMachine() 
    {
        if ( localDebug ){
            logger.info( "Resetting Ball Channel State Machine" ) ;
        }
        
        ballFeedingSubState_current = BallChannelFeedingSubState.INITIALIZE;
        ballFeedingSubState_next = BallChannelFeedingSubState.COLLECTING_FEED;

        // Need to reset BOTH current and next, otherwise there is a possibility that the 
        // existing curent state could introduce strange behavior
        firingSubState_current = FiringSubState.INITIALIZE;
        setFiringSubState(FiringSubState.INITIALIZE);

    }

    public void setFiringSubState(FiringSubState firingState) {
        firingSubState_next = firingState;
    }

    public List<AutomatedTest> createAutomatedTests() {
        ArrayList<AutomatedTest> tests = new ArrayList<>();
        tests.add(new MotorEncoderTest("verticalMotorEncoderTest", this::setVerticalRollersPower, this::getVerticalEncoder));
        tests.add(new MotorEncoderTest("horizontalMotorEncoderTest", this::setHorizontalRollersPower, this::getHorizontalEncoder));
        tests.add(new MotorEncoderTest("kingMotorEncoderTest", this::setKingRollerPower, this::getKingEncoder));
        return tests;
    }

    public List<TatorSparkMax> getSparkMaxes() {
        return Arrays.asList(horizontalRollers, kingRoller, verticalRollers);
    }

    public void configure(Config config) {
        this.config = config;
        this.horizontalRollers = config.horizontalRollers.create();
        this.verticalRollers = config.verticalRollers.create();
        this.kingRoller = config.kingRoller.create();
        this.kingEntranceSensor = config.kingEntranceSensor.create();
        this.topVerticalSensor = config.topVerticalSensor.create();
        this.lastBallSensor = config.lastBallSensor.create();
        this.kingEncoder = kingRoller.getNeoEncoder();
        this.verticalEncoder = verticalRollers.getNeoEncoder();
        this.horizontalEncoder = horizontalRollers.getNeoEncoder();
        this.verticalController = new SparkMaxPIDController(verticalRollers, "verticalRollerController");
        verticalController.configure(config.verticalController);

        this.verticalRollerPower = config.verticalRollerPower;
        this.horizontalRollerPower = config.horizontalRollerPower;
        this.kingRollerPower = config.kingRollerPower;
        this.verticalkV = config.verticalkV;

        this.spitVerticalPower = config.spitVerticalPower;
        this.spitHorizontalPower = config.spitHorizontalPower;
        this.spitKingPower = config.spitKingPower;

        this.verticalRollerQueuingPower = config.verticalRollerQueuingPower;
        this.horizontalRollerQueuingPower = config.horizontalRollerQueuingPower;
        this.kingRollerQueuingPower = config.kingRollerQueuingPower;

        localDebug = config.debug;
    }

    public static class Config {
        public SparkMaxConfig horizontalRollers;
        public SparkMaxConfig verticalRollers;
        public SparkMaxConfig kingRoller;
        public DigitalSensorConfig kingEntranceSensor;
        public DigitalSensorConfig topVerticalSensor;
        public DigitalSensorConfig lastBallSensor;
        public SparkMaxPIDController.Config verticalController;

        public double verticalKingRatio;

        public double kingRollerPower;
        public double verticalRollerPower;
        public double horizontalRollerPower;

        public double spitVerticalPower;
        public double spitHorizontalPower;
        public double spitKingPower;

        public double kingRollerQueuingPower;
        public double verticalRollerQueuingPower;
        public double horizontalRollerQueuingPower;
        public double verticalBallQueuingSpacing;

        public double verticalkV;
        public double shootingWaitTime;
        public boolean debug;
    }
}
