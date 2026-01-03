package frc.robot.subsystems.swerve;

import org.littletonrobotics.junction.AutoLog;
import org.teamtators.tester.ManualTest;

public interface GyroIO {
    @AutoLog
    public class GyroIOInputs {
        public double yawDegreesRaw;
        public double yawDegrees;
        public double rollDegrees;
        public double pitchDegrees;

        public double yawDegreesPerSec;
        public double rollDegreesPerSec;
        public double pitchDegreesPerSec;

        public boolean connected = false;
    }

    public default void updateInputs(GyroIOInputs inputs) {}

    public default void setCurrentAngle(double angle) {}

    public default void changeOffset(double offset) {}

    public default void zero() {}

    public default ManualTest getManualTest() {
        return new ManualTest("Gyro") {};
    }
}
