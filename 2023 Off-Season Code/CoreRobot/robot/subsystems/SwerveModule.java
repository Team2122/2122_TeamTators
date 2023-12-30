package frc.robot.subsystems;

import com.ctre.phoenix6.configs.CurrentLimitsConfigs;
import com.ctre.phoenix6.configs.MotorOutputConfigs;
import com.ctre.phoenix6.configs.Slot0Configs;
import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.configs.TorqueCurrentConfigs;
import com.ctre.phoenix6.controls.DutyCycleOut;
import com.ctre.phoenix6.controls.Follower;
import com.ctre.phoenix6.controls.VelocityDutyCycle;
import com.ctre.phoenix6.controls.VoltageOut;
import com.ctre.phoenix6.hardware.CANcoder;
import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.signals.NeutralModeValue;
import com.revrobotics.CANSparkMax;
import com.revrobotics.RelativeEncoder;
import com.revrobotics.SparkMaxPIDController;

import common.Util.SwerveCANPIDRotationController;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import frc.robot.constants.SwerveConstants;


public class SwerveModule {

    SwerveCANPIDRotationController azimuthPID;
    int moduleNumber = -1;
    public CANSparkMax azimuth;
    public RelativeEncoder azimuthEncoder;
    public SparkMaxPIDController azimuthController;
    // talon gow eww
    public TalonFX leaderFalcon;
    public TalonFX followerFalcon;
    public CANcoder caNcoder;
    private VelocityDutyCycle leaderVelocity;
    private Follower follower;
    
    public SwerveModule(int moduleNumber, TalonFX talon, TalonFX followerFalcon, CANSparkMax canSparkMax, CANcoder cancoder){
        this.moduleNumber = moduleNumber;
        this.azimuth = canSparkMax;
        this.leaderFalcon = talon;
        this.followerFalcon = followerFalcon;
        this.azimuthEncoder = canSparkMax.getEncoder();
        this.azimuthController = azimuth.getPIDController();
        configurePID(1, 0, .05, 0, 0);


        // talon.getConfigurator().apply(s);

        // if(moduleNumber == 3){
            // talon.setInverted(true);
            // innerEwwlon.setInverted(true);
        // }
        
        azimuthEncoder.setPosition(cancoder.getAbsolutePosition().getValue() * SwerveConstants.SwerveModule.MOTOR_ROTATIONS_PER_MODULE_ROTATION);
        azimuthPID = new SwerveCANPIDRotationController(azimuth, SwerveConstants.SwerveModule.MOTOR_ROTATIONS_PER_MODULE_ROTATION);
        this.caNcoder = cancoder;
        leaderFalcon.setRotorPosition(0);
       
        
        // TorqueCurrentConfigs current = new Torques

        // CurrentLimitsConfigs c = new CurrentLimitsConfigs();
        // c.StatorCurrentLimit = 400;
        // c.SupplyCurrentLimit = 400;
        // c.StatorCurrentLimitEnable = true;
        // c.SupplyCurrentLimitEnable = true;
        // ewwlon.getConfigurator().apply(c);
        // innerEwwlon.getConfigurator().apply(c);
        // ewwlon.getConfigurator().apply(m);
        // innerEwwlon.getConfigurator().apply(m);



        configForAuto();
       
        leaderVelocity = new VelocityDutyCycle(0);
        follower = new Follower(leaderFalcon.getDeviceID(), false);
    }


    public void configurePID(double p, double i, double d, double iZone, double f) {
        azimuthController.setP(p);
        azimuthController.setI(i);
        azimuthController.setD(d);
        azimuthController.setFF(f);
        azimuthController.setIZone(iZone);
    }

