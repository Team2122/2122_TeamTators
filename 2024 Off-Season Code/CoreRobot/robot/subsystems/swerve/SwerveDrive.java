package frc.robot.subsystems.swerve;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.Matrix;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.estimator.SwerveDrivePoseEstimator;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.estimator.SwerveDrivePoseEstimator;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Transform2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.geometry.Twist2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.kinematics.SwerveDriveOdometry;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.math.numbers.N1;
import edu.wpi.first.math.numbers.N3;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.DriverStation.Alliance;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.DriverStation.Alliance;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.button.RobotModeTriggers;
import frc.robot.Robot;
import frc.robot.constants.GeneralConstants;
import frc.robot.constants.GeneralConstants.RobotMedium;
import frc.robot.subsystems.swerve.Module.SwerveModuleMotor;
import frc.robot.subsystems.vision.Vision;
import frc.robot.util.AimUtil;
import frc.robot.util.AimUtil.AimTargets;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.littletonrobotics.junction.Logger;
import org.teamtators.Util.*;
import org.teamtators.tester.ManualTestGroup;
import org.teamtators.tester.components.SwerveTuningTest;

public class SwerveDrive extends Subsystem implements Consumer<ChassisSpeeds> {

    private Module backLeft;
    private Module backRight;
    private Module frontRight;
    private Module frontLeft;

    private SwerveDriveOdometry odometry;
    private Pose2d priorOdometryPose;

    private GyroIO gyro;
    private GyroIOInputsAutoLogged gyroInputs;

    private ChassisSpeeds chassisSpeeds;
    private ChassisSpeeds appliedChassisSpeeds;

    private SwerveDriveKinematics kinematics;
    private Module[] modules;

    private SwerveModuleState[] moduleStates;
    private SwerveModuleState[] setpointStates;
    private SwerveModulePosition[] lastModulePositions;
    private Rotation2d angle;

    private PIDController rotationController;

    private Vision vision;
    private SwerveDrivePoseEstimator poseEstimator;

    static final Lock odometryLock = new ReentrantLock();

    protected enum ModuleFramePositions {
        BACK_LEFT,
        BACK_RIGHT,
        FRONT_LEFT,
        FRONT_RIGHT,
    }

    public enum SwerveStates {
        INIT,
        DRIVE,
        SPECIAL_AIMING,
        INTERRUPT
    }

    private SwerveStates currentState = SwerveStates.DRIVE;
    private SwerveStates newState = SwerveStates.INIT;

    private boolean onTarget = false;

    public SwerveDrive() {
        super();

        priorOdometryPose = new Pose2d();

        backLeft = new Module(ModuleFramePositions.BACK_LEFT, 11, 12, 11);
        backRight = new Module(ModuleFramePositions.BACK_RIGHT, 31, 32, 31);
        frontRight = new Module(ModuleFramePositions.FRONT_RIGHT, 41, 42, 41);
        frontLeft = new Module(ModuleFramePositions.FRONT_LEFT, 21, 22, 21);
        this.modules = new Module[] {frontLeft, frontRight, backLeft, backRight};

        gyroInputs = new GyroIOInputsAutoLogged();
        gyro = Robot.isReal() ? new GyroIOReal() : new GyroIO() {};
        
        double horizontalPos = Units.inchesToMeters(20.4583/2);
        double verticalPos = Units.inchesToMeters(17.9583/2);
        Translation2d backLeftPos = new Translation2d(-horizontalPos, verticalPos);
        Translation2d backRightPos = new Translation2d(-horizontalPos, -verticalPos);
        Translation2d frontRightPos = new Translation2d(horizontalPos, -verticalPos);
        Translation2d frontLeftPos = new Translation2d(horizontalPos, verticalPos);
        
        kinematics = new SwerveDriveKinematics(frontLeftPos, frontRightPos, backLeftPos,backRightPos);
        chassisSpeeds = new ChassisSpeeds();
        moduleStates = new SwerveModuleState[4];
        setpointStates = new SwerveModuleState[4];
        setModuleSetpoints(new ChassisSpeeds());
        angle = new Rotation2d();

        rotationController = new PIDController(2.5, 0, 0);
        rotationController.enableContinuousInput(-Math.PI, Math.PI);

        lastModulePositions = new SwerveModulePosition[] {
            new SwerveModulePosition(),
            new SwerveModulePosition(),
            new SwerveModulePosition(),
            new SwerveModulePosition()
        };

        poseEstimator =
          new SwerveDrivePoseEstimator(
              kinematics,
              getYaw(),
              getSwerveModulePositions(),
              new Pose2d(),
              SwerveConstants.kOdometryStdDevs,
              SwerveConstants.kInitialVisionStdDevs);

        PhoenixOdometryThread.getInstance().start();
        
        odometry = new SwerveDriveOdometry(
            kinematics,
            getRotation2d(),
            getSwerveModulePositions()
        );
        
        RobotModeTriggers.autonomous().onTrue(Commands.runOnce(this::noZoom));
        // RobotModeTriggers.teleop().onTrue(Commands.runOnce(this::zoom));
        RobotModeTriggers.teleop().onTrue(Commands.runOnce(this::noZoom));
    }

