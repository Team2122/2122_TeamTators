// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import static edu.wpi.first.units.Units.Inches;
import static edu.wpi.first.units.Units.Meters;

import choreo.auto.AutoChooser;
import edu.wpi.first.math.filter.Debouncer.DebounceType;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Transform2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.net.PortForwarder;
import edu.wpi.first.units.measure.Distance;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.DriverStation.Alliance;
import edu.wpi.first.wpilibj.GenericHID.RumbleType;
import edu.wpi.first.wpilibj.PowerDistribution;
import edu.wpi.first.wpilibj.PowerDistribution.ModuleType;
import edu.wpi.first.wpilibj.RobotBase;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command.InterruptionBehavior;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.button.CommandGenericHID;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import edu.wpi.first.wpilibj2.command.button.RobotModeTriggers;
import edu.wpi.first.wpilibj2.command.button.Trigger;
import frc.robot.commands.AutoRoutines;
import frc.robot.commands.TatorCommands;
import frc.robot.constants.Constants;
import frc.robot.constants.Constants.RobotMedium;
import frc.robot.constants.FieldConstants;
import frc.robot.subsystems.Blinkin;
import frc.robot.subsystems.affector.Affector;
import frc.robot.subsystems.affector.Affector.AffectorStates;
import frc.robot.subsystems.chamberOfCorals.ChamberOfCorals;
import frc.robot.subsystems.climber.Climber;
import frc.robot.subsystems.climber.Climber.ClimberStates;
import frc.robot.subsystems.climber.ClimberConstants.ClimberPositions;
import frc.robot.subsystems.coralPicker.CoralPicker;
import frc.robot.subsystems.coralPicker.CoralPickerConstants.CoralPickerPositions;
import frc.robot.subsystems.operatorInterface.OperatorInterface;
import frc.robot.subsystems.overwatch.Graph.Node;
import frc.robot.subsystems.overwatch.Overwatch;
import frc.robot.subsystems.overwatch.Overwatch.RotationType;
import frc.robot.subsystems.overwatch.OverwatchConstants;
import frc.robot.subsystems.swerve.SwerveConstants;
import frc.robot.subsystems.swerve.SwerveDrive;
import frc.robot.subsystems.swerve.SwerveDrive.SwerveStates;
import frc.robot.subsystems.vision.Vision;
import frc.robot.subsystems.vision.VisionConstants;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.littletonrobotics.junction.LogFileUtil;
import org.littletonrobotics.junction.LoggedRobot;
import org.littletonrobotics.junction.Logger;
import org.littletonrobotics.junction.networktables.NT4Publisher;
import org.littletonrobotics.junction.wpilog.WPILOGReader;
import org.littletonrobotics.junction.wpilog.WPILOGWriter;
import org.teamtators.tester.ManualTestGroup;
import org.teamtators.tester.ManualTester;
import org.teamtators.util.DeviceHealthManager;
import org.teamtators.util.RobotStateListener;
import org.teamtators.util.Subsystem;
import org.teamtators.util.XBOXController;

public class Robot extends LoggedRobot {
    // testing library stuff
    public enum RobotControlMode {
        Teleop,
        Autonomous,
        Disabled,
        Test
    }

    public enum Direction {
        LEFT,
        RIGHT,
    }

    public enum AlgaeHeight {
        HIGH,
        LOW,
        ;
    }

    public enum CoralPlaceHeights {
        CORAL_L1(Node.L1PREP),
        CORAL_L2(Node.L2PREP),
        CORAL_L3(Node.L3PREP),
        CORAL_L4(Node.L4PREP),
        ;

        public final Node prepNode;

        CoralPlaceHeights(Node prepNode) {
            this.prepNode = prepNode;
        }
    }

    /**
     * Enum representing a specific branch on the coral reef. Cardinal direction is from perspective
     * of the driver. Left/right is from perspective of facing that face of the reef.
     */
    public enum CoralBranch {
        SOUTH_EAST_LEFT(0xb, 17, Direction.LEFT, AlgaeHeight.LOW),
        SOUTH_EAST_RIGHT(0xc, 17, Direction.RIGHT, AlgaeHeight.LOW),

        SOUTH_LEFT(0x9, 18, Direction.LEFT, AlgaeHeight.HIGH),
        SOUTH_RIGHT(0xa, 18, Direction.RIGHT, AlgaeHeight.HIGH),
        // SOUTH_LEFT      (0x9, 18, Direction.LEFT, AlgaeHeight.LOW),
        // SOUTH_RIGHT     (0xa, 18, Direction.RIGHT, AlgaeHeight.LOW),

