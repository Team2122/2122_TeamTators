package frc.robot.subsystems.climber;

import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.hardware.CANcoder;
import com.ctre.phoenix6.controls.Follower;
import com.ctre.phoenix6.controls.PositionVoltage;
import com.ctre.phoenix6.controls.VoltageOut;
import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.configs.CANcoderConfiguration;
import com.ctre.phoenix6.configs.CurrentLimitsConfigs;
import com.ctre.phoenix6.configs.FeedbackConfigs;
import com.ctre.phoenix6.configs.Slot0Configs;
import com.ctre.phoenix6.configs.MagnetSensorConfigs;
import com.ctre.phoenix6.configs.MotorOutputConfigs;
import com.ctre.phoenix6.signals.SensorDirectionValue;
import com.ctre.phoenix6.signals.FeedbackSensorSourceValue;
import com.ctre.phoenix6.signals.NeutralModeValue;

import org.teamtators.tester.ManualTestGroup;
import org.teamtators.tester.components.BinarySensorTest;
import org.teamtators.tester.components.MotorTest;
import org.teamtators.Util.DigitalSensor;

import frc.robot.constants.GeneralConstants;

public class ClimberIOReal implements ClimberIO {
    TalonFX leader;
    TalonFX follower;
    PositionVoltage positionRequest;
    VoltageOut voltageRequest;
    Follower followerRequest;

    CANcoder cancoder;

    DigitalSensor downSensor;
    DigitalSensor upSensor;

    public ClimberIOReal() {
        leader = new TalonFX(ClimberConstants.kLeaderID,
            GeneralConstants.kCanivoreBusName);
        follower = new TalonFX(ClimberConstants.kFollowerID,
            GeneralConstants.kCanivoreBusName);
        positionRequest = new PositionVoltage(0);
        voltageRequest = new VoltageOut(0);
        followerRequest = new Follower(leader.getDeviceID(), false);

        cancoder = new CANcoder(ClimberConstants.kCancoderID,
            GeneralConstants.kCanivoreBusName);

        downSensor = new DigitalSensor(ClimberConstants.kDownSensorID);
        upSensor = new DigitalSensor(ClimberConstants.kUpSensorID);

        CANcoderConfiguration cancoderConfig = new CANcoderConfiguration()
            .withMagnetSensor(new MagnetSensorConfigs()
                .withSensorDirection(SensorDirectionValue.Clockwise_Positive)
                .withAbsoluteSensorDiscontinuityPoint(1)
                .withMagnetOffset(0));
        for (int i = 0; i < 5; i++) {
            if (cancoder.getConfigurator().apply(cancoderConfig).isOK()) {
                break;
            }
        }

        TalonFXConfiguration motorConfig = new TalonFXConfiguration()
            .withCurrentLimits(new CurrentLimitsConfigs()
                .withStatorCurrentLimit(40)
                .withSupplyCurrentLimit(40)
                .withStatorCurrentLimitEnable(true)
                .withSupplyCurrentLimitEnable(true))
            .withSlot0(new Slot0Configs()
                .withKV(0)
                .withKA(0)
                .withKS(0)
                .withKP(0)
                .withKI(0)
                .withKD(0))
            .withMotorOutput(new MotorOutputConfigs()
                .withNeutralMode(NeutralModeValue.Brake));
        for (int i = 0; i < 5; i++) {
            if (leader.getConfigurator().apply(motorConfig).isOK()) {
                break;
            }
        }
        for (int i = 0; i < 5; i++) {
            if (follower.getConfigurator().apply(motorConfig).isOK()) {
                break;
            }
        }
    }

    @Override
    public void updateInputs(ClimberIOInputs inputs) {
        inputs.supplyCurrent          = leader.getSupplyCurrent().getValueAsDouble();
        inputs.statorCurrent          = leader.getStatorCurrent().getValueAsDouble();
        inputs.torqueCurrent          = leader.getTorqueCurrent().getValueAsDouble();
        inputs.dutyCycle              = leader.getDutyCycle().getValueAsDouble();
        inputs.controlMode            = leader.getControlMode().getValue().name();
        inputs.connected              = !leader.getPosition().getStatus().isError();
        inputs.motorVelocityRPS       = leader.getVelocity().getValueAsDouble();
        inputs.tempCelcius            = leader.getDeviceTemp().getValueAsDouble();
        inputs.appliedVolts           = leader.getMotorVoltage().getValueAsDouble();
        inputs.motorPositionRotations = leader.getPosition().getValueAsDouble();

        inputs.downSensorHit = downSensor.get();
        inputs.upSensorHit = upSensor.get();

        inputs.cancoderPosition = cancoder.getPosition().getValueAsDouble();
        inputs.cancoderVelocity = cancoder.getVelocity().getValueAsDouble();
    }

    @Override
    public void setSetpoint(Climber.Position position) {
        leader.setControl(positionRequest.withPosition(position.getRotations()));
        follower.setControl(followerRequest);
    }

    @Override
    public void setVoltage(double volts) {
        leader.setControl(voltageRequest.withOutput(volts));
        follower.setControl(followerRequest);
    }

    @Override
    public void setEncoderPosition(double motorRotations) {
        leader.setPosition(motorRotations);
        follower.setPosition(motorRotations);
    }

    @Override
    public ManualTestGroup getManualTest() {
        return new ManualTestGroup(
            "Climber",
            new MotorTest("Motor", input -> {
                leader  .set(input/2);
                follower.set(input/2);
            }),
            new BinarySensorTest("Up Sensor", upSensor::get),
            new BinarySensorTest("Down Sensor", downSensor::get));
    }
}
