package frc.robot.subsystems.swerve;

import static edu.wpi.first.units.Units.Amps;

import edu.wpi.first.units.measure.Current;
import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.math.Vector;
import edu.wpi.first.math.numbers.N3;
import com.ctre.phoenix6.CANBus;

import frc.robot.constants.GeneralConstants;

public abstract class SwerveConstants {
    public static final Vector<N3> kOdometryStdDevs = VecBuilder.fill(.1, .1, .1);
    public static final Vector<N3> kInitialVisionStdDevs = VecBuilder.fill(.9, .9, .9);
    
    public abstract static class SwerveModuleConstants {
        // 10-29-2023 using error driving marks
        public static final double kSteerGearing = 12.8;

        // Calibrated 10-29-2023 based on mechanical cad
        // Fudge factor of 1.146 added on 10-24-24
        public static final double kDriveGearing = 4.72 * 1.146;

        public static final Current driveZoomStatorLimit = Amps.of(65);
        public static final Current driveZoomSupplyLimit = Amps.of(45);
        public static final Current driveNoZoomStatorLimit = Amps.of(50);
        public static final Current driveNoZoomSupplyLimit = Amps.of(35);

        public static final Current azimuthCurrentLimit = Amps.of(40);

        // Measured wheel diameter September 22, 2024 with calipers
        // Correction factor applied 11/01/24 after swerve wheels changed
        public static final double kCircumference = (.097) * Math.PI / 0.9776;

        public static final double kDriveRampPeriod = 0;

        public static final double[] kCancoderOffsets = {
            // conductator
            // -0.206543,  // BL
            // -0.243408,  // BR
            // -0.176025,  // FL
            // 0.180664    // FR
            // tatorswift
            -0.113037109375, // BL
            -0.1123046875,   // BR
            0.28515625,      // FL
            0.23388671875    // FR
        };
    }

    public static final double kOdometryFrequency =
        new CANBus(GeneralConstants.kCanivoreBusName).isNetworkFD()
            ? 250.0
            : 100.0;
}
