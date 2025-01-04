package frc.robot.subsystems.swerve;

import com.ctre.phoenix6.BaseStatusSignal;
import com.ctre.phoenix6.StatusSignal;
import com.ctre.phoenix6.configs.*;
import com.ctre.phoenix6.controls.VelocityVoltage;
import com.ctre.phoenix6.hardware.CANcoder;
import com.ctre.phoenix6.hardware.ParentDevice;
import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.signals.FeedbackSensorSourceValue;
import com.ctre.phoenix6.signals.NeutralModeValue;
import com.ctre.phoenix6.signals.SensorDirectionValue;
import com.ctre.phoenix6.controls.PositionVoltage;
import org.teamtators.tester.ManualTest;
import org.teamtators.tester.components.SwerveModuleTest;

import java.util.Queue;

import org.teamtators.Util.QuickDebug;

import edu.wpi.first.math.filter.Debouncer;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import static edu.wpi.first.units.Units.*;
import edu.wpi.first.units.measure.*;
import frc.robot.constants.GeneralConstants;
import frc.robot.subsystems.swerve.Module.SwerveModuleMotor;
import frc.robot.subsystems.swerve.SwerveConstants.SwerveModuleConstants;
import frc.robot.subsystems.swerve.SwerveDrive.ModuleFramePositions;

public class ModuleIOReal implements ModuleIO {
    private TalonFX drive;
    private TalonFX azimuth;
    private CANcoder cancoder;

    private VelocityVoltage driveRequest;
    private PositionVoltage steerRequest;

    private CurrentLimitsConfigs zoomCurrents;
    private CurrentLimitsConfigs noZoomCurrents;

    ModuleFramePositions framePosition;

    // Timestamp inputs from odometry thread
    Queue<Double> timestampQueue;

    // Inputs from cancoder
    StatusSignal<Angle> cancoderAbsolutePosition;
    StatusSignal<AngularVelocity> cancoderVelocity;

    // Inputs from drive motor
    Queue<Double> drivePositionQueue;
    StatusSignal<Angle> drivePosition;
    StatusSignal<AngularVelocity> driveVelocity;
    StatusSignal<Temperature> driveDeviceTemp;
    StatusSignal<Current> driveStatorCurrent;
    StatusSignal<Current> driveSupplyCurrent;
    StatusSignal<Current> driveTorqueCurrent;
    StatusSignal<Voltage> driveAppliedVolts;

    // Inputs from azimuth motor
    Queue<Double> azimuthPositionQueue;
    StatusSignal<Angle> azimuthPosition;
    StatusSignal<AngularVelocity> azimuthVelocity;
    StatusSignal<Current> azimuthStatorCurrent;
    StatusSignal<Current> azimuthSupplyCurrent;
    StatusSignal<Current> azimuthTorqueCurrent;
    StatusSignal<Voltage> azimuthAppliedVolts;
    StatusSignal<Temperature> azimuthDeviceTemp;

    private final Debouncer driveConnectedDebouncer =
        new Debouncer(0.5);
    private final Debouncer azimuthConnectedDebouncer =
        new Debouncer(0.5);
    private final Debouncer cancoderConnectedDebouncer =
        new Debouncer(0.5);

