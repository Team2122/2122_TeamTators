package common.teamtators;

import java.util.Arrays;
import java.util.EnumSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import common.Tools.tester.ManualTestGroup;
import common.Tools.tester.ManualTestable;


import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Robot;
import frc.robot.Robot.EnumRobotState;
import frc.robot.RobotContainer;

public abstract class Subsystem extends SubsystemBase implements ManualTestable, RobotStateListener{

    protected final Logger logger;
    //protected final VCDModule module;
    private boolean debug;
    private final EnumSet<EnumRobotState> validStates;
    protected EnumRobotState robotState = EnumRobotState.Disabled;
    private boolean testSubsystem = false;
    private RobotContainer robotContainer;

    public Subsystem(RobotContainer robotContainer) {
        this.robotContainer = robotContainer;
        setName(getClass().getSimpleName());
        logger = LoggerFactory.getLogger(getName());
//        robotContainer.getSubsystems().add(this);
        validStates = EnumSet.of(EnumRobotState.Teleop, EnumRobotState.Autonomous);
        Robot.getInstance().registerStateListener(this);
        logger.info("Created");
//        CommandScheduler.getInstance().unregisterSubsystem(this);
    }

    @Override
    public void onEnterRobotState(EnumRobotState state) {
        setCurrentState(state);
        printName();
    }

    public final void testPeriodic() {
        doPeriodic();
    }

    @Override
    public final void periodic() {
        if (validIn(robotState)) {
            doPeriodic();
        }
        else if (robotState == EnumRobotState.Test) {
            if (testSubsystem == true) {
                doPeriodic();
            }

        }
    }

    public void setTestSubsystem(boolean bool) {
        testSubsystem = bool;
    }

    public void doPeriodic() {
    }

    public String printName(){
        return("unOverrided subsystem name");
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

    public EnumRobotState getRobotState() {
        return robotState;
    }

    public abstract void reset();

}