package frc.robot.subsystems.climber;

import com.ctre.phoenix6.BaseStatusSignal;
import com.ctre.phoenix6.StatusSignal;
import com.ctre.phoenix6.configs.CurrentLimitsConfigs;
import com.ctre.phoenix6.configs.MotorOutputConfigs;
import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.controls.PositionVoltage;
import com.ctre.phoenix6.controls.VoltageOut;
import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.signals.NeutralModeValue;
import edu.wpi.first.units.measure.Angle;
import edu.wpi.first.units.measure.Current;
import edu.wpi.first.units.measure.Temperature;
import edu.wpi.first.units.measure.Voltage;
import frc.robot.constants.Constants;
import frc.robot.subsystems.climber.ClimberConstants.ClimberPositions;
import org.teamtators.tester.ManualTestGroup;
import org.teamtators.tester.components.MotorTest;

public class ClimberIOReal implements ClimberIO {
    private final TalonFX motor;

    private final VoltageOut voltageRequest = new VoltageOut(0);
    private final PositionVoltage positionRequest = new PositionVoltage(0);
    private final StatusSignal<Angle> positionSignal;
    private final StatusSignal<Temperature> tempSignal;
    private final StatusSignal<Current> supplyCurrentSignal;
    private final StatusSignal<Current> statorCurrentSignal;
    private final StatusSignal<Voltage> voltageSignal;

    public ClimberIOReal() {
        motor = new TalonFX(ClimberConstants.kMotorID, Constants.kCanivoreBusName);

        TalonFXConfiguration config =
                new TalonFXConfiguration()
                        .withMotorOutput(new MotorOutputConfigs().withNeutralMode(NeutralModeValue.Brake))
                        .withCurrentLimits(
                                new CurrentLimitsConfigs()
                                        .withSupplyCurrentLimitEnable(true)
                                        .withSupplyCurrentLimit(40)
                                        .withStatorCurrentLimitEnable(false)
                                        .withStatorCurrentLimit(40));

        for (int i = 0; i < 5; i++) {
            if (motor.getConfigurator().apply(config).isOK()) {
                break;
            }
        }

        positionSignal = motor.getPosition();
        tempSignal = motor.getDeviceTemp();
        supplyCurrentSignal = motor.getSupplyCurrent();
        statorCurrentSignal = motor.getStatorCurrent();
        voltageSignal = motor.getMotorVoltage();
    }

    public void setVolts(double volts) {
        motor.setControl(voltageRequest.withOutput(volts));
    }

    public void setSetpoint(double rotations) {
        motor.setControl(positionRequest.withPosition(rotations));
    }

    /** Called once after inductance sensor = true */
    public void calibrateEncoder() {
        motor.setPosition(ClimberPositions.STOWED.rotations);
    }

    @Override
    public void updateInputs(ClimberIOInputs inputs) {
        inputs.motorConnected =
                BaseStatusSignal.refreshAll(
                                positionSignal, tempSignal, supplyCurrentSignal, statorCurrentSignal, voltageSignal)
                        .isOK();

        inputs.currentPosition = positionSignal.getValueAsDouble();
        inputs.supplyCurrent = supplyCurrentSignal.getValueAsDouble();
        inputs.statorCurrent = statorCurrentSignal.getValueAsDouble();
        inputs.tempCelsius = tempSignal.getValueAsDouble();
        inputs.appliedVolts = voltageSignal.getValueAsDouble();
    }

    @Override
    public ManualTestGroup getManualTests() {
        return new ManualTestGroup("Climber", new MotorTest("climber motor", motor::set));
    }
}
