package frc.robot.subsystems.swerve;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import frc.robot.constants.Constants;
import frc.robot.constants.FieldConstants;
import java.util.ArrayList;
import java.util.List;

/**
 * RepulsorFieldPlanner houses all of the logic for obstacle avoidance during alignment by creating
 * obstacles representing various field elements and creating a vector field from those defined
 * obstacles
 */
class RepulsorFieldPlanner {
    /** An Obstacle represents something the robot should avoid hitting */
    private abstract static class Obstacle {
        double strength;
        boolean positive;

        public Obstacle(double strength, boolean positive) {
            this.strength = strength;
            this.positive = positive;
        }

        /**
         * Get the force pushing the robot away from this obstacle
         *
         * @return The force pushing the robot away from this obstacle
         */
        public abstract Translation2d getForceAtPosition(Translation2d position, Translation2d target);

        /**
         * Calculate how much the magnitude of the force vector driving the robot away from this object
         * is scaled by due to the robot's distance from the obstacle
         */
        protected double distToForceMag(double dist, double maxRange) {
            if (Math.abs(dist) > maxRange) {
                return 0;
            }
            if (MathUtil.isNear(0, dist, 1e-2)) {
                dist = 1e-2;
            }
            var forceMag = strength / (dist * dist);
            forceMag -= strength / (maxRange * maxRange);
            forceMag *= positive ? 1 : -1;
            return forceMag;
        }
    }

    /**
     * A TeardropObstacle is an {@link Obstacle} that is shaped like a teardrop (circular with a
     * pointed tail)
     */
    private static class TeardropObstacle extends Obstacle {
        final Translation2d loc;
        final double primaryMaxRange;
        final double primaryRadius;
        final double tailStrength;
        final double tailLength;

        /**
         * Construct a {@link TeardropObstacle}
         *
         * @param loc The x and y coordinates of the center of the circular region of the teardrop on
         *     the field
         * @param primaryStrength How strong the robot should drive away from the circular portion of
         *     this obstacle
         * @param primaryMaxRange How far the robot can be from this obstacle before the obstacle
         *     avoidance kicks in
         * @param primaryRadius The radius of the circular portion of this obstacle
         * @param tailStrength How strong the robot should drive away from the pointed end of this
         *     obstacle
         * @param tailLength How long the pointed end of the teardrop should be
         */
        public TeardropObstacle(
                Translation2d loc,
                double primaryStrength,
                double primaryMaxRange,
                double primaryRadius,
                double tailStrength,
                double tailLength) {
            super(primaryStrength, true);
            this.loc = loc;
            this.primaryMaxRange = primaryMaxRange;
            this.primaryRadius = primaryRadius;
            this.tailStrength = tailStrength;
            this.tailLength = tailLength + primaryMaxRange;
        }

        public Translation2d getForceAtPosition(Translation2d position, Translation2d target) {
            var targetToLoc = loc.minus(target);
            var targetToLocAngle = targetToLoc.getAngle();
            var sidewaysPoint = new Translation2d(tailLength, targetToLoc.getAngle()).plus(loc);

            var positionToLocation = position.minus(loc);
            var positionToLocationDistance = positionToLocation.getNorm();
            Translation2d outwardsForce;
            if (positionToLocationDistance <= primaryMaxRange) {
                outwardsForce =
                        new Translation2d(
                                distToForceMag(
                                        Math.max(positionToLocationDistance - primaryRadius, 0),
                                        primaryMaxRange - primaryRadius),
                                positionToLocation.getAngle());
            } else {
                outwardsForce = Translation2d.kZero;
            }

            var positionToLine = position.minus(loc).rotateBy(targetToLocAngle.unaryMinus());
            var distanceAlongLine = positionToLine.getX();

            Translation2d sidewaysForce;
            var distanceScalar = distanceAlongLine / tailLength;
            if (distanceScalar >= 0 && distanceScalar <= 1) {
                var secondaryMaxRange =
                        MathUtil.interpolate(primaryMaxRange, 0, distanceScalar * distanceScalar);
                var distanceToLine = Math.abs(positionToLine.getY());
                if (distanceToLine <= secondaryMaxRange) {
                    double strength;
                    if (distanceAlongLine < primaryMaxRange) {
                        strength = tailStrength * (distanceAlongLine / primaryMaxRange);
                    } else {
                        strength =
                                -tailStrength * distanceAlongLine / (tailLength - primaryMaxRange)
                                        + tailLength * tailStrength / (tailLength - primaryMaxRange);
                    }
                    strength *= 1 - distanceToLine / secondaryMaxRange;

                    var sidewaysMag = tailStrength * strength * (secondaryMaxRange - distanceToLine);
                    // flip the sidewaysMag based on which side of the goal-sideways circle the robot is on
                    var sidewaysTheta =
                            target.minus(position).getAngle().minus(position.minus(sidewaysPoint).getAngle());
                    sidewaysForce =
                            new Translation2d(
                                    sidewaysMag * Math.signum(Math.sin(sidewaysTheta.getRadians())),
                                    targetToLocAngle.rotateBy(Rotation2d.kCCW_90deg));
                } else {
                    sidewaysForce = Translation2d.kZero;
                }
            } else {
                sidewaysForce = Translation2d.kZero;
            }

            return outwardsForce.plus(sidewaysForce);
        }
    }

