package frc.robot.constants;

import static edu.wpi.first.units.Units.Inches;

import com.ctre.phoenix6.CANBus;
import edu.wpi.first.units.measure.Distance;
import edu.wpi.first.wpilibj.RobotBase;

public class Constants {
    public static final int kDriverPort = 0;
    public static final int kGunnerPort = 1;

    public static final boolean kReplay = false;

    public enum RobotMedium {
        REAL,
        SIM,
        REPLAY
    }

    public static final RobotMedium kRobotMedium;

    static {
        if (RobotBase.isReal()) {
            kRobotMedium = RobotMedium.REAL;
        } else if (kReplay) {
            kRobotMedium = RobotMedium.REPLAY;
        } else {
            kRobotMedium = RobotMedium.SIM;
        }
    }

    public static final String kCanivoreBusName = "canbuse";
    public static final boolean kCANFDEnabled = new CANBus(kCanivoreBusName).isNetworkFD();
    public static final double kTickPeriod = .02;
    public static final String kNTRobotStatusKey = "Robot Status";

    // width of bot WITH BUMBERS
    public static final Distance BOT_WIDTH = Inches.of(36);
    public static final Distance BOT_LENGTH = Inches.of(36);

    public static final double POST_ALGAE_PICK_DELAY = 0.5;
}
