package frc.robot.subsystems.overwatch.pivot;

import static edu.wpi.first.units.Units.Rotations;

import com.ctre.phoenix6.BaseStatusSignal;
import com.ctre.phoenix6.StatusSignal;
import com.ctre.phoenix6.configs.CANcoderConfiguration;
import com.ctre.phoenix6.configs.ClosedLoopGeneralConfigs;
import com.ctre.phoenix6.configs.CommutationConfigs;
import com.ctre.phoenix6.configs.CurrentLimitsConfigs;
import com.ctre.phoenix6.configs.ExternalFeedbackConfigs;
import com.ctre.phoenix6.configs.MagnetSensorConfigs;
import com.ctre.phoenix6.configs.MotorOutputConfigs;
import com.ctre.phoenix6.configs.Slot0Configs;
import com.ctre.phoenix6.configs.Slot1Configs;
import com.ctre.phoenix6.configs.Slot2Configs;
import com.ctre.phoenix6.configs.TalonFXSConfiguration;
import com.ctre.phoenix6.controls.PositionVoltage;
import com.ctre.phoenix6.hardware.CANcoder;
import com.ctre.phoenix6.hardware.TalonFXS;
import com.ctre.phoenix6.signals.GravityTypeValue;
import com.ctre.phoenix6.signals.MotorArrangementValue;
import com.ctre.phoenix6.signals.NeutralModeValue;
import com.ctre.phoenix6.signals.SensorDirectionValue;
import com.ctre.phoenix6.signals.StaticFeedforwardSignValue;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.units.measure.Angle;
import edu.wpi.first.units.measure.AngularVelocity;
import edu.wpi.first.units.measure.Current;
import edu.wpi.first.units.measure.Temperature;
import edu.wpi.first.units.measure.Voltage;
import frc.robot.constants.Constants;
import org.teamtators.tester.ManualTestGroup;
import org.teamtators.tester.components.ContinuousSensorTest;
import org.teamtators.tester.components.MotorTest;

public class PivotIOReal implements PivotIO {
    private TalonFXS motor;

    private StatusSignal<Angle> positionSignal;
    private StatusSignal<AngularVelocity> velocitySignal;
    private StatusSignal<Temperature> tempSignal;
    private StatusSignal<Current> supplyCurrentSignal;
    private StatusSignal<Current> statorCurrentSignal;
    private StatusSignal<Voltage> voltageSignal;
    private StatusSignal<Angle> cancoderPositionSignal;

    private CANcoder cancoder;

    private PositionVoltage positionReq = new PositionVoltage(0);

    private final PivotControlMode[] CONTROL_MODES = PivotControlMode.values();

    // which control parameters to use
    // 0 is normal operation
    // 1 is holding an algae
    // 2 is same as 0 but when a velocity feedforward is unavailable
    private int pidSlot = 0;

