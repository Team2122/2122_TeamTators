// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import java.io.File;
import java.util.ArrayList;

import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.system.plant.DCMotor;

import org.littletonrobotics.junction.LogFileUtil;
import org.littletonrobotics.junction.LoggedRobot;
import org.littletonrobotics.junction.Logger;
import org.littletonrobotics.junction.networktables.NT4Publisher;
import org.littletonrobotics.junction.wpilog.WPILOGReader;
import org.littletonrobotics.junction.wpilog.WPILOGWriter;

import org.teamtators.Util.*;
import org.teamtators.tester.ManualTester;

import com.pathplanner.lib.auto.AutoBuilder;
import com.pathplanner.lib.config.ModuleConfig;
import com.pathplanner.lib.config.PIDConstants;
import com.pathplanner.lib.config.RobotConfig;
import com.pathplanner.lib.controllers.PPHolonomicDriveController;
import com.pathplanner.lib.path.PathPlannerPath;

import edu.wpi.first.math.Pair;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.net.PortForwarder;
import edu.wpi.first.units.measure.MomentOfInertia;

import static edu.wpi.first.units.Units.*;
import edu.wpi.first.wpilibj.PowerDistribution;
import edu.wpi.first.wpilibj.RobotBase;
import edu.wpi.first.wpilibj.DriverStation.Alliance;
import edu.wpi.first.wpilibj.GenericHID.RumbleType;
import edu.wpi.first.wpilibj.PowerDistribution.ModuleType;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.button.RobotModeTriggers;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import edu.wpi.first.wpilibj2.command.button.Trigger;
import frc.robot.commands.TatorCommands;
import frc.robot.commands.AutoRoutines;
import frc.robot.commands.DriveToPoseTarget;
import frc.robot.commands.AutoRoutines.*;
import frc.robot.constants.GeneralConstants;
import frc.robot.subsystems.kingRollers.KingRollers;
import frc.robot.subsystems.operatorInterface.OperatorInterface;
import frc.robot.subsystems.picker.Picker;
import frc.robot.subsystems.picker.Picker.PickerStates;
import frc.robot.subsystems.swerve.SwerveDrive;
import frc.robot.subsystems.swerve.SwerveConstants.SwerveModuleConstants;
import frc.robot.subsystems.Blinkin;
import frc.robot.subsystems.climber.Climber;
import frc.robot.subsystems.upperNotePath.UpperNotePath;
import frc.robot.subsystems.upperNotePath.UpperNotePath.ShotType;
import frc.robot.subsystems.upperNotePath.UpperNotePath.UpperNotePathStates;
import frc.robot.subsystems.vision.Vision;
import frc.robot.subsystems.pivot.Pivot;
import frc.robot.subsystems.pivot.PivotConstants.PivotPositions;
import frc.robot.util.AimUtil;
import frc.robot.util.AimUtil.AimTargets;

public class Robot extends LoggedRobot {
    public enum RobotControlMode {
        Teleop,
        Autonomous,
        Disabled,
        Test
    }
    private RobotControlMode currentControlMode
        = RobotControlMode.Disabled;
    private RobotControlMode newControlMode
        = RobotControlMode.Disabled;

    private Command autonomousCommand;
    private SendableChooser<Command> autoChooser;

    private CommandXboxController driverController;
    private CommandXboxController gunnerController;

    private SwerveDrive swerve;
    private OperatorInterface operatorInterface;
    private Picker picker;
    private KingRollers kingRollers;
    private Climber climber;
    private UpperNotePath upperNotePath;
    private Pivot pivot;
    private Blinkin blinkin;

    private Vision vision;

    // stuff for the testing library
    private ArrayList<RobotStateListener> stateListeners;
    private ManualTester manualTester;

    private static Robot instance;

