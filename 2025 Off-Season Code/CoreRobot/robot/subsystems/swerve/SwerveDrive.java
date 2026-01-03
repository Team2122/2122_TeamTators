package frc.robot.subsystems.swerve;

import choreo.trajectory.SwerveSample;
import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.Matrix;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.estimator.SwerveDrivePoseEstimator;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Transform2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.math.numbers.N1;
import edu.wpi.first.math.numbers.N3;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.RobotBase;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.button.Trigger;
import frc.robot.Robot;
import frc.robot.constants.Constants;
import frc.robot.constants.Constants.RobotMedium;
import frc.robot.subsystems.operatorInterface.OperatorInterfaceConstants;
import frc.robot.subsystems.swerve.Module.SwerveModuleMotor;
import frc.robot.subsystems.swerve.RepulsorFieldPlanner.RepulsorSample;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import org.littletonrobotics.junction.Logger;
import org.teamtators.tester.ManualTestGroup;
import org.teamtators.tester.components.SwerveTuningTest;
import org.teamtators.util.*;

public class SwerveDrive extends Subsystem {

    private Module backLeft;
    private Module backRight;
    private Module frontRight;
    private Module frontLeft;

    private GyroIO gyro;
    private GyroIOInputsAutoLogged gyroInputs;

    private ChassisSpeeds chassisSpeeds;

    private SwerveDriveKinematics kinematics;
    private Module[] modules;
    private Optional<Double> gyroStabilizeYaw = Optional.empty();
    private boolean poseCalibrated = false;

    private SwerveModuleState[] moduleStates;
    private SwerveModuleState[] setpointStates;
    private SwerveModulePosition[] lastModulePositions;
    private Rotation2d gyroYaw;
    private SwerveDrivePoseEstimator poseEstimator;

    private RepulsorFieldPlanner repulsor = new RepulsorFieldPlanner();
    private Pose2d alignmentGoal = new Pose2d();
    private PIDController translationXController = new PIDController(2, 0, 0);
    private PIDController translationYController = new PIDController(2, 0, 0);
    private PIDController rotationController;
    private double maxSpeed = 3.5;
    private double slowdownDistance = 1.1;
    private double maxRotSpeedDeg = Double.MAX_VALUE;

    private SwerveSample swerveSample;

    static final Lock odometryLock = new ReentrantLock();

    Rotation2d rotationTolerance = Rotation2d.fromDegrees(3);
    public final Trigger aligned = new Trigger(() -> isAligned(0.1, 0.1, rotationTolerance));

    protected enum ModuleFramePositions {
        BACK_LEFT,
        BACK_RIGHT,
        FRONT_LEFT,
        FRONT_RIGHT,
    }

    public enum SwerveStates {
        INIT,
        MOVING,
        ALIGNING,
        INTERRUPT
    }

    private SwerveStates currentState = SwerveStates.MOVING;
    private SwerveStates newState = SwerveStates.INIT;

    public enum SpinDirection {
        LEFT,
        RIGHT,
    }

    private Optional<SpinDirection> spinDirection = Optional.empty();

    public SwerveDrive() {
        super();

        backLeft = new Module(ModuleFramePositions.BACK_LEFT, 11, 12, 11);
        backRight = new Module(ModuleFramePositions.BACK_RIGHT, 21, 22, 21);
        frontLeft = new Module(ModuleFramePositions.FRONT_LEFT, 31, 32, 31);
        frontRight = new Module(ModuleFramePositions.FRONT_RIGHT, 41, 42, 41);
        this.modules = new Module[] {frontLeft, frontRight, backLeft, backRight};

        gyroInputs = new GyroIOInputsAutoLogged();
        gyro = Robot.isReal() ? new GyroIOReal() : new GyroIO() {};
        // gyro = new GyroIO() {};
        gyroYaw = new Rotation2d();

        double horizontalPos = Units.inchesToMeters(24.5 / 2);
        double verticalPos = Units.inchesToMeters(24.5 / 2);
        Translation2d backLeftPos = new Translation2d(-horizontalPos, verticalPos);
        Translation2d backRightPos = new Translation2d(-horizontalPos, -verticalPos);
        Translation2d frontRightPos = new Translation2d(horizontalPos, -verticalPos);
        Translation2d frontLeftPos = new Translation2d(horizontalPos, verticalPos);

        kinematics = new SwerveDriveKinematics(frontLeftPos, frontRightPos, backLeftPos, backRightPos);
        chassisSpeeds = new ChassisSpeeds();
        moduleStates = new SwerveModuleState[4];
        setpointStates = new SwerveModuleState[4];
        setModuleSetpoints(new ChassisSpeeds());

        rotationController = new PIDController(5, 0, 0);
        rotationController.enableContinuousInput(-Math.PI, Math.PI);

        lastModulePositions =
                new SwerveModulePosition[] {
                    new SwerveModulePosition(),
                    new SwerveModulePosition(),
                    new SwerveModulePosition(),
                    new SwerveModulePosition()
                };

        poseEstimator =
                new SwerveDrivePoseEstimator(
                        kinematics,
                        gyroYaw,
                        getSwerveModulePositions(),
                        new Pose2d(),
                        SwerveConstants.kOdometryStdDevs,
                        SwerveConstants.kInitialVisionStdDevs);

        PhoenixOdometryThread.getInstance().start();
    }

