package frc.robot.subsystems.overwatch;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.trajectory.TrapezoidProfile;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.units.measure.AngularVelocity;
import edu.wpi.first.wpilibj.Alert;
import edu.wpi.first.wpilibj.Alert.AlertType;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.util.Color8Bit;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.button.Trigger;
import frc.robot.Robot;
import frc.robot.subsystems.affector.Affector;
import frc.robot.subsystems.affector.Affector.AffectorStates;
import frc.robot.subsystems.overwatch.Graph.Node;
import frc.robot.subsystems.overwatch.Graph.RotationalDirection;
import frc.robot.subsystems.overwatch.OverwatchConstants.OverwatchPos;
import frc.robot.subsystems.overwatch.lift.Lift;
import frc.robot.subsystems.overwatch.lift.LiftConstants;
import frc.robot.subsystems.overwatch.pivot.Pivot;
import frc.robot.subsystems.overwatch.pivot.PivotConstants;
import frc.robot.subsystems.overwatch.pivot.PivotIO.PivotControlMode;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.littletonrobotics.junction.AutoLogOutput;
import org.littletonrobotics.junction.Logger;
import org.littletonrobotics.junction.mechanism.LoggedMechanism2d;
import org.littletonrobotics.junction.mechanism.LoggedMechanismLigament2d;
import org.teamtators.util.Subsystem;
import org.teamtators.util.TatorMath;

public class Overwatch extends Subsystem {
    public enum RotationType {
        CLOCKWISE,
        COUNTER_CLOCKWISE,
        SHORTEST
    }

    private final Pivot pivot = new Pivot();
    private final Lift lift = new Lift();

    private Alert pathNotFoundAlert = new Alert("Overwatch Path Not Found!", AlertType.kWarning);

    @AutoLogOutput private Node finalDestination = Node.FIRST_DEST;

    private LoggedMechanism2d visualization;
    private LoggedMechanismLigament2d liftLigament;
    private LoggedMechanismLigament2d pivotLigament;

    private GraphVisualizer graphVisualizer;

    private boolean initDone = false;

    private TrapezoidProfile defaultMotionProfile = OverwatchConstants.MOTION_PROFILE;
    private TrapezoidProfile.State momentarySetpoint = new TrapezoidProfile.State();
    private SequenceInformation sequenceInformation;

    private Affector affector;

    public Overwatch() {
        visualization = new LoggedMechanism2d(Units.inchesToMeters(36), Units.inchesToMeters(36));
        var liftRoot = visualization.getRoot("Overwatch", Units.inchesToMeters(18), 0.2);
        liftLigament = liftRoot.append(new LoggedMechanismLigament2d("Lift", 0, 90));
        pivotLigament =
                liftLigament.append(
                        new LoggedMechanismLigament2d(
                                "Pivot", Units.inchesToMeters(20), -90, 10, new Color8Bit(0, 0, 255)));

        graphVisualizer = new GraphVisualizer();

        var currentPosition =
                OverwatchPos.of(TatorMath.wrapRotation2d(pivot.getAngle()), lift.getHeightInches());
        for (var direction : RotationalDirection.values()) {
            Graph.pathfind(currentPosition, Node.TRANSPORT_SAFETY, direction);
            Graph.pathfind(currentPosition, Node.HOME, direction);
            Graph.pathfind(Node.HOME, Node.L4PREP, direction);
            Graph.pathfind(Node.HOME, Node.L3PREP, direction);
            Graph.pathfind(Node.HOME, Node.L2PREP, direction);
            Graph.pathfind(Node.HOME, Node.CORAL_PICK, direction);
            Graph.pathfind(Node.TRANSPORT_SAFETY, Node.L4PREP, direction);
            Graph.pathfind(Node.TRANSPORT_SAFETY, Node.L3PREP, direction);
            Graph.pathfind(Node.TRANSPORT_SAFETY, Node.L2PREP, direction);
            Graph.pathfind(Node.L4PREP, Node.L4PLACE, direction);
            Graph.pathfind(Node.L3PREP, Node.L3PLACE, direction);
            Graph.pathfind(Node.L3PREP, Node.L3PLACE, direction);
            Graph.pathfind(Node.L4PLACE, Node.HOME, direction);
            Graph.pathfind(Node.L3PLACE, Node.HOME, direction);
            Graph.pathfind(Node.L3PLACE, Node.HOME, direction);
        }
    }