    public Robot() {
        instance = this;
        CommandScheduler.getInstance().enable();

        DriverStation.silenceJoystickConnectionWarning(true);

        configureAdvantageKit();

        SmartDashboard.setDefaultBoolean(GeneralConstants.kNTRobotStatusKey, true);

        driverController = new CommandXboxController(GeneralConstants.kDriverPort);
        gunnerController = new CommandXboxController(GeneralConstants.kGunnerPort);

        stateListeners = new ArrayList<>();

        kingRollers = new KingRollers();
        picker = new Picker();
        climber = new Climber();
        swerve = new SwerveDrive();
        operatorInterface = new OperatorInterface(
            driverController,
            swerve);
        swerve.setDefaultCommand(swerve.drive(operatorInterface));
        upperNotePath = new UpperNotePath();
        pivot = new Pivot();

        vision = new Vision();

        TatorCommands.prepare(
            picker,
            kingRollers,
            upperNotePath,
            pivot,
            swerve,
            climber,
            operatorInterface
        );

        blinkin = new Blinkin();

        manualTester = new ManualTester(operatorInterface);

        configureDriverBindings();
        configureGunnerBindings();

        for(Subsystem subsystem : Subsystem.getSubsystemList()) {
            subsystem.configure();
        }

        // sets up port forwarding so we can connect to the limelight embedded
        // web server over USB at the IP address 172.22.11.2:5<ip><last digit of port>
        // the normal IP address over the radio is 10.21.22.<ip>:<port> for reference
        // so 10.21.22.13:5801 would become 172.22.11.2:5131 over USB
        for (int ip = 11; ip <= 14; ip++) {
            for (int port = 5800; port <= 5807; port++) {
                PortForwarder.add(5000 + ip*10 + (port % 10), "10.21.22." + ip, port);
            }
        }

        // does the same as the above but for tatorvision instead of limelight
        PortForwarder.add(5150, "10.21.22.15", 80);

        /* AUTO INITIALIZATION */
        AutoRoutines.prepare(
            swerve,
            picker,
            upperNotePath,
            kingRollers
        );
        autoChooser = new SendableChooser<>();

        RobotConfig robotConfig = new RobotConfig(
            Kilograms.of(49.39198466872796), // robot mass
            KilogramSquareMeters.of(4.278), // robot moi
            new ModuleConfig(
                Meters.of(0.0485), // wheel radius
                MetersPerSecond.of(4.5), // max bot speed
                1.0, // Coefficient of friction
                DCMotor.getKrakenX60(1), // drive motor
                SwerveModuleConstants.driveZoomStatorLimit, // current limit on drive motor
                1
            ),
            Meters.of(0.5196405393941087), // trackwidth
            Meters.of(0.4561405736840902) // wheelbase
        );
        
        AutoBuilder.configure(
            swerve::getPose, // Robot pose supplier
            //(x) -> poseEstimator.reset(x), // Method to reset odometry (will be called if your auto has a starting pose)
            x -> {},
            swerve::getChassisSpeeds, // ChassisSpeeds supplier. MUST BE ROBOT RELATIVE
            swerve, // Method that will drive the robot given ROBOT RELATIVE ChassisSpeeds
            new PPHolonomicDriveController( // HolonomicPathFollowerConfig, this should likely live in your Constants class
                new PIDConstants(1.5, 0.0, 0), // Translation PID Constants
                new PIDConstants(1.2, 0.0, 0.0) // Rotation PID Constants
            ),
            robotConfig,
            () -> getAlliance().equals(Alliance.Red),
            swerve // Reference to this subsystem to set requirements
        );

        // 2025 autos
        try {
            autoChooser.setDefaultOption("Close Three", AutoRoutines.closeThree());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // beach blitz autos
        // autoChooser.addOption("Beach Blitz M4", AutoRoutines
        //     .beachBlitzAuto(4, BranchDirection.DOWN, "source to M4", 5));
        // autoChooser.addOption("Beach Blitz M5", AutoRoutines
        //     .beachBlitzAuto(5, BranchDirection.UP, "source to M5", 4));
        // autoChooser.setDefaultOption("Beach Blitz M2", AutoRoutines
        //     .beachBlitzAuto(2, BranchDirection.UP, "amp to M2", 1));
        // autoChooser.addOption("Beach Blitz M1", AutoRoutines
        //     .beachBlitzAuto(1, BranchDirection.DOWN, "amp to M1", 2));

        // testing autos
        // try {
        //     autoChooser.addOption("15 feet", AutoBuilder.followPath(PathPlannerPath.fromChoreoTrajectory("15 feet")));
        //     autoChooser.addOption("10 feet", AutoBuilder.followPath(PathPlannerPath.fromChoreoTrajectory("10 feet")));
        //     autoChooser.addOption("5 feet", AutoBuilder.followPath(PathPlannerPath.fromChoreoTrajectory("5 feet")));
        //     autoChooser.addOption("10 feet 5 feet", AutoBuilder.followPath(PathPlannerPath.fromChoreoTrajectory("10 feet 5 feet")));
        //     autoChooser.addOption("5 feet 5 feet 5 feet", AutoBuilder.followPath(PathPlannerPath.fromChoreoTrajectory("5 feet 5 feet 5 feet")));

        //     // simple autos
        //     autoChooser.addOption("Look Pretty", AutoRoutines.lookPretty());
        //     autoChooser.addOption("Simple Close", AutoRoutines.simpleClose());
        // } catch (Exception e) {
        //     e.printStackTrace();
        // }

        SmartDashboard.putData(autoChooser);

        RobotModeTriggers.teleop()
            .onTrue(AimUtil.stopAim());
    }

    @SuppressWarnings({"resource"})
    private void configureAdvantageKit() {
        Logger.recordMetadata("ProjectName", BuildConstants.MAVEN_NAME);
        Logger.recordMetadata("BuildDate", BuildConstants.BUILD_DATE);
        Logger.recordMetadata("GitSha", BuildConstants.GIT_SHA);
        Logger.recordMetadata("GitDate", BuildConstants.GIT_DATE);
        Logger.recordMetadata("GitBranch", BuildConstants.GIT_BRANCH);
    
        switch (BuildConstants.DIRTY) {
            case 0:
                Logger.recordMetadata("GitDirty", "All changes committed");
                break;
            case 1:
                Logger.recordMetadata("GitDirty", "Uncomitted changes");
                break;
            default:
                Logger.recordMetadata("GitDirty", "Unknown");
                break;
        }
    
        if(!GeneralConstants.kReplay) {
            File logDir = new File(RobotBase.isReal() ? "/home/lvuser/logs" : "logs");
            if(!logDir.exists()) {
                logDir.mkdirs();
            }
    
            Logger.addDataReceiver(new WPILOGWriter(logDir.toString()));
            Logger.addDataReceiver(new NT4Publisher());
            new PowerDistribution(1, ModuleType.kRev); // Enables power distribution logging
        } else { // replaying logged data
            setUseTiming(false); // disable 20ms loop, process the replay as fast as possible
    
            String logPath = LogFileUtil.findReplayLog();
            Logger.setReplaySource(new WPILOGReader(logPath));
            Logger.addDataReceiver(new WPILOGWriter(LogFileUtil.addPathSuffix(logPath, "_replay")));
        }
            
        Logger.start();
    }

    private void configureDriverBindings() {
        var driveTo = new DriveToPoseTarget(DriveToPoseTarget.Target.TRAP, swerve);
        driverController.leftBumper().onTrue(
            Commands.runOnce(driveTo::initialize).andThen(
                swerve.drive(driveTo).until(driveTo::isFinished)
            )
        );

        driverController.povLeft()
            .onTrue(Commands.runOnce(() -> swerve.gyroZero()));

        Trigger shotReady = upperNotePath.shooting
            .and(() -> upperNotePath.upToSpeed()
                && pivot.inPosition()
                && swerve.onTarget())
            .and(RobotModeTriggers.teleop());
        shotReady
            .onTrue(Commands.runOnce(() -> driverController.getHID().setRumble(RumbleType.kBothRumble, 1)))
            .onFalse(Commands.runOnce(() -> driverController.getHID().setRumble(RumbleType.kBothRumble, 0)));

        new Trigger(() -> AimUtil.getTarget() == AimTargets.NONE)
            .and(RobotModeTriggers.teleop())
            .onTrue(TatorCommands.stopAiming());
        new Trigger(() -> AimUtil.getTarget() != AimTargets.NONE)
            .and(RobotModeTriggers.teleop())
            // must be aiming if not none
            .onTrue(TatorCommands.specialAim());

        driverController.leftTrigger()
            .onTrue(TatorCommands.pick()
                .andThen(Commands.waitUntil(() -> picker.getState() == PickerStates.IDLE))
                .repeatedly())
            .onFalse(TatorCommands.stopPicking());

        driverController.back()
            .whileTrue(TatorCommands.ejectUpper())
            .onFalse(TatorCommands.fullReset());
        driverController.start()
            .whileTrue(TatorCommands.ejectLower())
            .onFalse(TatorCommands.fullReset());

        // SHOOTING/AIMING CONTROLS
        driverController.x().and(upperNotePath.startable)
            .onTrue(AimUtil.speakerAim());
        driverController.x()
            .onFalse(AimUtil.stopAim());

        driverController.y().and(upperNotePath.startable)
            .onTrue(AimUtil.lobAim());
        driverController.y()
            .onFalse(AimUtil.stopAim());

        driverController.b().and(upperNotePath.startable)
            .onTrue(TatorCommands.prepStaticShot())
            .onFalse(TatorCommands.stopStaticShot());

        driverController.rightTrigger()
            .and(driverController.a().negate())
            .and(upperNotePath.shooting)
            .onTrue(TatorCommands.takeShot());

        // AMP CONTROLS
        driverController.a().and(driverController.rightTrigger())
            .and(upperNotePath.startable)
            .onTrue(TatorCommands.startAmp());

        driverController.rightBumper().and(upperNotePath.doneWithStartAmp)
            .onTrue(TatorCommands.endAmp());
    }

    private void configureGunnerBindings() {
        // gunnerController.b().and(upperNotePath.startable)
        //     .onTrue(upperNotePath.startAmp()
        //         .alongWith(pivot.goTo(PivotPositions.AMP))
        //         .alongWith(Commands.runOnce(() -> AimUtil.setTarget(AimTargets.NONE))));

        // gunnerController.x().and(upperNotePath.doneWithStartAmp)
        //     .onTrue(upperNotePath.endAmp()
        //         .andThen(pivot.goTo(PivotPositions.HOME)));

        // trap steps:
        // 1. flip climbers up
        // 2. flip pivot up & startTrap upperNotePath command
        // 3. stow the note, then extend. Parallel to climbing up
        // 4. drop note

        Command stepOne = climber.goTo(Climber.Position.PRE_CLIMB)
            // .alongWith(upperNotePath.startTrap())
            .withName("TrapOne");

        Command stepFour = climber.goTo(Climber.Position.CLIMB)
            .withName("TrapFour");
        
        var climbSafety = gunnerController.leftTrigger().and(gunnerController.rightTrigger())
            .and(gunnerController.leftBumper()).and(gunnerController.rightBumper());
        gunnerController.a().and(climbSafety).onTrue(stepOne);
        gunnerController.b().and(climbSafety).onTrue(stepFour);

        // gunnerController.a().and(upperNotePath.startable)
        //     .onTrue(stepOne);

        // gunnerController.b().and(upperNotePath.doneWithStartTrap)
        //     .onTrue(stepTwo.andThen(stepThree));

        // gunnerController.x()
        //     .onTrue(stepFour);

        // gunnerController.y().and(upperNotePath.doneWithMidTrap)
        //     .onTrue(stepFive);

        gunnerController.start()
            .onTrue(Commands.runOnce(blinkin::recover));
    }

    @Override
    public void robotPeriodic() {
        CommandScheduler.getInstance().run();

        for (Subsystem subsystem : Subsystem.getSubsystemList()) {
            subsystem.log();
        }
        DeviceHealthManager.logHealth();

        Logger.recordOutput("AimUtil/AimTarget", AimUtil.getTarget());
        // var opt = AimUtil.getDistanceFrom(/* AimUtil.getTarget() */ AimTargets.AMP);
        // if (opt.isPresent()) {
        //     Logger.recordOutput("AimUtil/TargetDistance", opt.get());
        // } else {
        //     Logger.recordOutput("AimUtil/TargetDistance", -1.0);
        // }
        //QuickDebug.output("Tuning/Limelight Distance", poseEstimator.getLimelightAimingDistance());
    }

    @Override
    public void disabledInit() {
        CommandScheduler.getInstance().enable();
        if (currentControlMode != RobotControlMode.Disabled) {
            DeviceHealthManager.printHealth(currentControlMode);
        }
        updateStateListeners(RobotControlMode.Disabled);
    }

    @Override
    public void disabledPeriodic() {}

    @Override
    public void disabledExit() {}

    @Override
    public void autonomousInit() {
        CommandScheduler.getInstance().enable();
        updateStateListeners(RobotControlMode.Autonomous);

        Pose2d backwards = new Pose2d(
            new Translation2d(),
            Rotation2d.fromDegrees((Robot.getAlliance() == Alliance.Red) ? 0 : 180)
        );

        autonomousCommand = Commands.runOnce(() -> swerve.resetGyroFrom(backwards))
            .andThen(autoChooser.getSelected());
        //autonomousCommand = swerve.drive(() -> new ChassisSpeeds(0.01, 0, 0));
            //.withTimeout(5);
        if (autonomousCommand != null) {
            autonomousCommand.schedule();
        }
    }

    @Override
    public void autonomousPeriodic() {
        AutoRoutines.eventLoop.poll();
    }

    @Override
    public void autonomousExit() {}

    @Override
    public void teleopInit() {
        CommandScheduler.getInstance().enable();
        updateStateListeners(RobotControlMode.Teleop);

        if (autonomousCommand != null) {
            autonomousCommand.cancel();
        }
    }

    @Override
    public void teleopPeriodic() {}

    @Override
    public void teleopExit() {}

    @Override
    public void testInit() {
        CommandScheduler.getInstance().cancelAll();
        CommandScheduler.getInstance().disable();
        updateStateListeners(RobotControlMode.Test);
        configureTests();
        manualTester.initialize();
    }

    @Override
    public void testPeriodic() {
        manualTester.execute();
    }

    @Override
    public void testExit() {}

    public static Robot getInstance() {
        return instance;
    }

    /* Subsystem Getters */

    public static KingRollers getKingRollers() {
        return instance.kingRollers;
    }

    public static Picker getPicker() {
        return instance.picker;
    }

    public static Climber getClimber() {
        return instance.climber;
    }

    public static OperatorInterface getOperatorInterface() {
        return instance.operatorInterface;
    }

    public static Pivot getPivot() {
        return instance.pivot;
    }

    public static UpperNotePath getUpperNotePath() {
        return instance.upperNotePath;
    }

    public static SwerveDrive getSwerve() {
        return instance.swerve;
    }

    public static Blinkin getBlinkin() {
        return instance.blinkin;
    }

    public static Vision getVision() {
        return instance.vision;
    }

    public static Alliance getAlliance() {
        return DriverStation.getAlliance()
            .orElse(Alliance.Blue);
    }

    // stuff needed for our testing software
    public void registerStateListener(RobotStateListener stateListener) {
        stateListeners.add(stateListener);
    }

    public void unregisterStateListener(RobotStateListener stateListener) {
        stateListeners.remove(stateListener);
    }
    public void updateStateListeners(RobotControlMode controlMode) {
        newControlMode = controlMode;
        if (currentControlMode != newControlMode) {
            if (currentControlMode == RobotControlMode.Test) {
                manualTester.end(true);
            }
            currentControlMode = newControlMode;
        }
        for (int i = 0; i < stateListeners.size(); i++) {
            stateListeners.get(i).onEnterRobotState(controlMode);
        }
    }
    
    protected void configureTests() {
        System.out.println("Configuring tests");
        manualTester.clearTestGroups();
        manualTester.setController(new XBOXController(GeneralConstants.kDriverPort));

        for (Subsystem subsystem : Subsystem.getSubsystemList()) {
            manualTester.registerTestGroup(subsystem.createManualTests());
            System.out.println(subsystem.getName());
        }
    }    
}