    @Override
    public void configure() {
        this.vision = Robot.getVision();
    }
    
    @Override
    public void log() {
        odometryLock.lock();
        updateModuleData();
        gyro.updateInputs(gyroInputs);
        odometryLock.unlock();

        Logger.processInputs("Swerve/Gyro", gyroInputs);
        Logger.recordOutput("Swerve/StabilizationYaw", gyroStabilizeYaw.orElse(-1.0));
        Logger.recordOutput("Swerve/ChassisSpeeds", chassisSpeeds);
        Logger.recordOutput("Swerve/AppliedChassisSpeeds", appliedChassisSpeeds);
        Logger.recordOutput("Swerve/ModuleStatesSetpoints", setpointStates);
        Logger.recordOutput("Swerve/ModuleStatesMeasured", moduleStates);
        Logger.recordOutput("Swerve/CurrentState", currentState);
        Logger.recordOutput("Swerve/NewState", newState);
        Logger.recordOutput("Swerve/OnTarget", onTarget);
        Logger.recordOutput("Swerve/Pose", poseEstimator.getEstimatedPosition());
    }

    @Override
    public void doPeriodic() {
        poseEstimator.update(
            getRotation2dWithoutOffset(),
            lastModulePositions);
        
        if (currentState != newState) {
            switch (newState) {
                case INIT -> newState = SwerveStates.DRIVE;
                default -> {} // no transitionary logic, that's handled at a higher level
            }

            currentState = newState;
        }

        switch (currentState) {
            case INIT, INTERRUPT -> {}
            case SPECIAL_AIMING -> {
                if (AimUtil.getTarget() == AimTargets.SPEAKER) {
                    double yaw = Math.toRadians(gyroInputs.yawDegrees);
                    double errorRadians;
                    Pose2d pose = vision.getFrontCameraPose().orElse(poseEstimator.getEstimatedPosition());

                    var maybeTargetPose = AimUtil.getTargetPos();
                    if (!maybeTargetPose.isPresent()) {
                        setModuleSetpoints(chassisSpeeds);
                        return;
                    }
                    var targetPose = maybeTargetPose.get();

                    double diffX = targetPose.getX() - pose.getX();
                    double diffY = targetPose.getY() - pose.getY();

                    double targetYaw = Math.atan2(diffY, diffX);
                    errorRadians = targetYaw - yaw;
                    errorRadians = MathUtil.clamp(MathUtil.angleModulus(errorRadians), -1.3, 1.3);
                    chassisSpeeds.omegaRadiansPerSecond =
                      rotationController.calculate(yaw, yaw + errorRadians);
                } else if (AimUtil.getTarget() == AimTargets.AMP) {
                    var maybeTargetPose = AimUtil.getTargetPos();
                    if (!maybeTargetPose.isPresent()) {
                        setModuleSetpoints(chassisSpeeds);
                        return;
                    }
                    var targetPose = maybeTargetPose.get();
                    var pose = poseEstimator.getEstimatedPosition();

                    double diffX = targetPose.getX() - pose.getX();
                    double diffY = targetPose.getY() - pose.getY();

                    chassisSpeeds.omegaRadiansPerSecond =
                      rotationController.calculate(
                          pose.getRotation().getRadians(), Math.atan2(diffY, diffX));
                }

                onTarget = chassisSpeeds.omegaRadiansPerSecond < .2;

                setGyroStabilization(true);
                setModuleSetpoints(chassisSpeeds);
            }
            case DRIVE -> {
                onTarget = true; // so the controller rumbles for static shot
                doGyroStabilization();
                setModuleSetpoints(chassisSpeeds);
            }
        }
        appliedChassisSpeeds = chassisSpeeds;
    }