    @Override
    public void log() {
        odometryLock.lock();
        updateModuleData();
        odometryLock.unlock();
        gyro.updateInputs(gyroInputs);

        Logger.processInputs("Swerve/Gyro", gyroInputs);
        Logger.recordOutput("Swerve/StabilizationYaw", gyroStabilizeYaw.orElse(-1.0));
        Logger.recordOutput("Swerve/ChassisSpeeds", chassisSpeeds);
        Logger.recordOutput("Swerve/ModuleStatesSetpoints", setpointStates);
        Logger.recordOutput("Swerve/ModuleStatesMeasured", moduleStates);
        Logger.recordOutput("Swerve/CurrentState", currentState);
        Logger.recordOutput("Swerve/NewState", newState);
        Logger.recordOutput("Swerve/Pose", poseEstimator.getEstimatedPosition());
        Logger.recordOutput("Swerve/AlignmentGoal", alignmentGoal);
        Logger.recordOutput(
                "Swerve/CurrentCommand", getPossibleCommand().map(Command::getName).orElse("nothing."));
        if (swerveSample != null) {
            Logger.recordOutput(
                    "Swerve/SwerveSample",
                    new Pose2d(
                            new Translation2d(swerveSample.x, swerveSample.y),
                            new Rotation2d(swerveSample.heading)));
        }

        if (RobotBase.isSimulation()) {
            Logger.recordOutput("Repulsor/ObstacleArrows", repulsor.getObstacleArrows());
            Logger.recordOutput("Repulsor/GoalArrows", repulsor.getGoalArrows());
            Logger.recordOutput("Repulsor/TotalArrows", repulsor.getTotalArrows());
        }
    }

    @Override
    public void doPeriodic() {
        if (gyroInputs.connected) {
            gyroYaw = Rotation2d.fromDegrees(gyroInputs.yawDegrees);
        } else {
            Rotation2d rotationDelta =
                    Rotation2d.fromRadians(
                            getChassisSpeeds().times(Constants.kTickPeriod).omegaRadiansPerSecond);
            gyroYaw = gyroYaw.plus(rotationDelta);
        }

        if (currentState != newState) {
            switch (newState) {
                case INIT -> newState = SwerveStates.MOVING;
                case ALIGNING -> {
                    repulsor.setGoal(alignmentGoal.getTranslation());
                }
                default -> {} // no transitionary logic, that's handled at a higher level
            }

            currentState = newState;
        }

        switch (currentState) {
            case INIT, INTERRUPT -> {}
            case MOVING -> {
                doGyroStabilization();

                setModuleSetpoints(chassisSpeeds);
            }
            case ALIGNING -> {
                Pose2d pose = getPose();

                RepulsorSample sample =
                        repulsor.sampleField(pose.getTranslation(), maxSpeed, slowdownDistance);

                ChassisSpeeds feedforward = new ChassisSpeeds(sample.vx(), sample.vy(), 0);
                ChassisSpeeds feedback =
                        new ChassisSpeeds(
                                translationXController.calculate(
                                        pose.getTranslation().getX(), sample.intermediateGoal().getX()),
                                translationYController.calculate(
                                        pose.getTranslation().getY(), sample.intermediateGoal().getY()),
                                MathUtil.clamp(
                                        rotationController.calculate(
                                                pose.getRotation().getRadians(), alignmentGoal.getRotation().getRadians()),
                                        -Units.degreesToRadians(maxRotSpeedDeg),
                                        Units.degreesToRadians(maxRotSpeedDeg)));

                Transform2d error = pose.minus(alignmentGoal);

                ChassisSpeeds fieldRelativeOutput = feedforward.plus(feedback);
                chassisSpeeds =
                        ChassisSpeeds.fromFieldRelativeSpeeds(fieldRelativeOutput, getPose().getRotation());

                if ((chassisSpeeds.omegaRadiansPerSecond < 0
                                && spinDirection.equals(Optional.of(SpinDirection.LEFT)))
                        || (chassisSpeeds.omegaRadiansPerSecond > 0
                                        && spinDirection.equals(Optional.of(SpinDirection.RIGHT)))
                                && Math.abs(error.getRotation().getDegrees()) > 60) {
                    chassisSpeeds.omegaRadiansPerSecond *= -1;
                }
                setModuleSetpoints(chassisSpeeds);

                if (error.getTranslation().getNorm() < OperatorInterfaceConstants.posError
                        && Math.abs(error.getRotation().getRadians()) < OperatorInterfaceConstants.angleError) {
                    chassisSpeeds = new ChassisSpeeds();
                    newState = SwerveStates.MOVING;
                }
            }
        }
    }

