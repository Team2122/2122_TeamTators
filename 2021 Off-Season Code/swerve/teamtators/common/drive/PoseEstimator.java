package org.teamtators.common.drive;

import org.teamtators.common.control.AbstractUpdatable;
import org.teamtators.common.math.Pose2d;
import org.teamtators.common.math.Rotation;
import org.teamtators.common.util.IndexedCircularBuffer;

import java.util.function.IntConsumer;

/**
 * @author Alex Mikhalev
 */
public class PoseEstimator extends AbstractUpdatable {
    private final TankDrive drive;
    private Pose2d pose = Pose2d.zero();
    private volatile Pose2d setPose = null;
    private double lastCenterDistance;
    private TankKinematics kinematics;
    private double initialYaw = 90;
    private final Object lock = new Object();
    private IndexedCircularBuffer<Pose2d> recentPoses;
    private IntConsumer idx;
    private double totalDistance;

    public PoseEstimator(TankDrive drive) {
        super("PoseEstimator");
        this.drive = drive;
        setPose(Pose2d.zero());
        totalDistance = 0;
    }

    public PoseEstimator(TankDrive drive, IndexedCircularBuffer<Pose2d> poses, IntConsumer idx) {
        this(drive);
        recentPoses = poses;
        this.idx = idx;
    }

    @Override
    public synchronized void start() {
        super.start();
        pose = pose.withYaw(getYawRotation());
        lastCenterDistance = drive.getCenterDistance();
        totalDistance = 0;
    }

    private Rotation gyroToPoseAngle(double gyroAngle) {
        return Rotation.fromDegrees(initialYaw - gyroAngle);
    }

    private double poseToGyroAngle(Rotation poseAngle) {
        return initialYaw - poseAngle.toDegrees();
    }

    private Rotation getYawRotation() {
        return gyroToPoseAngle(drive.getYawAngle());
    }

    private void setYawRotation(Rotation poseRotation) {
//        drive.setYawAngle(poseToGyroAngle(poseRotation));
        initialYaw = poseRotation.toDegrees();
    }

    public Pose2d getPose() {
        return pose;
    }

    public void setPose(Pose2d pose) {
        setYawRotation(pose.getYaw()); // update yaw immediately
        drive.setYawAngle(pose.getYawDegrees());
    }

    public void resetTotalDistance() {
        totalDistance = 0;
    }

    public double getTotalDistance() {
        return Math.abs(totalDistance);
    }

    @Override
    protected void doUpdate(double delta) {
        Rotation endHeading = getYawRotation();
        double centerDistance = drive.getCenterDistance();
        double deltaWheel = centerDistance - lastCenterDistance;
        totalDistance += Math.abs(deltaWheel);
        Pose2d newPose = kinematics.integratePoseChange(pose, endHeading, deltaWheel);

        lastCenterDistance = centerDistance;
        pose = newPose;
//        DebugTools.periodic(() -> logger.info(">>>>>>>>>>>> newPose is {}", newPose), 0.1);
        if(recentPoses != null) {
            idx.accept(recentPoses.push(pose));
        }
    }

    public void setKinematics(TankKinematics tankKinematics) {
        this.kinematics = tankKinematics;
    }

    public TankKinematics getKinematics() {
        return kinematics;
    }
}
