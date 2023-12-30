package frc.robot.subsystems;

import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.hardware.CANcoder;
import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.signals.InvertedValue;
import com.revrobotics.CANSparkMax;
import com.revrobotics.CANSparkMaxLowLevel.MotorType;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Transform2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.smartdashboard.Field2d;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.FunctionalCommand;
import frc.robot.Robot;
import frc.robot.RobotContainer;
import frc.robot.constants.SwerveConstants;

import java.util.function.Consumer;
import java.util.function.Supplier;

import common.Tools.tester.ManualTestGroup;
import common.Tools.tester.components.EightMotorSwerveModuleTest;
import common.Tools.tester.components.TatorPigeonTest;
import common.Util.*;
import common.teamtators.Subsystem;

public class SwerveDrive extends Subsystem implements Consumer<ChassisSpeeds>{

    private SwerveModule backLeft;           // The four swerve modules
    private SwerveModule backRight;
    private SwerveModule frontRight;
    private SwerveModule frontLeft;
    private TatorPigeon gyro;

    private ChassisSpeeds chassisSpeeds;

    private final double MOTOR_ROTATIONS_PER_WHEEL_ROTATION;
    private final double WHEEL_CIRCUMFERENCE;

    private Pose2d desiredDeltaPose = new Pose2d();

    int counter = 0;
    private SwerveDriveKinematics swerveDriveKinematics;
    private SwerveModule[] modules;
    private Field2d chassisSpeedsField;

    //    private Vision vision;
    public SwerveDrive(RobotContainer robotContainer) {
        super(robotContainer);
        this.MOTOR_ROTATIONS_PER_WHEEL_ROTATION = SwerveConstants.SwerveModule.MOTOR_ROTATIONS_PER_WHEEL_ROTATION;
        this.WHEEL_CIRCUMFERENCE = SwerveConstants.SwerveModule.WHEEL_CIRCUMFERENCE;
        chassisSpeeds = ChassisSpeeds.fromFieldRelativeSpeeds(0, 0, 0, new Rotation2d());

        setValidity(Robot.EnumRobotState.Autonomous, Robot.EnumRobotState.Teleop);


        // Translation2d module0Pos = new Translation2d(-.2828575, .307575);
        // Translation2d module1Pos = new Translation2d(-.2828575, -.307575);
        // Translation2d module2Pos = new Translation2d(-.2828575, .307575);
        // Translation2d module3Pos = new Translation2d(-.2828575, -.307575);

        // Translation2d module3Pos = new Translation2d(.3, .30);
        // Translation2d module0Pos = new Translation2d(-.3, .30);
        // Translation2d module1Pos  = new Translation2d(-.3, -.30);
        // Translation2d module2Pos = new Translation2d(.3, -.30);

        // THE TAGS ON THE BOT ARE WRONG, THE UNCOMMENTED ONE IS CORRECT
        // Translation2d module0Pos = new Translation2d(-.3, -.30);
        // Translation2d module1Pos = new Translation2d(.3, -.30);
        // Translation2d module2Pos = new Translation2d(.3, .30);
        // Translation2d module3Pos = new Translation2d(-.3, .30);
        double value = .3;
        Translation2d backLeftPos = new Translation2d(-value, value);
        Translation2d backRightPos = new Translation2d(-value, -value);
        Translation2d frontRightPos = new Translation2d(value, -value);
        Translation2d frontLeftPos = new Translation2d(value, value);

        swerveDriveKinematics = new SwerveDriveKinematics(frontLeftPos, frontRightPos, backLeftPos,backRightPos);

        // FRONTLEFT FRONTRIGHT BACKLEFT BACKRIGHT

        moduleConfigure();
        gyro = new TatorPigeon(0, SwerveConstants.SwerveModule.canivoreBusName);
        this.modules = new SwerveModule[]{frontLeft, frontRight, backLeft,backRight};
        chassisSpeedsField = new Field2d();
        SmartDashboard.putData("ChassisSpeeds",chassisSpeedsField);
    }

    
    @Override
    public void doPeriodic() {
        updateModules();

        // double num = Math.random();
        // if (num > .9){
        //     System.out.println(chassisSpeeds);
        // }
    }


    public void updateModules() {
        SwerveModuleState[] swerveModuleStates = swerveDriveKinematics.toSwerveModuleStates(chassisSpeeds);
        frontLeft.setMotion(swerveModuleStates[0]);
        frontRight.setMotion(swerveModuleStates[1]);
        backLeft.setMotion(swerveModuleStates[2]);
        backRight.setMotion(swerveModuleStates[3]);
    }

    public void setStatorCurrentLimit(double limit) {
        frontLeft.setStatorCurrentLimit(limit);
        frontRight.setStatorCurrentLimit(limit);
        backLeft.setStatorCurrentLimit(limit);
        backRight.setStatorCurrentLimit(limit);
    }
    public void setConfigForTele() {
        frontLeft.configForTele();
        frontRight.configForTele();
        backLeft.configForTele();
        backRight.configForTele();
    }

    public double rotationsToMeters(double rotations) {
        double speed = (rotations / MOTOR_ROTATIONS_PER_WHEEL_ROTATION) * WHEEL_CIRCUMFERENCE;
        return speed;
    }


