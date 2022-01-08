package org.teamtators.bbt8r.continuous_commands;

import org.teamtators.bbt8r.MotorMonitor;
import org.teamtators.bbt8r.TatorRobot;
import org.teamtators.bbt8r.subsystems.*;
import org.teamtators.common.control.Timer;
import org.teamtators.common.scheduler.Command;

public class SuperStructureContinuous extends Command {

    private SuperStructure superStructure;
    private BallChannel ballChannel;
    private GroundPicker picker;
    private Turret turret;

    private Config config;
    private Vision vision;
    private boolean isExtended = false;
    private boolean isExtendedForClearing = false;
    private Timer timer;
    private double waitToShoot = 5.0;
    private boolean firing = false; // Whether a ball is being fired out or not
    private boolean localDebug = false;

    private MotorMonitor pickFlipMonitor;

    public SuperStructureContinuous(TatorRobot robot) {
        super("SuperStructureContinuous");
        superStructure = robot.getSubsystems().getSuperStructure();
        ballChannel = robot.getSubsystems().getBallChannel();
        picker = robot.getSubsystems().getGroundPicker();
        turret = robot.getSubsystems().getTurret();
        vision = robot.getSubsystems().getVision();

        // Because this is used later in the action commands, need to initalize the member correctly
        ballChannel.setRobotSubSystemPointers( superStructure, turret ) ;

        pickFlipMonitor = new MotorMonitor(picker.flipMotor, 50, .2, 1);

        timer = new Timer();
    }

    public void configure (Config config) {
        this.config = config;
        localDebug = config.debug;
    }

    public boolean step() {
        if (localDebug) {
            logger.info("Superstructure state: " + superStructure.getCurrentSuperStructureState());
        }

        if (superStructure.isWaitingToSwitch()) { // Wait to switch feature
            superStructure.enterSuperStructureState(superStructure.getSuperStructureStateToSwitchTo());
            if (localDebug) {
                logger.info("Waiting to Switch: " + superStructure.getSuperStructureStateToSwitchTo());
            }
        }

        // Updates Network Tables To View State Of Robot on ShuffleBoard
        superStructure.pickerState.setString("" + superStructure.getCurrentSuperStructureState());
        superStructure.superStructureState.setString("" + picker.getPickerAction());

        // Every iteration, call each of these to update the counters, etc.
        tickBallChannel();
        tickPicker();
        tickTurret();
        tickVision();

        return false;
    }

    // Exit the class
    public void finish(boolean interrupted) {
        super.finish(interrupted, localDebug);
    }

    // -------------------------------------------------------------------------------------------
    // This code needs rework, roller powers are being set every itteration of the
    // code
    // -------------------------------------------------------------------------------------------

    public void tickBallChannel() 
    {
        // Update the ball channel
        ballChannel.updateBallChannelState();
    }


    // -------------------------------------------------------------------------------------------
    // This code needs rework, roller motors are being set every iteration of the
    // code
    // -------------------------------------------------------------------------------------------

    public void tickPicker() 
    {
        // Update the picker
        picker.updatePickerState();
    }

    // -------------------------------------------------------------------------------------------
    // This code needs rework, why is this code not in turret.java
    // -------------------------------------------------------------------------------------------

    public void tickTurret() {
        // Update the turret
        turret.updateTurretState();
    }

    // -------------------------------------------------------------------------------------------
    // -------------------------------------------------------------------------------------------

    public void tickVision() {
        // Get current turret angle
        vision.sendData(turret.getTurretAngle());

        // Check the status of the Raspberry Pi
        vision.checkPiStatus();
    }

    public static class Config {
        public boolean debug;
    }
}
