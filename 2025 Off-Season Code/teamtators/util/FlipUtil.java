package org.teamtators.util;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.DriverStation.Alliance;
import frc.robot.constants.FieldConstants;

public abstract class FlipUtil {
    public enum FlipType {
        XFLIP,
        YFLIP,
        SPIN180
    }

    public static final FlipType kFlipType = FlipType.SPIN180;

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
    public static Pose2d conditionalFlip(Pose2d pose) {
        if (shouldFlip()) {
            return flip(pose);
        } else {
            return pose;
        }
    }

    /***
     * Flips a {@link Rotation2d} only if the Driver Station reports Red alliance.
     */
    public static Rotation2d conditionalFlip(Rotation2d angle) {
        if (shouldFlip()) {
            return flip(angle);
        } else {
            return angle;
        }
    }

    /***
     * Flips a {@link Translation2d} only if the Driver Station reports Red alliance.
     */
    public static Translation2d conditionalFlip(Translation2d trans) {
        if (shouldFlip()) {
            return flip(trans);
        } else {
            return trans;
        }
    }

    /***
     * Flips a {@link Pose2d} according to the field parameters.
     */
    public static Pose2d flip(Pose2d pose) {
        return flip(pose, kFlipType);
    }

    /***
     * Flips a {@link Pose2d} according to the field parameters.
     */
    public static Pose2d flip(Pose2d pose, FlipType type) {
        return new Pose2d(flip(pose.getTranslation(), type), flip(pose.getRotation(), type));
    }

    /***
     * Flips a {@link Rotation2d} according to the field parameters.
     */
    public static Rotation2d flip(Rotation2d angle) {
        return flip(angle, kFlipType);
    }

    /***
     * Flips a {@link Rotation2d} according to the field parameters.
     */
    public static Rotation2d flip(Rotation2d angle, FlipType type) {
        if (type == FlipType.XFLIP) {
            return Rotation2d.kCCW_90deg.minus(angle).plus(Rotation2d.kCCW_90deg);
        } else if (type == FlipType.YFLIP) {
            return angle.unaryMinus();
        } else if (type == FlipType.SPIN180) {
            return angle.plus(Rotation2d.k180deg);
        } else {
            return angle;
        }
    }

    /***
     * Flips a {@link Translation2d} according to the field parameters.
     */
    public static Translation2d flip(Translation2d trans) {
        return flip(trans, kFlipType);
    }

    /***
     * Flips a {@link Translation2d} according to the field parameters.
     */
    public static Translation2d flip(Translation2d trans, FlipType type) {
        if (type == FlipType.XFLIP) {
            return new Translation2d(FieldConstants.FIELD_LENGTH_METERS - trans.getX(), trans.getY());
        } else if (type == FlipType.YFLIP) {
            return new Translation2d(trans.getX(), FieldConstants.FIELD_WIDTH_METERS - trans.getY());
        } else if (type == FlipType.SPIN180) {
            return new Translation2d(
                    FieldConstants.FIELD_LENGTH_METERS - trans.getX(),
                    FieldConstants.FIELD_WIDTH_METERS - trans.getY());
        } else {
            return trans;
        }
    }
}
