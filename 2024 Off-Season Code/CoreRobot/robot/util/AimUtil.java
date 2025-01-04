package frc.robot.util;

import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.DriverStation.Alliance;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.button.Trigger;
import frc.robot.Robot;
import frc.robot.subsystems.pivot.PivotConstants.PivotPositions;
import frc.robot.subsystems.swerve.SwerveDrive;
import frc.robot.subsystems.upperNotePath.UpperNotePath.ShooterSpeeds;
import frc.robot.subsystems.vision.Vision;
import java.util.Optional;
import org.teamtators.Util.FlipUtil;
import org.teamtators.Util.QuickDebug;
import org.teamtators.Util.TatorMath;
import org.teamtators.Util.Vector2d;

public class AimUtil {

    // {dist, left shooter, right shooter, arm angle}
    private static final double[][] kSpeakerShotTable = {
        {1.32, 50, 35, 22.0},
        {1.83, 50, 35, 19.0},
        {2.30, 60, 55, 13.5},
        {2.80, 60, 55, 11.0},
        {3.20, 60, 55, 9.00},
        {3.85, 75, 60, 6.50},
        {4.40, 75, 60, 5.65},
        {4.80, 75, 60, 4.80},
        {5.30, 75, 60, 3.90},
        {5.50, 75, 60, 3.50},
        {5.70, 75, 60, 3.20}
    };
    // {dist, left shooter, right shooter, arm angle}
    private static final double[][] kAmpShotTable = {
        {7.800, 35, 35, 16},
        {8.915, 39, 39, 14},
        {9.370, 40, 40, 11},
        {10.16, 40, 40, 10},
        {11.60, 45, 45, 10}

        // {7.800, 35, 35, 0},
        // {9.370, 40, 40, 0},
        // {10.16, 40, 40, 0},
        // {11.60, 45, 45, 0}

        // {9.7  , 40, 50, 13.48},
        // {9.98 , 35, 35, 14.58},
        // {11.06, 40, 50, 13.8 }
    };

    private static final boolean TUNING = false;

    public enum AimTargets {
        NONE(new Vector2d()),
        SPEAKER(new Vector2d(0, 5.5)),
        AMP(new Vector2d(0.5, 7));

        private Vector2d pos;
        private boolean flipped = false;

        private AimTargets(Vector2d pos) {
            this.pos = pos;
        }

        private void flip() {
            pos.setX(FlipUtil.kFieldWidthMeters - pos.getX());
            // pos.setY(kFieldHeightMeters - pos.getY());
            flipped = !flipped;
        }
    }

    private static AimTargets aimTarget = AimTargets.NONE;

    public static final Trigger speakerAiming = new Trigger(() -> aimTarget == AimTargets.SPEAKER);
    public static final Trigger ampAiming = new Trigger(() -> aimTarget == AimTargets.AMP);
    public static final Trigger notAiming = new Trigger(() -> aimTarget == AimTargets.NONE);

    public static AimTargets getTarget() {
        return aimTarget;
    }

    public static void setTarget(AimTargets target) {
        aimTarget = target;
    }

    public static Optional<Vector2d> getTargetPos() {
        return getTargetPos(aimTarget);
    }

    public static Optional<Vector2d> getTargetPos(AimTargets target) {
        switch (target) {
            case NONE:
                return Optional.empty();
            case AMP:
            case SPEAKER:
                // if (Math.random() > .95) {
                //     System.out.println("flipped: " + target.flipped);
                //     System.out.println("target.x: " + target.pos.getX());
                //     System.out.println("target.y: " + target.pos.getY());
                // }
                if (DriverStation.getAlliance().isPresent()) {
                    if ((DriverStation.getAlliance().get() == Alliance.Red && !target.flipped)
                        || (DriverStation.getAlliance().get() == Alliance.Blue && target.flipped)) {
                        target.flip();
                    }
                    return Optional.of(target.pos);
                }
                return Optional.empty();
        }

        return Optional.empty();
    }

    public static double getLeftShooterSpeed() {
        var dist = getDistanceFrom(aimTarget);
        if (dist.isEmpty()) {
            return ShooterSpeeds.IDLE.kLeftRPS;
        } else {
            if (TUNING) return QuickDebug.input("leftrps", 0);
            return getShotTableValues(dist.get())[0];
        }
    }

    public static double getRightShooterSpeed() {
        var dist = getDistanceFrom(aimTarget);
        if (dist.isEmpty()) {
            return ShooterSpeeds.IDLE.kRightRPS;
        } else {
            if (TUNING) return QuickDebug.input("rightrps", 0);
            return getShotTableValues(dist.get())[1];
        }
    }

    public static double getPivotAngle() {
        var dist = getDistanceFrom(aimTarget);
        if (dist.isEmpty()) {
            return PivotPositions.HOME.kDegrees;
        } else {
            if (TUNING) return QuickDebug.input("pivotangleee", 0) + PivotPositions.HOME.kDegrees;
            return getShotTableValues(dist.get())[2] + PivotPositions.HOME.kDegrees;
        }
    }

    public static double[] getShotTableValues(double distanceFromTarget) {
        return TatorMath.linearlyInterpolate(distanceFromTarget, getShotTable(), new int[] {1, 2, 3});
    }

    private static double[][] getShotTable() {
        return getShotTable(aimTarget);
    }

    private static double[][] getShotTable(AimTargets target) {
        switch (target) {
            case SPEAKER:
                return kSpeakerShotTable;
            case AMP:
                return kAmpShotTable;
            case NONE:
                break;
        }
        return new double[][] {{0, 0, 0, 0}};
    }

    public static Optional<Double> getDistanceFrom(AimTargets target) {
        Vision vision = Robot.getVision();
        if (vision == null) return Optional.empty();

        // return Optional.of(QuickDebug.input("Tuning/Distance Override", 2.0));

        if (vision.frontCanSeeSpeakerTag() && target == AimTargets.SPEAKER) {
            return vision.getSpeakerTagDistance();
        } else {
            SwerveDrive swerve = Robot.getSwerve();
            if (swerve == null) return Optional.empty();

            var pose = swerve.getPose().getTranslation();
            var maybeTargetPose = AimUtil.getTargetPos(target);
            if (!maybeTargetPose.isPresent()) {
                return Optional.empty();
            }
            var targetPose = maybeTargetPose.get();

            var diffX = pose.getX() - targetPose.getX();
            var diffY = pose.getY() - targetPose.getY();

            return Optional.of(Math.hypot(diffX, diffY));
        }
    }

    public static Command speakerAim() {
        return Commands.runOnce(() -> setTarget(AimTargets.SPEAKER)).withName("SpeakerAim");
    }

    public static Command lobAim() {
        return Commands.runOnce(() -> setTarget(AimTargets.AMP)).withName("LobAim");
    }

    public static Command stopAim() {
        return Commands.runOnce(() -> setTarget(AimTargets.NONE)).withName("StopAim");
    }
}