    /**
     * An {@link Obstacle} that is a flat line parallel to the y-axis of the field and extends
     * infinitely along the x-axis
     */
    static class HorizontalObstacle extends Obstacle {
        final double y;
        final double maxRange;

        /**
         * Construct a {@link HorizontalObstacle}
         *
         * @param y The y coordinate of this line
         * @param strength How strongly the robot should drive away from this obstacle
         * @param maxRange How far the robot can be from this obstacle before the obstacle avoidance
         *     kicks in
         * @param positive Which direction the obstacle is facing (true means drive the robot towards +y
         *     to get away, false means drive the robot towards -y get away)
         */
        public HorizontalObstacle(double y, double strength, double maxRange, boolean positive) {
            super(strength, positive);
            this.y = y;
            this.maxRange = maxRange;
        }

        public Translation2d getForceAtPosition(Translation2d position, Translation2d target) {
            var dist = Math.abs(position.getY() - y);
            if (dist > maxRange) {
                return Translation2d.kZero;
            }
            return new Translation2d(0, distToForceMag(y - position.getY(), maxRange));
        }
    }

    /**
     * An {@link Obstacle} that is a vertical line parallel to the x-axis of the field and extends
     * infinitely along the y-axis
     */
    private static class VerticalObstacle extends Obstacle {
        final double x;
        final double maxRange;

        /**
         * Construct a {@link VerticalObstacle}
         *
         * @param x The x coordinate of this line
         * @param strength How strongly the robot should drive away from this obstacle
         * @param maxRange How far the robot can be from this obstacle before the obstacle avoidance
         *     kicks in
         * @param positive Which direction the obstacle is facing (true means drive the robot towards +x
         *     to get away, false means drive the robot towards -x to get away)
         */
        public VerticalObstacle(double x, double strength, double maxRange, boolean positive) {
            super(strength, positive);
            this.x = x;
            this.maxRange = maxRange;
        }

        public Translation2d getForceAtPosition(Translation2d position, Translation2d target) {
            var dist = Math.abs(position.getX() - x);
            if (dist > maxRange) {
                return Translation2d.kZero;
            }
            return new Translation2d(distToForceMag(x - position.getX(), maxRange), 0);
        }
    }

    /** An {@link Obstacle} that is a straight line from point to point */
    private static class LineObstacle extends Obstacle {
        final Translation2d startPoint;
        final Translation2d endPoint;
        final double length;
        final Rotation2d angle;
        final Rotation2d inverseAngle;
        final double maxRange;

        /**
         * Construct a {@link LineObstacle}
         *
         * @param start The first point making up the line
         * @param end The second point making up the line
         * @param strength
         * @param strength How strongly the robot should drive away from this obstacle
         * @param maxRange How far the robot can be from this obstacle before the obstacle avoidance
         *     kicks in
         */
        public LineObstacle(Translation2d start, Translation2d end, double strength, double maxRange) {
            super(strength, true);
            startPoint = start;
            endPoint = end;
            var delta = end.minus(start);
            length = delta.getNorm();
            angle = delta.getAngle();
            inverseAngle = angle.unaryMinus();
            this.maxRange = maxRange;
        }

        @Override
        public Translation2d getForceAtPosition(Translation2d position, Translation2d target) {
            var positionToLine = position.minus(startPoint).rotateBy(inverseAngle);
            if (positionToLine.getX() > 0 && positionToLine.getX() < length) {
                return new Translation2d(
                        Math.copySign(distToForceMag(positionToLine.getY(), maxRange), positionToLine.getY()),
                        angle.rotateBy(Rotation2d.kCCW_90deg));
            }
            Translation2d closerPoint;
            if (positionToLine.getX() <= 0) {
                closerPoint = startPoint;
            } else {
                closerPoint = endPoint;
            }
            return new Translation2d(
                    distToForceMag(position.getDistance(closerPoint), maxRange),
                    position.minus(closerPoint).getAngle());
        }
    }

