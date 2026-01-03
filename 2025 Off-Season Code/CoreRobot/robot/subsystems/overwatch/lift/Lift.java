package frc.robot.subsystems.overwatch.lift;

import edu.wpi.first.math.MathUtil;
import frc.robot.constants.Constants;
import org.littletonrobotics.junction.AutoLogOutput;
import org.littletonrobotics.junction.Logger;
import org.teamtators.tester.ManualTestGroup;
import org.teamtators.util.Subsystem;

public class Lift extends Subsystem {
    @AutoLogOutput private double momentarySetpoint;

    private final LiftIO io;
    private final LiftIOInputsAutoLogged inputs;

    public Lift() {
        // spotless:off
        io = switch (Constants.kRobotMedium) {
            case REAL -> new LiftIOReal();
            case SIM -> new LiftIOSim();
            default -> new LiftIO() {};
        };
        // spotless:on
        inputs = new LiftIOInputsAutoLogged();
        log();
        initEncoder();
    }

    @Override
    public void log() {
        io.updateInputs(inputs);
        Logger.processInputs("Lift", inputs);
    }

    @AutoLogOutput
    public double getHeightInches() {
        return LiftConstants.motorRotationsToInches(inputs.motorPositionRotations);
    }

    public void goTo(double heightInches) {
        io.setSetpoint(heightInches, 0);
    }

    public void goTo(double heightInches, double velocityInchesPerSecond) {
        momentarySetpoint = heightInches;
        io.setSetpoint(
                LiftConstants.inchesToMotorRotations(heightInches),
                LiftConstants.inchesToMotorRotations(velocityInchesPerSecond));
    }

    public void initEncoder() {
        if (inputs.canrangeConnected
                && inputs.canrangeDistance > LiftConstants.MIN_VALID_CANRANGE_RANGE) {
            io.initEncoder(LiftConstants.CANrangeToRotations(inputs.canrangeDistance));
            System.out.println("yuhhhh i like that canrange value");
        } else {
            io.initEncoder(LiftConstants.DEFAULT_LIFT_POS);
            System.out.println("that value SUCKS");
        }
    }

    @Override
    public boolean getHealth() {
        return inputs.motorConnected && inputs.canrangeConnected;
    }

    @Override
    public ManualTestGroup createManualTests() {
        return io.getManualTests();
    }

    public boolean isNear(double position) {
        return isNear(position, LiftConstants.ERROR_INCHES);
    }

    public boolean isNear(double position, double tolerance) {
        return MathUtil.isNear(getHeightInches(), position, tolerance);
    }
}
