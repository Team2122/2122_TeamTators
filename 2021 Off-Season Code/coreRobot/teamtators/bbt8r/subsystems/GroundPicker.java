package org.teamtators.bbt8r.subsystems;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.can.VictorSPX;
import edu.wpi.first.wpilibj.Solenoid;
import org.teamtators.common.config.helpers.*;
import org.teamtators.common.control.Timer;
import org.teamtators.common.hw.DigitalSensor;
import org.teamtators.common.hw.NEOEncoder;
import org.teamtators.common.hw.TatorSparkMax;
import org.teamtators.common.scheduler.Subsystem;
import org.teamtators.common.tester.AutomatedTest;
import org.teamtators.common.tester.ManualTestGroup;
import org.teamtators.common.tester.automated.DigitalSensorAutoTest;
import org.teamtators.common.tester.automated.MotorEncoderTest;
import org.teamtators.common.tester.components.*;

import java.util.ArrayList;
import java.util.List;

public class GroundPicker extends Subsystem {

    private VictorSPX upperPickMotor; // the motor that has wheels to roll in ball
    private TatorSparkMax lowerPickMotor;
    public TatorSparkMax flipMotor; // motor that extends down and out, or in and up
    private NEOEncoder flipEncoder;

    private Config config;
    private DigitalSensor pickerHomeSensor;
    private boolean localDebug = false;
    private Solenoid fluffer;

    private Timer timer;

    private PickerAction pickerAction_current = PickerAction.INITIALIZE;
    private PickerAction pickerAction_new = PickerAction.IDLING;
    
    private PickerPosition pickerPosition_last = PickerPosition.INITIALIZE;
    private PickerPosition pickerPosition_current = PickerPosition.RETRACTED;

    public enum PickerAction {
        PICKING,
        WALL_EXTEND,
        CLEARING,
        TELE_CLEARING,
        SHOOTING,
        HOMING,
        FLUFFING,
        IDLING,
        INITIALIZE
        //declares picker action enum that can be used by any class
    }

    public enum PickerPosition {
        EXTENDED,
        MIDDLING,
        RETRACTED,
        INITIALIZE
        //declares picker position enum that can be used by any class
    }

    public GroundPicker() {
        super("GroundPicker");
        setPickerAction( PickerAction.IDLING );
        timer = new Timer();
        //constructs the class, sets picker state to idling, starts a timer
    }
   
    // This should be private, unless absoultely necessary.  
    private void runFluffer()
    {
        if ( localDebug ){
            // logger.info("Running Fluffer");
        }

        if (timer.isRunning()) {
            if (!fluffer.get()) {
                if (timer.hasPeriodElapsed(.5)) {
                    fluffer.set(true);
                    timer.restart();
                    return;
                    //if the timer is running, the fluffer is not extended, and .5 secs have elapsed, the fluffer
                    //will extend and the timer will restart and return nothing
                }
            } else {
                if (timer.hasPeriodElapsed(.2)) {
                    fluffer.set(false);
                    //if the timer is running and the fluffer is extended, the fluffer will retract
                }
            }
        } else {
            timer.start();
            //if the timer isn't running, the timer starts
        }
    }

    // This MUST be private, because they are state driven.  
    // If they are public we cannot gaurantee that the state was updated
    // before calling into the function.
    private void setFlipMotorExtended()
    {    
        // in PickerPick that will detect extend and retract limits
        //depending on the position of the picker, it will set the power (to extend or retract)

        // Only perform this if the state has changed
        if (pickerPosition_current != pickerPosition_last) {

            if (localDebug) {
                // logger.info("Flipmotor State Change : current" + pickerPosition_current + "    last" + pickerPosition_last);
            }            
        
            switch (pickerPosition_current) {
                case EXTENDED:
                    flipMotor.set(config.extendedLowPower);
                    if(localDebug) {
                        // logger.info("extended motor power: " + flipMotor.getOutputCurrent());
                    }
                    break;
                case MIDDLING:
                case RETRACTED:
                    flipMotor.set(config.extendedPower);
                    if(localDebug){
                        // logger.info("retracted motor power: " + flipMotor.getOutputCurrent());
                    }
                    break;
            }
        }
    }

    // This MUST be private, because they are state driven.  
    // If they are public we cannot gaurantee that the state was updated
    // before calling into the function.
    private void setFlipMotorRetracted()
    {
        // Only perform this if the state has changed
        if( pickerPosition_current != pickerPosition_last )
        {
            switch (pickerPosition_current) 
            {
                case MIDDLING:
                case EXTENDED:
                    flipMotor.set(config.retractedPower);
                    if(localDebug){
                        // logger.info("extended motor power: " + flipMotor.getOutputCurrent());
                    }
                    break;
                case RETRACTED:
                    flipMotor.set(config.retractedLowPower);
                    if(localDebug){
                        // logger.info("retracted motor power: " + flipMotor.getOutputCurrent());
                    }
                    break;
            }
        }
    }

    // These MUST be private, because it is state realted.  
    // If they are public we cannot gaurantee that the state was updated
    // before calling into the function.

