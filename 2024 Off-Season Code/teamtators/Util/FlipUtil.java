package org.teamtators.Util;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.DriverStation.Alliance;

public abstract class FlipUtil {
    public enum FlipType {
        XFLIP, YFLIP, SPIN180
    }

    public static final double kFieldWidthMeters = 16.4846;
    public static final double kFieldHeightMeters = 8.014;
    public static final FlipType kFlipType = FlipType.XFLIP;

    private static final Rotation2d ANG90 = Rotation2d.fromDegrees(90);
    private static final Rotation2d ANG180 = Rotation2d.fromDegrees(180);

    /***
     * Helper function to check if the Driver Station reports Red alliance.
     */
    public static boolean shouldFlip() {
        var opt = DriverStation.getAlliance();
        return (opt.isPresent() && opt.get() == Alliance.Red);
    }
    /***
     * Flips a {@link Pose2d} only if the Driver Station reports Red alliance.
     */
    public static Pose2d conditionalFlipPose2d(Pose2d pose) {
        if (shouldFlip()) {
            return flipPose2d(pose);
        } else {
            return pose;
        }
    }
    /***
     * Flips a {@link Rotation2d} only if the Driver Station reports Red alliance.
     */
    public static Rotation2d conditionalFlipRotation2d(Rotation2d angle) {
        if (shouldFlip()) {
            return flipRotation2d(angle);
        } else {
            return angle;
        }
    }

    /***
     * Flips a {@link Pose2d} according to the field parameters.
     */
    public static Pose2d flipPose2d(Pose2d pose) {
        if (kFlipType == FlipType.XFLIP) {
            return new Pose2d(
                kFieldWidthMeters-pose.getX(),
                pose.getY(),
                flipRotation2d(pose.getRotation())
            );
        } else if (kFlipType == FlipType.YFLIP) {
            return new Pose2d(
                pose.getX(),
                kFieldHeightMeters-pose.getY(),
                flipRotation2d(pose.getRotation())
            );
        } else if (kFlipType == FlipType.SPIN180) {
            return new Pose2d(
                kFieldWidthMeters-pose.getX(),
                kFieldHeightMeters-pose.getY(),
                flipRotation2d(pose.getRotation())
            );
        } else {
            return pose;
        }
    }
    /***
     * Flips a {@link Rotation2d} according to the field parameters.
     */
    public static Rotation2d flipRotation2d(Rotation2d angle) {
        if (kFlipType == FlipType.XFLIP) {
            return ANG90.minus(angle).plus(ANG90);
        } else if (kFlipType == FlipType.YFLIP) {
            return angle.unaryMinus();
        } else if (kFlipType == FlipType.SPIN180) {
            return angle.plus(ANG180);
        } else {
            return angle;
        }
    }
}
