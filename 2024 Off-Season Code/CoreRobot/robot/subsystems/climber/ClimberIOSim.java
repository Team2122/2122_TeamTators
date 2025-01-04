package frc.robot.subsystems.climber;

import edu.wpi.first.math.system.plant.DCMotor;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.wpilibj.simulation.SingleJointedArmSim;

import frc.robot.subsystems.climber.ClimberIO.ClimberIOInputs;

public class ClimberIOSim implements ClimberIO {
    private SingleJointedArmSim motorSim;
    private double volts = 0;
    private double setpoint = 0;
    private boolean usePID = false;

    private PIDController controller;

    public ClimberIOSim() {
        motorSim = new SingleJointedArmSim(
            DCMotor.getKrakenX60(1),
            ClimberConstants.kGearing,
            SingleJointedArmSim.estimateMOI(0.4626, 1.062),
            0.4626,
            -999,
            999,
            false,
            0);

        controller = new PIDController(1, 0, 0);
    }

    @Override
    public void updateInputs(ClimberIOInputs inputs) {
        if (usePID) {
            motorSim.setInputVoltage(controller.calculate(inputs.motorPositionRotations));
        } else {
            double outVolts = 0.0;
            if (volts == 0) {
                outVolts = -motorSim.getVelocityRadPerSec();
            } else {
                outVolts = volts;
            }
            // robot weight sim
            if (motorSim.getAngleRads() < -10) {
                outVolts += 0.8;
            }
            motorSim.setInputVoltage(outVolts);
        }
        motorSim.update(.02);

        inputs.supplyCurrent = motorSim.getCurrentDrawAmps();
        inputs.statorCurrent = motorSim.getCurrentDrawAmps();
        inputs.torqueCurrent = motorSim.getCurrentDrawAmps();
        inputs.dutyCycle     = 0;
        inputs.controlMode   = "VoltageOut";
        // (meter/sec) * (rot/meter) = rot/sec
        // 1 rot = 2*pi*rad meters
        // 1/(2*pi*rad) rot = 1 meter
        inputs.motorVelocityRPS   = motorSim.getVelocityRadPerSec()
                                    / (2*Math.PI);
        inputs.tempCelcius   = 0;
        inputs.appliedVolts  = volts;
        // inputs.motorPositionDegrees   = Math.toDegrees(motorSim.getAngleRads())
        // inputs.motorPositionRotations = ClimberConstants
        //     .degreesToRotations(inputs.motorPositionRotations);
        inputs.motorPositionRotations   = motorSim.getAngleRads();

        inputs.downSensorHit = motorSim.hasHitLowerLimit();
        inputs.upSensorHit   = motorSim.hasHitUpperLimit();

        inputs.cancoderPosition = motorSim.getAngleRads() / (2*Math.PI);
        inputs.cancoderVelocity = motorSim.getVelocityRadPerSec() / (2*Math.PI);
    }

    @Override
    public void setSetpoint(Climber.Position position) {
        usePID = true;
        controller.setSetpoint(position.getRotations());
    }

    @Override
    public void setVoltage(double volts) {
        usePID = false;
        this.volts = volts;
    }

    @Override
    public void setEncoderPosition(double rotations) {
        motorSim.setState(rotations,
            motorSim.getVelocityRadPerSec());
    }
}