    public void moduleConfigure() {

  

        try {
            Thread.sleep(2000);
        } catch (Exception ignored) {

        }
        
        backLeft = new SwerveModule(
            0,
            new TalonFX(4, SwerveConstants.SwerveModule.canivoreBusName),
            new TalonFX(8, SwerveConstants.SwerveModule.canivoreBusName),

            new CANSparkMax(4, MotorType.kBrushless),
            new CANcoder(4, SwerveConstants.SwerveModule.canivoreBusName)
        );
        backRight = new SwerveModule(
            1,
            new TalonFX(1, SwerveConstants.SwerveModule.canivoreBusName),
            new TalonFX(5, SwerveConstants.SwerveModule.canivoreBusName),
            new CANSparkMax(1, MotorType.kBrushless),
            new CANcoder(1, SwerveConstants.SwerveModule.canivoreBusName)
        );
        frontRight = new SwerveModule(
        2,
            new TalonFX(2, SwerveConstants.SwerveModule.canivoreBusName),
            new TalonFX(6, SwerveConstants.SwerveModule.canivoreBusName),
            new CANSparkMax(2, MotorType.kBrushless),
            new CANcoder(2, SwerveConstants.SwerveModule.canivoreBusName)
        );
        frontLeft = new SwerveModule(
            3,
            new TalonFX(3, SwerveConstants.SwerveModule.canivoreBusName),
            new TalonFX(7, SwerveConstants.SwerveModule.canivoreBusName),
            new CANSparkMax(3, MotorType.kBrushless),
            new CANcoder(3, SwerveConstants.SwerveModule.canivoreBusName)
            );        // throw new NullPointerException();
    }

    public TatorPigeon getGyro() {
        return gyro;
    }

    public Rotation2d getGyroRotation2d() {
        return gyro.getRotation2d();
    }

    public Rotation2d getGyroRotation2d(Rotation2d offset) {
        return gyro.getRotation2d().plus(offset);
    }

    public SwerveModule[] getSwerveModules(){
        return modules;
    }
 
    public SwerveDriveKinematics getKin(){
        return swerveDriveKinematics;
    }
    
    @Override
    public void reset() {
        
    }

    @Override
    public void accept(ChassisSpeeds arg0) {
        //chassisSpeedsField.setRobotPose(arg0.vxMetersPerSecond, arg0.vyMetersPerSecond, Rotation2d.fromDegrees(arg0.omegaRadiansPerSecond));
        
        this.chassisSpeeds = discretize(arg0,.02);
    }

    public SwerveModulePosition[] getSwerveModulePositions(Rotation2d trans){
        SwerveModulePosition[] modulePositions = new SwerveModulePosition[4];

        for (int index = 0; index < modulePositions.length; index++) {
            var modPose = modules[index].getSwerveModulePosition();
            modPose = new SwerveModulePosition(modPose.distanceMeters, modPose.angle.plus(trans));
            modulePositions[index] = modPose;
        }

        return modulePositions;
    }

    public SwerveModulePosition[] getSwerveModulePositions(){
        SwerveModulePosition[] modulePositions = new SwerveModulePosition[4];

        for (int index = 0; index < modulePositions.length; index++) {
            modulePositions[index] = modules[index].getSwerveModulePosition();
        }

        return modulePositions;
    }


    public static ChassisSpeeds discretize(
        double vxMetersPerSecond,
        double vyMetersPerSecond,
        double omegaRadiansPerSecond,
        double dtSeconds) {
      var desiredDeltaPose =
          new Pose2d(
              vxMetersPerSecond * dtSeconds,
              vyMetersPerSecond * dtSeconds,
              new Rotation2d(omegaRadiansPerSecond * dtSeconds));
      var twist = new Pose2d().log(desiredDeltaPose);
      return new ChassisSpeeds(twist.dx / dtSeconds, twist.dy / dtSeconds, twist.dtheta / dtSeconds);
    }
    public static ChassisSpeeds discretize(ChassisSpeeds continuousSpeeds, double dtSeconds) {
        return discretize(
            continuousSpeeds.vxMetersPerSecond,
            continuousSpeeds.vyMetersPerSecond,
            continuousSpeeds.omegaRadiansPerSecond,
            dtSeconds);
      }

    @Override
    public ManualTestGroup createManualTests() {
        return new ManualTestGroup(
            "Swerve Drive",
            new EightMotorSwerveModuleTest("module0", backLeft, 5, 1),
            new EightMotorSwerveModuleTest("module1", backRight, 5, 1),
            new EightMotorSwerveModuleTest("module2", frontRight, 5, 1),
            new EightMotorSwerveModuleTest("module3", frontLeft, 5, 1),
            new TatorPigeonTest(getName(), gyro)
        );
    }

    // Command Factories
    public Command drive(Supplier<ChassisSpeeds> supplier) {
        return new FunctionalCommand(
            () -> {}, // onInit
            () -> accept(supplier.get()), // onExecute,
            interrupted -> {
                accept(new ChassisSpeeds());
                updateModules();
            }, // onEnd,
            () -> false, // isFinished,
            this // requirements
        );
    }
}
