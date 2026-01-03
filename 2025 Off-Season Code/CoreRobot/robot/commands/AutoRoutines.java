package frc.robot.commands;

import static edu.wpi.first.units.Units.Feet;

import choreo.auto.AutoFactory;
import choreo.auto.AutoRoutine;
import choreo.auto.AutoTrajectory;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.button.Trigger;
import frc.robot.Robot;
import frc.robot.Robot.AlgaeHeight;
import frc.robot.Robot.CoralBranch;
import frc.robot.Robot.CoralPlaceHeights;
import frc.robot.Robot.Direction;
import frc.robot.constants.FieldConstants;
import frc.robot.subsystems.overwatch.Graph.Node;
import frc.robot.subsystems.overwatch.Overwatch.RotationType;
import frc.robot.subsystems.overwatch.OverwatchConstants;
import java.util.List;
import java.util.Optional;
import org.teamtators.util.FlipUtil;
import org.teamtators.util.FlipUtil.FlipType;
import org.teamtators.util.TatorMath;

public class AutoRoutines {
    private static AutoFactory autoFactory;
    private static Robot robot;
    private static AutoState currentState = AutoState.INIT;
    private static final double FAST_PICK_SPEED = 2.8;
    private static final double FAST_PICK_SLOWDOWN_DIST = 1.25;
    private static final double FAST_PLACE_SPEED = 2.8;
    private static final double FAST_PLACE_SLOWDOWN_DIST = 1.4;

    public static AutoState getState() {
        return currentState;
    }

    public enum AutoState {
        INIT,
        PRE_BRANCHING, // exists because at the start of a match, the arm is in a weird position.
        PLACING,
        REPLACE,
        PICKING,
        PREPPING,
        ;

        Trigger trigger;

        AutoState() {
            this.trigger = new Trigger(() -> currentState == this);
        }
    }

    public static void prepare(Robot robot) {
        AutoRoutines.robot = robot;
        var swerve = robot.swerve;

        autoFactory =
                new AutoFactory(
                        swerve::getPose, // A function that returns the current robot pose
                        swerve::resetPose, // A function that resets the current robot pose to the provided
                        swerve::setSampleSetpoint, // The drive subsystem trajectory follower
                        true, // If alliance flipping should be enabled
                        swerve // The drive subsystem
                        );
        autoFactory.bind("L4Prep", robot.overwatch.goTo(Node.L4PREP));
    }

    public static AutoRoutine odometryTest() {
        AutoRoutine routine = autoFactory.newRoutine("Odometry Test");
        AutoTrajectory traj = routine.trajectory("forward_10ft");
        routine.active().onTrue(Commands.sequence(traj.resetOdometry(), traj.cmd()));
        return routine;
    }

    public static AutoRoutine freeRankPoint() {
        ChassisSpeeds backwards = new ChassisSpeeds(-0.3, 0, 0);
        AutoRoutine routine = autoFactory.newRoutine("Free Rank Point");
        routine
                .active()
                .onTrue(
                        Commands.sequence(
                                Commands.runOnce(() -> robot.swerve.resetPoseRotation()),
                                robot.swerve.drive(() -> backwards).withTimeout(3)));
        return routine;
    }

    private static final Rotation2d FIRST_PICK_OFFSET = Rotation2d.fromDegrees(15);
    private static final Rotation2d SECOND_PICK_OFFSET = Rotation2d.fromDegrees(0);

    private static final Pose2d LEFT_SOURCE_FIRST_PICK =
            TatorMath.rotateInPlace(
                    FieldConstants.LEFT_SOURCE_TARGET.plus(FieldConstants.LEFT_SOURCE_PICK_FIRST_OFFSET),
                    FIRST_PICK_OFFSET);
    private static final Pose2d LEFT_SOURCE_SECOND_PICK =
            TatorMath.rotateInPlace(
                    FieldConstants.LEFT_SOURCE_TARGET.plus(FieldConstants.LEFT_SOURCE_PICK_SECOND_OFFSET),
                    SECOND_PICK_OFFSET);

    private static final Pose2d RIGHT_SOURCE_FIRST_PICK =
            FlipUtil.flip(LEFT_SOURCE_FIRST_PICK, FlipType.YFLIP);
    private static final Pose2d RIGHT_SOURCE_SECOND_PICK =
            FlipUtil.flip(LEFT_SOURCE_SECOND_PICK, FlipType.YFLIP);

