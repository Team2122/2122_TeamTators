package frc.robot.subsystems.affector;

import com.ctre.phoenix6.BaseStatusSignal;
import com.ctre.phoenix6.StatusSignal;
import com.ctre.phoenix6.configs.CurrentLimitsConfigs;
import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.controls.VoltageOut;
import com.ctre.phoenix6.hardware.TalonFX;
import edu.wpi.first.units.measure.AngularVelocity;
import edu.wpi.first.units.measure.Current;
import edu.wpi.first.units.measure.Temperature;
import edu.wpi.first.units.measure.Voltage;
import frc.robot.constants.Constants;
import frc.robot.subsystems.affector.AffectorIO.AffectorIOInputs;
import org.teamtators.tester.ManualTestGroup;
import org.teamtators.tester.components.MotorTest;

public class AffectorIOReal implements AffectorIO {
    private final TalonFX motor;

    private StatusSignal<AngularVelocity> velocitySignal;
    private StatusSignal<Temperature> tempSignal;
    private StatusSignal<Current> supplyCurrentSignal;
    private StatusSignal<Current> statorCurrentSignal;
    private StatusSignal<Voltage> voltageSignal;
    final VoltageOut voltageRequest = new VoltageOut(0);

    public AffectorIOReal() {
        motor = new TalonFX(1, Constants.kCanivoreBusName);

        TalonFXConfiguration config =
                new TalonFXConfiguration()
                        .withCurrentLimits(
                                new CurrentLimitsConfigs()
                                        .withStatorCurrentLimit(40)
                                        .withStatorCurrentLimitEnable(true)
                                        .withSupplyCurrentLimit(40)
                                        .withSupplyCurrentLimitEnable(true));
        for (int i = 0; i < 5; i++) {
            if (motor.getConfigurator().apply(config).isOK()) {
                break;
            }
        }
        velocitySignal = motor.getVelocity();
        tempSignal = motor.getDeviceTemp();
        supplyCurrentSignal = motor.getSupplyCurrent();
        statorCurrentSignal = motor.getStatorCurrent();
        voltageSignal = motor.getMotorVoltage();
    }

    @Override
    public ManualTestGroup getManualTests() {
        return new ManualTestGroup("Affector", new MotorTest("Affector", motor::set));
    }

    @Override
    public void setVoltage(double volts) {
        motor.setControl(voltageRequest.withOutput(volts));
    }

    @Override
    public void updateInputs(AffectorIOInputs inputs) {
        inputs.connected =
                BaseStatusSignal.refreshAll(
                                velocitySignal, voltageSignal, tempSignal, supplyCurrentSignal, statorCurrentSignal)
                        .isOK();
        inputs.supplyCurrent = supplyCurrentSignal.getValueAsDouble();
        inputs.motorSpeedRPS = velocitySignal.getValueAsDouble();
        inputs.statorCurrent = statorCurrentSignal.getValueAsDouble();
        inputs.tempCelsius = tempSignal.getValueAsDouble();
        inputs.appliedVolts = voltageSignal.getValueAsDouble();
    }
}
