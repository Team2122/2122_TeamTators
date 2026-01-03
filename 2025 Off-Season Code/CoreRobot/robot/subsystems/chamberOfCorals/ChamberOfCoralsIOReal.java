package frc.robot.subsystems.chamberOfCorals;

import com.ctre.phoenix6.BaseStatusSignal;
import com.ctre.phoenix6.StatusSignal;
import com.ctre.phoenix6.configs.CurrentLimitsConfigs;
import com.ctre.phoenix6.configs.MotorOutputConfigs;
import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.configs.TalonFXSConfiguration;
import com.ctre.phoenix6.controls.VoltageOut;
import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.hardware.TalonFXS;
import com.ctre.phoenix6.signals.InvertedValue;
import com.ctre.phoenix6.signals.MotorArrangementValue;
import edu.wpi.first.units.measure.Current;
import edu.wpi.first.units.measure.Temperature;
import edu.wpi.first.units.measure.Voltage;
import frc.robot.constants.Constants;
import org.teamtators.tester.ManualTestGroup;
import org.teamtators.tester.components.BinarySensorTest;
import org.teamtators.tester.components.MotorTest;
import org.teamtators.util.DigitalSensor;

public class ChamberOfCoralsIOReal implements ChamberOfCoralsIO {
    private final TalonFX falcon;
    private final TalonFXS minion1;
    private final TalonFXS minion2;
    private final DigitalSensor breakbeam;

    private final VoltageOut voltageRequest = new VoltageOut(0);
    private final StatusSignal<Temperature> falconTemp, minion1Temp, minion2Temp;
    private final StatusSignal<Current> falconSupply, minion1Supply, minion2Supply;
    private final StatusSignal<Current> falconStator, minion1Stator, minion2Stator;
    private final StatusSignal<Voltage> falconVoltage, minion1Voltage, minion2Voltage;

    public ChamberOfCoralsIOReal() {
        falcon = new TalonFX(ChamberOfCoralsConstants.kFalconID, Constants.kCanivoreBusName);
        minion1 = new TalonFXS(ChamberOfCoralsConstants.kMinion1ID, Constants.kCanivoreBusName);
        minion2 = new TalonFXS(ChamberOfCoralsConstants.kMinion2ID, Constants.kCanivoreBusName);

        breakbeam = new DigitalSensor(ChamberOfCoralsConstants.kBreakbeamChannel, true);

        // Falcon motor config
        TalonFXConfiguration falconConfig =
                new TalonFXConfiguration()
                        .withCurrentLimits(
                                new CurrentLimitsConfigs()
                                        .withSupplyCurrentLimitEnable(true)
                                        .withSupplyCurrentLimit(40)
                                        .withStatorCurrentLimitEnable(true)
                                        .withStatorCurrentLimit(40))
                        .withMotorOutput(
                                new MotorOutputConfigs().withInverted(InvertedValue.Clockwise_Positive));

        for (int i = 0; i < 5; i++) {
            if (falcon.getConfigurator().apply(falconConfig).isOK()) break;
        }

        TalonFXSConfiguration minionConfig =
                new TalonFXSConfiguration()
                        .withCurrentLimits(
                                new CurrentLimitsConfigs()
                                        .withSupplyCurrentLimitEnable(true)
                                        .withSupplyCurrentLimit(40)
                                        .withStatorCurrentLimitEnable(true)
                                        .withStatorCurrentLimit(40));
        minionConfig.Commutation.MotorArrangement = MotorArrangementValue.Minion_JST;

        for (int i = 0; i < 5; i++) {
            if (minion1.getConfigurator().apply(minionConfig).isOK()) {
                break;
            }
        }

        minionConfig.withMotorOutput(
                new MotorOutputConfigs().withInverted(InvertedValue.Clockwise_Positive));

        for (int i = 0; i < 5; i++) {
            if (minion2.getConfigurator().apply(minionConfig).isOK()) {
                break;
            }
        }

        falconTemp = falcon.getDeviceTemp();
        falconSupply = falcon.getSupplyCurrent();
        falconStator = falcon.getStatorCurrent();
        falconVoltage = falcon.getMotorVoltage();

        minion1Temp = minion1.getDeviceTemp();
        minion1Supply = minion1.getSupplyCurrent();
        minion1Stator = minion1.getStatorCurrent();
        minion1Voltage = minion1.getMotorVoltage();

        minion2Temp = minion2.getDeviceTemp();
        minion2Supply = minion2.getSupplyCurrent();
        minion2Stator = minion2.getStatorCurrent();
        minion2Voltage = minion2.getMotorVoltage();
    }

    @Override
    public void setVolts(double falconVolts, double minionVolts) {
        falcon.setControl(voltageRequest.withOutput(falconVolts));
        minion1.setControl(voltageRequest.withOutput(minionVolts));
        minion2.setControl(voltageRequest.withOutput(minionVolts));
    }

    @Override
    public void updateInputs(ChamberOfCoralsIOInputs inputs) {
        inputs.falconMotorConnected =
                BaseStatusSignal.refreshAll(falconTemp, falconSupply, falconStator, falconVoltage).isOK();
        inputs.minion1MotorConnected =
                BaseStatusSignal.refreshAll(minion1Temp, minion1Supply, minion1Stator, minion1Voltage)
                        .isOK();
        inputs.minion2MotorConnected =
                BaseStatusSignal.refreshAll(minion2Temp, minion2Supply, minion2Stator, minion2Voltage)
                        .isOK();

        inputs.falconTempCelsius = falconTemp.getValueAsDouble();
        inputs.falconSupplyCurrent = falconSupply.getValueAsDouble();
        inputs.falconStatorCurrent = falconStator.getValueAsDouble();
        inputs.falconAppliedVolts = falconVoltage.getValueAsDouble();

        inputs.minion1TempCelsius = minion1Temp.getValueAsDouble();
        inputs.minion1SupplyCurrent = minion1Supply.getValueAsDouble();
        inputs.minion1StatorCurrent = minion1Stator.getValueAsDouble();
        inputs.minion1AppliedVolts = minion1Voltage.getValueAsDouble();

        inputs.minion2TempCelsius = minion2Temp.getValueAsDouble();
        inputs.minion2SupplyCurrent = minion2Supply.getValueAsDouble();
        inputs.minion2StatorCurrent = minion2Stator.getValueAsDouble();
        inputs.minion2AppliedVolts = minion2Voltage.getValueAsDouble();

        inputs.breakbeamTriggered = breakbeam.get();
    }

    @Override
    public ManualTestGroup getManualTests() {
        return new ManualTestGroup(
                "Chamber of Coral",
                new MotorTest("falcon motor", falcon::set),
                new MotorTest("minion1 motor", minion1::set),
                new MotorTest("minion2 motor", minion2::set),
                new BinarySensorTest("breakbeam", breakbeam::get));
    }
}