    // Put all of the obstacles on the field
    static final double SOURCE_X = 1.75;
    static final double SOURCE_Y = 1.25;
    static final List<Obstacle> FIELD_OBSTACLES =
            List.of(
                    // Reef
                    new TeardropObstacle(FieldConstants.BLUE_REEF, 1, 2.5, .95, 3, 2),
                    new TeardropObstacle(FieldConstants.RED_REEF, 1, 2.5, .95, 3, 2),
                    // Walls
                    new HorizontalObstacle(0.0, 0.5, .5, true),
                    new HorizontalObstacle(FieldConstants.FIELD_WIDTH_METERS, 0.5, .5, false),
                    new VerticalObstacle(0.0, 0.5, .5, true),
                    new VerticalObstacle(FieldConstants.FIELD_LENGTH_METERS, 0.5, .5, false),
                    // Sources
                    new LineObstacle(new Translation2d(0, SOURCE_Y), new Translation2d(SOURCE_X, 0), .5, .5),
                    new LineObstacle(
                            new Translation2d(0, FieldConstants.FIELD_WIDTH_METERS - SOURCE_Y),
                            new Translation2d(SOURCE_X, FieldConstants.FIELD_WIDTH_METERS),
                            .5,
                            .5),
                    new LineObstacle(
                            new Translation2d(FieldConstants.FIELD_LENGTH_METERS, SOURCE_Y),
                            new Translation2d(FieldConstants.FIELD_LENGTH_METERS - SOURCE_X, 0),
                            .5,
                            .5),
                    new LineObstacle(
                            new Translation2d(
                                    FieldConstants.FIELD_LENGTH_METERS, FieldConstants.FIELD_WIDTH_METERS - SOURCE_Y),
                            new Translation2d(
                                    FieldConstants.FIELD_LENGTH_METERS - SOURCE_X, FieldConstants.FIELD_WIDTH_METERS),
                            .5,
                            .5));

    // The target position of the robot
    private Translation2d goal = new Translation2d(1, 1);

    // How many arrows should be drawn along the x-axis
    private static final int ARROWS_X = 40;
    // How many arrows should be drawn along the y-axis
    private static final int ARROWS_Y = 20;

    // Cached goal to save on computing arrows for the same goal in the
    // various get<vector>Arrows() functions
    private Translation2d lastGoal;

    private Pose2d[] arrowList;

    /**
     * A grid of arrows meant to be drawn in Advantage Scope that represent the force vectors for each
     * {@link Obstacle}
     *
     * @return A grid of arrows meant to be drawn in Advantage Scope that represent the force vectors
     *     for each {@link Obstacle}
     */
    public Pose2d[] getObstacleArrows() {
        if (goal.equals(lastGoal)) {
            return arrowList;
        }
        var list = new ArrayList<Pose2d>();
        for (int x = 0; x <= ARROWS_X; x++) {
            for (int y = 0; y <= ARROWS_Y; y++) {
                var translation =
                        new Translation2d(
                                x * FieldConstants.FIELD_LENGTH_METERS / ARROWS_X,
                                y * FieldConstants.FIELD_WIDTH_METERS / ARROWS_Y);
                var force = getObstacleForce(translation, goal);
                if (force.getNorm() > 1e-6) {
                    var rotation = force.getAngle();
                    list.add(new Pose2d(translation, rotation));
                }
            }
        }
        lastGoal = goal;
        arrowList = list.toArray(new Pose2d[0]);
        return arrowList;
    }

    /**
     * A grid of arrows meant to be drawn in Advantage Scope that represent the force vectors driving
     * the robot towards the given goal
     *
     * @return A grid of arrows meant to be drawn in Advantage Scope that represent the force vectors
     *     driving the robot towards the given goal
     */
    public Pose2d[] getGoalArrows() {
        if (goal.equals(lastGoal)) {
            return arrowList;
        }
        var list = new ArrayList<Pose2d>();
        for (int x = 0; x <= ARROWS_X; x++) {
            for (int y = 0; y <= ARROWS_Y; y++) {
                var translation =
                        new Translation2d(
                                x * FieldConstants.FIELD_LENGTH_METERS / ARROWS_X,
                                y * FieldConstants.FIELD_WIDTH_METERS / ARROWS_Y);
                var force = getGoalForce(translation, goal);
                if (force.getNorm() > 1e-6) {
                    var rotation = force.getAngle();
                    list.add(new Pose2d(translation, rotation));
                }
            }
        }
        lastGoal = goal;
        arrowList = list.toArray(new Pose2d[0]);
        return arrowList;
    }

