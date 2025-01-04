package frc.robot.subsystems.pivot;

import com.ctre.phoenix6.configs.*;
import com.ctre.phoenix6.controls.Follower;
import com.ctre.phoenix6.controls.MotionMagicVoltage;
import com.ctre.phoenix6.controls.VoltageOut;
import com.ctre.phoenix6.hardware.CANcoder;
import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.signals.ReverseLimitValue;
import com.ctre.phoenix6.signals.SensorDirectionValue;
import edu.wpi.first.math.MathUtil;
import frc.robot.constants.GeneralConstants;

import org.teamtators.tester.*;
import org.teamtators.tester.components.*;

public class PivotIOReal implements PivotIO {
    private TalonFX leader;
    private TalonFX follower;
    private boolean magicEnabled;
    private MotionMagicVoltage magic;
    private VoltageOut voltage;
    private Follower followRequest;
    private CANcoder encoder;
    private double encoderPosition = 0;

    public PivotIOReal() {
        leader = new TalonFX(
            PivotConstants.kLeaderID,
            GeneralConstants.kCanivoreBusName);
        follower = new TalonFX(
            PivotConstants.kFollowerID,
            GeneralConstants.kCanivoreBusName);

        followRequest = new Follower(leader.getDeviceID(), false);
        follower.setControl(followRequest);
        magic = new MotionMagicVoltage(PivotConstants.PivotPositions.HOME.kDegrees);
        voltage = new VoltageOut(0.0);
        magicEnabled = true;

        encoder = new CANcoder(
            PivotConstants.kCancoderID,
            GeneralConstants.kCanivoreBusName);

        TalonFXConfiguration config = new TalonFXConfiguration()
            .withSlot0(new Slot0Configs()
                .withKS(0.18)
                .withKV(0.12)
                .withKP(6.4))
            .withMotionMagic(new MotionMagicConfigs()
                .withMotionMagicCruiseVelocity(60)
                .withMotionMagicAcceleration(140))
            //    .withMotionMagicCruiseVelocity(1)
            //    .withMotionMagicAcceleration(1))
            .withCurrentLimits(new CurrentLimitsConfigs()
                .withStatorCurrentLimit(60)
                .withSupplyCurrentLimit(60)
                .withStatorCurrentLimitEnable(true)
                .withSupplyCurrentLimitEnable(true));

        for(int i = 0; i < 5; i++) {
            if (leader.getConfigurator().apply(config).isOK()) {
                break;
            }
        }
        for(int i = 0; i < 5; i++) {
            if (follower.getConfigurator().apply(config).isOK()) {
                break;
            }
        }

        CANcoderConfiguration encoderConfig = new CANcoderConfiguration()
            .withMagnetSensor(new MagnetSensorConfigs()
                .withSensorDirection(SensorDirectionValue.Clockwise_Positive)
                .withMagnetOffset(0.052203932)
                .withAbsoluteSensorDiscontinuityPoint(1));
        for (int i = 0; i < 5; i++) {
            if (encoder.getConfigurator().apply(encoderConfig).isOK()) {
                break;
            }
        }
    }

    @Override
    public void updateInputs(PivotIOInputs inputs) {
        inputs.supplyCurrent = leader.getSupplyCurrent().getValueAsDouble();
        inputs.statorCurrent = leader.getStatorCurrent().getValueAsDouble();
        inputs.torqueCurrent = leader.getTorqueCurrent().getValueAsDouble();
        inputs.dutyCycle = leader.getDutyCycle().getValueAsDouble();
        inputs.controlMode = leader.getControlMode().getValue().toString();
        inputs.connected = !leader.getPosition().getStatus().isError();
        inputs.positionRotations = leader.getPosition().getValueAsDouble();
        inputs.velocityRPS = leader.getVelocity().getValueAsDouble();
        inputs.tempCelcius = leader.getDeviceTemp().getValueAsDouble();

        inputs.absoluteEncoderPosition = encoder.getAbsolutePosition().getValueAsDouble();
        encoderPosition = inputs.absoluteEncoderPosition;
    }
    
    @Override
    public void setEncoderPosition(double value) {
        leader.setPosition(value);
        follower.setPosition(value);
    }

    @Override
    public void setSetpoint(double theta) {
        var clamped = MathUtil.clamp(theta, PivotConstants.kHardMinTarget, PivotConstants.kHardMaxTarget);
        magic.Position = PivotConstants.degreesToRotations(clamped);
        //System.out.println("PIVOT THETA: " + theta);
        magicEnabled = true;
    }

    @Override
    public void setVolts(double volts) {
        voltage.Output = volts;
        magicEnabled = false;
    }

    @Override
    public void updateControls() {
        if (magicEnabled) {
            leader.setControl(magic
                .withLimitForwardMotion(encoderPosition > PivotConstants
                    .relativeEncoderToAbsoluteEncoder(PivotConstants.kHardMaxTarget))
                .withLimitReverseMotion(encoderPosition < PivotConstants
                    .relativeEncoderToAbsoluteEncoder(PivotConstants.kHardMinTarget)));
        } else {
            leader.setControl(voltage);
        }

        follower.setControl(followRequest);
    }

    @Override
    public ManualTestGroup getManualTest() {
        return new ManualTestGroup(
            "Pivot",
            new MotorTest("Leader", leader::set),
            new MotorTest("Follower", follower::set),
            new BinarySensorTest("Hard Reverse Limit", () -> leader.getReverseLimit().getValue() == ReverseLimitValue.ClosedToGround),
            new ContinuousSensorTest("Cancoder", () -> encoder
                .getAbsolutePosition()
                .getValueAsDouble())
        );
    }
}