    private void updatePickerPosition(double slowThresh) {
        updatePickerPosition(flipEncoder.getRotations(), slowThresh);
    }

    private void updatePickerPosition(double rotations, double slowThresh)
    {
        double highThresh = config.maxExtension - slowThresh;  // Ex : > 42 (67-25)
        double lowThresh  = 0 + slowThresh; // Ex 25

        //based on the position of the picker, the method sets and returns the state of the picker
        if ( ( rotations < highThresh ) && ( rotations > lowThresh ) ) {
            if(localDebug){
                // logger.info("Picker Middling : " + rotations);
            }
            pickerPosition_current = PickerPosition.MIDDLING;
        } else if (rotations >= highThresh ) {
            if(localDebug){
                // logger.info("Picker Extended : " + rotations);
            }
            pickerPosition_current = PickerPosition.EXTENDED;
        }else {
            if(localDebug){
                // logger.info("Picker Retracted : " + rotations);
            }
            pickerPosition_current = PickerPosition.RETRACTED;
        }

    }
    
    // This MUST be private, because it is state realted.  
    // If they are public we cannot gaurantee that the state was updated
    // before calling into the function.
    
    private void oscillatePicker(double maxOscillateDistance){
        // Depending on where the picker is we need to move the picker in a different direction

        if(flipEncoder.getRotations() >= maxOscillateDistance){ // Move Back
            //Picker has reached max distance reversing picker direction
            if (localDebug){ 
                // logger.info("rotations for oscillation max " + getFlipEncoderRotations());
            }
            flipMotor.set(-config.oscillateSpeed);

        } else if(flipEncoder.getRotations() <= config.minOscillateDistance) { // Move Forward
            //Picker has reached min distance positive picker direction (going out)
            if (localDebug){
                // logger.info("rotations for oscillation min " + getFlipEncoderRotations());
            }
            flipMotor.set(config.oscillateSpeed);
        } else {
            //In between min and max distance -- run motor
        }
    }

    // Run the picker
    public void updatePickerState() 
    { 

        // This code only gets run when there is a change in state 
        if ( pickerAction_new != pickerAction_current )
        {    
            // Sets the state of all picker motors
            if (localDebug) {
                logger.info("Picker Action : " + pickerAction_current);
                logger.info("New Picker Action : " + pickerAction_new);
            }

            pickerAction_current = pickerAction_new ;

            switch (pickerAction_current) {
                case PICKING:
                    // Fall through case
                case FLUFFING:
                    // Fall through case
                case SHOOTING:
                    // Fall through case
                case TELE_CLEARING:
                    break;
               case CLEARING:
                   // upperPickMotor.set(ControlMode.PercentOutput, config.clearUpperMotorPower);
                   //lowerPickMotor.set(config.clearLowerMotorPower);
                    break;
                case WALL_EXTEND:                    
                    upperPickMotor.set(ControlMode.PercentOutput, 0);
                    lowerPickMotor.set(0);
                    break;
                case HOMING:
                    setFlipMotor(config.retractedLowPower);
                    break;
                case IDLING:
                    upperPickMotor.set(ControlMode.PercentOutput, 0);
                    lowerPickMotor.stopMotor();
                    break;
            }
        }

        // Determine the current picker position
        if (pickerAction_current == PickerAction.CLEARING) {
            updatePickerPosition(config.slowThresh_Auto);
        }
        else {
            updatePickerPosition(config.slowThresh_Tele);
        }

        // This code only gets run when the picker position changes
        if ( pickerPosition_last != pickerPosition_current )
        {
            if (localDebug) {
                logger.info("Picker Position     : " + pickerPosition_last);
                logger.info("New Picker Position : " + pickerPosition_current);
            }
            
            switch ( pickerAction_current ) 
            {
                case WALL_EXTEND :
                    switch ( pickerPosition_current )
                    {
                        case EXTENDED:
                        case MIDDLING:
                            stopFlipMotor();
                            break;
                        case RETRACTED:
                            setFlipMotor(config.wallExtend);
                            break;
                    }
                    break;

                case PICKING:
                    switch ( pickerPosition_current )
                    {
                        case EXTENDED:
                            upperPickMotor.set(ControlMode.PercentOutput, config.upperPickMotorPower);
                            lowerPickMotor.set(config.lowerPickMotorPower);
                            break;
                    }
                    break;

                case SHOOTING:
                    switch ( pickerPosition_current )
                    {
                        case MIDDLING:
                            upperPickMotor.set(ControlMode.PercentOutput, 0);
                            lowerPickMotor.stopMotor();
                            break;
                    }
                    break;

                case TELE_CLEARING:
                    switch ( pickerPosition_current )
                    {
                        case EXTENDED:
                            upperPickMotor.set(ControlMode.PercentOutput, config.clearUpperMotorPower);
                            lowerPickMotor.set(config.clearLowerMotorPower);
                            break;
                    }
                    break;
            }
        }

        // This code gets run every iteration of the loop
        switch( pickerAction_current )
        {
            case PICKING:
                // Fall through to CLEARING
            case CLEARING:
                // Fall through to TELE_CLEARING
                // runs setFlipMotorExtended with different thresh values.
            case TELE_CLEARING:
                setFlipMotorExtended();
                break;
            case SHOOTING:
                // Fall through to FLUFFING
                oscillatePicker(config.maxOscillateDistance);
            case FLUFFING:
                // Fall through to Wall Extend
            case WALL_EXTEND:
                runFluffer();
                break;
            case HOMING:
                break;
            case IDLING:
                setFlipMotorRetracted();
                break;
        }

        // Keep track of the picker position
        pickerPosition_last = pickerPosition_current ;

    }

