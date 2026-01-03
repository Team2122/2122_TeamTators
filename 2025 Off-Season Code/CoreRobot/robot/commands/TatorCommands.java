package frc.robot.commands;

import static edu.wpi.first.units.Units.Inches;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Transform2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.ScheduleCommand;
import frc.robot.Robot;
import frc.robot.Robot.AlgaeHeight;
import frc.robot.Robot.CoralBranch;
import frc.robot.Robot.CoralPlaceHeights;
import frc.robot.constants.Constants;
import frc.robot.subsystems.affector.Affector.AffectorStates;
import frc.robot.subsystems.overwatch.Graph.Node;
import frc.robot.subsystems.overwatch.Overwatch.OverwatchFollowerMethod;
import frc.robot.subsystems.overwatch.Overwatch.RotationType;
import frc.robot.subsystems.overwatch.OverwatchConstants.OverwatchPos;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class TatorCommands {
    private static Robot robot;

    public static void prepare(Robot robot) {
        TatorCommands.robot = robot;
    }

    public static Command pickCoral() {
        return robot.chamberOfCorals.pick().deadlineFor(robot.coralPicker.deploy());
    }

    public static Command reefPickAlgae(CoralBranch position) {
        return reefPickAlgae(position, RotationType.COUNTER_CLOCKWISE);
    }

    public static Command reefPickAlgae(CoralBranch position, RotationType rotationType) {
        return robot
                .overwatch
                // .goTo(position.algaeHeight == AlgaeHeight.HIGH ? Node.ALGAE_REEF_HIGH :
                // Node.ALGAE_REEF_LOW)
                .followSequence(
                        List.of(
                                position.algaeHeight == AlgaeHeight.HIGH
                                        ? Node.ALGAE_REEF_HIGH
                                        : Node.ALGAE_REEF_LOW),
                        rotationType)
                .andThen(robot.affector.pickAlgae());
    }

    public static Command pickAlgaeGround() {
        return robot
                .overwatch
                .followSequence(
                        List.of(Node.ALGAE_PRE_GROUND_PICK, Node.ALGAE_GROUND_PICK),
                        RotationType.COUNTER_CLOCKWISE)
                .andThen(robot.affector.pickAlgae());
    }

    public static Command releaseAlgae() {
        return robot
                .affector
                .ejectAlgae()
                // .andThen(
                //         Commands.defer(
                //                 () ->
                // Commands.waitUntil(robot.operatorInterface.nonzeroInput.debounce(1)),
                //                 Set.of()))
                // .andThen(robot.overwatch.goTo(Node.HOME));
                .andThen((robot.overwatch.followSequence(List.of(Node.HOME), RotationType.CLOCKWISE)));
    }

    public static Command handoff() {
        return handoff(false);
    }

    public static Command handoff(boolean forAuto) {
        Command handoff =
                robot
                        .overwatch
                        .followSequence(List.of(Node.CORAL_PICK), RotationType.SHORTEST)
                        .andThen(
                                Commands.either(
                                        robot.overwatch.followSequence(
                                                List.of(Node.CORAL_HOLDING, Node.L4FAST_PREPREP, Node.L4PREP),
                                                RotationType.CLOCKWISE,
                                                OverwatchFollowerMethod.NONSTOP),
                                        robot.overwatch.followSequence(
                                                List.of(Node.CORAL_HOLDING), RotationType.SHORTEST),
                                        () ->
                                                robot.placePosition.isPresent()
                                                        && robot.placePosition.get() == CoralPlaceHeights.CORAL_L4));
        if (!forAuto) {
            handoff = handoff.asProxy();
        }

        Command rehome;
        Command grabCoral;
        if (forAuto) {
            rehome =
                    robot
                            .overwatch
                            .recover(Node.HOME)
                            .asProxy()
                            .onlyIf(() -> robot.overwatch.getFinalDestination() != Node.HOME);
            grabCoral = robot.affector.pickCoral();
        } else {
            rehome = Commands.none();
            grabCoral = new ScheduleCommand(robot.affector.pickCoral());
        }
        return Commands.sequence(
                        rehome,
                        Commands.waitUntil(robot.overwatch.isAt(Node.HOME)),
                        handoff.alongWith(grabCoral))
                .withName("Handoff");
    }

    public static Command prep(CoralPlaceHeights position) {
        return robot.overwatch.followSequence(
                List.of(position.prepNode),
                position == CoralPlaceHeights.CORAL_L1
                        ? RotationType.COUNTER_CLOCKWISE
                        : RotationType.CLOCKWISE);
    }

    private static final Transform2d algaeLineupTransform =
            new Transform2d(new Translation2d(Inches.of(-8), Inches.of(0)), Rotation2d.kZero);

    public static Command place(CoralBranch branch, CoralPlaceHeights position) {
        Node prep =
                switch (position) {
                    default -> Node.PICK_SAFETY;
                    case CORAL_L1 -> Node.L1PREP;
                    case CORAL_L2 -> Node.L2PREP;
                    case CORAL_L3 -> Node.L3PREP;
                    case CORAL_L4 -> Node.L4PREP;
                };
        ArrayList<Node> otherPreps = new ArrayList<>();
        otherPreps.add(Node.PICK_SAFETY);
        otherPreps.add(Node.L1PREP);
        otherPreps.add(Node.L2PREP);
        otherPreps.add(Node.L3PREP);
        otherPreps.add(Node.L4PREP);
        otherPreps.remove(prep);
        Node place =
                switch (position) {
                    default -> Node.PICK_SAFETY;
                    case CORAL_L2 -> Node.L2PLACE;
                    case CORAL_L3 -> Node.L3PLACE;
                    case CORAL_L4 -> Node.L4PLACE;
                };
        Pose2d algaeLineupPose = branch.getAlgaePickPose().plus(algaeLineupTransform);
        Command pickAlgae =
                Commands.sequence(
                                robot.swerve.alignTo(algaeLineupPose).asProxy(),
                                TatorCommands.reefPickAlgae(branch, RotationType.SHORTEST)
                                        .deadlineFor(
                                                Commands.waitUntil(
                                                                robot
                                                                        .overwatch
                                                                        .isAt(Node.ALGAE_REEF_HIGH)
                                                                        .or(robot.overwatch.isAt(Node.ALGAE_REEF_LOW)))
                                                        .andThen(robot.swerve.alignTo(branch.getAlgaePickPose()).asProxy())),
                                robot
                                        .swerve
                                        .alignTo(algaeLineupPose)
                                        // proxied because we still want the next part to proceed if interrupted
                                        .asProxy(),
                                Commands.waitUntil(robot.operatorInterface.nonzeroInput)
                                        .andThen(Commands.waitSeconds(Constants.POST_ALGAE_PICK_DELAY))
                                        .andThen(robot.overwatch.goTo(Node.ALGAE_HOLDING)))
                        .asProxy()
                        .finallyDo(() -> robot.algaeAfterPlace = false);

        List<OverwatchPos> homeRoute =
                switch (position) {
                    case CORAL_L1 -> List.of(Node.L1TRANSPORT, Node.HOME);
                    case CORAL_L2 -> List.of(Node.PICK_SAFETY, Node.HOME);
                    case CORAL_L3 -> List.of(Node.L3POSTPLACE, Node.HOME);
                    case CORAL_L4 -> List.of(Node.L4POSTPLACE, Node.HOME);
                    default -> List.of(Node.PICK_SAFETY, Node.HOME);
                };

        Command goHome =
                robot
                        .overwatch
                        .followSequence(homeRoute, RotationType.CLOCKWISE)
                        .alongWith(robot.affector.checkCoral());

        return Commands.sequence(
                // Commands.none()
                robot
                        .swerve
                        .alignTo(branch.getCoralPlacePose(position))
                        .alongWith(
                                robot
                                        .overwatch
                                        .followSequence(List.of(prep), RotationType.SHORTEST)
                                        .asProxy()
                                        .onlyIf(() -> otherPreps.contains(robot.overwatch.getFinalDestination()))),
                Commands.waitUntil(robot.overwatch.isAt(prep)),
                new ScheduleCommand(
                                robot
                                        .overwatch
                                        .followSequence(List.of(place), RotationType.CLOCKWISE)
                                        .andThen(robot.affector.checkCoral())
                                        .andThen(
                                                Commands.runOnce(
                                                        () -> {
                                                            robot.placePosition = Optional.empty();
                                                            robot.coralBranch = Optional.empty();
                                                        }))
                                        .andThen(
                                                Commands.either(
                                                        pickAlgae,
                                                        // go to CORAL_HOLDING if the placement missed
                                                        Commands.waitUntil(robot.operatorInterface.nonzeroInput)
                                                                .andThen(
                                                                        Commands.either(
                                                                                goHome,
                                                                                robot.overwatch.followSequence(
                                                                                        List.of(Node.CORAL_HOLDING), RotationType.CLOCKWISE),
                                                                                () -> robot.affector.getState() == AffectorStates.IDLE)),
                                                        () ->
                                                                robot.algaeAfterPlace()
                                                                        && robot.affector.getState() == AffectorStates.IDLE)))
                        .asProxy());
    }

    public static Command home() {
        return Commands.either(
                robot.overwatch.recover(
                        Node.ALGAE_HOLDING, Node.PICK_ALGAE_SAFETY.liftHeightInches(), RotationType.SHORTEST),
                robot
                        .affector
                        .checkCoral()
                        .andThen(
                                Commands.either(
                                        Commands.defer(
                                                () ->
                                                        robot.overwatch.recover(
                                                                robot.placePosition.isPresent()
                                                                        ? robot.placePosition.get().prepNode
                                                                        : Node.CORAL_HOLDING),
                                                Set.of(robot.overwatch)),
                                        robot.overwatch.recover(Node.HOME),
                                        () -> robot.affector.getState() == AffectorStates.PICKED_CORAL)),
                () -> robot.affector.getState() == AffectorStates.PICKED_ALGAE);
    }

    private TatorCommands() {}
}