    @Override
    public void configure() {
        affector = Robot.getInstance().affector;

        affector
                .hasAlgae
                .onTrue(
                        Commands.runOnce(
                                () -> {
                                    pivot.setControlMode(PivotControlMode.HOLDING_ALGAE);
                                    defaultMotionProfile = OverwatchConstants.ALGAE_PROFILE;
                                }))
                .onFalse(
                        Commands.runOnce(
                                () -> {
                                    pivot.setControlMode(PivotControlMode.NORMAL_OPERATION);
                                    defaultMotionProfile = OverwatchConstants.MOTION_PROFILE;
                                }));
    }

    @Override
    public void doPeriodic() {
        if (!initDone) {
            lift.initEncoder();
            initDone = true;
        }
    }

    Command emptyCommand = Commands.none().withName("nothing.");

    @Override
    public void log() {
        Logger.recordOutput("Overwatch/PathNotFound", pathNotFoundAlert.get());
        // use the below code to visualize nodes with Translation2d's in AdvantageScope
        // not useful at the moment since we're just using manual control
        // graphVisualizer.visualize(
        //         finalDestination, TatorMath.wrapRotation2d(pivot.getAngle()),
        // lift.getHeightInches());
        liftLigament.setLength(Units.inchesToMeters(lift.getHeightInches()));
        pivotLigament.setAngle(pivot.getAngle().minus(Rotation2d.kCCW_90deg));
        Logger.recordOutput("Overwatch/Mechanism", visualization);
        Logger.recordOutput(
                "Overwatch/CurrentCommand", getPossibleCommand().orElse(emptyCommand).getName());
        String destinationName;
        try {
            destinationName = ((Node) sequenceInformation.destination).toString();
        } catch (ClassCastException e) {
            destinationName = sequenceInformation.destination.string();
        } catch (Exception e) {
            destinationName = "undefined";
        }
        Logger.recordOutput("Overwatch/MomentaryDestination", destinationName);
    }

    public Node getFinalDestination() {
        return finalDestination;
    }

    public Command goTo(Node node) {
        return goTo(node, RotationType.SHORTEST);
    }

    public AngularVelocity getPivotVelocity() {
        return pivot.getVelocity();
    }

    public Command goTo(Node node, RotationType rotationType) {
        return this.defer(
                () -> {
                    List<OverwatchPos> path = List.of();
                    RotationalDirection dir;

                    OverwatchPos currentPosition = OverwatchPos.of(pivot.getAngle(), lift.getHeightInches());

                    Optional<Node> maybeNearestNode =
                            currentPosition.closestNode(
                                    PivotConstants.ALLOWED_ERROR.getRadians(), LiftConstants.ERROR_INCHES);
                    if (maybeNearestNode.isPresent()) {
                        currentPosition = maybeNearestNode.get();
                    }

                    dir =
                            switch (rotationType) {
                                case CLOCKWISE -> RotationalDirection.CLOCKWISE;
                                case COUNTER_CLOCKWISE -> RotationalDirection.COUNTER_CLOCKWISE;
                                default -> {
                                    // choose whichever direction has the shortest time when
                                    // going straight from node A to node B
                                    double travelTimeClockwise =
                                            Graph.travelTime(currentPosition, node, RotationalDirection.CLOCKWISE);
                                    double travelTimeCounterClockwise =
                                            Graph.travelTime(
                                                    currentPosition, node, RotationalDirection.COUNTER_CLOCKWISE);
                                    if (travelTimeCounterClockwise < travelTimeClockwise) {
                                        yield RotationalDirection.COUNTER_CLOCKWISE;
                                    } else {
                                        yield RotationalDirection.CLOCKWISE;
                                    }
                                }
                            };
                    System.out.println("goTo " + node.name() + " from " + currentPosition + " dir: " + dir);
                    pathNotFoundAlert.set(false);
                    path =
                            Graph.pathfind(currentPosition, node, dir)
                                    .orElseGet(
                                            () -> {
                                                pathNotFoundAlert.set(true);
                                                return List.of();
                                            });
                    System.out.println(Arrays.toString(path.toArray()));
                    finalDestination = node;

                    return followSequence(
                            path,
                            switch (dir) {
                                case CLOCKWISE -> RotationType.CLOCKWISE;
                                case COUNTER_CLOCKWISE -> RotationType.COUNTER_CLOCKWISE;
                            });
                });
    }

    /**
     * SequenceInformation is used just to store information in a member variable that lambda
     * expressions inside of {@link Command}s can mutate, since local variables cannot be mutated
     * inside of a lambda expression.
     */
    private class SequenceInformation {
        OverwatchPos destination;
        OverwatchPos startingPosition;