    // pick from the pick pose, place on the branch, then update the cycle
    private record SourceCycle(Pose2d pickPose, CoralBranch branch) {}

    private static int cycleIndex = 0;

    public static AutoRoutine branchingSource(Direction direction) {
        AutoRoutine routine = autoFactory.newRoutine("Branching Source (" + direction.name() + ")");
        AutoTrajectory start = routine.trajectory("dead_" + direction.name().toLowerCase() + "_start");
        CoralBranch firstBranch =
                switch (direction) {
                    case LEFT -> CoralBranch.NORTH_WEST_RIGHT;
                    case RIGHT -> CoralBranch.NORTH_EAST_LEFT;
                };

        SourceCycle[] cycles =
                switch (direction) {
                    case LEFT ->
                            new SourceCycle[] {
                                new SourceCycle(LEFT_SOURCE_FIRST_PICK, CoralBranch.NORTH_WEST_RIGHT),
                                new SourceCycle(LEFT_SOURCE_FIRST_PICK, CoralBranch.SOUTH_WEST_LEFT),
                                new SourceCycle(LEFT_SOURCE_SECOND_PICK, CoralBranch.SOUTH_WEST_RIGHT),
                                new SourceCycle(LEFT_SOURCE_SECOND_PICK, CoralBranch.SOUTH_LEFT)
                            };
                    case RIGHT ->
                            new SourceCycle[] {
                                new SourceCycle(RIGHT_SOURCE_FIRST_PICK, CoralBranch.NORTH_EAST_LEFT),
                                new SourceCycle(RIGHT_SOURCE_FIRST_PICK, CoralBranch.SOUTH_EAST_RIGHT),
                                new SourceCycle(RIGHT_SOURCE_SECOND_PICK, CoralBranch.SOUTH_EAST_LEFT),
                                new SourceCycle(RIGHT_SOURCE_SECOND_PICK, CoralBranch.SOUTH_RIGHT)
                            };
                };

        routine
                .active()
                .onTrue(
                        Commands.sequence(
                                start.resetOdometry(),
                                Commands.runOnce(
                                        () -> {
                                            cycleIndex = 0;
                                            currentState = AutoState.PRE_BRANCHING;
                                        })));

        new Trigger(routine.loop(), AutoState.PRE_BRANCHING.trigger)
                .onTrue(
                        Commands.parallel(
                                        robot.swerve.alignTo(firstBranch.getCoralPlacePose(CoralPlaceHeights.CORAL_L4)),
                                        robot.overwatch.followSequence(
                                                List.of(Node.L4PREP),
                                                RotationType.SHORTEST,
                                                OverwatchConstants.SLOW_MOTION_PROFILE))
                                .andThen(Commands.runOnce(() -> currentState = AutoState.PLACING)));

        new Trigger(routine.loop(), AutoState.PLACING.trigger)
                .onTrue(
                        Commands.sequence(
                                robot.overwatch.followSequence(
                                        List.of(Node.L4PLACE),
                                        RotationType.SHORTEST,
                                        OverwatchConstants.SLOW_MOTION_PROFILE),
                                robot.affector.checkCoral(),
                                Commands.waitSeconds(.02),
                                Commands.either(
                                        Commands.runOnce(() -> currentState = AutoState.REPLACE),
                                        Commands.runOnce(
                                                () -> {
                                                    if (cycleIndex < cycles.length - 1) {
                                                        cycleIndex++;
                                                    }
                                                    currentState = AutoState.PICKING;
                                                }),
                                        robot.affector.hasCoral)));

        ChassisSpeeds backwards = new ChassisSpeeds(-1, 0, 0);
        new Trigger(routine.loop(), AutoState.REPLACE.trigger)
                .onTrue(
                        Commands.sequence(
                                robot.swerve.drive(() -> backwards).withTimeout(0.5),
                                robot.overwatch.followSequence(List.of(Node.L4PREP), RotationType.SHORTEST),
                                robot.swerve.defer(
                                        () ->
                                                robot.swerve.alignTo(
                                                        cycles[cycleIndex]
                                                                .branch()
                                                                .getCoralPlacePose(CoralPlaceHeights.CORAL_L4))),
                                Commands.runOnce(() -> currentState = AutoState.PLACING)));

        new Trigger(routine.loop(), AutoState.PICKING.trigger)
                .onTrue(pick(CoralPlaceHeights.CORAL_L4))
                .onTrue(
                        robot
                                .swerve
                                .defer(() -> robot.swerve.alignTo(cycles[cycleIndex].pickPose()))
                                .andThen(() -> currentState = AutoState.PREPPING));

        new Trigger(routine.loop(), AutoState.PREPPING.trigger)
                .onTrue(
                        robot.swerve.defer(
                                () ->
                                        robot.swerve.alignTo(
                                                cycles[cycleIndex].branch().getCoralPlacePose(CoralPlaceHeights.CORAL_L4))))
                .debounce(1.5)
                .onTrue(
                        Commands.either(
                                Commands.sequence(
                                        Commands.waitUntil(robot.overwatch.isAt(Node.L4PREP).and(robot.swerve.aligned)),
                                        Commands.runOnce(() -> currentState = AutoState.PLACING)),
                                Commands.runOnce(() -> currentState = AutoState.PICKING),
                                robot.affector.hasNothing.negate().or(robot.chamberOfCorals.hasCoral)));

        return routine;
    }

