package frc.robot.constants;

import com.ctre.phoenix6.CANBus;

import edu.wpi.first.wpilibj.RobotBase;

public class GeneralConstants {
    public static final int kDriverPort = 0;
    public static final int kGunnerPort = 1;

    public static final boolean kReplay = false;

    public enum RobotMedium {
        REAL,
        SIM,
        REPLAY
    }
    public static final RobotMedium kRobotMedium = RobotBase.isReal() ? RobotMedium.REAL : kReplay ? RobotMedium.REPLAY : RobotMedium.SIM;

    public static final String kCanivoreBusName = "canbuse";
    public static final boolean kCANFDEnabled =
        new CANBus(kCanivoreBusName).isNetworkFD();

    public static final String kNTRobotStatusKey = "Robot Status";
}
