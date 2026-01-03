package frc.robot.subsystems.overwatch.lift;

import edu.wpi.first.math.util.Units;

public final class LiftConstants {
    public static final double rotationsPerInch = 1.22008;

    public static double motorRotationsToInches(double rotations) {
        return rotations / rotationsPerInch;
    }

    public static double inchesToMotorRotations(double inches) {
        return inches * rotationsPerInch;
    }

    public static final double MIN_VALID_CANRANGE_RANGE = 0.09;
    public static final double DEFAULT_LIFT_POS = 0.0;

    public static double CANrangeToRotations(double range) {
        /**
         * determined in Desmos by linear regression
         *
         * <p>https://www.desmos.com/calculator/ljiaqmufoh
         *
         * <p>converted because i forgot to unconvert it in advantagescope
         */
        return rotationsPerInch * Units.metersToInches(range) - 2.30759;
    }

    public static final double ERROR_INCHES = 0.5;
}