    public ModuleIOReal(ModuleFramePositions modulePosition,
        int driveId, int azimuthId, int cancoderId)
    {
        this.framePosition = modulePosition;

        drive = new TalonFX(driveId, GeneralConstants.kCanivoreBusName);
        cancoder = new CANcoder(cancoderId, GeneralConstants.kCanivoreBusName);
        azimuth = new TalonFX(azimuthId, GeneralConstants.kCanivoreBusName);

        driveRequest = new VelocityVoltage(0);
        steerRequest = new PositionVoltage(0);

        zoomCurrents = new CurrentLimitsConfigs()
            .withStatorCurrentLimitEnable(true)
            .withSupplyCurrentLimitEnable(true)
            .withStatorCurrentLimit(65)
            .withSupplyCurrentLimit(45);
        noZoomCurrents = new CurrentLimitsConfigs()
            .withStatorCurrentLimitEnable(true)
            .withSupplyCurrentLimitEnable(true)
            .withStatorCurrentLimit(50)
            .withSupplyCurrentLimit(35);

        QuickDebug.output("Zoomy Mode", false);

        TalonFXConfiguration driveConfig = new TalonFXConfiguration()
            .withSlot0(new Slot0Configs()
                .withKS(0.19)
                .withKV(0.114)
                .withKP(0.32))
            .withMotorOutput(new MotorOutputConfigs()
                .withNeutralMode(NeutralModeValue.Brake))
            .withClosedLoopRamps(new ClosedLoopRampsConfigs()
                .withDutyCycleClosedLoopRampPeriod(SwerveModuleConstants.kDriveRampPeriod)
                .withTorqueClosedLoopRampPeriod(SwerveModuleConstants.kDriveRampPeriod)
                .withVoltageClosedLoopRampPeriod(SwerveModuleConstants.kDriveRampPeriod))
            .withOpenLoopRamps(new OpenLoopRampsConfigs()
                .withDutyCycleOpenLoopRampPeriod(SwerveModuleConstants.kDriveRampPeriod)
                .withTorqueOpenLoopRampPeriod(SwerveModuleConstants.kDriveRampPeriod)
                .withVoltageOpenLoopRampPeriod(SwerveModuleConstants.kDriveRampPeriod))
            .withCurrentLimits(zoomCurrents)
            .withTorqueCurrent(new TorqueCurrentConfigs()
                .withPeakForwardTorqueCurrent(40)
                .withPeakReverseTorqueCurrent(-40));

        // grumble grumble no `new ClosedLoopGeneralConfigs.withContinuousWrap(boolean)`
        // grumble grumble
        ClosedLoopGeneralConfigs continuousWrapConfig = new ClosedLoopGeneralConfigs();
        continuousWrapConfig.ContinuousWrap = true;
        TalonFXConfiguration azimuthConfig = new TalonFXConfiguration()
            .withSlot0(new Slot0Configs()
                .withKP(48))
            .withClosedLoopGeneral(continuousWrapConfig)
            .withFeedback(new FeedbackConfigs()
                .withRotorToSensorRatio(SwerveModuleConstants.kSteerGearing)
                // TODO license motors and make this FusedCANcoder
                .withFeedbackSensorSource(FeedbackSensorSourceValue.RemoteCANcoder)
                .withFeedbackRemoteSensorID(cancoder.getDeviceID())
                .withSensorToMechanismRatio(1.0))
            .withCurrentLimits(new CurrentLimitsConfigs()
                .withStatorCurrentLimit(40)
                .withSupplyCurrentLimit(40)
                .withStatorCurrentLimitEnable(true)
                .withSupplyCurrentLimitEnable(true));

        CANcoderConfiguration cancoderConfig = new CANcoderConfiguration()
            .withMagnetSensor(new MagnetSensorConfigs()
                .withSensorDirection(SensorDirectionValue.CounterClockwise_Positive)
                .withMagnetOffset(SwerveModuleConstants.kCancoderOffsets[modulePosition.ordinal()]));

        // continuously flash the configs until it succeeds
        // if it takes more than 5 tries just give up
        for (int i = 0; i < 5; i++) {
            if (drive.getConfigurator().apply(driveConfig).isOK()) {
                break;
            }
        }

        for (int i = 0; i < 5; i++) {
            if (azimuth.getConfigurator().apply(azimuthConfig).isOK()) {
                break;
            }
        }

        for (int i = 0; i < 5; i++) {
            if (cancoder.getConfigurator().apply(cancoderConfig).isOK()) {
                break;
            }
        }

        drive.setPosition(0);
        azimuth.setPosition(cancoder.getAbsolutePosition().getValueAsDouble()
                          * SwerveModuleConstants.kSteerGearing);

        // create status signals
        cancoderAbsolutePosition = cancoder.getAbsolutePosition();
        cancoderVelocity         = cancoder.getVelocity();

        drivePositionQueue = PhoenixOdometryThread.getInstance()
            .registerSignal(() -> (Double)drive.getPosition().getValueAsDouble());
        drivePosition      = drive.getPosition();
        driveVelocity      = drive.getVelocity();
        driveDeviceTemp    = drive.getDeviceTemp();
        driveStatorCurrent = drive.getStatorCurrent();
        driveSupplyCurrent = drive.getSupplyCurrent();
        driveTorqueCurrent = drive.getTorqueCurrent();
        driveAppliedVolts  = drive.getMotorVoltage();

        azimuthPositionQueue = PhoenixOdometryThread.getInstance()
            .registerSignal(() -> (Double)azimuth.getPosition().getValueAsDouble());
        azimuthPosition = azimuth.getPosition();
        azimuthVelocity   = azimuth.getVelocity();
        azimuthStatorCurrent = azimuth.getStatorCurrent();
        azimuthSupplyCurrent = azimuth.getSupplyCurrent();
        azimuthTorqueCurrent = azimuth.getTorqueCurrent();
        azimuthAppliedVolts  = azimuth.getMotorVoltage();
        azimuthDeviceTemp     = azimuth.getDeviceTemp();

        timestampQueue = PhoenixOdometryThread.getInstance()
            .makeTimestampQueue();

        // Configure update frequency
        BaseStatusSignal.setUpdateFrequencyForAll(
            SwerveConstants.kOdometryFrequency,
            drivePosition,
            azimuthPosition);
        BaseStatusSignal.setUpdateFrequencyForAll(
            50.0,
            
            cancoderAbsolutePosition,
            cancoderAbsolutePosition,
            cancoderVelocity,

            drivePosition,
            driveVelocity,
            driveDeviceTemp,
            driveStatorCurrent,
            driveSupplyCurrent,
            driveTorqueCurrent,
            driveAppliedVolts,

            azimuthVelocity,
            azimuthStatorCurrent,
            azimuthSupplyCurrent,
            azimuthTorqueCurrent,
            azimuthAppliedVolts,
            azimuthDeviceTemp);
        ParentDevice.optimizeBusUtilizationForAll(drive, azimuth);
    }

