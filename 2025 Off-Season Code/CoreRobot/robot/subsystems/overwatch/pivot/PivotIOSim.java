package frc.robot.subsystems.overwatch.pivot;

import static edu.wpi.first.units.Units.RadiansPerSecond;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.system.plant.DCMotor;
import edu.wpi.first.wpilibj.simulation.SingleJointedArmSim;

public class PivotIOSim implements PivotIO {
    // These values do not match the real pivot at all
    private SingleJointedArmSim sim =
            new SingleJointedArmSim(
                    DCMotor.getKrakenX60(1),
                    100,
                    SingleJointedArmSim.estimateMOI(1.0, 20.0),
                    1.0,
                    -Double.MAX_VALUE,
                    Double.MAX_VALUE,
                    false,
                    Math.PI / 2,
                    0,
                    0);
    private PIDController pidController;
    private Rotation2d setpoint = new Rotation2d(sim.getAngleRads());

    public PivotIOSim() {
        pidController = new PIDController(40.0, 0.0, 2.2);
        pidController.enableContinuousInput(-Math.PI, Math.PI);
    }

    @Override
    public void updateInputs(PivotIOInputs inputs) {
        var pos = sim.getAngleRads();

        var input = MathUtil.clamp(pidController.calculate(pos, setpoint.getRadians()), -12, 12);
        if (!((Double) input).isNaN()) {
            sim.setInput(input);
        } else {
            sim.setInput(0);
        }
        sim.update(.02);

        inputs.motorConnected = true;
        inputs.position = Rotation2d.fromRadians(sim.getAngleRads());
        inputs.velocity = RadiansPerSecond.of(sim.getVelocityRadPerSec());
        inputs.supplyCurrent = sim.getCurrentDrawAmps();
        inputs.statorCurrent = sim.getCurrentDrawAmps();
        inputs.tempCelsius = -1.0;
        inputs.appliedVolts = input;
    }

    @Override
    public void setSetpoint(Rotation2d angle, double velocityRPS) {
        setpoint = angle;
    }
}