    public PivotIOReal() {
        motor = new TalonFXS(9, Constants.kCanivoreBusName);
        cancoder = new CANcoder(4, Constants.kCanivoreBusName);

        CANcoderConfiguration cancoderConfig =
                new CANcoderConfiguration()
                        .withMagnetSensor(
                                new MagnetSensorConfigs()
                                        .withAbsoluteSensorDiscontinuityPoint(1)
                                        .withMagnetOffset(0.183594)
                                        .withSensorDirection(SensorDirectionValue.Clockwise_Positive));

        for (int i = 0; i < 5; i++) {
            if (cancoder.getConfigurator().apply(cancoderConfig).isOK()) {
                break;
            }
        }

        TalonFXSConfiguration config =
                new TalonFXSConfiguration()
                        .withMotorOutput(new MotorOutputConfigs().withNeutralMode(NeutralModeValue.Brake))
                        .withCurrentLimits(
                                new CurrentLimitsConfigs()
                                        .withSupplyCurrentLimitEnable(true)
                                        .withSupplyCurrentLimit(40)
                                        .withStatorCurrentLimitEnable(true)
                                        .withStatorCurrentLimit(40))
                        .withCommutation(
                                new CommutationConfigs().withMotorArrangement(MotorArrangementValue.Minion_JST))
                        .withSlot0(
                                // normal gains
                                new Slot0Configs()
                                        .withGravityType(GravityTypeValue.Arm_Cosine)
                                        .withKS(0.07)
                                        .withKG(0.33)
                                        .withKV(6.8)
                                        .withKP(120)
                                        .withKI(0)
                                        .withStaticFeedforwardSign(StaticFeedforwardSignValue.UseVelocitySign))
                        .withSlot1(
                                // algae gains
                                new Slot1Configs()
                                        .withGravityType(GravityTypeValue.Arm_Cosine)
                                        .withStaticFeedforwardSign(StaticFeedforwardSignValue.UseVelocitySign)
                                        .withKS(.05)
                                        .withKG(0.64)
                                        .withKV(8)
                                        .withKI(0)
                                        .withKP(64))
                        .withSlot2(
                                // no velocity feedforward gains
                                new Slot2Configs()
                                        .withGravityType(GravityTypeValue.Arm_Cosine)
                                        .withKS(0.07)
                                        .withKG(0.33)
                                        .withKP(100)
                                        .withKI(0)
                                        .withKD(1)
                                        .withStaticFeedforwardSign(StaticFeedforwardSignValue.UseClosedLoopSign))
                        // .withVoltage(new
                        // VoltageConfigs().withPeakForwardVoltage(9).withPeakReverseVoltage(-9))
                        .withExternalFeedback(
                                new ExternalFeedbackConfigs()
                                        .withRotorToSensorRatio(PivotConstants.motorRotationsPerCANcoderRotation)
                                        .withRemoteCANcoder(cancoder))
                        .withClosedLoopGeneral(new ClosedLoopGeneralConfigs().withContinuousWrap(true));
        for (int i = 0; i < 5; i++) {
            if (motor.getConfigurator().apply(config).isOK()) {
                break;
            }
        }

        positionSignal = motor.getPosition();
        velocitySignal = motor.getVelocity();
        tempSignal = motor.getDeviceTemp();
        supplyCurrentSignal = motor.getSupplyCurrent();
        statorCurrentSignal = motor.getStatorCurrent();
        voltageSignal = motor.getMotorVoltage();

        cancoderPositionSignal = cancoder.getAbsolutePosition();
    }

    @Override
    public void updateInputs(PivotIOInputs inputs) {
        inputs.motorConnected =
                BaseStatusSignal.refreshAll(
                                positionSignal,
                                velocitySignal,
                                tempSignal,
                                supplyCurrentSignal,
                                statorCurrentSignal,
                                voltageSignal)
                        .isOK();

        inputs.position = new Rotation2d(positionSignal.getValue());
        inputs.velocity = velocitySignal.getValue();
        inputs.supplyCurrent = supplyCurrentSignal.getValueAsDouble();
        inputs.statorCurrent = statorCurrentSignal.getValueAsDouble();
        inputs.tempCelsius = tempSignal.getValueAsDouble();
        inputs.appliedVolts = voltageSignal.getValueAsDouble();

        inputs.canCoderConnected = BaseStatusSignal.refreshAll(cancoderPositionSignal).isOK();
        inputs.cancoderPositionRotations = cancoderPositionSignal.getValue().in(Rotations);

        inputs.controlMode = CONTROL_MODES[pidSlot];
    }

    @Override
    public void setSetpoint(Rotation2d angle, double velocityRPS) {
        motor.setControl(
                positionReq.withPosition(angle.getRotations()).withVelocity(velocityRPS).withSlot(pidSlot));
    }

    @Override
    public void setControlMode(PivotControlMode controlMode) {
        pidSlot = controlMode.ordinal();
    }

    public ManualTestGroup getManualTests() {
        return new ManualTestGroup(
                "Pivot",
                new MotorTest("Pivot", motor::set),
                new ContinuousSensorTest(
                        "Pivot CANCoder", () -> cancoder.getAbsolutePosition().getValue().in(Rotations)));
    }
}
