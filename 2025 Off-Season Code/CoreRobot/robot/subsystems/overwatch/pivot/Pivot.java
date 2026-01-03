package frc.robot.subsystems.overwatch.pivot;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.units.measure.AngularVelocity;
import frc.robot.constants.Constants;
import frc.robot.subsystems.overwatch.pivot.PivotIO.PivotControlMode;
import org.littletonrobotics.junction.AutoLogOutput;
import org.littletonrobotics.junction.Logger;
import org.teamtators.tester.ManualTestGroup;
import org.teamtators.util.Subsystem;
import org.teamtators.util.TatorMath;

public class Pivot extends Subsystem {
    private PivotIO io;
    private final PivotIOInputsAutoLogged inputs = new PivotIOInputsAutoLogged();

    @AutoLogOutput private Rotation2d momentarySetpoint = Rotation2d.kZero;
    @AutoLogOutput private Rotation2d currentPosition = Rotation2d.kZero;
    @AutoLogOutput private double targetVelocityRPS = 0;

    public Pivot() {
        io =
                switch (Constants.kRobotMedium) {
                    case REAL -> new PivotIOReal();
                    case SIM -> new PivotIOSim();
                    default -> new PivotIO() {};
                };
    }

    @Override
    public ManualTestGroup createManualTests() {
        return io.getManualTests();
    }

    @Override
    public void log() {
        io.updateInputs(inputs);
        Logger.processInputs("Pivot", inputs);
    }

    @Override
    public void doPeriodic() {
        currentPosition = TatorMath.wrapRotation2d(inputs.position);
    }

    public Rotation2d getAngle() {
        return currentPosition;
    }

    public AngularVelocity getVelocity() {
        return inputs.velocity;
    }

    public void goTo(Rotation2d angle) {
        goTo(angle, 0);
    }

    public void goTo(Rotation2d angle, double velocityRPS) {
        momentarySetpoint = angle;
        targetVelocityRPS = velocityRPS;
        io.setSetpoint(angle, velocityRPS);
    }

    @Override
    public boolean getHealth() {
        return inputs.motorConnected && inputs.canCoderConnected;
    }

    public void setControlMode(PivotControlMode controlMode) {
        io.setControlMode(controlMode);
    }

    public boolean isNear(Rotation2d position) {
        return isNear(position, PivotConstants.ALLOWED_ERROR);
    }

    public boolean isNear(Rotation2d position, Rotation2d tolerance) {
        return MathUtil.isNear(
                currentPosition.getDegrees() - position.getDegrees(), 0, tolerance.getDegrees());
    }
}
