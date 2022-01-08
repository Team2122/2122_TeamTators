package org.teamtators.common.drive;

import edu.wpi.first.wpilibj.controller.RamseteController;
import edu.wpi.first.wpilibj.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj.kinematics.DifferentialDriveKinematics;
import edu.wpi.first.wpilibj.kinematics.DifferentialDriveWheelSpeeds;
import edu.wpi.first.wpilibj.trajectory.Trajectory;
import edu.wpi.first.wpilibj.util.Units;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.control.AbstractUpdatable;
import org.teamtators.common.datalogging.DataCollector;
import org.teamtators.common.datalogging.LogDataProvider;
import org.teamtators.common.math.Pose2d;
import org.teamtators.common.math.Translation2d;

import java.util.Arrays;
import java.util.List;

public class DriveTrajectoryFollower extends AbstractUpdatable implements Configurable<DriveTrajectoryFollower.Config> {
//    private final Drive drive;
    private Config config;
    private Pose2d currentPose;
    private TrajectoryPath path;
    private Trajectory trajectory;
    private DifferentialDriveKinematics driveKinematics;
    private RamseteController controller;
    private PoseEstimator poseEstimator;
    private DataProvider logDataProvider = new DataProvider();
    private double totalTime;
    private boolean finished;

//    public DriveTrajectoryFollower(Drive drive) {
//        super("DriveTrajectoryFollower");
//        this.drive = drive;
//        poseEstimator = this.drive.getPoseEstimator();
//        reset();
//    }

    public void setDriveKinematics(DifferentialDriveKinematics kinematics) {
        this.driveKinematics = kinematics;
    }


    @Override
    public void start() {
        if (!running) {
            running = true;
            reset();
            if (config.logData)
                DataCollector.getDataCollector().startProvider(logDataProvider);
        }
    }

    @Override
    public void stop() {
        super.stop();
        finished = true;
        if (config.logData)
            DataCollector.getDataCollector().stopProvider(logDataProvider);
    }

    @Override
    protected void doUpdate(double delta) {
        if (isFinished()) {
//            drive.stop();
            return;
        }
//        currentPose = drive.getPose();
//        drive.setSpeeds(getWheelSpeeds());
//        logger.info("distance parameterized? {}", config.distanceParameterized);
        if (config.distanceParameterized) {
            path.getParameterizer().updateDistance(poseEstimator.getTotalDistance());
            totalTime = path.getParameterizer().getEstimatedTime(poseEstimator.getTotalDistance());
            if (getRemainingDistance() < config.endThreshold)
                stop();
        } else {
            totalTime += delta;
//            logger.info("current time into trajectory {}, time left {}", totalTime, getTimeLeft());
            if (getTimeLeft() < config.endThreshold)
                stop();
        }
    }

    public Pose2d getCurrentGoalPose() {
        return Pose2d.fromWpiLibPose(getGoalState().poseMeters);
    }

    public boolean isFinished() {
        return finished;
    }

    public Translation2d getClosestPathPoint () {
        return path.getParameterizer().getEstimatedGoalPoint(poseEstimator.getTotalDistance());
    }

    public void reset() {
//        currentPose = drive.getPose();
        totalTime = 0.0;
//        drive.getPoseEstimator().resetTotalDistance();
        finished = false;
        if(path != null) path.getParameterizer().reset();
    }

//    public Pose2d getCurrentPose() {
//        return this.drive.getPose();
//    }

    public Pose2d getSimulatedPose() {
//        Pose2d pose = this.drive.getPose();
//        final Pose2d fpose = pose;
//        DebugTools.periodic(() -> logger.info("drive pose is {}", fpose), 0.1);
        if (path.getReversed()) {
            // reverse input yaw
            // reverse point directions
//            pose = pose.withYaw(pose.getYaw().inverse());
        }
//        return pose;
        return null;
    }