    public void setMotion(SwerveModuleState swerveModuleState){
        // if(swerveModuleState.speedMetersPerSecond != 0){    s
            double dir = azimuthPID.setOptimizedPositionNew(swerveModuleState.angle.getRadians());
            leaderVelocity.Velocity = metersToRotations(swerveModuleState.speedMetersPerSecond * dir * SwerveConstants.SwerveModule.SWERVE_MODULE_OFFSETS[moduleNumber]);
            leaderFalcon.setControl(leaderVelocity);
            // ewwlon.setControl(new VoltageOut(swerveModuleState.speedMetersPerSecond * dir * SwerveConstants.SwerveModule.SWERVE_MODULE_OFFSETS[moduleNumber],false,false));

            followerFalcon.setControl(follower);
            
        // } `

        // else{
            // ewwlon.setControl(new VelocityDutyCycle(0));
        // }
        
    }

    public static double rotationsToMeters(double rotations){
        return rotations * SwerveConstants.SwerveModule.WHEEL_CIRCUMFERENCE / SwerveConstants.SwerveModule.MOTOR_ROTATIONS_PER_WHEEL_ROTATION;
    }

    public static double metersToRotations(double meters){
        return meters * SwerveConstants.SwerveModule.MOTOR_ROTATIONS_PER_WHEEL_ROTATION / SwerveConstants.SwerveModule.WHEEL_CIRCUMFERENCE;
    }

    public SwerveModulePosition getSwerveModulePosition(){
        return new SwerveModulePosition(rotationsToMeters(leaderFalcon.getPosition().getValue()), Rotation2d.fromRotations(caNcoder.getAbsolutePosition().getValue()));
    }

    public double getXComponent(){
        return Math.cos(caNcoder.getAbsolutePosition().getValue()*6.28) * leaderFalcon.get();
    }

    public double getYComponent(){
        return Math.sin(caNcoder.getAbsolutePosition().getValue()*6.28) * leaderFalcon.get();
    }

    public double getXComponentNEO(){
        return Math.cos(azimuthEncoder.getPosition()/12.8*6.28) * leaderFalcon.get();
    }

    public double getYComponentNEO(){
        return Math.sin(azimuthEncoder.getPosition()/12.8*6.28) * leaderFalcon.get();
    }
    
    public void reset() {
        // azimuthPID.setReference(0, CANSparkMax.ControlType.kPosition);
    }

    public void setStatorCurrentLimit(double limit) {
        CurrentLimitsConfigs config = new CurrentLimitsConfigs();
        config.StatorCurrentLimit = limit;
        config.StatorCurrentLimitEnable = true;
        followerFalcon.getConfigurator().apply(config);
        leaderFalcon.getConfigurator().apply(config);
    }


    public void configForAuto(){
        TalonFXConfiguration configuration = new TalonFXConfiguration();
        configuration.Slot0.kP = .03;
        configuration.Slot0.kV = 1;
        configuration.MotorOutput.NeutralMode = NeutralModeValue.Brake;
        configuration.CurrentLimits.StatorCurrentLimitEnable = true;
        configuration.CurrentLimits.StatorCurrentLimit = 30;
        configuration.CurrentLimits.SupplyCurrentLimit = 25;
        configuration.CurrentLimits.SupplyCurrentLimitEnable = true;
        leaderFalcon.getConfigurator().apply(configuration);
        followerFalcon.getConfigurator().apply(configuration);
    }

    public void configForTele(){
        TalonFXConfiguration configuration = new TalonFXConfiguration();
        configuration.Slot0.kP = .03;
        configuration.Slot0.kV = 1;
        configuration.MotorOutput.NeutralMode = NeutralModeValue.Brake;
        configuration.CurrentLimits.StatorCurrentLimitEnable = false;
        configuration.CurrentLimits.StatorCurrentLimit = 50;
        configuration.CurrentLimits.SupplyCurrentLimit = 20;
        configuration.CurrentLimits.SupplyCurrentLimitEnable = true;
        leaderFalcon.getConfigurator().apply(configuration);
        followerFalcon.getConfigurator().apply(configuration);
    }
}
