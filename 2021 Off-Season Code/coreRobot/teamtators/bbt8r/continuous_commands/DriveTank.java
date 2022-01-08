package org.teamtators.bbt8r.continuous_commands;

import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import org.teamtators.bbt8r.DriveInputSupplier.DriveInput;
import org.teamtators.bbt8r.TatorRobot;
import org.teamtators.bbt8r.subsystems.Drive;
import org.teamtators.bbt8r.subsystems.Drive.Gear;
import org.teamtators.bbt8r.subsystems.OperatorInterface;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.control.Timer;
import org.teamtators.common.datalogging.DataCollector;
import org.teamtators.common.datalogging.LogDataProvider;
import org.teamtators.common.scheduler.Command;
import org.teamtators.common.scheduler.RobotState;

import java.util.Arrays;
import java.util.List;

/**
 * continuous command for drive contains almost all of the gear-shifting logic
 */
public class DriveTank extends Command implements Configurable<DriveTank.Config> {
    private final Drive drive;
    private final OperatorInterface oi;
    // private Timer timer = new Timer();   // Not used 
    private Config config;

    private final String driveTableKey = "stateDriveTable";
    public NetworkTableEntry stateDrive;
    public final String driveEntry = "driveState"; // stateDrive

    private DriveInput input;

    private boolean localDebug = false;

    private LogDataProvider logDataProvider = new LogDataProviderImplementation();

    private DataCollector dataCollector = DataCollector.getDataCollector();

    public DriveTank(TatorRobot robot) {
        super("DriveTank");
        this.drive = robot.getSubsystems().getDrive();
        this.oi = robot.getSubsystems().getOi();
        requires(drive);
        validIn(RobotState.TELEOP);
        drive.inputSupplier.setSupplier(oi);
        stateDrive = NetworkTableInstance.getDefault().getTable(driveTableKey).getEntry(driveEntry);
        stateDrive.setString("" + drive.driveTrainState_current);
    }

    @Override
    protected void initialize() {
        super.initialize(localDebug);
        drive.readShiftedState();
        drive.initalizeDriveTrainState();
        drive.setOpenLoopRamp(.5);
        drive.setCloseLoopRamp(.5);
        drive.shiftGear(Gear.HIGH);
        // timer.start();
        // dataCollector.startProvider(logDataProvider);
    }

    private boolean lastRightBump = false;

    @Override
    public boolean step() 
    {
        input = drive.inputSupplier.get();
        double left = input.left;
        double right = input.right;
    
        boolean shiftGear = oi.isRightBumperHeld(); // rightBumper is used for gearShift
    
        String shift_mode_str = "" ;
        shift_mode_str = drive.setDriveTrainState_new( left, right, shiftGear ) ;

        stateDrive.setString(shift_mode_str);
        return false; // signifies that it's NOT finished
    }

    @Override
    protected void finish(boolean interrupted) {
        super.finish(interrupted, localDebug);
        drive.stop();
        drive.shiftGear(Gear.HIGH);
        dataCollector.stopProvider(logDataProvider);
    }

    private boolean calcShiftState(boolean current, double speed, double thresholdUp, double thresholdDown) {
        // current: true for high gear, false for low gear
        if (speed > thresholdUp) {
            return true;
        }
        if (speed < thresholdDown) {
            return false;
        }
        return current;
    }

    @Override
    public boolean isValidInState(RobotState state) {
        return state == RobotState.AUTONOMOUS || state == RobotState.TELEOP;
    }

    public void configure(Config config) {
        this.config = config;
        localDebug = config.debug;
    }

    private final class LogDataProviderImplementation implements LogDataProvider {
		@Override
        public String getName() {
            return "DriveTank";
        }

		@Override
        public List<Object> getKeys() {
            return Arrays.asList("ltt", "rtt", "lts", "rts");
        }

		@Override
        public List<Object> getValues() {
            return Arrays.asList(   drive.leftTransmissionTarget, drive.rightTransmissionTarget, 
                                    drive.leftTransmissionSpeed, drive.rightTransmissionSpeed);
        }
	}

	public static class Config {
        public double maxALow;
        public double slowerHeight;
        public double slowHeight;
        public double maxAHigh;
        public double maxAHighest;
        public double kWheelToTarget;
        public boolean enableAutoShift;
        public boolean debug;
    }

}