        int nodeIndex;
        Rotation2d edgeAngle;
        RotationType rotationType;

        TrapezoidProfile.State initialState;
        TrapezoidProfile.State finalState;
        double edgeMagnitude;

        double timeToFinish;
        boolean isFinished = false;

        Timer timer = new Timer();

        public SequenceInformation(
                List<OverwatchPos> nodes,
                RotationType rotationType_,
                TrapezoidProfile motionProfile,
                OverwatchFollowerMethod followerMethod) {
            nodeIndex = -1; // forces the next to line to update nodeIndex to 0
            updateSequenceInformation(this, nodes, rotationType_, motionProfile, followerMethod);
        }
    }

    private void updateSequenceInformation(
            SequenceInformation info,
            List<OverwatchPos> nodes,
            RotationType rotationType_,
            TrapezoidProfile motionProfile,
            OverwatchFollowerMethod followerMethod) {

        if (info.nodeIndex < nodes.size() - 1) {
            info.nodeIndex++;
            info.isFinished = false;
        } else {
            info.isFinished = true;
        }
        OverwatchPos node = nodes.get(info.nodeIndex);

        if (info.nodeIndex > 0) {
            info.startingPosition = nodes.get(info.nodeIndex - 1);
        } else {
            info.startingPosition = OverwatchPos.of(pivot.getAngle(), lift.getHeightInches());
        }

        if (info.nodeIndex == 0 || info.nodeIndex == nodes.size() - 1) {
            if (affector.getState() == AffectorStates.PICKED_ALGAE) {
                pivot.setControlMode(PivotControlMode.HOLDING_ALGAE);
            } else {
                pivot.setControlMode(PivotControlMode.NORMAL_OPERATION);
            }
        } else if (followerMethod == OverwatchFollowerMethod.NONSTOP) {
            pivot.setControlMode(PivotControlMode.NO_VELOCITY_FEEDFORWARD);
        }

        info.rotationType = rotationType_;
        if (Math.abs(node.angleDelta(info.startingPosition).getDegrees()) < 5) {
            info.rotationType = RotationType.SHORTEST;
        }
        RotationalDirection dir =
                switch (info.rotationType) {
                    case CLOCKWISE -> RotationalDirection.CLOCKWISE;
                    case COUNTER_CLOCKWISE -> RotationalDirection.COUNTER_CLOCKWISE;
                    case SHORTEST -> {
                        double clockwiseDist =
                                Graph.repositionNode(info.startingPosition, node, RotationalDirection.CLOCKWISE)
                                        .distanceFrom(info.startingPosition);
                        double counterClockwiseDist =
                                Graph.repositionNode(
                                                info.startingPosition, node, RotationalDirection.COUNTER_CLOCKWISE)
                                        .distanceFrom(info.startingPosition);
                        if (clockwiseDist < counterClockwiseDist) {
                            yield RotationalDirection.CLOCKWISE;
                        } else {
                            yield RotationalDirection.COUNTER_CLOCKWISE;
                        }
                    }
                };

        info.destination = Graph.repositionNode(info.startingPosition, node, dir);

        info.edgeAngle = info.destination.angleFrom(info.startingPosition);

        info.edgeMagnitude = info.destination.distanceFrom(info.startingPosition);

        if (info.nodeIndex > 0 && followerMethod == OverwatchFollowerMethod.NONSTOP) {
            info.initialState = new TrapezoidProfile.State(0, OverwatchConstants.DEFAULT_MAX_VELOCITY);
        } else {
            info.initialState = new TrapezoidProfile.State();
        }

        if (info.nodeIndex < nodes.size() - 1 && followerMethod == OverwatchFollowerMethod.NONSTOP) {
            info.finalState =
                    new TrapezoidProfile.State(info.edgeMagnitude, OverwatchConstants.DEFAULT_MAX_VELOCITY);
        } else {
            info.finalState = new TrapezoidProfile.State(info.edgeMagnitude, 0);
        }

        motionProfile.calculate(0.02, info.initialState, info.finalState);
        info.timeToFinish = motionProfile.totalTime();

        info.timer.restart();
    }

    /** Whether or not the {@link Overwatch} should stop at the intermediate nodes along its path */
    public enum OverwatchFollowerMethod {
        NONSTOP,
        STOPPING
    }

