package frc.robot.subsystems.coralPicker;

import com.ctre.phoenix6.BaseStatusSignal;
import com.ctre.phoenix6.StatusSignal;
import com.ctre.phoenix6.configs.CANcoderConfiguration;
import com.ctre.phoenix6.configs.CurrentLimitsConfigs;
import com.ctre.phoenix6.configs.FeedbackConfigs;
import com.ctre.phoenix6.configs.MagnetSensorConfigs;
import com.ctre.phoenix6.configs.MotionMagicConfigs;
import com.ctre.phoenix6.configs.MotorOutputConfigs;
import com.ctre.phoenix6.configs.Slot0Configs;
import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.controls.MotionMagicVoltage;
import com.ctre.phoenix6.controls.VoltageOut;
import com.ctre.phoenix6.hardware.CANcoder;
import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.signals.GravityTypeValue;
import com.ctre.phoenix6.signals.InvertedValue;
import com.ctre.phoenix6.signals.NeutralModeValue;
import com.ctre.phoenix6.signals.SensorDirectionValue;
import edu.wpi.first.units.measure.Angle;
import edu.wpi.first.units.measure.Current;
import edu.wpi.first.units.measure.Temperature;
import edu.wpi.first.units.measure.Voltage;
import frc.robot.constants.Constants;
import org.teamtators.tester.ManualTestGroup;
import org.teamtators.tester.components.BinarySensorTest;
import org.teamtators.tester.components.MotorTest;
import org.teamtators.util.DigitalSensor;

public class CoralPickerIOReal implements CoralPickerIO {
    private final TalonFX motor;
    private final DigitalSensor inductanceSensor;
    private final CANcoder cancoder;

    private final VoltageOut voltageRequest = new VoltageOut(0);
    private final MotionMagicVoltage positionRequest = new MotionMagicVoltage(0);
    private final StatusSignal<Angle> positionSignal;
    private final StatusSignal<Temperature> tempSignal;
    private final StatusSignal<Current> supplyCurrentSignal;
    private final StatusSignal<Current> statorCurrentSignal;
    private final StatusSignal<Voltage> voltageSignal;

    public CoralPickerIOReal() {
        motor = new TalonFX(CoralPickerConstants.kMotorID, Constants.kCanivoreBusName);
        inductanceSensor = new DigitalSensor(11, true); // TODO: update DIO channel
        cancoder = new CANcoder(CoralPickerConstants.kCANCoderID, Constants.kCanivoreBusName);

        TalonFXConfiguration config =
                new TalonFXConfiguration()
                        .withMotorOutput(
                                new MotorOutputConfigs()
                                        .withNeutralMode(NeutralModeValue.Brake)
                                        .withInverted(InvertedValue.Clockwise_Positive))
                        .withCurrentLimits(
                                new CurrentLimitsConfigs()
                                        .withSupplyCurrentLimitEnable(true)
                                        .withSupplyCurrentLimit(40)
                                        .withStatorCurrentLimitEnable(true)
                                        .withStatorCurrentLimit(40))
                        .withSlot0(
                                new Slot0Configs()
                                        .withGravityType(GravityTypeValue.Arm_Cosine)
                                        .withKG(0.3)
                                        .withKS(0.21)
                                        .withKP(500)
                                        .withKV(3.5))
                        .withMotionMagic(
                                new MotionMagicConfigs()
                                        .withMotionMagicAcceleration(2)
                                        .withMotionMagicCruiseVelocity(8))
                        .withFeedback(
                                new FeedbackConfigs().withRotorToSensorRatio(0.6875).withRemoteCANcoder(cancoder));

        var cancoderConfig =
                new CANcoderConfiguration()
                        .withMagnetSensor(
                                new MagnetSensorConfigs()
                                        .withSensorDirection(SensorDirectionValue.Clockwise_Positive)
                                        .withMagnetOffset(0.102539));

        for (int i = 0; i < 5; i++) {
            if (motor.getConfigurator().apply(config).isOK()) {
                break;
            }
        }

        for (int i = 0; i < 5; i++) {
            if (cancoder.getConfigurator().apply(cancoderConfig).isOK()) {
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

    @Override
    public void updateInputs(CoralPickerIOInputs inputs) {
        inputs.motorConnected =
                BaseStatusSignal.refreshAll(
                                positionSignal, tempSignal, supplyCurrentSignal, statorCurrentSignal, voltageSignal)
                        .isOK();

        inputs.currentPosition = positionSignal.getValueAsDouble();
        inputs.supplyCurrent = supplyCurrentSignal.getValueAsDouble();
        inputs.statorCurrent = statorCurrentSignal.getValueAsDouble();
        inputs.tempCelsius = tempSignal.getValueAsDouble();
        inputs.appliedVolts = voltageSignal.getValueAsDouble();

        inputs.inductanceSensor = inductanceSensor.get();
    }

    @Override
    public ManualTestGroup getManualTests() {
        return new ManualTestGroup(
                "Coral Picker",
                new MotorTest("coral picker motor", motor::set),
                new BinarySensorTest("inductance sensor", inductanceSensor::get));
    }
}
