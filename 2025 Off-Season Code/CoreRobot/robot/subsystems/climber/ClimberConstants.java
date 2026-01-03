package frc.robot.subsystems.climber;

public abstract class ClimberConstants {

    public enum ClimberPositions {
        STOWED(0.0),
        DEPLOYED(50.0),
        CLIMB(158.0);

        public final double rotations;

        ClimberPositions(double rotations) {
            this.rotations = rotations;
        }
    }

    public static final int kMotorID = 6;
}
