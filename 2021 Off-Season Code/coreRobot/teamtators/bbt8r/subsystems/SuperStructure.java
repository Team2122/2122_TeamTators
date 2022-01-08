package org.teamtators.bbt8r.subsystems;

import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import org.teamtators.common.scheduler.Subsystem;

public class SuperStructure extends Subsystem {

    // Variables: Parts of SuperStructure
    private BallChannel ballChannel;
    private GroundPicker picker;
    private Turret turret;
    private Climber climber;


    public SuperStructureState currentSuperStructureState;
    private Config config;
    private long defaultTimeout;
    private boolean localDebug = false;

    // Variables for the waiting-to-switch feature
    private boolean waitingToSwitch;
    private SuperStructureState stateToSwitchTo;
    private long timeoutStartTime;
    private long currentTimeout;
    public NetworkTableEntry pickerState;
    public NetworkTableEntry superStructureState;

    private final String tableKey = "stateTable"; // stateTable
    private final String pickerKey = "statePicker"; // pickerState
    private final String superStructureKey = "stateStructure"; // structureState

    public enum SuperStructureState {
        PICKING, // Picking
        SHOOTING, // Shooting
        IDLING, // Nothing Special - Default State
        CLEARING, // Get Balls Out As Fast As Possible
        SPITTING, // Get Balls Out Through Turret
        PICKING_AND_SHOOTING // For Picking and Shooting
    }

    public SuperStructure(BallChannel ballChannel, GroundPicker picker, Turret turret, Climber climber) {
        super("SuperStructure");
        this.ballChannel = ballChannel;
        this.picker = picker;
        this.turret = turret;
        this.climber = climber;
        this.currentSuperStructureState = SuperStructureState.IDLING;

        // Get network table entries to update Shuffleboard
        pickerState = NetworkTableInstance.getDefault().getTable(tableKey).getEntry(pickerKey);
        superStructureState = NetworkTableInstance.getDefault().getTable(tableKey).getEntry(superStructureKey);
        
        // Update the values on the Shuffleboard
        pickerState.setString("" + getCurrentSuperStructureState());
        superStructureState.setString("" + picker.getPickerAction());
    }

    public boolean readyToSwitchState() 
    { 
        // Returns false if we are in a not-okay state to switch, returns true if it
        // is okay to switch
        // NOT YET IMPLEMENTED!

        switch (currentSuperStructureState) 
        {
            case IDLING:
                break;
            case PICKING:
                break;
            case CLEARING:
                break;
            case SHOOTING:
                break;
            case SPITTING:
                break;
            case PICKING_AND_SHOOTING:
                break;
        }

        return true;
    }

    public boolean enterSuperStructureState(SuperStructureState newSuperStructureState) 
    {
        // Must define state of every motor in SuperStructure for each state

        if (!readyToSwitchState()) 
        { 
            // Do not switch states if it is not safe
            if ( localDebug )
                logger.warn("You may not switch states at this time!");
            return false;
        }
        
        if ( localDebug )
            logger.info("Current SuperStructure State: " + newSuperStructureState);

        // Go into whatever state is necessary
        switch (newSuperStructureState) 
        {
            case IDLING:
                turret.setTurretState(Turret.TurretState.IDLING);
                picker.setPickerAction(GroundPicker.PickerAction.IDLING);
                break;
            case PICKING:
                turret.setTurretState(Turret.TurretState.IDLING);
                picker.setPickerAction(GroundPicker.PickerAction.PICKING);
                break;
            case CLEARING:
                turret.setTurretState(Turret.TurretState.IDLING);
                picker.setPickerAction(GroundPicker.PickerAction.CLEARING);
                break;
            case SPITTING:
                turret.setTurretState(Turret.TurretState.SHOOTING, turret.getConfig().spittingSpeed);
                picker.setPickerAction(GroundPicker.PickerAction.IDLING);
                break;
            case SHOOTING:
                ballChannel.setFirstBalltrue();
                turret.setTurretState(Turret.TurretState.SHOOTING);
                picker.setPickerAction(GroundPicker.PickerAction.IDLING);
            break;
        }

        waitingToSwitch = false;
        currentSuperStructureState = newSuperStructureState;

        return true;
    }

    public boolean enterSuperStructureState(SuperStructureState state, long timeout) 
    { 
        // Attempts to enter a state but waits to enter if switching is
        // not currently possible using the given timeout

        waitingToSwitch = true;
        stateToSwitchTo = state;
        currentTimeout = timeout;
        timeoutStartTime = System.currentTimeMillis();
        return enterSuperStructureState(state);
    }