    public DriveOutputs getWheelSpeeds(Trajectory.State goal) {
//        DebugTools.periodic(() -> logger.info("simulated pose is {} at {} seconds into auto", getSimulatedPose(), getTimeSinceStart()), 0.1);
        ChassisSpeeds adjustedSpeeds = controller.calculate(getSimulatedPose().toWpiLibPose(), goal); //robotPosition IS converted to meters
        DifferentialDriveWheelSpeeds wheelSpeeds = driveKinematics.toWheelSpeeds(adjustedSpeeds);
        if (path.getReversed())
        {      logger.info(" I am in the getReversed block");
            return new DriveOutputs(
                    -Units.metersToInches(wheelSpeeds.rightMetersPerSecond),
                    -Units.metersToInches(wheelSpeeds.leftMetersPerSecond)
            );
        } else
        {
            logger.info(" I am in the forward block");
            return new DriveOutputs(
                    Units.metersToInches(wheelSpeeds.leftMetersPerSecond),
                    Units.metersToInches(wheelSpeeds.rightMetersPerSecond)
            );
        }
    }

    public DriveOutputs getWheelSpeeds(double t) {
        return getWheelSpeeds(getGoalState(t));
    }

    public DriveOutputs getWheelSpeeds() {
        return getWheelSpeeds(totalTime);
    }

    private Trajectory.State getGoalState(double t) {
        //time in seconds from beginning
        return trajectory.sample(t);
    }

    private Trajectory.State getGoalState() {
        return getGoalState(totalTime);
    }

    public double getRemainingDistance () {
        return path.getParameterizer().getCompleteDistance() - poseEstimator.getTotalDistance();
    }
    public double getTimeLeft() {
        return trajectory.getTotalTimeSeconds() - totalTime;
    }
    public double getTimeSinceStart () {
        return totalTime;
    }

    public Trajectory getTrajectory() {
        return trajectory;
    }

    /**
     * Set the TrajectoryPath
     *
     * @param trajectoryPath
     */
    public void setTrajectoryPath(TrajectoryPath trajectoryPath) {
        path = trajectoryPath;
        this.trajectory = trajectoryPath.getTrajectory();
    }

    private void generateRamseteController() {
        controller = new RamseteController(this.config.b, this.config.zeta);
    }

    @Override
    public void configure(Config config) {
        this.config = config;
        generateRamseteController();
        if(config.distanceParameterized) { // need to test distance parameterized mode
            logger.warn("Brok distance mode used!");
        }
    }

    public class DataProvider implements LogDataProvider {
        @Override
        public String getName() {
            return DriveTrajectoryFollower.this.getName();
        }

        @Override
        public List<Object> getKeys() {
            return Arrays.asList(
                    "currentX",
                    "currentY",
                    "simulatedX",
                    "simulatedY",
                    "goalX",
                    "goalY",
                    "goalHeading",
                    "remainingDistance",
                    "distanceTraveled"
            );
        }

        @Override
        public List<Object> getValues() {
            DriveTrajectoryFollower follower = DriveTrajectoryFollower.this;
            return Arrays.asList(
//                    follower.drive.getPose().getX(),
//                    follower.drive.getPose().getY(),
                    follower.getSimulatedPose().getX(),
                    follower.getSimulatedPose().getY(),
                    follower.getCurrentGoalPose().getX(),
                    follower.getCurrentGoalPose().getY(),
                    follower.getCurrentGoalPose().getYaw().toDegrees()
//                    follower.getRemainingDistance(),
//                    follower.drive.getPoseEstimator().getTotalDistance()
            );
        }
    }

    public static class Config {
        // configuration values for trajectory generation
        // b = converge crisis
        // zeta = something else
        public double b, zeta;

        // tells the system to use the controller on a time or distance parameterized trajectory
        public boolean distanceParameterized;

        public boolean logData;

        // how close you can get to the end, whether it's time or distance
        public double endThreshold;
    }
}
