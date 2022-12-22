package org.teamtators.sassitator;

import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Robot;
import frc.robot.Robot.EnumRobotState;
import frc.robot.Robot;
import frc.robot.RobotContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamtators.Tools.tester.ManualTestGroup;
import org.teamtators.Tools.tester.ManualTestable;
import org.teamtators.logging.vcd.VCDScope;

import java.util.Arrays;
import java.util.EnumSet;

public abstract class Subsystem extends SubsystemBase implements Debuggable, ManualTestable, RobotStateListener{

    protected final Logger logger;
    //protected final VCDModule module;
    private boolean debug;
    private final EnumSet<EnumRobotState> validStates;
    private EnumRobotState robotState = EnumRobotState.Disabled;
    protected final ShuffleboardRegister shuffleboardRegister;
    public VCDScope scope;

    public Subsystem(RobotContainer robotContainer) {
        setName(getClass().getSimpleName());
        logger = LoggerFactory.getLogger(getName());
        robotContainer.getSubsystems().add(this);
        robotContainer.getDebuggableRegistry().registerDebuggable(this);
        validStates = EnumSet.of(EnumRobotState.Teleop, EnumRobotState.Autonomous);
        shuffleboardRegister = new ShuffleboardRegister(getClass().getSimpleName());
        Robot.getInstance().registerStateListener(this);
        logger.info("Created");
        scope = new VCDScope(getClass().getSimpleName());
        Robot.getInstance().registerStateListener(this);
    }

    @Override
    public void onEnterRobotState(EnumRobotState state) {
                
    }

    @Override
    public final void periodic() {
        shuffleboardRegister.update();
        if (validIn(robotState)) {
            doPeriodic();
        }
    }

    public void doPeriodic() {

    }

    @Override
    public ManualTestGroup createManualTests() {
        ManualTestGroup tests = new ManualTestGroup(getName());
        return tests;
    }

    public final void setValidity(EnumRobotState... validStates) {
        this.validStates.clear();
        this.validStates.addAll(Arrays.asList(validStates));
    }

    public void configure(RobotContainer robotContainer) {

    }
    
    public final boolean validIn(EnumRobotState robotState) {
        return validStates.contains(robotState);
    }

    public final void setCurrentState(EnumRobotState robotState) {
        this.robotState = robotState;
    }

    @Override
    public final void debugOn() {
        debug = true;
    }

    @Override
    public final void debugOff() {
        debug = false;
    }

    @Override
    public final void debugToggle() {
        debug = !debug;
    }

    public final boolean isDebugging() {
        return debug;
    }

    public abstract void reset();

}