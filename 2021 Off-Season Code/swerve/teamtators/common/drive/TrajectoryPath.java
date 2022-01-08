package org.teamtators.common.drive;

import com.fasterxml.jackson.annotation.JsonIgnore;
import edu.wpi.first.wpilibj.spline.Spline;
import edu.wpi.first.wpilibj.trajectory.Trajectory;
import edu.wpi.first.wpilibj.trajectory.TrajectoryGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamtators.common.math.Pose2d;
import org.teamtators.common.math.Rotation;
import org.teamtators.common.math.Translation2d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class for specific instances of auto paths. Feeds into DriveTrajectoryFollower
 *
 * @author Jacob
 * @see DriveTrajectoryFollower
 * @since 2020 season
 */
public class TrajectoryPath {
    private final Logger logger = LoggerFactory.getLogger(TrajectoryPath.class);
    private List<PathPoint> points = Collections.emptyList(); //
    public boolean reversed = false; //
    public boolean isCubic = false; // a cubic trajectory doesn't seem to work for some reason
    public double maxVelocity = 30; //
    public double maxAcceleration = 30; //
    private TrajectoryGenerator.ControlVectorList controlVectors;
    private List<edu.wpi.first.wpilibj.geometry.Pose2d> poses;
    private TrajectoryParameterizer parameterizer;
    private Trajectory trajectory;
    private String name;

    public TrajectoryPath() {
    }

    @JsonIgnore
    public void setParameterizer(TrajectoryParameterizer parameterizer) {
        this.parameterizer = parameterizer;
    }

    @JsonIgnore
    public void setTrajectory(Trajectory trajectory) {
        this.trajectory = trajectory;
    }

    public Trajectory getTrajectory() {
        return trajectory;
    }

    public TrajectoryParameterizer getParameterizer() {
        return parameterizer;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public TrajectoryGenerator.ControlVectorList getControlVectors() {
        return controlVectors;
    }

    public List<edu.wpi.first.wpilibj.geometry.Pose2d> getPoses() {
        return poses;
    }

    public void setPoints(List<PathPoint> points) {
        this.points = points;
        controlVectors = new TrajectoryGenerator.ControlVectorList(getNumPoints());
        for (int i = 0; i < getNumPoints() - 1; i++) {
            var speed = 1.2 * points.get(i).point.getDistance(points.get(i + 1).point);
            if (i == 0) {
                controlVectors.add(this.points.get(i).getControlVector(speed));
            }
            controlVectors.add(this.points.get(i + 1).getControlVector(speed));
        }

        poses = new ArrayList<>();
        for (int i = 0; i<getNumPoints(); i++) {
            poses.add(this.points.get(i).getPose().toWpiLibPose());
        }
    }

    public void reverse () {
        logger.info("-------------REVERSING POINTS-----------------");
        points.forEach( p -> logger.info(p.toString()));
        for(PathPoint p: points) {
            p.reverse();
        }
        logger.info("---------------REVERSED--------------");
        points.forEach( p -> logger.info(p.toString()));
    }

    public List<PathPoint> getPoints() {
        return points;
    }

    /**
     * @return a list of PathPoints that are defined by user
     * @see PathPoint
     */
    public List<PathPoint> getWayPoints() {
        return points;
    }

    public PathPoint getPoint(int i) {
        return points.get(i);
    }

    public int getNumPoints() {
        return points.size();
    }

    public PathPoint getStart() {
        return points.get(0);
    }

    public PathPoint getEnd() {
        return points.get(getNumPoints() - 1);
    }

    public void setCubic(boolean isCubic) {
        this.isCubic = isCubic;
    }

    public boolean isCubic() {
        return isCubic;
    }

    public double getMaxAcceleration() {
        return maxAcceleration;
    }

    public double getMaxVelocity() {
        return maxVelocity;
    }

    public boolean getReversed() {
        return reversed;
    }

    /**
     * Class for holding Poses, but with potential headings that may be null
     */
    public static class PathPoint {
        private boolean hasRotation;
        private Translation2d point; //
        private Rotation rotation; //

        public PathPoint() {
            point = null;
            rotation = null;
            hasRotation = true;
        }

        public PathPoint(Translation2d point) {
            this.point = point;
            hasRotation = false;
        }

        public PathPoint(Translation2d point, Rotation rotation) {
            this(point);
            this.rotation = rotation;
            hasRotation = true;
        }

        public Pose2d getPose() {
            if (!hasRotation) return new Pose2d(point, new Rotation(0, 1));
            return new Pose2d(point, rotation);
        }

        public void setPoint(Translation2d point) {
            this.point = point;
        }

        public Translation2d getPoint() {
            return this.point;
        }

        public void setRotation(Rotation rotation) {
            this.rotation = rotation;
            hasRotation = true;
        }

        public Rotation getRotation() {
            if (hasRotation) return rotation;
            return null;
        }

        public void reverse() {
            if (hasRotation) rotation = rotation.inverse();
        }

        public String toString() {
            return "translation: " + point + ", rotation: " + getRotation();
        }

        /**
         * @param speed
         * @return position, speed, and acceleration in X direction
         */
        public double[] getXControlVector(double speed) {
            if (!hasRotation) return new double[]{this.point.getX(), 1, 0};
            return new double[]{this.point.getX(), this.rotation.cos() * speed, 0};
        }

        /**
         * @param speed
         * @return position, speed, and acceleration in Y direction
         */
        public double[] getYControlVector(double speed) {
            if (!hasRotation) return new double[]{this.point.getY(), 1, 0};
            return new double[]{this.point.getY(), this.rotation.sin() * speed, 0};
        }

        /**
         * @param speed
         * @return wpilib control vector
         */
        public Spline.ControlVector getControlVector(double speed) {
            return new Spline.ControlVector(getXControlVector(speed), getYControlVector(speed));
        }
    }
}