    private Optional<Double> gyroStabilizeYaw = Optional.empty();
    private static final int STABILIZATION_DELAY = 5;
    private int gyroStabilizeDelay = STABILIZATION_DELAY;

    private void setGyroStabilization(boolean resetTimer) {
        gyroStabilizeYaw = Optional.of(gyroInputs.yawDegreesRaw);
        if (resetTimer) {
            gyroStabilizeDelay = STABILIZATION_DELAY;
        }
    }

    /**
     * Modifies the desired velocity vector to stay on target as the robot drifts left and right after
     * STABILIZATION_DELAY ticks without any rotation input from the driver.
     */
    private void doGyroStabilization() {
        if (gyroShouldStabilize()) {
            if (gyroStabilizeDelay > 0) { // count down to when we should stabilize
                gyroStabilizeDelay--;
                setGyroStabilization(false);
            } else { // the timer is up, we should stabilize
                chassisSpeeds.omegaRadiansPerSecond =
                  rotationController.calculate(
                      // PID is set up for radians
                      Math.toRadians(gyroInputs.yawDegreesRaw), Math.toRadians(gyroStabilizeYaw.get()));

                // ensures the bot doesn't jiggle trying to stay on target
                if (Math.abs(chassisSpeeds.omegaRadiansPerSecond) < 0.04) {
                    chassisSpeeds.omegaRadiansPerSecond = 0;
                }
            }
        } else {
            // passing true ensures the timer is reset properly when the gyro
            // does need to stabilize
            setGyroStabilization(true);
        }
    }

    private boolean gyroShouldStabilize() {
        return Math.abs(chassisSpeeds.omegaRadiansPerSecond) < 0.03
               && gyroStabilizeYaw.isPresent()
               && DriverStation.isTeleopEnabled();
    }

    private void updateModuleData() {
        frontLeft.updateLogs();
        frontRight.updateLogs();
        backLeft.updateLogs();
        backRight.updateLogs();

        for (int i = 0; i < modules.length; i++) {
            moduleStates[i] = modules[i].getCurrentState();
        }

        priorOdometryPose = odometry.getPoseMeters();
        odometry.update(getRotation2d(), getSwerveModulePositions());

        // Update odometry
        double[] sampleTimestamps =
            modules[0].getOdometryTimestamps(); // All signals are sampled together
        int sampleCount = sampleTimestamps.length;
        for (int i = 0; i < sampleCount; i++) {
            // Read wheel positions and deltas from each module
            SwerveModulePosition[] modulePositions = new SwerveModulePosition[4];
            SwerveModulePosition[] moduleDeltas = new SwerveModulePosition[4];
            for (int moduleIndex = 0; moduleIndex < 4; moduleIndex++) {
                modulePositions[moduleIndex] = modules[moduleIndex].getOdometryPositions()[i];
                moduleDeltas[moduleIndex] =
                  new SwerveModulePosition(
                      modulePositions[moduleIndex].distanceMeters
                      - lastModulePositions[moduleIndex].distanceMeters,
                      modulePositions[moduleIndex].angle);
                lastModulePositions[moduleIndex] = modulePositions[moduleIndex];
            }
        }
    }

