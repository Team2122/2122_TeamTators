package org.teamtators.bbt8r.staging;

import edu.wpi.first.wpilibj.geometry.Pose2d;
import edu.wpi.first.wpilibj.geometry.Rotation2d;
import edu.wpi.first.wpilibj.kinematics.SwerveDriveKinematics;
import edu.wpi.first.wpilibj.kinematics.SwerveDriveOdometry;
import edu.wpi.first.wpilibj.kinematics.SwerveModuleState;

// FIXME This is very broken, please dont use it, you will shed many tears
public class WPIPositionTracker {

    private SwerveDriveOdometry swerveDriveOdometry;

    private SwerveDriveKinematics kinematics;
    private Pose2d initialPosition;
    private Rotation2d initialAngle;

    private Pose2d currentPose;

    private SwerveModuleState[] moduleStates;
    private Rotation2d gyroAngle;

    public WPIPositionTracker(SwerveDriveKinematics kinematics, Pose2d initialPosition, Rotation2d initialAngle) {
        this.kinematics = kinematics;
        this.initialPosition = initialPosition;
        this.initialAngle = initialAngle;
        swerveDriveOdometry = new SwerveDriveOdometry(kinematics, initialAngle, initialPosition);
        moduleStates = new SwerveModuleState[4];
        gyroAngle = new Rotation2d(initialAngle.getRadians());
    }

    public void setGyroAngle(double angle) {
        gyroAngle = new Rotation2d(angle);
    }

    public Rotation2d getGyroAngle() {
        return gyroAngle;
    }

    public void setModuleState(int index, SwerveModuleState state) {
        moduleStates[index] = state;
    }

    public void setModuleStates(SwerveModuleState... moduleStates) {
        this.moduleStates = moduleStates;
    }

    public SwerveModuleState getSwerveModuleState(int index) {
        return moduleStates[index];
    }

    public SwerveModuleState[] getModuleStates() {
        return moduleStates;
    }

    public void update() {
        update(gyroAngle, moduleStates);
    }

    public void updateWithTime(double time) {
        updateWithTime(time, gyroAngle, moduleStates);
    }

    public void update(Rotation2d gyroAngle, SwerveModuleState... moduleStates) {
        currentPose = swerveDriveOdometry.update(gyroAngle, moduleStates);
    }

    public void updateWithTime(double currentTimeSeconds, Rotation2d gyroAngle, SwerveModuleState... moduleStates) {
        currentPose = swerveDriveOdometry.updateWithTime(currentTimeSeconds, gyroAngle, moduleStates);
    }

    public Pose2d getCurrentPose() {
        return currentPose;
    }

}
