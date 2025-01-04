package frc.robot.subsystems.kingRollers;

import com.ctre.phoenix6.configs.MotorOutputConfigs;
import com.ctre.phoenix6.signals.NeutralModeValue;
import org.teamtators.Util.DigitalSensor;
import org.teamtators.tester.ManualTestGroup;
import org.teamtators.tester.components.BinarySensorTest;
import org.teamtators.tester.components.MotorTest;

import com.ctre.phoenix6.configs.CurrentLimitsConfigs;
import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.controls.VoltageOut;
import com.ctre.phoenix6.hardware.TalonFX;

import frc.robot.constants.GeneralConstants;

public class KingRollersIOReal implements KingRollersIO {
    TalonFX motor;
    VoltageOut voltage;

    DigitalSensor exitSensor;
    DigitalSensor safetySensor;

    public KingRollersIOReal() {
        motor = new TalonFX(KingRollersConstants.kMotorID,
                              GeneralConstants.kCanivoreBusName);
        TalonFXConfiguration config = new TalonFXConfiguration()
            .withCurrentLimits(new CurrentLimitsConfigs()
                .withStatorCurrentLimit(40)
                .withSupplyCurrentLimit(40)
                .withStatorCurrentLimitEnable(true)
                .withSupplyCurrentLimitEnable(true))
            .withMotorOutput(new MotorOutputConfigs()
                .withNeutralMode(NeutralModeValue.Brake));
        motor.getConfigurator().apply(config);
        voltage = new VoltageOut(0);

        exitSensor = new DigitalSensor(KingRollersConstants.kExitSensorID);
        safetySensor = new DigitalSensor(KingRollersConstants.kSafetySensorID, true);
    }

    @Override
    public void updateInputs(KingRollersIOInputs inputs) {
        inputs.supplyCurrent = motor.getSupplyCurrent().getValueAsDouble();
        inputs.statorCurrent = motor.getStatorCurrent().getValueAsDouble();
        inputs.torqueCurrent = motor.getTorqueCurrent().getValueAsDouble();
        inputs.dutyCycle     = motor.getDutyCycle().getValueAsDouble();
        inputs.controlMode   = motor.getControlMode().getValue().name();
        inputs.connected     = !motor.getPosition().getStatus().isError();
        inputs.velocityRPS   = motor.getVelocity().getValueAsDouble();
        inputs.tempCelcius   = motor.getDeviceTemp().getValueAsDouble();
        inputs.appliedVolts  = motor.getMotorVoltage().getValueAsDouble();

        inputs.noteSensor = exitSensor.get();
        inputs.safetySensor = safetySensor.get();
    }

    @Override
    public void setSpeed(KingRollers.Speeds speed) {
        motor.setControl(voltage.withOutput(speed.kVolts));
    }

    @Override
    public ManualTestGroup getManualTest() {
        return new ManualTestGroup(
            "King Rollers",
            new MotorTest("Motor", motor::set),
            new BinarySensorTest("Exit Sensor", exitSensor::get),
            new BinarySensorTest("Safety Sensor", safetySensor::get));
    }
}