    public boolean enterStateDefaultTimeout(SuperStructureState state) 
    { 
        // Attempts to enter a state but waits to enter
        // if switching is not currently possible using
        // the default timeout
        return enterSuperStructureState(state, config.defaultTimeout);
    }

    // todo: decide on default behavior for entering and exiting states, should we
    // wait and what timeout status?

    /**
     * Used mainly to exit from PICKING, SHOOTING and PICKING_AND_SHOOTING states
     *
     * @param state The state that you want to exit from, not the state you want to
     *              enter into
     * @return If the state could be successfully exited;
     */

    public boolean exitSuperStructureState(SuperStructureState state) 
    { 
        // Used to exit states, must be called instead
        // of switching to IDLING
        if ( localDebug )
            logger.info("Exiting State: " + state);
        
        // Check to see if the state you are trying to switch out of is the state you
        // are in
        if (currentSuperStructureState == SuperStructureState.PICKING_AND_SHOOTING
                && state != SuperStructureState.PICKING && state != SuperStructureState.SHOOTING) 
        {
            if ( localDebug )
                logger.warn("You are trying to exit a state you are not in!");
            return false;
        } 
        else if (currentSuperStructureState != SuperStructureState.PICKING_AND_SHOOTING
                && currentSuperStructureState != state) 
        {
            if ( localDebug )
                logger.warn("You are trying to exit a state you are not in!");
            return false;
        }

        // Standard exit state process -> Enter IDLING
        if (currentSuperStructureState != SuperStructureState.PICKING_AND_SHOOTING) 
        {
            return enterSuperStructureState(SuperStructureState.IDLING);
        }

        // Switch out of PICKING_AND_SHOOTING into the correct state
        if (state == SuperStructureState.PICKING) 
        {
            return enterSuperStructureState(SuperStructureState.SHOOTING);
        }
        else 
        {
            if (localDebug)
                logger.info( "Picking and Shooting Condition");
            return enterSuperStructureState(SuperStructureState.PICKING);
        }
    }

    public boolean isWaitingToSwitch() 
    { 
        // Returns true if the superStructure is waiting to switch

        if (waitingToSwitch) 
        {
            if (currentTimeout < 0) 
            {
                return true;
            }
            
            waitingToSwitch = ( System.currentTimeMillis() - timeoutStartTime ) > currentTimeout;
        }

        return waitingToSwitch;
    }

    public SuperStructureState getSuperStructureStateToSwitchTo() 
    { 
        // returns the state that the superStructure is
        // waiting to switch to
        
        if (isWaitingToSwitch()) 
        {
            return stateToSwitchTo;
        } 
        else 
        {
            return currentSuperStructureState;
        }
    }

    public SuperStructureState getCurrentSuperStructureState() 
    { 
        // returns the current state
        return currentSuperStructureState;
    }

    /**
     * @param rotating whether or not the turret will able to rotate
     * @param override whether or not to put the superStructure in the correct state
     *                 if we are not in a turret-rotation state
     * @return whether or not the turret will start rotating
     */

    public boolean setTurretRotating(boolean rotating, boolean override) 
    {
        if (!rotating) 
        { 
            // It is always okay for us to move out of ROTATING
            if (turret.getCurrentTurretState() == Turret.TurretState.ROTATING) 
            { 
                // but we only want to do so if we are
                // in the ROTATING state
                turret.setTurretState(Turret.TurretState.IDLING);
            }
            return true;
        }

        boolean okay = false;

        switch (currentSuperStructureState)
        { 
            // Depending on the state it may or may not be okay to rotate the turret
            case IDLING:
            case PICKING:
            case CLEARING:
                okay = true;
                break;
            case SHOOTING:
            case SPITTING:
            case PICKING_AND_SHOOTING:
                okay = false;
                break;
        }

        if (okay) 
        { 
            // If we are all good begin rotating the turret
            turret.updateBasePoint();
            turret.setTurretState(Turret.TurretState.ROTATING);
        } 
        else 
        {
            if (override) 
            { 
                // If we are wanting to override move into the IDLING 
                // state and begin rotating the turret

                enterSuperStructureState(SuperStructureState.IDLING);
                turret.updateBasePoint();
                turret.setTurretState(Turret.TurretState.ROTATING);
            }
        }
        return okay;
    }

    public void printSuperStructureState() 
    {
        if ( localDebug )
            logger.info("Current SuperStructure State: " + currentSuperStructureState);
    }

    public void configure(Config config) 
    {
        this.config = config;
        this.defaultTimeout = config.defaultTimeout;
        localDebug = config.debug;
    }

    public static class Config 
    {
        public boolean debug;

        // Do not put motor powers in here, those will be moved to the configs of the
        // individual subsystems

        public long defaultTimeout;      // For switching SuperStructure states
    }

}