    public static AutoRoutine runAroundLeft() {
        AutoRoutine routine = autoFactory.newRoutine("Run Around (Left)");
        AutoTrajectory start = routine.trajectory("run_around_left_start");

        routine
                .active()
                .onTrue(Commands.sequence(start.resetOdometry(), start.cmd()))
                .onTrue(robot.overwatch.goTo(Node.TRANSPORT_SAFETY));

        start
                .inactive()
                .onTrue(
                        Commands.sequence(
                                robot.swerve.alignTo(
                                        CoralBranch.SOUTH_LEFT.getCoralPlacePose(CoralPlaceHeights.CORAL_L4)),
                                robot.overwatch.followSequence(
                                        List.of(Node.L4PLACE),
                                        RotationType.CLOCKWISE,
                                        OverwatchConstants.SLOW_MOTION_PROFILE),
                                oldPick(
                                        new Pose2d(
                                                FieldConstants.MIDDLE_LOLLIPOP.minus(
                                                        new Translation2d(Feet.of(0.1), Feet.of(0.075))),
                                                Rotation2d.fromDegrees(10))),
                                TatorCommands.handoff(true),
                                TatorCommands.prep(CoralPlaceHeights.CORAL_L4)
                                        .alongWith(
                                                robot.swerve.alignTo(
                                                        CoralBranch.SOUTH_RIGHT.getCoralPlacePose(CoralPlaceHeights.CORAL_L4))),
                                robot.overwatch.followSequence(
                                        List.of(Node.L4PLACE),
                                        RotationType.CLOCKWISE,
                                        OverwatchConstants.SLOW_MOTION_PROFILE),
                                oldPick(
                                        new Pose2d(
                                                FieldConstants.BOTTOM_LOLLIPOP.minus(
                                                        new Translation2d(Feet.of(-1), Feet.of(.75))),
                                                Rotation2d.fromDegrees(15))),
                                TatorCommands.handoff(true)
                                        .alongWith(
                                                robot.swerve.alignTo(
                                                        CoralBranch.SOUTH_EAST_LEFT.getCoralPlacePose(
                                                                CoralPlaceHeights.CORAL_L4)))));

        return routine;
    }

    public static AutoRoutine runAroundRight() {
        AutoRoutine routine = autoFactory.newRoutine("Run Around (Right)");
        AutoTrajectory start = routine.trajectory("run_around_right_star");
        routine
                .active()
                .onTrue(Commands.sequence(start.resetOdometry(), start.cmd()))
                .onTrue(robot.overwatch.goTo(Node.TRANSPORT_SAFETY));

        start
                .inactive()
                .onTrue(
                        Commands.sequence(
                                robot.swerve.alignTo(
                                        CoralBranch.SOUTH_RIGHT.getCoralPlacePose(CoralPlaceHeights.CORAL_L4)),
                                robot.overwatch.followSequence(
                                        List.of(Node.L4PLACE),
                                        RotationType.CLOCKWISE,
                                        OverwatchConstants.SLOW_MOTION_PROFILE),
                                oldPick(
                                        new Pose2d(
                                                FieldConstants.MIDDLE_LOLLIPOP.minus(
                                                        new Translation2d(Feet.of(0.1), Feet.of(0.075))),
                                                Rotation2d.fromDegrees(10))),
                                TatorCommands.handoff(true),
                                TatorCommands.prep(CoralPlaceHeights.CORAL_L4)
                                        .alongWith(
                                                robot.swerve.alignTo(
                                                        CoralBranch.SOUTH_LEFT.getCoralPlacePose(CoralPlaceHeights.CORAL_L4))),
                                robot.overwatch.followSequence(
                                        List.of(Node.L4PLACE),
                                        RotationType.CLOCKWISE,
                                        OverwatchConstants.SLOW_MOTION_PROFILE),
                                oldPick(
                                        new Pose2d(
                                                FieldConstants.TOP_LOLLIPOP.minus(
                                                        new Translation2d(Feet.of(-1), Feet.of(-.75))),
                                                Rotation2d.fromDegrees(-15))),
                                TatorCommands.handoff(true)
                                        .alongWith(
                                                robot.swerve.alignTo(
                                                        CoralBranch.SOUTH_WEST_RIGHT.getCoralPlacePose(
                                                                CoralPlaceHeights.CORAL_L4)))));

        return routine;
    }