    public void setIdling(){
        setPickerAction(PickerAction.IDLING);
    }

    public void setPickerAction(PickerAction pickerAction) {
        // Set the new picker action, to force an update
        pickerPosition_last = PickerPosition.INITIALIZE; // DO NOT TAKE THIS OUT! Important for picker movement
        pickerAction_new = pickerAction;
    }

    public PickerAction getPickerAction(){
        return pickerAction_current;
    }

    private void setUpperPickMotor(double power) {
        if (power != 0) {
//            logger.info("[{}] Set pow {}", getName(), power);
        }
        upperPickMotor.set(ControlMode.PercentOutput, power);
    }

    private void setLowerPickMotor(double power) {
        if (power != 0) {
//            logger.info("[{}] Set pow {}", getName(), power);
        }
        lowerPickMotor.set(power);
    }

    private void setFlipMotor(double power) {
        flipMotor.set(power);
        if (power != 0) {
//            logger.info("[{}] Set pow {}", getName(), power);
        }
    }

    public void resetFlipEncoder(){
        flipEncoder.reset();
    }

    public void stopUpperPickMotor() {
        setUpperPickMotor(0);
    }
    public void stopLowerPickMotor() {
        setLowerPickMotor(0);
    }

    public void stopFlipMotor() {
        setFlipMotor(0);
    }

    public double getFlipEncoderRotations() {
        return flipEncoder.getRotations();
    }

    public boolean getPickerHomeSensor(){
        return pickerHomeSensor.get();
    }

    public double getUpperPickMotorPower() {
        return config.upperPickMotorPower;
    }

    public double getLowerPickMotorPower() {
        return config.lowerPickMotorPower;
    }

    public void printFlipMotorRotations() {
        if (localDebug) {
            logger.info("Flip NEOMotor Rotations: " + flipEncoder.getRotations());
            logger.info("Flip MotorEncoder Rotations: " + flipMotor.getEncoder().getPosition());
        }
    }

    @Override
    public ManualTestGroup createManualTests() {
        var tests = super.createManualTests();
        tests.addTest(new VictorSPXManualTest("upperPickMotor", upperPickMotor));
        tests.addTest(new SpeedControllerTest("lowerPickMotor" , lowerPickMotor));
        tests.addTest(new DigitalSensorTest("pickerHomeSensor", pickerHomeSensor));
        tests.addTest(new SpeedControllerTest("flipMotor" , flipMotor));
        tests.addTest(new NEOEncoderTest("flipMotorEncoder", flipEncoder));
        tests.addTest(new SolenoidTest("fluffer", fluffer));
        return tests;
    }

    public List<AutomatedTest> createAutomatedTests() {
        ArrayList<AutomatedTest> tests = new ArrayList<>();
        tests.add(new MotorEncoderTest("lowerPickMotorEncoderTest", this::setLowerPickMotor, lowerPickMotor.getNeoEncoder()::getRotations));
        tests.add(new MotorEncoderTest("flipMotorEncoderTest", this::setFlipMotor, this::getFlipEncoderRotations));
        tests.add(new DigitalSensorAutoTest("pickerHomeSensorTest", this::getPickerHomeSensor));
        return tests;
    }

    public void configure(Config config) {
        upperPickMotor = config.upperPickMotor.create();
        lowerPickMotor = config.lowerPickMotor.create();
        flipMotor = config.flipMotor.create();
        flipEncoder = flipMotor.getNeoEncoder();
        pickerHomeSensor = config.pickerHomeSensor.create();
        this.fluffer = config.fluffer.create();
        this.config = config;
        localDebug = config.debug;
    }

    // need to add NEOEncoderConfig to helpers
    public static class Config {
        public VictorSPXConfig upperPickMotor;
        public SparkMaxConfig lowerPickMotor;
        public SparkMaxConfig flipMotor;
        public DigitalSensorConfig pickerHomeSensor;
        public SolenoidConfig fluffer;
        public double extendedPower;
        public double extendedLowPower;
        public double retractedPower;
        public double retractedLowPower;
        public double upperPickMotorPower;
        public double lowerPickMotorPower;
        public double clearUpperMotorPower;
        public double clearLowerMotorPower;
        public double wallExtend;
        public double maxExtension;
        public double slowThresh_Tele;
        public double slowThresh_Auto;
        public double oscillateSpeed;
        public double maxOscillateDistance;
        public double minOscillateDistance;
        public boolean debug;
    }

    public enum ExtendPosition {
        EXTENDED,
        RETRACTED,
        MIDDLING
    }

}