        SOUTH_WEST_LEFT(0x7, 19, Direction.LEFT, AlgaeHeight.LOW),
        SOUTH_WEST_RIGHT(0x8, 19, Direction.RIGHT, AlgaeHeight.LOW),

        NORTH_WEST_LEFT(0x2, 20, Direction.LEFT, AlgaeHeight.HIGH),
        NORTH_WEST_RIGHT(0x1, 20, Direction.RIGHT, AlgaeHeight.HIGH),

        NORTH_LEFT(0x4, 21, Direction.LEFT, AlgaeHeight.LOW),
        NORTH_RIGHT(0x3, 21, Direction.RIGHT, AlgaeHeight.LOW),

        NORTH_EAST_LEFT(0x6, 22, Direction.LEFT, AlgaeHeight.HIGH),
        NORTH_EAST_RIGHT(0x5, 22, Direction.RIGHT, AlgaeHeight.HIGH),
        ;

        public final int buttonCombo;
        private final Pose2d pose; // pose for L2 and L3 placements
        private final Pose2d algaePose;
        private final Pose2d poseL4Left;
        private final Pose2d poseL4Right;
        private final Pose2d poseL2L3Left;
        private final Pose2d poseL2L3Right;
        public final Direction direction;
        public final AlgaeHeight algaeHeight;
        private final Distance shiftSide = Inches.of(6.5);
        private final Distance shiftBackCoral =
                FieldConstants.CORAL_WIDTH.plus(Constants.BOT_LENGTH.div(2));
        private final Distance shiftBackAlgae = Meters.of(0.7);
        private final Transform2d transformLeft =
                new Transform2d(shiftBackCoral, shiftSide.unaryMinus(), Rotation2d.k180deg);
        private final Transform2d transformRight =
                new Transform2d(shiftBackCoral, shiftSide, Rotation2d.k180deg);
        private final Transform2d transformAlgae =
                new Transform2d(shiftBackAlgae, Inches.zero(), Rotation2d.k180deg);

        private CoralBranch(int buttonCombo, int tagID, Direction direction, AlgaeHeight algaeHeight) {
            this.buttonCombo = buttonCombo;
            this.direction = direction;
            this.algaeHeight = algaeHeight;
            var id = VisionConstants.TAG_LAYOUT.getTagPose(tagID);
            var tagPose = id.orElse(VisionConstants.TAG_LAYOUT.getTagPose(18).get()).toPose2d();
            if (direction == Direction.LEFT) {
                this.pose = tagPose.plus(transformLeft);
            } else {
                this.pose = tagPose.plus(transformRight);
            }
            this.poseL4Left =
                    pose.plus(
                            new Transform2d(
                                    FieldConstants.CORAL_WIDTH.minus(FieldConstants.REEF_L4_OFFSET_LEFT).unaryMinus(),
                                    Inches.of(0),
                                    Rotation2d.kZero));
            this.poseL4Right =
                    pose.plus(
                            new Transform2d(
                                    FieldConstants.CORAL_WIDTH
                                            .minus(FieldConstants.REEF_L4_OFFSET_RIGHT)
                                            .unaryMinus(),
                                    Inches.of(0),
                                    Rotation2d.kZero));
            this.poseL2L3Left =
                    pose.plus(
                            new Transform2d(
                                    FieldConstants.REEF_L2L3_OFFSET_RIGHT.unaryMinus(),
                                    Inches.of(0),
                                    Rotation2d.kZero));
            this.poseL2L3Right =
                    pose.plus(
                            new Transform2d(
                                    FieldConstants.REEF_L2L3_OFFSET_RIGHT.unaryMinus(),
                                    Inches.of(0),
                                    Rotation2d.kZero));
            this.algaePose = tagPose.plus(transformAlgae);
        }

        public Pose2d getCoralPlacePose(CoralPlaceHeights height) {
            return switch (height) {
                case CORAL_L2, CORAL_L3 -> {
                    if (direction == Direction.LEFT) {
                        yield poseL2L3Left;
                    } else {
                        yield poseL2L3Right;
                    }
                }
                case CORAL_L4 -> {
                    if (direction == Direction.LEFT) {
                        yield poseL4Left;
                    } else {
                        yield poseL4Right;
                    }
                }
                case CORAL_L1 -> pose;
            };
        }

