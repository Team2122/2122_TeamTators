package frc.robot.subsystems.overwatch;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.trajectory.TrapezoidProfile;
import frc.robot.subsystems.overwatch.Graph.Node;
import java.util.Optional;

public final class OverwatchConstants {
    public static final double DEFAULT_MAX_VELOCITY = 6.0;
    public static final TrapezoidProfile MOTION_PROFILE =
            // fast profile for production
            new TrapezoidProfile(new TrapezoidProfile.Constraints(DEFAULT_MAX_VELOCITY, 13.0));
    // slow profile for safely testing new stuff
    // new TrapezoidProfile(new TrapezoidProfile.Constraints(DEFAULT_MAX_VELOCITY, 10));
    public static final TrapezoidProfile LOW_ACCELERATION_MOTION_PROFILE =
            new TrapezoidProfile(new TrapezoidProfile.Constraints(DEFAULT_MAX_VELOCITY, 6));
    public static final TrapezoidProfile SLOW_MOTION_PROFILE =
            new TrapezoidProfile(new TrapezoidProfile.Constraints(15, 15));
    public static final TrapezoidProfile BARGE_PROFILE =
            new TrapezoidProfile(new TrapezoidProfile.Constraints(60, 23));
    public static final TrapezoidProfile ALGAE_PROFILE =
            new TrapezoidProfile(new TrapezoidProfile.Constraints(6.0, 2.0));

    // new TrapezoidProfile(new TrapezoidProfile.Constraints(20, 10));

    /**
     * How much the lift height axis should be scaled down by
     *
     * <p>This is used to change how much a change in lift height should affect the time it takes to
     * travel from node to node
     *
     * <p>For example, say you want to move 1 radian (~57.3 degrees) and 1 inch. With
     * LIFT_HEIGHT_DIVISOR set to 1, this line would be a 45 degree angle on the lift height vs arm
     * angle graph, and take kind of a long time.
     *
     * <p>As LIFT_HEIGHT_DIVISOR increases, the slope of that line will decrease, getting closer to a
     * horizontal line on the graph so that lift height makes less and less of an impact on the timing
     * of the move.
     *
     * <p>This DOES NOT impact the actual height the lift goes to, just how much a change in height
     * affects the time to move across the graph.
     */
    public static final double LIFT_HEIGHT_DIVISOR = 10.0;

    public interface OverwatchPos {
        // value getters
        // override with functions returning their values
        public double liftHeightInches();

        public Rotation2d pivotAngle();

        public static OverwatchPos of(Rotation2d pivotAngle, double liftHeightInches) {
            return new OverwatchPos() {
                public double liftHeightInches() {
                    return liftHeightInches;
                }

                public Rotation2d pivotAngle() {
                    return pivotAngle;
                }
            };
        }

        /**
         * Return the distance from one node to another on the graph.
         *
         * <p>This will not take continuity into account. For example, the angular distance between -170
         * and 170 degrees will be assumed as 340 degrees, not 20 degrees.
         *
         * @param other The node to measure distance from.
         */
        public default double distanceFrom(OverwatchPos other) {
            var liftDelta = scaledHeightDelta(other);
            var angleDelta = this.angleDelta(other);
            return Math.hypot(liftDelta, angleDelta.getRadians());
        }

        /**
         * Returns the height delta between this node and the other node scaled by {@link
         * OverwatchPos#LIFT_HEIGHT_DIVISOR}.
         *
         * <p>See {@link OverwatchPos#LIFT_HEIGHT_DIVISOR} documentation for more information on when to
         * use this
         *
         * @param other The other node to measure distance from
         */
        public default double scaledHeightDelta(OverwatchPos other) {
            return this.heightDelta(other) / LIFT_HEIGHT_DIVISOR;
        }

        /**
         * Calculates the angle between the line drawn from this position to the other position and a
         * horizontal line drawn at this position
         *
         * <p>Keep in mind, this uses the SCALED lift height.
         *
         * @param other The node to measure angle from
         */
        public default Rotation2d angleFrom(OverwatchPos other) {
            return Rotation2d.fromRadians(
                    Math.atan2(scaledHeightDelta(other), angleDelta(other).getRadians()));
        }

        public default double heightDelta(OverwatchPos other) {
            return liftHeightInches() - other.liftHeightInches();
        }

        public default Rotation2d angleDelta(OverwatchPos other) {
            return Rotation2d.fromRadians(
                    // can't use Rotation2d.minus() since that wraps the output between -pi and pi
                    pivotAngle().getRadians() - other.pivotAngle().getRadians());
        }

        public default Node closestNode() {
            return closestNode(Double.MAX_VALUE, Double.MAX_VALUE).get();
        }

        public default Optional<Node> closestNode(
                double angleToleranceRads, double heightToleranceMeters) {
            Node ret = Node.HOME;
            for (Node node : Node.values()) {
                double currentNodeDist = distanceFrom(ret);
                double otherNodeDist = distanceFrom(node);

                if (otherNodeDist < currentNodeDist) {
                    ret = node;
                }
            }

            boolean liftHeightClose =
                    MathUtil.isNear(ret.liftHeightInches(), liftHeightInches(), heightToleranceMeters);
            boolean pivotAngleClose =
                    MathUtil.isNear(
                            ret.pivotAngle().getRadians(), pivotAngle().getRadians(), angleToleranceRads);

            if (liftHeightClose && pivotAngleClose) {
                return Optional.of(ret);
            } else {
                return Optional.empty();
            }
        }

        public default String string() {
            return "theta: " + pivotAngle().getDegrees() + ", height: " + liftHeightInches();
        }
    }
}
