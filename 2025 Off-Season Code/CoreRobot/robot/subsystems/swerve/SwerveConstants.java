package frc.robot.subsystems.swerve;

import static edu.wpi.first.units.Units.Amps;

import com.ctre.phoenix6.CANBus;
import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.math.Vector;
import edu.wpi.first.math.numbers.N3;
import edu.wpi.first.units.measure.Current;
import frc.robot.constants.Constants;

public abstract class SwerveConstants {
    public static final Vector<N3> kOdometryStdDevs = VecBuilder.fill(.1, .1, .1);
    public static final Vector<N3> kInitialVisionStdDevs = VecBuilder.fill(.9, .9, .9);

    public static final String kDriveCommandName = "Driving!";

    public abstract static class SwerveModuleConstants {
        // 10-29-2023 using error driving marks
        public static final double kSteerGearing = 12.8;

        // L2 gearing, from SDS website Feb. 23, 2025
        public static final double kDriveGearing = 6.75;

        public static final Current kDriveStatorLimit = Amps.of(65);
        public static final Current kDriveSupplyLimit = Amps.of(45);

        public static final Current azimuthCurrentLimit = Amps.of(40);

        // Measured wheel diameter September 22, 2024 with calipers
        // Correction factor applied 11/01/24 after swerve wheels changed
        public static final double kCircumference = (.097) * Math.PI;

        public static final double kDriveRampPeriod = 0;

        public static final double[] kCancoderOffsets = {
            0.176270, // BL
            0.256348, // BR
            -0.186279 + 0.5, // FL
            0.292725 - 0.5 // FR
        };
    }

    public static final int STABILIZATION_DELAY = 5;

    public static final double kOdometryFrequency =
            new CANBus(Constants.kCanivoreBusName).isNetworkFD() ? 250.0 : 100.0;
}