    public void updateInputs(ModuleIOInputs inputs) {
        // refresh signals
        var driveStatus = BaseStatusSignal.refreshAll(
            drivePosition,
            driveVelocity,
            driveDeviceTemp,
            driveStatorCurrent,
            driveSupplyCurrent,
            driveTorqueCurrent,
            driveAppliedVolts);
        var azimuthStatus = BaseStatusSignal.refreshAll(
            azimuthVelocity,
            azimuthStatorCurrent,
            azimuthSupplyCurrent,
            azimuthTorqueCurrent,
            azimuthAppliedVolts,
            azimuthDeviceTemp);
        var cancoderStatus = BaseStatusSignal.refreshAll(
            cancoderAbsolutePosition,
            cancoderVelocity);

        // update cancoder inputs
        inputs.cancoderAbsolutePosition = cancoderAbsolutePosition.getValue().in(Rotations);
        inputs.cancoderVelocity         = cancoderVelocity.getValue().in(RotationsPerSecond);
        inputs.cancoderConnected        = cancoderConnectedDebouncer
            .calculate(cancoderStatus.isOK());

        // update drive inputs
        inputs.drivePositionRotations = drivePosition.getValue().in(Rotations);
        inputs.driveVelocityRPS       = driveVelocity.getValue().in(RotationsPerSecond);
        inputs.driveMotorTemp         = driveDeviceTemp.getValue().in(Celsius);
        inputs.driveStatorCurrent     = driveStatorCurrent.getValue().in(Amps);
        inputs.driveSupplyCurrent     = driveSupplyCurrent.getValue().in(Amps);
        inputs.driveTorqueCurrent     = driveTorqueCurrent.getValue().in(Amps);
        inputs.driveAppliedVolts      = driveAppliedVolts.getValue().in(Volts);
        inputs.driveConnected         = driveConnectedDebouncer
            .calculate(driveStatus.isOK());

        // update azimuth inputs
        inputs.azimuthPositionRotations = azimuthPosition.getValue().in(Rotations);
        inputs.azimuthVelocityRPM       = azimuthVelocity.getValue().in(RotationsPerSecond);
        inputs.azimuthStatorCurrent     = azimuthStatorCurrent.getValue().in(Amps);
        inputs.azimuthSupplyCurrent     = azimuthSupplyCurrent.getValue().in(Amps);
        inputs.azimuthTorqueCurrent     = azimuthTorqueCurrent.getValue().in(Amps);
        inputs.azimuthAppliedVolts      = azimuthAppliedVolts.getValue().in(Volts);
        inputs.azimuthMotorTemp         = azimuthDeviceTemp.getValue().in(Celsius);
        inputs.azimuthConnected        = azimuthConnectedDebouncer
            .calculate(azimuthStatus.isOK());

        // update odometry inputs
        inputs.odometryTimestamps = timestampQueue.stream()
            .mapToDouble((Double value) -> value).toArray();

        inputs.odometryDrivePositionsRotations =
          drivePositionQueue.stream()
              .mapToDouble((Double value) -> value)
              .toArray();

        inputs.odometryAzimuthPositionsRotations =
          azimuthPositionQueue.stream()
              .mapToDouble((Double value) -> value)
              .toArray();

        timestampQueue.clear();
        drivePositionQueue.clear();
        azimuthPositionQueue.clear();
    }