    public static AutoRoutine sourceDeadLeft() {
        AutoRoutine routine = autoFactory.newRoutine("Dead Reckon from Source (left)");
        AutoTrajectory start = routine.trajectory("dead_left_start");

        Command command =
                Commands.sequence(
                        start.resetOdometry(),
                        robot
                                .swerve
                                .alignTo(
                                        CoralBranch.NORTH_WEST_RIGHT.getCoralPlacePose(CoralPlaceHeights.CORAL_L4),
                                        3.2,
                                        1.0,
                                        Double.MAX_VALUE)
                                .alongWith(
                                        robot.overwatch.followSequence(List.of(Node.L4PREP), RotationType.SHORTEST)),
                        robot.overwatch.followSequence(
                                List.of(Node.L4PLACE),
                                RotationType.CLOCKWISE,
                                OverwatchConstants.SLOW_MOTION_PROFILE),
                        pickNPrep(
                                LEFT_SOURCE_FIRST_PICK,
                                FAST_PICK_SPEED,
                                FAST_PICK_SLOWDOWN_DIST,
                                CoralBranch.SOUTH_WEST_LEFT,
                                CoralPlaceHeights.CORAL_L4),
                        robot.overwatch.followSequence(
                                List.of(Node.L4PLACE),
                                RotationType.CLOCKWISE,
                                OverwatchConstants.SLOW_MOTION_PROFILE),
                        pickNPrep(
                                LEFT_SOURCE_SECOND_PICK,
                                FAST_PICK_SPEED,
                                FAST_PICK_SLOWDOWN_DIST,
                                CoralBranch.SOUTH_WEST_RIGHT,
                                CoralPlaceHeights.CORAL_L4),
                        robot.overwatch.followSequence(
                                List.of(Node.L4PLACE),
                                RotationType.CLOCKWISE,
                                OverwatchConstants.SLOW_MOTION_PROFILE),
                        TatorCommands.home());
        routine
                .active()
                .onTrue(command)
                .onTrue(
                        Commands.print(
                                "HANDOFF REQUIREMENTS: "
                                        + TatorCommands.handoff(true).getRequirements().toString()))
                .onTrue(Commands.print("AUTO REQUIREMENTS: " + command.getRequirements().toString()));

        return routine;
    }

