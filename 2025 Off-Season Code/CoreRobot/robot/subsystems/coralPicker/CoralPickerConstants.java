package frc.robot.subsystems.coralPicker;

public abstract class CoralPickerConstants {

    public enum CoralPickerPositions {
        STOWED(0.144, 0),
        DEPLOYED(0.025, 0),
        COMPLETE_STOWED(.25, .493);

        public final double rotations;
        public final double holdPower;

        CoralPickerPositions(double rotations, double holdPower) {
            this.rotations = rotations;
            this.holdPower = holdPower;
        }
    }

    public static final int kMotorID = 10;
    public static final int kCANCoderID = 8;

    // Allowed error for "in position" check
    public static final double ALLOWED_ERROR = 0.05; // 5% of rotation
}
