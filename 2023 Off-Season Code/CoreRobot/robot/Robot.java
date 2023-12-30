package frc.robot;

import common.teamtators.RobotStateListener;
import common.teamtators.Subsystem;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.livewindow.LiveWindow;
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import frc.robot.subsystems.ArmExtension.ExtensionPosition;
import frc.robot.subsystems.ArmRotation.RotationPosition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.ArrayList;

/**
 * The VM is configured to automatically run this class, and to call the functions corresponding to
 * each mode, as described in the TimedRobot documentation. If you change the name of this class or
 * the package after creating this project, you must also update the build.gradle file in the
 * project.
 */
public class Robot extends TimedRobot {

    private RobotContainer robotContainer;

    private double autoOffset;


    private final ArrayList<RobotStateListener> stateListeners = new ArrayList<>();

    private EnumRobotState current_robotState = EnumRobotState.Disabled;
    private EnumRobotState new_robotState = EnumRobotState.Disabled;

    private final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    private static Robot robotInstance;

    public static Robot getInstance() {
        return robotInstance;
    }

    @Override
    public void simulationInit() {

    }

    @Override
    public void simulationPeriodic() {

    }

    /**
     * This function is run when the robot is first started up and should be used for any
     * initialization code.
     */
    private int autoCounterI;
    private boolean autoCounterB;

    @Override
    public void robotInit() {
        // Instantiate our RobotContainer.  This will perform all our button bindings, and put our
        // autonomous chooser on the dashboard.
        robotInstance = this;
        logger.info("Starting NetworkTable Process");

        NetworkTableInstance networkTables = NetworkTableInstance.getDefault();
        networkTables.startServer();


        networkTables.getTable(""); // Forces NetworkTables to Initialize
        networkTables.getTable("LiveWindow").getSubTable(".status").getEntry("LW Enabled").setBoolean(false);

        LiveWindow.setEnabled(false);
        Shuffleboard.disableActuatorWidgets();

        robotContainer = new RobotContainer(this);
        
        // auto chooser
        robotContainer.autoChooser();
        
        CommandScheduler.getInstance().enable();



        // I am a goofy boi
        SmartDashboard.putData("FieldPP", RobotContainer.fieldSim);

    }

    /**
     * This function is called every robot packet, no matter the mode. Use this for items like
     * diagnostics that you want ran during disabled, autonomous, teleoperated and test.
     *
     * <p>This runs after the mode specific periodic functions, but before LiveWindow and
     * SmartDashboard integrated updating.
     */
    @Override
    public void robotPeriodic() {
        // Runs the Scheduler.  This is responsible for polling buttons, adding newly-scheduled
        // commands, running already-scheduled commands, removing finished or interrupted commands,
        // and running subsystem periodic() methods.  This must be called from the robot's periodic
        // block in order for anything in the Command-based framework to work.
        CommandScheduler.getInstance().run();
        // robotContainer.poseStuff.tick();
    }


    @Override
    public void disabledInit() {
        updateStateListeners(EnumRobotState.Disabled);
        robotContainer.getTester().clearTestGroups();
    }

    @Override
    public void disabledPeriodic() {

    }



    public boolean isAutoCounterB() {
        return autoCounterB;
    }

    @Override
    public void autonomousInit() {
        // current limits for auto get set in swerve module automatically,
        // don't need to set it here

        updateStateListeners(EnumRobotState.Autonomous);

        Command autoCommand = robotContainer.getAutonomousCommand();
        // autoCommand = robotContainer.holDrive;

        autoCommand.schedule();
    }

    @Override
    public void autonomousPeriodic() {
        // RobotContainer.fieldSim.setRobotPose(RobotContainer.getInstance().traj.getState(autoCounterI).poseMeters);
        // autoCounterI++;
        robotContainer.getSwerveDrive().setStatorCurrentLimit(15);;
    }

    @Override
    public void teleopInit() {
        updateStateListeners(EnumRobotState.Teleop);
        // robotContainer.getSwerveDrive().getGyro().zero();
		robotContainer.driveCommand.schedule();
        robotContainer.getSwerveDrive().setConfigForTele();
    }

    
    @Override
    public void teleopPeriodic() {
        if (!robotContainer.driveCommand.isScheduled() && !robotContainer.rotate180.isScheduled()) {
            System.out.print("base drive rescedule");
            robotContainer.driveCommand.schedule();
        }
    }

    @Override
    public void testInit() {
        CommandScheduler.getInstance().cancelAll();
        updateStateListeners(EnumRobotState.Test);
        configureTests();
        robotContainer.getTester().initialize();
    }

    @Override
    public void testPeriodic() {
        robotContainer.getTester().execute();
    }

    public void updateStateListeners(EnumRobotState robotState) {
        new_robotState = robotState;
        if (current_robotState != new_robotState) {
            if (current_robotState == EnumRobotState.Test) {
                robotContainer.getTester().end(false);
            }
            current_robotState = new_robotState;
        }
        for (int i = 0; i < stateListeners.size(); i++) {
            stateListeners.get(i).onEnterRobotState(robotState);
        }
    }
    protected void configureTests() {
        logger.debug("Configuring tests");
        robotContainer.getTester().setController(robotContainer.getDriverController());

        for (Subsystem subsystem : robotContainer.getSubsystemList()) {
            robotContainer.getTester().registerTestGroup(subsystem.createManualTests());
            System.out.println(subsystem.getName());
        }

    }



    public void registerStateListener(RobotStateListener stateListener) {
        stateListeners.add(stateListener);
    }

    public void unregisterStateListener(RobotStateListener stateListener) {
        stateListeners.remove(stateListener);
    }

    public RobotContainer getRobotContainer() {
        return robotContainer;
    }

    public EnumRobotState getCurrent_robotState() {
        return current_robotState;
    }

    public enum EnumRobotState {
        Teleop, Autonomous, Disabled, Test
    }

    public void setAutoOffset(double autoOffset) {
        this.autoOffset = autoOffset;
    }

    public double getAutoOffset() {
        return autoOffset;
    }
}