    public static AutoRoutine sourceDeadRightOptimized() {
        AutoRoutine routine = autoFactory.newRoutine("Dead Reckon from Source (right)");
        AutoTrajectory start = routine.trajectory("dead_right_start");

        Command command =
                Commands.sequence(
                        start.resetOdometry(),
                        robot
                                .swerve
                                .alignTo(
                                        CoralBranch.NORTH_EAST_LEFT.getCoralPlacePose(CoralPlaceHeights.CORAL_L4),
                                        3.2,
                                        1.0,
                                        Double.MAX_VALUE)
                                .alongWith(
                                        robot.overwatch.followSequence(List.of(Node.L4PREP), RotationType.SHORTEST)),
                        robot.overwatch.followSequence(
                                List.of(Node.L4PLACE),
                                RotationType.CLOCKWISE,
                                OverwatchConstants.SLOW_MOTION_PROFILE),
                        pickNPrep(
                                TatorMath.rotateInPlace(FieldConstants.RIGHT_SOURCE_TARGET, FIRST_PICK_OFFSET),
                                FAST_PICK_SPEED,
                                FAST_PICK_SLOWDOWN_DIST,
                                CoralBranch.SOUTH_EAST_RIGHT,
                                CoralPlaceHeights.CORAL_L4),
                        robot.overwatch.followSequence(
                                List.of(Node.L4PLACE),
                                RotationType.CLOCKWISE,
                                OverwatchConstants.SLOW_MOTION_PROFILE),
                        pickNPrep(
                                TatorMath.rotateInPlace(FieldConstants.RIGHT_SOURCE_TARGET, SECOND_PICK_OFFSET),
                                FAST_PICK_SPEED,
                                FAST_PICK_SLOWDOWN_DIST,
                                CoralBranch.SOUTH_EAST_LEFT,
                                CoralPlaceHeights.CORAL_L4),
                        robot.overwatch.followSequence(
                                List.of(Node.L4PLACE),
                                RotationType.CLOCKWISE,
                                OverwatchConstants.SLOW_MOTION_PROFILE),
                        TatorCommands.home());
        routine
                .active()
                .onTrue(command)
                .onTrue(
                        Commands.print(
                                "HANDOFF REQUIREMENTS: "
                                        + TatorCommands.handoff(true).getRequirements().toString()))
                .onTrue(Commands.print("AUTO REQUIREMENTS: " + command.getRequirements().toString()));

        return routine;
    }

    public static AutoRoutine sourceDeadRight() {
        AutoRoutine routine = autoFactory.newRoutine("Dead Reckon from Source (right)");
        AutoTrajectory start = routine.trajectory("dead_right_start");
        Command command =
                Commands.sequence(
                        start.resetOdometry(),
                        robot
                                .swerve
                                .alignTo(CoralBranch.NORTH_EAST_LEFT.getCoralPlacePose(CoralPlaceHeights.CORAL_L4))
                                .alongWith(
                                        robot.overwatch.followSequence(List.of(Node.L4PREP), RotationType.SHORTEST)),
                        robot.overwatch.followSequence(
                                List.of(Node.L4PLACE),
                                RotationType.CLOCKWISE,
                                OverwatchConstants.SLOW_MOTION_PROFILE),
                        oldPick(FieldConstants.RIGHT_SOURCE_TARGET),
                        Commands.runOnce(() -> robot.placePosition = Optional.of(CoralPlaceHeights.CORAL_L4)),
                        TatorCommands.handoff(true)
                                .alongWith(
                                        robot.swerve.alignTo(
                                                CoralBranch.SOUTH_EAST_RIGHT.getCoralPlacePose(
                                                        CoralPlaceHeights.CORAL_L4))),
                        robot.overwatch.followSequence(
                                List.of(Node.L4PLACE),
                                RotationType.CLOCKWISE,
                                OverwatchConstants.SLOW_MOTION_PROFILE),
                        oldPick(FieldConstants.RIGHT_SOURCE_TARGET),
                        Commands.runOnce(() -> robot.placePosition = Optional.of(CoralPlaceHeights.CORAL_L4)),
                        TatorCommands.handoff(true)
                                .alongWith(
                                        robot.swerve.alignTo(
                                                CoralBranch.SOUTH_EAST_LEFT.getCoralPlacePose(CoralPlaceHeights.CORAL_L4))),
                        robot.overwatch.followSequence(
                                List.of(Node.L4PLACE),
                                RotationType.CLOCKWISE,
                                OverwatchConstants.SLOW_MOTION_PROFILE),
                        TatorCommands.home());
        routine
                .active()
                .onTrue(command)
                .onTrue(
                        Commands.print(
                                "HANDOFF REQUIREMENTS: "
                                        + TatorCommands.handoff(true).getRequirements().toString()))
                .onTrue(Commands.print("AUTO REQUIREMENTS: " + command.getRequirements().toString()));

        return routine;
    }

    private static Command pick(CoralPlaceHeights height) {
        return Commands.sequence(
                Commands.runOnce(() -> robot.placePosition = Optional.of(height)),
                TatorCommands.pickCoral()
                        .alongWith(robot.overwatch.followSequence(List.of(Node.HOME), RotationType.SHORTEST))
                        .alongWith(robot.affector.checkCoral()),
                TatorCommands.handoff(true)
                        .andThen(
                                TatorCommands.prep(height)
                                        .asProxy()
                                        .onlyIf(() -> height != CoralPlaceHeights.CORAL_L4)));
    }

