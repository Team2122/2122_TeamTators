package org.teamtators.common.drive;

import edu.wpi.first.wpilibj.geometry.Pose2d;
import edu.wpi.first.wpilibj.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj.kinematics.DifferentialDriveKinematics;
import edu.wpi.first.wpilibj.trajectory.constraint.TrajectoryConstraint;

public class DriveKinematicsMinConstraint implements TrajectoryConstraint {

    private final double m_maxSpeedMetersPerSecond;
    private final DifferentialDriveKinematics m_kinematics;

    /**
     * Constructs a differential drive dynamics constraint.
     *
     * @param kinematics A kinematics component describing the drive geometry.
     * @param maxSpeedMetersPerSecond The max speed that a side of the robot can travel at.
     */
    public DriveKinematicsMinConstraint (final DifferentialDriveKinematics kinematics,
                                                 double maxSpeedMetersPerSecond) {
        m_maxSpeedMetersPerSecond = maxSpeedMetersPerSecond;
        m_kinematics = kinematics;
    }

    @Override
    public double getMaxVelocityMetersPerSecond(Pose2d poseMeters, double curvatureRadPerMeter,
                                                double velocityMetersPerSecond) {
        // Create an object to represent the current chassis speeds.
        var chassisSpeeds = new ChassisSpeeds(velocityMetersPerSecond,
                0, velocityMetersPerSecond * curvatureRadPerMeter);

        // Get the wheel speeds and normalize them to within the max velocity.
        var wheelSpeeds = m_kinematics.toWheelSpeeds(chassisSpeeds);
        wheelSpeeds.normalize(m_maxSpeedMetersPerSecond);

        // Return the new linear chassis speed.
        return m_kinematics.toChassisSpeeds(wheelSpeeds).vxMetersPerSecond;
    }

    @Override
    public MinMax getMinMaxAccelerationMetersPerSecondSq(Pose2d poseMeters, double curvatureRadPerMeter, double velocityMetersPerSecond) {
        return null;
    }
}