        public Pose2d getAlgaePickPose() {
            return algaePose;
        }
    }

    private RobotControlMode currentControlMode = RobotControlMode.Disabled;
    private RobotControlMode newControlMode = RobotControlMode.Disabled;
    private ArrayList<RobotStateListener> stateListeners;
    private ManualTester manualTester;

    public final OperatorInterface operatorInterface;
    public final SwerveDrive swerve;
    public final CoralPicker coralPicker;
    public final ChamberOfCorals chamberOfCorals;
    public final Overwatch overwatch;
    public final Affector affector;
    public final Vision vision;
    public final Climber climber;

    public final CommandXboxController driverController;
    public final CommandGenericHID gunnerController;

    private static Robot instance;

    private AutoChooser autoChooser;

    public Optional<CoralBranch> coralBranch = Optional.empty();
    public Optional<CoralPlaceHeights> placePosition = Optional.empty();

    public boolean algaeAfterPlace = false;

    public Robot() {
        instance = this;
        CommandScheduler.getInstance().enable();

        DriverStation.silenceJoystickConnectionWarning(true);

        configureAdvantageKit();

        driverController = new CommandXboxController(Constants.kDriverPort);
        gunnerController = new CommandXboxController(Constants.kGunnerPort);

        stateListeners = new ArrayList<>();

        swerve = new SwerveDrive();
        operatorInterface = new OperatorInterface();
        affector = new Affector();
        swerve.setDefaultCommand(
                swerve.drive(operatorInterface::get).withName(SwerveConstants.kDriveCommandName));
        coralPicker = new CoralPicker();
        climber = new Climber();
        chamberOfCorals = new ChamberOfCorals();
        vision = new Vision();
        overwatch = new Overwatch();

        Blinkin blinkin = new Blinkin();

        TatorCommands.prepare(this);

        manualTester = new ManualTester(operatorInterface);

        for (Subsystem subsystem : Subsystem.getSubsystemList()) {
            subsystem.configure();
        }

        configureDriverBindings();
        configureGunnerBindings();

        // sets up port forwarding so we can connect to the limelight embedded
        // web server over USB at the IP address 172.22.11.2:5<ip><last digit of port>
        // the normal IP address over the radio is 10.21.22.<ip>:<port> for reference
        // so 10.21.22.13:5801 would become 172.22.11.2:5131 over USB
        for (int ip = 11; ip <= 14; ip++) {
            for (int port = 5800; port <= 5807; port++) {
                PortForwarder.add(5000 + ip * 10 + (port % 10), "10.21.22." + ip, port);
            }
        }

        // does the same as the above but for tatorvision instead of limelight
        PortForwarder.add(5150, "10.21.22.15", 80);

        /* AUTO INITIALIZATION */
        autoChooser = new AutoChooser();
        AutoRoutines.prepare(this);
        autoChooser.addRoutine("Odometry Test", AutoRoutines::odometryTest);
        autoChooser.addRoutine("Run Around (Left)", AutoRoutines::runAroundLeft);
        autoChooser.addRoutine("Run Around (Right)", AutoRoutines::runAroundRight);
        autoChooser.addRoutine("Dead Reckoned Source (Left)", AutoRoutines::sourceDeadLeft);
        autoChooser.addRoutine("Free Rank Point", AutoRoutines::freeRankPoint);
        autoChooser.addRoutine("Unfinished Algae Auto", AutoRoutines::algaeAuto);
        autoChooser.addRoutine("Dead Reckoned Source (Right)", AutoRoutines::sourceDeadRight);
        autoChooser.addRoutine(
                "Path Branching (Left)", () -> AutoRoutines.branchingSource(Direction.LEFT));
        autoChooser.addRoutine(
                "Path Branching (Right)", () -> AutoRoutines.branchingSource(Direction.RIGHT));
        autoChooser.addRoutine(
                "Dead Reackoned Source (Right)(Optimized)", AutoRoutines::sourceDeadRightOptimized);
        SmartDashboard.putData("Auto Chooser", autoChooser);
        RobotModeTriggers.autonomous()
                .whileTrue(
                        autoChooser
                                .selectedCommandScheduler()
                                .withInterruptBehavior(InterruptionBehavior.kCancelIncoming));
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

        if (Constants.kRobotMedium != RobotMedium.REPLAY) {
            File logDir = new File(RobotBase.isReal() ? "/home/lvuser/logs" : "logs");
            if (!logDir.exists()) {
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
        driverController.start().onTrue(Commands.runOnce(swerve::resetPoseRotation));

        affector.hasAlgae.onTrue(
                Commands.runOnce(() -> driverController.getHID().setRumble(RumbleType.kBothRumble, 1))
                        .andThen(Commands.waitSeconds(0.5))
                        .andThen(
                                Commands.runOnce(
                                        () -> driverController.getHID().setRumble(RumbleType.kBothRumble, 0))));

        driverController
                .leftTrigger()
                .and(driverController.leftBumper().negate())
                .whileTrue(TatorCommands.pickCoral());
        final Trigger leftTriggerDebounced =
                driverController.leftTrigger().debounce(3, DebounceType.kFalling);
        Commands.run(
                        () -> {
                            Logger.recordOutput("AAAAAAAA", leftTriggerDebounced.getAsBoolean());
                        })
                .ignoringDisable(true)
                .schedule();
        chamberOfCorals
                .hasCoral
                .and(affector.hasNothing)
                .and(driverController.back().negate())
                .and(RobotModeTriggers.teleop())
                .onTrue(TatorCommands.handoff());
        affector
                .hasCoral
                .and(overwatch.isAt(Node.CORAL_HOLDING))
                .and(() -> !placePosition.isEmpty())
                .and(RobotModeTriggers.teleop())
                .onTrue(
                        Commands.defer(
                                () -> TatorCommands.prep(placePosition.get()),
                                TatorCommands.prep(CoralPlaceHeights.CORAL_L2).getRequirements()));

        driverController
                .leftBumper()
                .and(driverController.leftTrigger().negate())
                .whileTrue(
                        Commands.defer(
                                        () -> {
                                            if (coralBranch.isPresent()) {
                                                return TatorCommands.reefPickAlgae(coralBranch.get());
                                            } else {
                                                return Commands.none();
                                            }
                                        },
                                        Set.of())
                                .onlyIf(overwatch.isAt(Node.HOME)))
                .and(
                        new Trigger(() -> overwatch.getFinalDestination() == Node.ALGAE_REEF_HIGH)
                                .or(() -> overwatch.getFinalDestination() == Node.ALGAE_REEF_LOW))
                .onFalse(
                        Commands.either(
                                overwatch
                                        .followSequence(List.of(Node.HOME), RotationType.CLOCKWISE)
                                        .alongWith(affector.checkCoral()), // force a reset if still trying to pick,
                                Commands.sequence(
                                        Commands.waitUntil(operatorInterface.nonzeroInput),
                                        Commands.waitSeconds(Constants.POST_ALGAE_PICK_DELAY),
                                        Commands.either(
                                                overwatch.followSequence(
                                                        List.of(Node.ALGAE_AFTER_PICKED_HIGH), RotationType.COUNTER_CLOCKWISE),
                                                overwatch.followSequence(
                                                        List.of(Node.ALGAE_AFTER_PICKED_LOW), RotationType.COUNTER_CLOCKWISE),
                                                () -> overwatch.getFinalDestination() == Node.ALGAE_REEF_HIGH),
                                        overwatch.followSequence(
                                                List.of(Node.ALGAE_HOLDING), RotationType.COUNTER_CLOCKWISE)),
                                () -> affector.getState() != AffectorStates.PICKED_ALGAE));
        // affector.hasCoral.onTrue(overwatch.goTo(Node.TRANSPORT_SAFETY, RotationType.CLOCKWISE));

        driverController.b().onTrue(TatorCommands.home());
        driverController
                .a()
                .and(overwatch.isAt(Node.ALGAE_HOLDING))
                .onTrue(
                        overwatch.followSequence(
                                List.of(Node.ALGAE_PROCESSOR_PLACEMENT), RotationType.CLOCKWISE));
        driverController
                .y()
                .and(overwatch.isAt(Node.ALGAE_HOLDING))
                .onTrue(
                        overwatch.followSequence(
                                List.of(Node.ALGAE_BARGE_PLACEMENT),
                                RotationType.COUNTER_CLOCKWISE,
                                OverwatchConstants.BARGE_PROFILE));
        driverController
                .x()
                .onTrue(
                        Commands.either(
                                Commands.waitUntil(overwatch.isAt(Node.CORAL_HOLDING))
                                        .andThen(
                                                overwatch
                                                        .followSequence(List.of(Node.L1PREP), RotationType.COUNTER_CLOCKWISE)
                                                        .asProxy()),
                                overwatch.recover(Node.L1PREP).asProxy(),
                                () -> overwatch.getFinalDestination() == Node.CORAL_HOLDING));
        driverController
                .rightBumper()
                .and(() -> coralBranch.isPresent() && placePosition.isPresent())
                .and(() -> swerve.getState() != SwerveStates.ALIGNING)
                .and(
                        () -> {
                            var dest = overwatch.getFinalDestination();
                            return dest == Node.L2PREP || dest == Node.L3PREP || dest == Node.L4PREP;
                        })
                .and(affector.hasCoral)
                .onTrue(
                        Commands.defer(
                                () -> TatorCommands.place(coralBranch.get(), placePosition.get()),
                                TatorCommands.place(CoralBranch.SOUTH_LEFT, CoralPlaceHeights.CORAL_L2)
                                        .getRequirements()));

        var emptyCommand = Commands.none().withName("nothing.");
        operatorInterface
                .nonzeroInput
                .and(
                        () ->
                                !swerve
                                        .getPossibleCommand()
                                        .orElse(emptyCommand)
                                        .getName()
                                        .equals(SwerveConstants.kDriveCommandName))
                .onTrue(swerve.drive(operatorInterface).withTimeout(0.02));
        driverController
                .rightTrigger()
                .and(
                        overwatch
                                .isAt(Node.ALGAE_BARGE_PLACEMENT)
                                .or(overwatch.isAt(Node.ALGAE_PROCESSOR_PLACEMENT))
                                .or(overwatch.isAt(Node.ALGAE_HOLDING)))
                .onTrue(TatorCommands.releaseAlgae());
        driverController
                .rightTrigger()
                .and(overwatch.isAt(Node.L1PREP))
                .onTrue(
                        affector
                                .ejectL1()
                                .andThen(
                                        overwatch.followSequence(
                                                List.of(Node.L1POSTPLACE, Node.HOME), RotationType.CLOCKWISE))
                                .andThen(
                                        Commands.runOnce(
                                                () -> {
                                                    coralBranch = Optional.empty();
                                                    placePosition = Optional.empty();
                                                })));

        driverController
                .povDown()
                .onTrue(climber.goTo(ClimberPositions.DEPLOYED).alongWith(overwatch.recover(Node.CLIMB)));
        driverController
                .povUp()
                .and(
                        () ->
                                climber.getDesiredPosition() == ClimberPositions.DEPLOYED
                                        && climber.getState() == ClimberStates.IDLE)
                .onTrue(climber.goTo(ClimberPositions.CLIMB));
        driverController.povRight().onTrue(coralPicker.stow());
        driverController
                .leftBumper()
                .and(driverController.leftTrigger())
                .whileTrue(TatorCommands.pickAlgaeGround().asProxy().onlyIf(overwatch.isAt(Node.HOME)))
                .and(() -> overwatch.getFinalDestination() == Node.ALGAE_GROUND_PICK)
                .onFalse(
                        Commands.either(
                                overwatch.followSequence(
                                        List.of(Node.ALGAE_PRE_GROUND_PICK, Node.HOME), RotationType.CLOCKWISE),
                                overwatch.followSequence(
                                        List.of(Node.ALGAE_HOLDING), RotationType.COUNTER_CLOCKWISE),
                                () -> affector.getState() != AffectorStates.PICKED_ALGAE));

        // pick recovery
        driverController
                .back()
                .and(overwatch.isAt(Node.HOME))
                .onTrue(
                        Commands.sequence(
                                overwatch.followSequence(List.of(Node.CORAL_HOLDING), RotationType.CLOCKWISE),
                                overwatch.followSequence(List.of(Node.HOME), RotationType.CLOCKWISE)));
    }

    private void configureGunnerBindings() {
        var b1 = gunnerController.button(1);
        var b2 = gunnerController.button(2);
        var b3 = gunnerController.button(3);
        var b4 = gunnerController.button(4);
        for (CoralBranch branch : CoralBranch.values()) {
            if (branch.buttonCombo != 0) {
                Trigger combo =
                        (((branch.buttonCombo & 1) > 0) ? b1 : b1.negate())
                                .and(((branch.buttonCombo & 2) > 0) ? b2 : b2.negate())
                                .and(((branch.buttonCombo & 4) > 0) ? b3 : b3.negate())
                                .and(((branch.buttonCombo & 8) > 0) ? b4 : b4.negate());
                combo.onTrue(
                        Commands.runOnce(() -> coralBranch = Optional.of(branch)).ignoringDisable(true));
            }
        }

        gunnerController.button(11).onTrue(Commands.runOnce(() -> algaeAfterPlace = false));
        gunnerController.button(12).onTrue(Commands.runOnce(() -> algaeAfterPlace = true));

        /* CORAL PLACEMENT */
        gunnerController.button(10).onTrue(TatorCommands.home());
        gunnerController
                .button(6)
                .onTrue(Commands.runOnce(() -> placePosition = Optional.of(CoralPlaceHeights.CORAL_L1)));
        gunnerController
                .button(7)
                .onTrue(Commands.runOnce(() -> placePosition = Optional.of(CoralPlaceHeights.CORAL_L2)));
        gunnerController
                .button(8)
                .onTrue(Commands.runOnce(() -> placePosition = Optional.of(CoralPlaceHeights.CORAL_L3)));
        gunnerController
                .button(9)
                .onTrue(Commands.runOnce(() -> placePosition = Optional.of(CoralPlaceHeights.CORAL_L4)));

        // map WASD keys on gunner keypad to nudge the robot
        gunnerController
                .button(14)
                .onTrue(swerve.drive(() -> new ChassisSpeeds(0.1, 0, 0)).withTimeout(.1));
        gunnerController
                .button(15)
                .onTrue(swerve.drive(() -> new ChassisSpeeds(-0.1, 0, 0)).withTimeout(.1));
        gunnerController
                .button(16)
                .onTrue(swerve.drive(() -> new ChassisSpeeds(0, 0.1, 0)).withTimeout(0.1));
        gunnerController
                .button(17)
                .onTrue(swerve.drive(() -> new ChassisSpeeds(0, -0.1, 0)).withTimeout(0.1));

        gunnerController.button(13).onTrue(overwatch.goTo(Node.ALGAE_BARGE_PLACEMENT));

        // gunnerController.button(12).onTrue(Commands.runOnce(() -> this.algaeAfterPlace = true));
        // gunnerController.button(11).onTrue(Commands.runOnce(() -> this.algaeAfterPlace = false));

        gunnerController
                .button(18)
                .and(() -> coralPicker.getDesiredPosition() != CoralPickerPositions.DEPLOYED)
                .whileTrue(coralPicker.completeStow());
    }

    public static Robot getInstance() {
        return instance;
    }

    @Override
    public void robotPeriodic() {
        CommandScheduler.getInstance().run();

        for (Subsystem subsystem : Subsystem.getSubsystemList()) {
            subsystem.log();
        }

        Logger.recordOutput(
                "CoralBranch", coralBranch.isPresent() ? coralBranch.get().name() : "empty");
        Logger.recordOutput(
                "PlaceHeight", placePosition.isPresent() ? placePosition.get().name() : "empty");
        Logger.recordOutput("AlgaeAfterPlace", algaeAfterPlace);
        Logger.recordOutput("AutoState", AutoRoutines.getState());

        DeviceHealthManager.logHealth();
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
    }

    @Override
    public void autonomousPeriodic() {}

    @Override
    public void autonomousExit() {}

    @Override
    public void teleopInit() {
        CommandScheduler.getInstance().enable();
        updateStateListeners(RobotControlMode.Teleop);
    }

    @Override
    public void teleopPeriodic() {}

    @Override
    public void teleopExit() {}

    @Override
    public void testInit() {
        var commandScheduler = CommandScheduler.getInstance();
        commandScheduler.cancelAll();
        commandScheduler.disable();
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

    public boolean algaeAfterPlace() {
        return algaeAfterPlace;
    }

    public static Alliance getAlliance() {
        return DriverStation.getAlliance().orElse(Alliance.Blue);
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
        manualTester.setController(new XBOXController(Constants.kDriverPort));

        for (Subsystem subsystem : Subsystem.getSubsystemList()) {
            ManualTestGroup group = subsystem.createManualTests();
            if (group != null) {
                manualTester.registerTestGroup(group);
                System.out.println(subsystem.getName());
            }
        }
    }
}
