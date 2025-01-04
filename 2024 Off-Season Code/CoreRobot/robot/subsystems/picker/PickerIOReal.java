package frc.robot.subsystems.picker;

import org.teamtators.Util.DigitalSensor;
import org.teamtators.tester.ManualTestGroup;
import org.teamtators.tester.components.BinarySensorTest;
import org.teamtators.tester.components.MotorTest;

import com.ctre.phoenix6.configs.CurrentLimitsConfigs;
import com.ctre.phoenix6.configs.MotorOutputConfigs;
import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.controls.VoltageOut;
import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.signals.NeutralModeValue;

import frc.robot.constants.GeneralConstants;

public class PickerIOReal implements PickerIO {
    private TalonFX motor;
    private VoltageOut voltage;

    /*private DigitalSensor closeEntrance;*/
    private DigitalSensor farEntrance;

    private DigitalSensor chimney;

    public PickerIOReal() {
        voltage = new VoltageOut(0);

        motor = new TalonFX(
            PickerConstants.MOTOR_ID,
            GeneralConstants.kCanivoreBusName);

        TalonFXConfiguration config = new TalonFXConfiguration()
            .withCurrentLimits(new CurrentLimitsConfigs()
                .withStatorCurrentLimit(40)
                .withSupplyCurrentLimit(40)
                .withStatorCurrentLimitEnable(true)
                .withSupplyCurrentLimitEnable(true))
            .withMotorOutput(new MotorOutputConfigs()
                .withNeutralMode(NeutralModeValue.Brake));

        for(int i = 0; i < 5; i++) {
            if (motor.getConfigurator().apply(config).isOK()) {
                break;
            }
        }

        farEntrance = new DigitalSensor(11);
        chimney = new DigitalSensor(12);
    }

    @Override
    public void updateInputs(PickerIOInputs inputs) {
        inputs.supplyCurrent = motor.getSupplyCurrent().getValueAsDouble();
        inputs.statorCurrent = motor.getStatorCurrent().getValueAsDouble();
        inputs.torqueCurrent = motor.getTorqueCurrent().getValueAsDouble();
        inputs.dutyCycle     = motor.getDutyCycle().getValueAsDouble();
        inputs.controlMode   = motor.getControlMode().getValue().name();
        inputs.connected     = !motor.getPosition().getStatus().isError();
        inputs.velocityRPS   = motor.getVelocity().getValueAsDouble();
        inputs.tempCelcius   = motor.getDeviceTemp().getValueAsDouble();
        inputs.appliedVolts  = motor.getMotorVoltage().getValueAsDouble();

        inputs.farEntranceActivated = farEntrance.get();
        inputs.chimneySensorActivated = chimney.get();
    }

    @Override
    public void setSpeed(Picker.Speeds speed) {
        motor.setControl(voltage.withOutput(speed.volts));
    }

    @Override
    public ManualTestGroup getManualTest() {
        return new ManualTestGroup(
            "Picker",
            new MotorTest("Motor", motor::set),
            new BinarySensorTest("Far Entrance Sensor", farEntrance::get),
            new BinarySensorTest("Chimney Sensor", chimney::get));
    }
}