    /**
     * A grid of arrows meant to be drawn in Advantage Scope that represent the combined force vectors
     * that will drive to the goal while avoiding obstacles
     *
     * @return A grid of arrows meant to be drawn in Advantage Scope that represent the combined force
     *     vectors that will drive to the goal while avoiding obstacles
     */
    public Pose2d[] getTotalArrows() {
        if (goal.equals(lastGoal)) {
            return arrowList;
        }
        var list = new ArrayList<Pose2d>();
        for (int x = 0; x <= ARROWS_X; x++) {
            for (int y = 0; y <= ARROWS_Y; y++) {
                var translation =
                        new Translation2d(
                                x * FieldConstants.FIELD_LENGTH_METERS / ARROWS_X,
                                y * FieldConstants.FIELD_WIDTH_METERS / ARROWS_Y);
                var force = getForce(translation, goal);
                if (force.getNorm() > 1e-6) {
                    var rotation = force.getAngle();
                    list.add(new Pose2d(translation, rotation));
                }
            }
        }
        lastGoal = goal;
        arrowList = list.toArray(new Pose2d[0]);
        return arrowList;
    }

    /**
     * Get the force that will drive the robot toward the goal in a straight line, i.e. without
     * considering any {@link Obstacle}s
     *
     * @return The force that will drive the robot toward the goal in a straight line, i.e. without
     *     considering any {@link Obstacle}s
     */
    Translation2d getGoalForce(Translation2d curLocation, Translation2d goal) {
        var displacement = goal.minus(curLocation);
        if (displacement.getNorm() == 0) {
            return new Translation2d();
        }
        var direction = displacement.getAngle();
        var mag = (1 + 1.0 / (1e-6 + displacement.getNorm()));
        return new Translation2d(mag, direction);
    }

    /**
     * Get the overall compounded force of every obstacle upon the robot
     *
     * @return The overall compounded force of every obstacle upon the robot
     */
    Translation2d getObstacleForce(Translation2d curLocation, Translation2d target) {
        var force = Translation2d.kZero;
        for (Obstacle obs : FIELD_OBSTACLES) {
            force = force.plus(obs.getForceAtPosition(curLocation, target));
        }
        return force;
    }

    /**
     * Get the combined force that will drive the robot towards the goal while avoiding any obstacles
     *
     * @return The combined force that will drive the robot towards the goal while avoiding any
     *     obstacles
     */
    Translation2d getForce(Translation2d curLocation, Translation2d target) {
        return getGoalForce(curLocation, target).plus(getObstacleForce(curLocation, target));
    }

    /**
     * Replan the overall vector field to drive towards this goal
     *
     * @param goal The point in 2d field space the robot should drive towards
     */
    public void setGoal(Translation2d goal) {
        this.goal = goal;
    }

    /** A record containing all information needed to follow the calculated vector field */
    public record RepulsorSample(Translation2d intermediateGoal, double vx, double vy) {}

    /**
     * Calculates a {@link RepulsorSample} that will drive the robot to the provided goal, given the
     * configured obstacles. This should be called periodically in order to get a vector matching the
     * robot as it moves
     *
     * @return A {@link RepulsorSample} that will drive the robot to the provided goal, given the
     *     configured obstacles
     * @param curTrans Where the robot is on the field
     * @param maxSpeed The maximum speed of the robot when following the vector field
     * @param slowdownDistance How far the robot should be from the goal before slowing down to a
     *     speed of 0
     */
    public RepulsorSample sampleField(
            Translation2d curTrans, double maxSpeed, double slowdownDistance) {
        var err = curTrans.minus(goal);
        var netForce = getForce(curTrans, goal);

        double stepSize_m;
        if (err.getNorm() < slowdownDistance) {
            stepSize_m =
                    MathUtil.interpolate(
                            0, maxSpeed * Constants.kTickPeriod, err.getNorm() / slowdownDistance);
        } else {
            stepSize_m = maxSpeed * Constants.kTickPeriod;
        }
        var step = new Translation2d(stepSize_m, netForce.getAngle());
        return new RepulsorSample(
                curTrans.plus(step),
                step.getX() / Constants.kTickPeriod,
                step.getY() / Constants.kTickPeriod);
    }
}