    private int gyroStabilizeDelay = SwerveConstants.STABILIZATION_DELAY;

    private void setGyroStabilization(boolean resetTimer) {
        gyroStabilizeYaw = Optional.of(gyroYaw.getDegrees());
        if (resetTimer) {
            gyroStabilizeDelay = SwerveConstants.STABILIZATION_DELAY;
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
                                gyroYaw.getRadians(), Math.toRadians(gyroStabilizeYaw.get()));

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
        return Math.abs(chassisSpeeds.omegaRadiansPerSecond) < 0.1
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

        // Update odometry
        double[] sampleTimestamps =
                modules[0].getOdometryTimestamps(); // All signals are sampled together
        int sampleCount = sampleTimestamps.length;
        Rotation2d yaw = gyroYaw;
        for (int i = 0; i < sampleCount; i++) {
            // Read wheel positions and deltas from each module
            for (int moduleIndex = 0; moduleIndex < 4; moduleIndex++) {
                lastModulePositions[moduleIndex] = modules[moduleIndex].getOdometryPositions()[i];
            }

            poseEstimator.updateWithTime(sampleTimestamps[i], yaw, lastModulePositions);
        }
    }

    private void setModuleSetpoints(ChassisSpeeds speed) {
        speed = ChassisSpeeds.discretize(speed, .02);

        setpointStates = kinematics.toSwerveModuleStates(speed);

        for (int i = 0; i < modules.length; i++) {
            modules[i].setMotion(setpointStates[i]);
        }
    }

    /* Odometry Functions */
    public Module[] getModules() {
        return modules;
    }
    ;

    public SwerveDriveKinematics getKinematics() {
        return kinematics;
    }

    public SwerveModulePosition[] getSwerveModulePositions() {
        return lastModulePositions;
    }

    public ChassisSpeeds getChassisSpeeds() {
        if (moduleStates != null && moduleStates[0] != null) {
            return kinematics.toChassisSpeeds(moduleStates);
        } else {
            return null;
        }
    }

    public SwerveStates getState() {
        return currentState;
    }

    // DO NOT USE OUTSIDE OF THE CONTEXT OF THE SWERVE TEST!!!
    // DOING SO COULD INTERFERE WITH THE COMMAND FRAMEWORK
    public void setVelocitySetpoint(ChassisSpeeds chassisSpeeds) {
        this.chassisSpeeds = chassisSpeeds;
    }

    public Rotation2d getYawVelocity() {
        if (gyroInputs.connected) {
            return Rotation2d.fromDegrees(gyroInputs.yawDegreesPerSec);
        } else {
            return Rotation2d.fromRadians(getChassisSpeeds().omegaRadiansPerSecond);
        }
    }

    public void resetPoseRotation() {
        poseCalibrated = true;
        var newPose = getPose();
        newPose = new Pose2d(newPose.getTranslation(), FlipUtil.conditionalFlip(Rotation2d.kZero));
        resetPose(newPose);
    }

    public boolean poseCalibrated() {
        return poseCalibrated;
    }

    /* Pose Functions */
    public SwerveDrivePoseEstimator getPoseEstimator() {
        return poseEstimator;
    }

    public Pose2d getPose() {
        return poseEstimator.getEstimatedPosition();
    }

    public void resetPose(Pose2d pose) {
        poseCalibrated = true;
        poseEstimator.resetPosition(gyroYaw, getSwerveModulePositions(), pose);
    }

    public void addVisionMeasurement(Pose2d pose, double timestamp, Matrix<N3, N1> stdDevs) {
        poseEstimator.addVisionMeasurement(pose, timestamp, stdDevs);
    }

