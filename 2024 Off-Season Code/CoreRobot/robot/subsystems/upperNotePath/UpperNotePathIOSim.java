package frc.robot.subsystems.upperNotePath;

import edu.wpi.first.math.system.plant.DCMotor;
import edu.wpi.first.math.system.plant.LinearSystemId;
import edu.wpi.first.units.measure.AngularVelocity;
import edu.wpi.first.wpilibj.simulation.FlywheelSim;
import edu.wpi.first.wpilibj.simulation.DCMotorSim;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.controller.SimpleMotorFeedforward;

import frc.robot.subsystems.upperNotePath.UpperNotePath.ShooterSpeeds;

import static edu.wpi.first.units.Units.RotationsPerSecond;
import static edu.wpi.first.units.Units.Volts;

import org.teamtators.Util.QuickDebug;

public class UpperNotePathIOSim implements UpperNotePathIO {
    private FlywheelSim leftShooterSim;
    private double leftShooterVolts = 0;
    private double leftShooterVelocitySetpoint = 0;

    private DCMotorSim rightShooterSim;
    private double rightShooterVolts = 0;
    private boolean rightShooterVoltageControl = false;
    private double rightShooterVelocitySetpoint = 0;

    private PIDController shooterVelocityController;
    private SimpleMotorFeedforward shooterVelocityFF;

    private FlywheelSim dunkerSim;
    private double dunkerVolts = 0;

    public UpperNotePathIOSim() {
        leftShooterSim = new FlywheelSim(
            LinearSystemId.createFlywheelSystem(
                DCMotor.getKrakenX60(1),
                .01,
                1
            ),
            DCMotor.getKrakenX60(1),
            .001
        );
        
        rightShooterSim = new DCMotorSim(
            LinearSystemId.createDCMotorSystem(
                DCMotor.getKrakenX60(1),
                .01,
                1
            ),
            DCMotor.getKrakenX60(1),
            .001, .001
        );
        
        dunkerSim = new FlywheelSim(
            LinearSystemId.createFlywheelSystem(
                DCMotor.getNeo550(1),
                .01,
                1
            ),
            DCMotor.getKrakenX60(1),
            .001
        );

        shooterVelocityController = new PIDController(1, 0, 0);
        shooterVelocityFF = new SimpleMotorFeedforward(0, 1);
    }

    @Override
    public void updateInputs(UpperNotePathIOInputs inputs) {
        double pidControl = shooterVelocityController.calculate(inputs.leftShooterVelocityRPS, leftShooterVelocitySetpoint);

        AngularVelocity leftShooterSpeed = RotationsPerSecond.of(leftShooterVelocitySetpoint);
        double ffControl = shooterVelocityFF.calculate(leftShooterSpeed).in(Volts);
        leftShooterSim.setInput(pidControl + ffControl);
        if (rightShooterVoltageControl) {
            double input = UpperNotePathConstants.kDiverterFlipVoltage;
            rightShooterSim.setInput(input);
        } else {
            pidControl = shooterVelocityController.calculate(inputs.rightShooterVelocityRPS, rightShooterVelocitySetpoint);
            AngularVelocity rightShooterVelocity = RotationsPerSecond.of(rightShooterVelocitySetpoint);
            ffControl = shooterVelocityFF.calculate(rightShooterVelocity).in(Volts);
            rightShooterSim.setInput(pidControl + ffControl);
        }

        dunkerSim.setInput(dunkerVolts);

        leftShooterSim.update(.02);
        rightShooterSim.update(.02);
        dunkerSim.update(.02);

        inputs.leftShooterSupplyCurrent = leftShooterSim.getCurrentDrawAmps();
        inputs.leftShooterStatorCurrent = leftShooterSim.getCurrentDrawAmps();
        inputs.leftShooterTorqueCurrent = leftShooterSim.getCurrentDrawAmps();
        inputs.leftShooterDutyCycle = 0;
        inputs.leftShooterControlMode = "VelocityVoltage";
        // inputs.leftShooterVelocityRPS = leftShooterSim.getAngularVelocityRPM() / 60;
        inputs.leftShooterVelocityRPS = leftShooterVelocitySetpoint;
        inputs.leftShooterTempCelcius = -1;
        inputs.leftShooterAppliedVolts = leftShooterVolts;

        inputs.rightShooterSupplyCurrent = rightShooterSim.getCurrentDrawAmps();
        inputs.rightShooterStatorCurrent = rightShooterSim.getCurrentDrawAmps();
        inputs.rightShooterTorqueCurrent = rightShooterSim.getCurrentDrawAmps();
        inputs.rightShooterDutyCycle = 0;
        inputs.rightShooterControlMode = rightShooterVoltageControl ? "VoltageOut" : "VelocityVoltage";
        // inputs.rightShooterVelocityRPS = rightShooterSim.getAngularVelocityRPM() / 60;
        inputs.rightShooterVelocityRPS = rightShooterVelocitySetpoint;
        inputs.rightShooterTempCelcius = -1;
        inputs.rightShooterAppliedVolts = rightShooterVolts;
        inputs.rightShooterPositionRotations = rightShooterSim.getAngularPositionRotations();
        inputs.rightShooterPositionDegrees = inputs.rightShooterPositionRotations * 360;

        inputs.diverterSensor = QuickDebug.input("UpperNotePath/Diverter Sensor", false);

        inputs.dunkerDutyCycle = dunkerVolts;
        inputs.dunkerMotorInverted = false;
        inputs.dunkerSensor = QuickDebug.input("UpperNotePath/Dunker Sensor", false);
    }

    @Override
    public void setShooterSpeeds(ShooterSpeeds speeds) {
        setShooterSpeeds(speeds.kLeftRPS, speeds.kRightRPS);
    }

    @Override
    public void setShooterSpeeds(double left, double right) {
        rightShooterVoltageControl = false;
        leftShooterVelocitySetpoint = left;
        rightShooterVelocitySetpoint = right;
    }

    @Override
    public void flipDiverterUp() {
        rightShooterVoltageControl = true;
    }

    @Override
    public void setDunkerVoltage(double volts) {
        dunkerVolts = volts;
    }
}
