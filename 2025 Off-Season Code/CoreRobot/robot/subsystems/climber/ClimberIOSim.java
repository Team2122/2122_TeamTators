package frc.robot.subsystems.climber;

import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.system.plant.DCMotor;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.simulation.SingleJointedArmSim;

public class ClimberIOSim implements ClimberIO {
    private final SingleJointedArmSim armSim;
    private double volts = 0.0;

    private final PIDController pid = new PIDController(0.5, 0, 0);
    private boolean isPidBeingUsed = false;

    private boolean inductanceSensorSim;

    public ClimberIOSim() {
        // Arm parameters
        double armLengthMeters = Units.inchesToMeters(14.93);
        double armMassKg = Units.lbsToKilograms(7);
        double moi = SingleJointedArmSim.estimateMOI(armLengthMeters, armMassKg);

        armSim =
                new SingleJointedArmSim(
                        DCMotor.getKrakenX60(1),
                        1.0,
                        moi,
                        armLengthMeters,
                        0,
                        999999999, // max angle (rad) (90)
                        false,
                        0);
    }

    @Override
    public void setVolts(double volts) {
        this.volts = volts;
        isPidBeingUsed = false;
    }

    public void setSetpoint(double rotations) {
        double radians = rotations * 2 * Math.PI;
        pid.setSetpoint(radians);
        isPidBeingUsed = true;
    }

    @Override
    public void updateInputs(ClimberIOInputs inputs) {
        if (isPidBeingUsed) {
            double pidOutput = pid.calculate(armSim.getAngleRads());
            armSim.setInputVoltage(pidOutput);
        } else {
            armSim.setInputVoltage(volts);
        }

        armSim.update(0.02);

        // Simulate sensor: "true" if within ~2 degrees of stowed (0 rad)
        inductanceSensorSim = Math.abs(armSim.getAngleRads()) < Math.toRadians(2.0);

        inputs.appliedVolts = volts;
        inputs.statorCurrent = armSim.getCurrentDrawAmps();
        inputs.supplyCurrent = armSim.getCurrentDrawAmps();
        inputs.tempCelsius = 25.0;
        inputs.currentPosition = armSim.getAngleRads() / (2 * Math.PI);
    }
}