    /*
    private double ks;
    private double kv;
    private double kp;
    private double ki;
    private double kd;

     */
    public void setSetpoint(SwerveModuleState state) {
        azimuth.setControl(steerRequest
            .withPosition(state.angle.getRotations()));

        /*
        double input;

        input = QuickDebug.input("Tuning/Module KS", 0.0);
        if (ks != input) {
            ks = input;
            Slot0Configs conf = new Slot0Configs();
            drive.getConfigurator().refresh(conf);
            conf.kS = input;
            drive.getConfigurator().apply(conf);
        }

        input = QuickDebug.input("Tuning/Module kV", 0.0);
        if (kv != input) {
            kv = input;
            Slot0Configs conf = new Slot0Configs();
            drive.getConfigurator().refresh(conf);
            conf.kV = input;
            drive.getConfigurator().apply(conf);
        }

        input = QuickDebug.input("Tuning/Module kP", 0.0);
        if (kp != input) {
            kp = input;
            Slot0Configs conf = new Slot0Configs();
            drive.getConfigurator().refresh(conf);
            conf.kP = input;
            drive.getConfigurator().apply(conf);
        }

        input = QuickDebug.input("Tuning/Module kI", 0.0);
        if (ki != input) {
            ki = input;
            Slot0Configs conf = new Slot0Configs();
            drive.getConfigurator().refresh(conf);
            conf.kI = input;
            drive.getConfigurator().apply(conf);
        }

        input = QuickDebug.input("Tuning/Module kD", 0.0);
        if (kd != input) {
            kd = input;
            Slot0Configs conf = new Slot0Configs();
            drive.getConfigurator().refresh(conf);
            conf.kD = input;
            drive.getConfigurator().apply(conf);
        }*/

        double speed = Module.metersToRotations(state.speedMetersPerSecond);
        drive.setControl(driveRequest
            .withVelocity(speed));
    }

    public void zoom() {
        drive.getConfigurator().apply(zoomCurrents);
        QuickDebug.output("Zoomy Mode", true);
    }

    public void noZoom() {
        drive.getConfigurator().apply(noZoomCurrents);
        QuickDebug.output("Zoomy Mode", false);
    }

    public void setPDSV(double P, double D, double S, double V, SwerveModuleMotor target) {
        (target == SwerveModuleMotor.AZIMUTH ? azimuth : drive).getConfigurator().apply(new Slot0Configs()
            .withKP(P)
            .withKD(D)
            .withKS(S)
            .withKV(V));
    }

    @Override
    public ManualTest getManualTest() {
        return new SwerveModuleTest(framePosition.toString(), drive, azimuth);
    }
}