    private static Command pickNPrep(
            Pose2d coralPose,
            double maxTransSpeed,
            double slowDownDistance,
            CoralBranch branch,
            CoralPlaceHeights height) {
        return robot
                .swerve
                .alignTo(coralPose, maxTransSpeed, slowDownDistance, Double.MAX_VALUE)
                .andThen(
                        robot.swerve.alignTo(
                                branch.getCoralPlacePose(height),
                                FAST_PLACE_SPEED,
                                FAST_PLACE_SLOWDOWN_DIST,
                                Double.MAX_VALUE))
                .alongWith(pick(height));
    }

    // the old way of picking; stops until the bot knows it has a coral
    private static Command oldPick(Pose2d coralPose) {
        return TatorCommands.pickCoral()
                .deadlineFor(robot.swerve.alignTo(coralPose, 1.2, 0.7, Double.MAX_VALUE))
                .alongWith(robot.overwatch.followSequence(List.of(Node.HOME), RotationType.CLOCKWISE))
                .alongWith(robot.affector.checkCoral());
    }

    private static Command freeze() {
        return robot.swerve.runOnce(
                () -> {
                    robot.swerve.setVelocitySetpoint(new ChassisSpeeds());
                });
    }

    public static AutoRoutine algaeAuto() {
        AutoRoutine routine = autoFactory.newRoutine("Algae Dunk");
        AutoTrajectory startPoseTraj = routine.trajectory("algae-poses");
        // does this need a backup? if this fails it's really not safe to follow the path
        Pose2d bargePose = FlipUtil.conditionalFlip(startPoseTraj.getFinalPose().get());

        // REMOVE TIMEOUTS!
        //  REMOVE TIMEOUTS!
        //   REMOVE TIMEOUTS!
        //    REMOVE TIMEOUTS!
        //     REMOVE TIMEOUTS!
        //    REMOVE TIMEOUTS!
        //   REMOVE TIMEOUTS!
        //  REMOVE TIMEOUTS!
        // REMOVE TIMEOUTS!

        var branch = CoralBranch.NORTH_LEFT;

        Command pickAlgae =
                robot
                        .overwatch
                        // .goTo(position.algaeHeight == AlgaeHeight.HIGH ? Node.ALGAE_REEF_HIGH :
                        // Node.ALGAE_REEF_LOW)
                        .followSequence(
                                List.of(
                                        branch.algaeHeight == AlgaeHeight.HIGH
                                                ? Node.ALGAE_REEF_HIGH
                                                : Node.ALGAE_REEF_LOW),
                                RotationType.COUNTER_CLOCKWISE)
                        .asProxy()
                        .andThen(
                                robot.swerve.drive(() -> new ChassisSpeeds(0.3, 0, 0)).withTimeout(1).asProxy())
                        .alongWith(Commands.deadline(robot.affector.pickAlgae().asProxy()));
        Command seq =
                Commands.sequence(
                        startPoseTraj.resetOdometry().asProxy(),
                        TatorCommands.home().asProxy(),
                        robot.swerve.alignTo(branch.getAlgaePickPose()).asProxy(),
                        pickAlgae.asProxy(),
                        Commands.either(
                                        robot.overwatch.followSequence(
                                                List.of(Node.ALGAE_AFTER_PICKED_HIGH), RotationType.COUNTER_CLOCKWISE),
                                        robot.overwatch.followSequence(
                                                List.of(Node.ALGAE_AFTER_PICKED_LOW), RotationType.COUNTER_CLOCKWISE),
                                        () -> robot.overwatch.getFinalDestination() == Node.ALGAE_REEF_HIGH)
                                .asProxy(),
                        robot.swerve.drive(() -> new ChassisSpeeds(-0.8, 0, 0)).withTimeout(0.5).asProxy(),
                        robot
                                .overwatch
                                .followSequence(List.of(Node.ALGAE_HOLDING), RotationType.COUNTER_CLOCKWISE)
                                .asProxy(),
                        // barge time
                        robot.swerve.alignTo(bargePose).asProxy(),
                        robot
                                .overwatch
                                .followSequence(List.of(Node.ALGAE_BARGE_PLACEMENT), RotationType.CLOCKWISE)
                                .asProxy(),
                        robot.affector.ejectAlgae().asProxy(),
                        robot.swerve.drive(() -> new ChassisSpeeds(-0.8, 0, 0)).withTimeout(0.5).asProxy(),
                        // barge done
                        TatorCommands.home().asProxy());

        routine.active().onTrue(seq);

        return routine;
    }
}