    private void setModuleSetpoints(ChassisSpeeds speed) {
        speed.discretize(.02);

        setpointStates = kinematics.toSwerveModuleStates(speed);

        for (int i = 0; i < modules.length; i++) {
            modules[i].setMotion(setpointStates[i]);
        }
    }

    public SwerveDrivePoseEstimator getPoseEstimator() {
        return poseEstimator;
    }

    public Rotation2d getYaw() {
        if (gyroInputs.connected) {
            return Rotation2d.fromDegrees(gyroInputs.yawDegrees);
        } else {
            angle = angle.plus(getYawVelocity().times(.02));
            return angle;
        }
    }

    public Rotation2d getYawBlueOrigin() {
        return DriverStation.getAlliance()
            .map(alliance -> {
                if (alliance == Alliance.Red) {
                    return Rotation2d.k180deg.minus(getYaw()).unaryMinus();
                } else {
                    return getYaw();
                }
            }).orElse(getYaw());
    }

    public Rotation2d getYawVelocity() {
        if (gyroInputs.connected) {
            return Rotation2d.fromDegrees(gyroInputs.yawDegreesPerSec);
        } else {
            return Rotation2d.fromRadians(getSpeedsFromWheels().omegaRadiansPerSecond);
        }
    }

    public Rotation2d getYawRaw() {
        if (gyroInputs.connected) {
            return Rotation2d.fromDegrees(gyroInputs.yawDegreesRaw);
        } else {
            return angle.plus(Rotation2d.fromRadians(getSpeedsFromWheels().omegaRadiansPerSecond * .02));
        }
    }

    private ChassisSpeeds getSpeedsFromWheels() {
        for (int i = 0; i < modules.length; i++) {
            moduleStates[i] = modules[i].getCurrentState();
        }

        return kinematics.toChassisSpeeds(moduleStates);
    }

    public Rotation2d getYaw(Rotation2d offset) {
        return getYaw().plus(offset);
    }

    public Module[] getModules() {
        return modules;
    }

    public SwerveDriveKinematics getKinematics() {
        return kinematics;
    }

    @Override
    public void reset() {}

    public void gyroZero() {
        gyro.zero();
    }

    public void setGyroOffset(double degrees) {
        gyro.setCurrentAngle(degrees);
    }

    /**
     * Resets the gyro such that zero will face away from the alliance wall.
     *
     * <p>Assumes a blue origin, even if on the red alliance
     */
    public void resetGyroFrom(Pose2d pose) {
        double angle = pose.getRotation().getDegrees();
        if (Robot.getAlliance().equals(Alliance.Red)) {
            angle += 180;
        }
        gyro.changeOffset(angle);
    }

    public void zoom() {
        QuickDebug.output("Zoomy Mode", true);
        backLeft.zoom();
        backRight.zoom();
        frontLeft.zoom();
        frontRight.zoom();
    }

    public void noZoom() {
        QuickDebug.output("Zoomy Mode", false);
        backLeft.noZoom();
        backRight.noZoom();
        frontLeft.noZoom();
        frontRight.noZoom();
    }

    public void testing_setPDSV(double P, double D, double S, double V, SwerveModuleMotor target) {
        backLeft.testing_setPDSV(P, D, S, V, target);
        backRight.testing_setPDSV(P, D, S, V, target);
        frontLeft.testing_setPDSV(P, D, S, V, target);
        frontRight.testing_setPDSV(P, D, S, V, target);
    }

    public void toggleZoom() {
        backLeft.toggleZoom();
        backRight.toggleZoom();
        frontLeft.toggleZoom();
        frontRight.toggleZoom();
    }

    public boolean onTarget() {
        return onTarget;
    }

    public SwerveModulePosition[] getSwerveModulePositions(){
        return lastModulePositions;
    }

