package frc.robot.subsystems.overwatch.lift;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.system.plant.DCMotor;
import edu.wpi.first.math.system.plant.LinearSystemId;
import edu.wpi.first.wpilibj.simulation.DCMotorSim;

public class LiftIOSim implements LiftIO {
    private DCMotorSim sim =
            new DCMotorSim(
                    LinearSystemId.createDCMotorSystem(
                            DCMotor.getKrakenX60(2),
                            .097, // ~ 10lb 10in rod pivoting around one end
                            5),
                    DCMotor.getKrakenX60(2),
                    0,
                    0);
    private PIDController pidController = new PIDController(3.0, 0, 0.1);

    public LiftIOSim() {
        sim.setState(0, 0.0);
    }

    @Override
    public void updateInputs(LiftIOInputs inputs) {
        var input = MathUtil.clamp(pidController.calculate(sim.getAngularPositionRotations()), -12, 12);
        if (!((Double) input).isNaN()) {
            sim.setInput(input);
        } else {
            sim.setInput(0);
        }
        sim.update(0.02);

        // sim was tuned for inches, not actual motor rotations
        inputs.motorPositionRotations =
                LiftConstants.inchesToMotorRotations(sim.getAngularPositionRotations());

        inputs.canrangeDistance = inputs.motorPositionRotations;
    }

    @Override
    public void setSetpoint(double position, double velocity) {
        // sim was tuned for inches, not actual motor rotations
        position = LiftConstants.motorRotationsToInches(position);
        pidController.setSetpoint(position);
    }

    @Override
    public void initEncoder(double hei) {
        sim.setState(hei * (2 * Math.PI), sim.getAngularVelocityRadPerSec());
    }
}
