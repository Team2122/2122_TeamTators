package frc.robot.subsystems.upperNotePath;

import com.ctre.phoenix6.configs.*;
import com.ctre.phoenix6.signals.InvertedValue;
import edu.wpi.first.wpilibj.motorcontrol.PWMSparkMax;
import org.teamtators.Util.DigitalSensor;
import org.teamtators.tester.ManualTestGroup;
import org.teamtators.tester.components.BinarySensorTest;
import org.teamtators.tester.components.MotorTest;

import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.controls.VelocityVoltage;
import com.ctre.phoenix6.controls.VoltageOut;

import frc.robot.constants.GeneralConstants;
import frc.robot.subsystems.upperNotePath.UpperNotePath.ShooterSpeeds;

public class UpperNotePathIOReal implements UpperNotePathIO {
    private TalonFX leftShooter;
    private VelocityVoltage leftVelocity;

    private TalonFX rightShooter;
    private VelocityVoltage rightVelocity;
    private VoltageOut rightVoltage;

    private PWMSparkMax dunkerMotor;
    private DigitalSensor dunkerSensor;
    private double dunkerVolts;

    private DigitalSensor diverterSensor;

    public UpperNotePathIOReal() {
        leftShooter = new TalonFX(
            UpperNotePathConstants.kLeftShooterID,
            GeneralConstants.kCanivoreBusName);
        leftVelocity = new VelocityVoltage(0);

        rightShooter = new TalonFX(
            UpperNotePathConstants.kRightShooterID,
            GeneralConstants.kCanivoreBusName);
        rightVelocity = new VelocityVoltage(0)
            .withSlot(0);
        rightVoltage = new VoltageOut(0);
        
        dunkerMotor = new PWMSparkMax(UpperNotePathConstants.kDunkerMotorID);
        dunkerSensor = new DigitalSensor(UpperNotePathConstants.kDunkerSensorChannel, true);

        diverterSensor = new DigitalSensor(UpperNotePathConstants.kDiverterLimitChannel, true);

        TalonFXConfiguration conf = new TalonFXConfiguration()
            .withCurrentLimits(new CurrentLimitsConfigs()
                .withStatorCurrentLimit(40)
                .withSupplyCurrentLimit(40)
                .withStatorCurrentLimitEnable(true)
                .withSupplyCurrentLimitEnable(true))
            // velocity
            .withSlot0(new Slot0Configs()
                .withKV(0.116)
                .withKA(0)
                .withKS(0.150390625)
                .withKP(0.06)
                .withKI(0.002)
                .withKD(0.006));

        for (int i = 0; i < 5; i++) {
            if (leftShooter.getConfigurator().apply(conf).isOK()) {
                break;
            }
        }

        var invertedConf = conf
            .withMotorOutput(new MotorOutputConfigs()
                .withInverted(InvertedValue.Clockwise_Positive));

        for (int i = 0; i < 5; i++) {
            if (rightShooter.getConfigurator().apply(invertedConf).isOK()) {
                break;
            }
        }
    }

    @Override
    public void updateInputs(UpperNotePathIOInputs inputs) {
        inputs.leftShooterSupplyCurrent = leftShooter.getSupplyCurrent().getValueAsDouble();
        inputs.leftShooterStatorCurrent = leftShooter.getStatorCurrent().getValueAsDouble();
        inputs.leftShooterTorqueCurrent = leftShooter.getTorqueCurrent().getValueAsDouble();
        inputs.leftShooterDutyCycle     = leftShooter.getDutyCycle().getValueAsDouble();
        inputs.leftShooterControlMode   = leftShooter.getControlMode().getValue().name();
        inputs.leftShooterConnected     = !leftShooter.getPosition().getStatus().isError();
        inputs.leftShooterVelocityRPS   = leftShooter.getVelocity().getValueAsDouble();
        inputs.leftShooterTempCelcius   = leftShooter.getDeviceTemp().getValueAsDouble();
        inputs.leftShooterAppliedVolts  = leftShooter.getMotorVoltage().getValueAsDouble();

        inputs.rightShooterSupplyCurrent     = rightShooter.getSupplyCurrent().getValueAsDouble();
        inputs.rightShooterStatorCurrent     = rightShooter.getStatorCurrent().getValueAsDouble();
        inputs.rightShooterTorqueCurrent     = rightShooter.getTorqueCurrent().getValueAsDouble();
        inputs.rightShooterDutyCycle         = rightShooter.getDutyCycle().getValueAsDouble();
        inputs.rightShooterControlMode       = rightShooter.getControlMode().getValue().name();
        inputs.rightShooterVelocityRPS       = rightShooter.getVelocity().getValueAsDouble();
        inputs.rightShooterTempCelcius       = rightShooter.getDeviceTemp().getValueAsDouble();
        inputs.rightShooterAppliedVolts      = rightShooter.getMotorVoltage().getValueAsDouble();
        inputs.rightShooterPositionRotations = rightShooter.getPosition().getValueAsDouble();
        inputs.rightShooterPositionDegrees   = inputs.rightShooterPositionRotations * 360;
        inputs.rightShooterConnected         = !rightShooter.getPosition().getStatus().isError();

        inputs.diverterSensor = diverterSensor.get();

        inputs.dunkerDutyCycle = dunkerVolts;
        inputs.dunkerMotorInverted = dunkerMotor.getInverted();
        inputs.dunkerSensor = dunkerSensor.get();
    }

    @Override
    public void updateControls() {
        dunkerMotor.setVoltage(dunkerVolts);
    }

    @Override
    public void setShooterSpeeds(ShooterSpeeds speeds) {
        if (speeds != ShooterSpeeds.DROP && speeds != ShooterSpeeds.STOW) {
            setShooterSpeeds(speeds.kLeftRPS, speeds.kRightRPS);
        } else {
            leftShooter.setControl(leftVelocity.withVelocity(speeds.kLeftRPS));
            holdDiverter();
        }
    }

    @Override
    public void setShooterSpeeds(double left, double right) {
        leftShooter.setControl(leftVelocity.withVelocity(left));
        rightShooter.setControl(rightVelocity.withVelocity(right));
    }

    @Override
    public void flipDiverterUp() {
        double volts = UpperNotePathConstants.kDiverterFlipVoltage;
        rightShooter.setControl(rightVoltage
            .withOutput(volts));
    }

    @Override
    public void holdDiverter() {
        double volts = UpperNotePathConstants.kDiverterHoldVoltage;
        rightShooter.setControl(rightVoltage
            .withOutput(volts));
    }

    @Override
    public void setDunkerVoltage(double volts) {
        dunkerVolts = volts;
    }

    @Override
    public ManualTestGroup getManualTests() {
        return new ManualTestGroup(
            "Upper Note Path",
            new MotorTest("Left Shooter", leftShooter::set),
            new MotorTest("Right Shooter", rightShooter::set),
            new MotorTest("Dunker Motor", dunkerMotor::set),
            new BinarySensorTest("Dunker Sensor", dunkerSensor::get),
            new BinarySensorTest("Diverter Sensor", diverterSensor::get)
        );
    }
}