    public Command followSequence(List<OverwatchPos> nodes, RotationType rotationType) {
        return followSequence(
                nodes, rotationType, OverwatchConstants.MOTION_PROFILE, OverwatchFollowerMethod.STOPPING);
    }

    public Command followSequence(
            List<OverwatchPos> nodes, RotationType rotationType, TrapezoidProfile motionProfile) {
        return followSequence(nodes, rotationType, motionProfile, OverwatchFollowerMethod.STOPPING);
    }

    public Command followSequence(
            List<OverwatchPos> nodes, RotationType rotationType, OverwatchFollowerMethod followerMethod) {
        return followSequence(nodes, rotationType, OverwatchConstants.MOTION_PROFILE, followerMethod);
    }

    public Command followSequence(
            List<OverwatchPos> nodes,
            RotationType rotationType_,
            TrapezoidProfile motionProfile,
            OverwatchFollowerMethod followerMethod) {
        return this.run(
                        () -> {
                            momentarySetpoint =
                                    motionProfile.calculate(
                                            sequenceInformation.timer.get(),
                                            sequenceInformation.initialState,
                                            sequenceInformation.finalState);
                            if (momentarySetpoint.position > sequenceInformation.edgeMagnitude) {
                                momentarySetpoint = sequenceInformation.finalState;
                            }

                            var liftHeight =
                                    MathUtil.interpolate(
                                            sequenceInformation.startingPosition.liftHeightInches(),
                                            sequenceInformation.destination.liftHeightInches(),
                                            momentarySetpoint.position / sequenceInformation.edgeMagnitude);
                            double liftVelocity =
                                    momentarySetpoint.velocity * sequenceInformation.edgeAngle.getSin();
                            lift.goTo(liftHeight, liftVelocity);

                            var pivotAngle =
                                    MathUtil.interpolate(
                                            sequenceInformation.startingPosition.pivotAngle().getRadians(),
                                            sequenceInformation.destination.pivotAngle().getRadians(),
                                            momentarySetpoint.position / sequenceInformation.edgeMagnitude);

                            double pivotVelocity =
                                    momentarySetpoint.velocity * sequenceInformation.edgeAngle.getCos();
                            pivot.goTo(
                                    Rotation2d.fromRadians(MathUtil.angleModulus(pivotAngle)),
                                    Units.radiansToRotations(pivotVelocity));

                            if (sequenceInformation.timer.get() > sequenceInformation.timeToFinish) {
                                updateSequenceInformation(
                                        sequenceInformation, nodes, rotationType_, motionProfile, followerMethod);
                            }
                        })
                .until(() -> sequenceInformation.isFinished)
                .beforeStarting(
                        () -> {
                            sequenceInformation =
                                    new SequenceInformation(nodes, rotationType_, motionProfile, followerMethod);
                            try {
                                finalDestination = (Node) nodes.get(nodes.size() - 1);
                            } catch (Exception e) {
                                if (!DriverStation.isFMSAttached()) {
                                    throw (e);
                                } else {
                                    e.printStackTrace();
                                }
                            }
                        });
    }

    public Command recover(Node pos) {
        return recover(pos, 40, RotationType.CLOCKWISE);
    }

    public Command recover(Node pos, RotationType rotationType) {
        return recover(pos, 40, rotationType);
    }

    public Command recover(Node pos, double liftHeight) {
        return recover(pos, liftHeight, RotationType.CLOCKWISE);
    }

    public Command recover(Node pos, double liftHeight, RotationType rotationType) {
        return this.defer(
                () -> {
                    OverwatchPos firstPosition = OverwatchPos.of(pivot.getAngle(), liftHeight);
                    OverwatchPos secondPosition =
                            OverwatchPos.of(pos.pivotAngle(), firstPosition.liftHeightInches());

                    return this.followSequence(List.of(firstPosition, secondPosition, pos), rotationType);
                });
    }

    public Trigger isAt(Node node) {
        return isNear(node, PivotConstants.ALLOWED_ERROR, LiftConstants.ERROR_INCHES);
    }

    public Trigger isNear(Node node, Rotation2d pivotTolerance, double liftToleranceInches) {
        return new Trigger(
                () ->
                        pivot.isNear(node.pivotAngle(), pivotTolerance)
                                && lift.isNear(node.liftHeightInches(), liftToleranceInches)
                                && finalDestination == node);
    }

    @Override
    public boolean getHealth() {
        // overwatcher is never bad due to not directly having any motors/sensors to its name.
        // only lift or pivot can be bad.
        return true;
    }
}
