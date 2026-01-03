package frc.robot.subsystems.overwatch.lift;

import com.ctre.phoenix6.BaseStatusSignal;
import com.ctre.phoenix6.StatusSignal;
import com.ctre.phoenix6.configs.CANrangeConfiguration;
import com.ctre.phoenix6.configs.CurrentLimitsConfigs;
import com.ctre.phoenix6.configs.MotorOutputConfigs;
import com.ctre.phoenix6.configs.Slot0Configs;
import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.controls.PositionVoltage;
import com.ctre.phoenix6.hardware.CANrange;
import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.signals.NeutralModeValue;
import com.ctre.phoenix6.signals.StaticFeedforwardSignValue;
import edu.wpi.first.units.measure.Angle;
import edu.wpi.first.units.measure.AngularVelocity;
import edu.wpi.first.units.measure.Current;
import edu.wpi.first.units.measure.Distance;
import edu.wpi.first.units.measure.Temperature;
import edu.wpi.first.units.measure.Voltage;
import frc.robot.constants.Constants;
import org.teamtators.tester.ManualTestGroup;
import org.teamtators.tester.components.ContinuousSensorTest;
import org.teamtators.tester.components.MotorTest;

public class LiftIOReal implements LiftIO {
    private final TalonFX motor;
    private final CANrange canRange;

    private PositionVoltage positionRequest = new PositionVoltage(0);

    private StatusSignal<Angle> positionSignal;
    private StatusSignal<AngularVelocity> velocitySignal;
    private StatusSignal<Temperature> tempSignal;
    private StatusSignal<Current> supplyCurrentSignal;
    private StatusSignal<Current> statorCurrentSignal;
    private StatusSignal<Voltage> voltageSignal;

    private StatusSignal<Distance> canrangeDistanceSignal;

    public LiftIOReal() {
        motor = new TalonFX(8, Constants.kCanivoreBusName);
        canRange = new CANrange(0, Constants.kCanivoreBusName);

        TalonFXConfiguration config =
                new TalonFXConfiguration()
                        .withMotorOutput(new MotorOutputConfigs().withNeutralMode(NeutralModeValue.Brake))
                        .withCurrentLimits(
                                new CurrentLimitsConfigs()
                                        .withStatorCurrentLimit(40)
                                        .withStatorCurrentLimitEnable(true)
                                        .withSupplyCurrentLimit(40)
                                        .withSupplyCurrentLimitEnable(true))
                        .withSlot0(
                                new Slot0Configs()
                                        .withKS(0.035)
                                        .withKP(3.25)
                                        .withKV(0.128)
                                        .withKG(0.355)
                                        .withStaticFeedforwardSign(StaticFeedforwardSignValue.UseVelocitySign));
        // .withVoltage(new VoltageConfigs().withPeakForwardVoltage(9).withPeakReverseVoltage(-9));
        for (int i = 0; i < 5; i++) {
            if (motor.getConfigurator().apply(config).isOK()) {
                break;
            }
        }

        CANrangeConfiguration canrangeConfig = new CANrangeConfiguration();
        for (int i = 0; i < 5; i++) {
            if (canRange.getConfigurator().apply(canrangeConfig).isOK()) {
                break;
            }
        }

        positionSignal = motor.getPosition();
        velocitySignal = motor.getVelocity();
        tempSignal = motor.getDeviceTemp();
        supplyCurrentSignal = motor.getSupplyCurrent();
        statorCurrentSignal = motor.getStatorCurrent();
        voltageSignal = motor.getMotorVoltage();

        canrangeDistanceSignal = canRange.getDistance();
    }

    public ManualTestGroup getManualTests() {
        return new ManualTestGroup(
                "Lift",
                new MotorTest("Lift", motor::set),
                new ContinuousSensorTest("Lift CANrange", () -> canRange.getDistance().getValueAsDouble()));
    }

    @Override
    public void setSetpoint(double motorRotations, double motorRotationsPerSecond) {
        motor.setControl(
                positionRequest.withPosition(motorRotations).withVelocity(motorRotationsPerSecond));
    }

    public void updateInputs(LiftIOInputs inputs) {

        inputs.motorConnected =
                BaseStatusSignal.refreshAll(
                                positionSignal,
                                velocitySignal,
                                tempSignal,
                                supplyCurrentSignal,
                                statorCurrentSignal,
                                voltageSignal)
                        .isOK();
        inputs.canrangeConnected = BaseStatusSignal.refreshAll(canrangeDistanceSignal).isOK();

        inputs.motorPositionRotations = positionSignal.getValueAsDouble();
        inputs.motorVelocityRPS = velocitySignal.getValueAsDouble();
        inputs.motorMotorTemp = tempSignal.getValueAsDouble();
        inputs.motorSupplyCurrent = supplyCurrentSignal.getValueAsDouble();
        inputs.motorStatorCurrent = supplyCurrentSignal.getValueAsDouble();
        inputs.motorAppliedVolts = voltageSignal.getValueAsDouble();

        inputs.canrangeDistance = canrangeDistanceSignal.getValueAsDouble();
    }

    @Override
    public void initEncoder(double hei) {
        motor.setPosition(hei);
    }
}
