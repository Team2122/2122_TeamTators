package org.teamtators.bbt8r.staging;

import edu.wpi.first.wpilibj.geometry.Pose2d;
import edu.wpi.first.wpilibj.geometry.Rotation2d;
import edu.wpi.first.wpilibj.geometry.Translation2d;
import edu.wpi.first.wpilibj.kinematics.SwerveDriveKinematics;
import edu.wpi.first.wpilibj.kinematics.SwerveDriveOdometry;
import edu.wpi.first.wpilibj.kinematics.SwerveModuleState;
import org.teamtators.common.control.Timer;

public class OmniscientPositionTracker {
    private SwerveDriveOdometry wpiOdometry;
    private PositionTracker notWpiPositionTracker;
    private Timer timer;
    private Vector avgVec;
    private Vector pos;
    private double gyroAngle;

    public enum PositionTrackerType {
        WPI_POSITION_TRACKER(0), NOT_SPI_POSITION_TRACKER(1);
        public final int value;

        PositionTrackerType(int value) {
            this.value = value;
        }
    }

    public OmniscientPositionTracker() {
        SwerveDriveKinematics swerveDriveKinematics = new SwerveDriveKinematics(new Translation2d(.3048, .3048),
                new Translation2d(-.3048, .3048), new Translation2d(-.3048, -.3048), new Translation2d(.3048, -.3048));
        wpiOdometry = new SwerveDriveOdometry(swerveDriveKinematics, new Rotation2d(0));
        notWpiPositionTracker = new PositionTracker();
        avgVec = new Vector();
    }

    // FIXME: nothing is broken, this is just super important, the gyro angle must be negated when passed into here
    public void update( double gyroAngle, Vector... moduleVectors) {
            wpiOdometry.update(new Rotation2d(gyroAngle), vectorToModuleState(moduleVectors));
            avgVec = getAverageVector(moduleVectors);
            avgVec.setTheta(avgVec.getTheta() + gyroAngle);
            notWpiPositionTracker.updateWithTime(avgVec, timer.restart());
            this.gyroAngle = gyroAngle;
    }

    public Vector getPosition(PositionTrackerType trackerType){
        if(trackerType == PositionTrackerType.WPI_POSITION_TRACKER){
            Pose2d position = wpiOdometry.getPoseMeters();
            pos.setXY(position.getX(), position.getY());
            pos.setPolar(pos.getTheta() - gyroAngle, pos.getMagnitude());
        } else {
            pos = notWpiPositionTracker.getPosition();
        }
        return pos;
    }

    public static SwerveModuleState[] vectorToModuleState(Vector... vector) {
        SwerveModuleState[] swerveModuleStates = new SwerveModuleState[vector.length];
        for (int i = 0; i < vector.length; i++) {
            swerveModuleStates[i] = new SwerveModuleState();
            swerveModuleStates[i].angle = new Rotation2d(vector[i].getTheta());
            swerveModuleStates[i].speedMetersPerSecond = vector[i].getMagnitude();
        }
        return swerveModuleStates;
    }

    private Vector getAverageVector(Vector... vectors){
        Vector out = new Vector();
        for (Vector vector: vectors) {
            out.addSelf(vector);
        }
        out.scale(1.0/vectors.length);
        return out;
    }

    @Override
    public String toString() {
        return "OmniscientPositionTracker{" +
                "wpiOdometry=" + wpiOdometry.getPoseMeters() +
                ", notWpiPositionTracker=" + notWpiPositionTracker +
                ", avgVector=" + avgVec +
                ", position=" + pos +
                ", gyroAngle=" + gyroAngle +
                '}';
    }
}