    /* Testing Functions */
    public void testing_setPDSV(double P, double D, double S, double V, SwerveModuleMotor target) {
        backLeft.testing_setPDSV(P, D, S, V, target);
        backRight.testing_setPDSV(P, D, S, V, target);
        frontLeft.testing_setPDSV(P, D, S, V, target);
        frontRight.testing_setPDSV(P, D, S, V, target);
    }

    @Override
    public ManualTestGroup createManualTests() {
        if (Constants.kRobotMedium == RobotMedium.REAL) {
            return new ManualTestGroup(
                    "Swerve Drive",
                    backLeft.getManualTest(),
                    backRight.getManualTest(),
                    frontRight.getManualTest(),
                    frontLeft.getManualTest(),
                    gyro.getManualTest(),
                    new SwerveTuningTest("Swerve Tuning", Robot.getInstance().operatorInterface, this));
        } else {
            return new ManualTestGroup("Swerve Drive");
        }
    }

    public Pose2d getGoal() {
        return alignmentGoal;
    }

    /**
     * Whether or not the robot is aligned with its goal
     *
     * @return Whether or not the robot is aligned with its goal
     * @param xTolerance forward/back tolerance for "in position" in meters
     * @param yTolerance left/right tolerance for "in position" in meters
     * @param rotTolerance rotational tolerance for "in position" in meters
     */
    public boolean isAligned(double xTolerance, double yTolerance, Rotation2d rotTolerance) {
        Pose2d error = alignmentGoal.relativeTo(getPose());
        return Math.abs(error.getX()) < xTolerance
                && Math.abs(error.getY()) < yTolerance
                && Math.abs(error.getRotation().getDegrees()) < rotTolerance.getDegrees();
    }

    public void setSampleSetpoint(SwerveSample sample) {
        this.swerveSample = sample;

        // Get the current pose of the robot
        Pose2d pose = getPose();

        // Generate the next speeds for the robot
        ChassisSpeeds speeds =
                ChassisSpeeds.fromFieldRelativeSpeeds(
                        sample.vx + translationXController.calculate(pose.getX(), sample.x),
                        sample.vy + translationYController.calculate(pose.getY(), sample.y),
                        sample.omega
                                + rotationController.calculate(pose.getRotation().getRadians(), sample.heading),
                        pose.getRotation());

        // Apply the generated speeds
        setVelocitySetpoint(speeds);
    }

    /* Command Factories */
    public Command drive(Supplier<ChassisSpeeds> speedSupplier) {
        return this.runOnce(
                        () -> {
                            currentState = SwerveStates.INTERRUPT;
                            newState = SwerveStates.MOVING;
                        })
                .andThen(Commands.run(() -> this.setVelocitySetpoint(speedSupplier.get())))
                .until(() -> newState != SwerveStates.MOVING)
                .finallyDo(() -> this.setVelocitySetpoint(new ChassisSpeeds()))
                .withName("Driving");
    }

    public Command alignTo(Pose2d pose) {
        return alignTo(pose, 1.5, 0.45, Double.MAX_VALUE);
    }

    // direction represents which way the robot should rotate
    // e.g. passing in SpinDirection.LEFT will make the robot spin left, even if
    // spinning right would be more optimal
    public Command alignTo(Pose2d pose, SpinDirection direction) {
        return Commands.runOnce(() -> this.spinDirection = Optional.of(direction))
                .andThen(alignTo(pose))
                .finallyDo(() -> this.spinDirection = Optional.empty());
    }

    public Command alignTo(
            Pose2d pose, double maxTransSpeed, double slowdownDistance, double maxRotSpeedDeg) {
        return this.runOnce(
                        () -> {
                            currentState = SwerveStates.INTERRUPT;
                            newState = SwerveStates.ALIGNING;
                            alignmentGoal = FlipUtil.conditionalFlip(pose);
                            this.maxSpeed = maxTransSpeed;
                            this.slowdownDistance = slowdownDistance;
                            this.maxRotSpeedDeg = maxRotSpeedDeg;
                        })
                .andThen(Commands.idle())
                .until(() -> newState != SwerveStates.ALIGNING)
                .finallyDo(() -> gyroStabilizeYaw = Optional.of(gyroYaw.getDegrees()))
                .withName("Aligning");
    }

    @Override
    public boolean getHealth() {
        return backLeft.getHealth()
                && backRight.getHealth()
                && frontLeft.getHealth()
                && frontRight.getHealth();
    }
}