    public ChassisSpeeds getChassisSpeeds() {
        return kinematics.toChassisSpeeds(moduleStates);
    }

    @Override
    public void accept(ChassisSpeeds chassisSpeeds) {
        this.chassisSpeeds = chassisSpeeds;
    }

    public void addVisionMeasurement(Pose2d pose, double timestamp) {
        poseEstimator.addVisionMeasurement(pose, timestamp);
    }

    public Transform2d getOdometryDelta() {
        return odometry.getPoseMeters().minus(priorOdometryPose);
    }

    public boolean gyroConnected() {
        return gyroInputs.connected;
    }

    public Pose2d getPose() {
        return poseEstimator.getEstimatedPosition();
    }

    public void resetPose(Pose2d pose) {
        poseEstimator.resetPosition(getYaw(), getSwerveModulePositions(), pose);
    }

    public void addVisionMeasurement(Pose2d pose, double timestamp, Matrix<N3, N1> stdDevs) {
        poseEstimator.addVisionMeasurement(pose, timestamp, stdDevs);
    }

    @Override
    public ManualTestGroup createManualTests() {
        if (GeneralConstants.kRobotMedium == RobotMedium.REAL) {
            return new ManualTestGroup(
                "Swerve Drive",
                backLeft.getManualTest(),
                backRight.getManualTest(),
                frontRight.getManualTest(),
                frontLeft.getManualTest(),
                gyro.getManualTest(),
                new SwerveTuningTest("Swerve Tuning", Robot.getOperatorInterface(), this));
        } else {
            return new ManualTestGroup(
                "Swerve Drive",
                new SwerveTuningTest("Swerve Tuning", Robot.getOperatorInterface(), this));
        }
    }

    public Rotation2d getRotation2d() {
        if (gyroInputs.connected) {
            return Rotation2d.fromDegrees(gyroInputs.yawDegrees);
        } else {
            Twist2d twist = kinematics.toTwist2d(lastModulePositions);
            return poseEstimator.getEstimatedPosition()
                .getRotation()
                .plus(Rotation2d.fromRadians(twist.dtheta));
        }
    }

    public Rotation2d getRotation2dWithoutOffset() {
        if (gyroInputs.connected) {
            return Rotation2d.fromDegrees(gyroInputs.yawDegreesRaw);
        } else {
            Twist2d twist = kinematics.toTwist2d(lastModulePositions);
            return poseEstimator.getEstimatedPosition()
                .getRotation()
                .plus(Rotation2d.fromRadians(twist.dtheta));
        }
    }

    /*
     * DO NOT USE UNLESS ABSOLUTELY NECESSARY.
     * This is for bypassing command requirements
     * in auto, and should NEVER be used outside
     * of that special circumstance
     */
    public void setState(SwerveStates state) {
        this.newState = state;
    }

    // Command Factories
    public Command drive(Supplier<ChassisSpeeds> speedSupplier) {
        return this.runOnce(
            () -> {
                currentState = SwerveStates.INTERRUPT;
                newState = SwerveStates.DRIVE;
            })
            .andThen(Commands.run(() -> this.accept(speedSupplier.get())))
            .until(() -> newState != SwerveStates.DRIVE)
            .finallyDo(() -> this.accept(new ChassisSpeeds()))
            .withName("Driving");
    }

    public Command aim(Supplier<ChassisSpeeds> speedSupplier) {
        return this.runOnce(
            () -> {
                currentState = SwerveStates.INTERRUPT;
                newState = SwerveStates.SPECIAL_AIMING;
            })
            .andThen(Commands.run(() -> this.accept(speedSupplier.get())))
            .until(() -> newState != SwerveStates.SPECIAL_AIMING)
            .finallyDo(() -> this.accept(new ChassisSpeeds()))
            .withName("Aiming");
    }

    @Override
    public boolean getHealth() {
        return backLeft.getHealth()
            && backRight.getHealth()
            && frontLeft.getHealth()
            && frontRight.getHealth();
    }
}
