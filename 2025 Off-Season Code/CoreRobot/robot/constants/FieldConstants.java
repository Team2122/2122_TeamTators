package frc.robot.constants;

import static edu.wpi.first.units.Units.Inches;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Transform2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.units.measure.Distance;
import frc.robot.subsystems.vision.VisionConstants;
import org.teamtators.util.FlipUtil;

public abstract class FieldConstants {
    // x-axis length of the field
    public static final double FIELD_LENGTH_METERS = VisionConstants.TAG_LAYOUT.getFieldLength();
    // y-axis length of the field
    public static final double FIELD_WIDTH_METERS = VisionConstants.TAG_LAYOUT.getFieldWidth();
    public static final Translation2d FIELD_CENTER =
            new Translation2d(FIELD_LENGTH_METERS / 2, FIELD_WIDTH_METERS / 2);

    // reef is vertically centered and in between tags 21 & 18
    private static final double tag18X = VisionConstants.TAG_LAYOUT.getTagPose(18).get().getX();
    private static final double tag21X = VisionConstants.TAG_LAYOUT.getTagPose(21).get().getX();
    public static final Translation2d BLUE_REEF =
            new Translation2d((tag18X + tag21X) / 2, FIELD_WIDTH_METERS / 2);
    public static final double BARGE_LENGTH_METERS = 3.855677;
    public static final Translation2d RED_REEF = FlipUtil.flip(BLUE_REEF);

    public static final Distance CORAL_WIDTH = Inches.of(4.5);

    // values pulled from choreo
    public static final Translation2d TOP_LOLLIPOP =
            new Translation2d(1.2243608236312866, 5.849671840667725);
    public static final Translation2d MIDDLE_LOLLIPOP =
            new Translation2d(1.2243608236312866, 4.020630359649658);
    public static final Translation2d BOTTOM_LOLLIPOP =
            new Translation2d(1.2243608236312866, 2.191588878631592);

    public static final Pose2d LEFT_SOURCE_TARGET =
            new Pose2d(1.643, 6.866, Rotation2d.fromRadians(-0.5036));

    public static final Pose2d RIGHT_SOURCE_TARGET =
            new Pose2d(1.643, 1.1859016, Rotation2d.fromRadians(0.847106));

    // offsets used for the first & second picks in the left source auto
    public static final Transform2d LEFT_SOURCE_PICK_FIRST_OFFSET =
            new Transform2d(Inches.of(0), Inches.of(0), Rotation2d.kZero);
    public static final Transform2d LEFT_SOURCE_PICK_SECOND_OFFSET =
            new Transform2d(Inches.of(-12), Inches.of(0), Rotation2d.kZero);

    // All distances will be subtracted from pose.
    public static final Distance REEF_L4_OFFSET_RIGHT = Inches.of(1.75);

    public static final Distance REEF_L4_OFFSET_LEFT = Inches.of(.5);

    public static final Distance REEF_L2L3_OFFSET_RIGHT = Inches.of(0);

    public static final Distance REEF_L2L3_OFFSET_LEFT = Inches.of(0);
}
