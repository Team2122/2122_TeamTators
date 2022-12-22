package frc.robot.subsystems;

import SplineGenerator.Util.DPoint;
import SplineGenerator.Util.DVector;
import com.revrobotics.CANSparkMax;
import com.revrobotics.CANSparkMaxLowLevel.MotorType;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.kinematics.SwerveDriveOdometry;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.networktables.EntryListenerFlags;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.SPI;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import frc.robot.Robot;
import frc.robot.RobotContainer;
import frc.robot.constants.SwerveConstants;
import org.teamtators.Tools.tester.ManualTestGroup;
import org.teamtators.Util.*;
import org.teamtators.sassitator.Subsystem;

public class SwerveDrive extends Subsystem {

    private SwerveModule[] moduleArray;
    private SwerveModule module0;           // The four swerve modules
    private SwerveModule module1;
    private SwerveModule module2;
    private SwerveModule module3;
    private SwerveGyro gyro;

    private NetworkTableEntry gyroTable = NetworkTableInstance.getDefault().getTable("visionTable").getEntry("newGyroAngle");//    private AHRS gyro;
    private NetworkTableEntry gyroRecal = NetworkTableInstance.getDefault().getTable("visionTable").getEntry("gyroRecal");//    private AHRS gyro;

    private SwerveDriveOdometry wpiPositionTracker;

    private Vector pos = new Vector();
    private final double PI = Math.PI;
    private Vector avgVec = new Vector();

    private Timer timer;
    private boolean setAngleOverride;


    // PID Controllers
    private GeneralPIDController rotationPID = new GeneralPIDController(7, 0, 0, 0, 1);

    private double desiredAngle;

    private DVector velocity;
    private PositionTracker positionTracker;

    private Vector initalOffset = new Vector(.641, -2.502);
    private SplineGenerator.Applied.PositionTracker SGPositionTracker;

    private double theta;
    private double currentAngle;

    private final double MOTOR_ROTATIONS_PER_MODULE_ROTATION;
    private final double MOTOR_ROTATIONS_PER_WHEEL_ROTATION;
    private final double WHEEL_CIRCUMFERENCE;

    private NetworkTableInstance inst;

    private final String tableKey = "PositionTable";
    private final String xVectorsKey = "XVectors";
    private final String yVectorsKey = "YVectors";
    private final String posInTableKey = "posInTable";
    private final String gyroKey = "gyroTheta";

    int counter = 0;

    private NetworkTableEntry xVectors;
    private NetworkTableEntry yVectors;
    private NetworkTableEntry posInTable;
    private NetworkTableEntry gyroTheta;

    private double[] posArray = new double[2];
    private double[] xVectorComps;
    private double[] yVectorComps;

    private boolean lockVel;
    private double acceleration = .32;
    private double zoomyModeMultiple = .9;
    private boolean lockAng;
    private Vector lastVel = new Vector(0,0);
    private RobotContainer robotContainer;
    //    private Vision vision;
    public SwerveDrive(RobotContainer robotContainer) {
        super(robotContainer);
        this.robotContainer = robotContainer;
        this.MOTOR_ROTATIONS_PER_WHEEL_ROTATION = SwerveConstants.SwerveModule.MOTOR_ROTATIONS_PER_WHEEL_ROTATION;
        this.MOTOR_ROTATIONS_PER_MODULE_ROTATION = SwerveConstants.SwerveModule.MOTOR_ROTATIONS_PER_MODULE_ROTATION;
        this.WHEEL_CIRCUMFERENCE = SwerveConstants.SwerveModule.WHEEL_CIRCUMFERENCE;

        setValidity(Robot.EnumRobotState.Autonomous, Robot.EnumRobotState.Teleop);

        positionTracker = new PositionTracker();

        Translation2d module0Pos = new Translation2d(-16, -13.5);
        Translation2d module1Pos = new Translation2d(16, -13.5);
        Translation2d module2Pos = new Translation2d(16, 13.5);
        Translation2d module3Pos = new Translation2d(-16, 13.5);

        wpiPositionTracker = new SwerveDriveOdometry(new SwerveDriveKinematics(module0Pos, module1Pos, module2Pos, module3Pos), new Rotation2d());
        SGPositionTracker = new SplineGenerator.Applied.PositionTracker(new DPoint(0, 0));
        velocity = new DVector(0, 0);
        timer = new Timer();
        timer.start();

        inst = NetworkTableInstance.getDefault();
        xVectors = inst.getTable(tableKey).getEntry(xVectorsKey);
        yVectors = inst.getTable(tableKey).getEntry(yVectorsKey);
        posInTable = inst.getTable(tableKey).getEntry(posInTableKey);
        gyroTheta = inst.getTable(tableKey).getEntry(gyroKey);

        xVectorComps = new double[5];
        yVectorComps = new double[5];
        configure();
        gyro = new TatorPigeon(16,"Default Name");
        gyro.zero();
        // Shuffleboard stuff:
        putDataOnShuffleboard();
        gyroRecal.setBoolean(false);
        gyroRecal.addListener(timed -> {
            if (gyroRecal.getBoolean(false)) {
                gyro.zero();
                gyroRecal.setBoolean(false);
            }
        }, EntryListenerFlags.kImmediate | EntryListenerFlags.kUpdate);
    }

    @Override
    public void configure(RobotContainer robotContainer) {
    }

    public void putDataOnShuffleboard() {
        SmartDashboard.putNumber("Gyro Angle", gyro.getYawContinuous());
        SmartDashboard.putBoolean("Is the Gyro Connected? ", gyro.isConnected());

        shuffleboardRegister.addData("Gyro Angle", () -> gyro.getYawContinuous());
        shuffleboardRegister.addData("Gyro Yaw", () -> gyro.getYawD());
    }

    public void doPeriodic() {
        gyroTable.setDouble(gyro.getYawContinuous());
    }

    public void lockVel() {
        lockVel = true;
    }

    public void unlockVel() {
        lockVel = false;
    }

    public void lockAng(){
        lockAng = true;
    }
    public void unlockAng(){
        lockAng = false;
    }
    public void unlock(){
        unlockVel();
        unlockAng();
    }

    public void updateModules(SwerveInputProxy.SwerveInput input) {
        gyroTable.setDouble(gyro.getYawContinuous());
        avgVec = getAverageVector();
        theta = -getRotation();
        double goodRotation;
        if (setAngleOverride) {
            goodRotation = Math.toRadians(-gyro.getYawContinuous());
        } else {
            goodRotation = Math.toRadians(-gyro.getYawContinuous());
        }
        rotationPID.setCurrentState(goodRotation);

        input.rotationScalar *= 1.5;
        if ((input.rotationScalar == 0) && !(!input.override && input.vector.getX() == 0 && input.vector.getY() == 0)) {
            // input.rotationScalar = rotationPID.getOutput();
        } else {
            if (!setAngleOverride) {
                currentAngle = goodRotation;
                desiredAngle = currentAngle;
                setDesiredAngle(desiredAngle);
            }
        }
//        System.out.println(input);

        if(lockVel){
            input.vector.setXY(0,0);
        }
        if(lockAng){
            input.rotationScalar = 0;
        }
//        if(!robotContainer.getDriverController().getRightBumper()){
//            Vector remapping = input.vector.clone().subtract(lastVel);
//            input.vector = remapping.getMagnitude()>acceleration ? remapping.setMagnitude(acceleration).add(lastVel) : input.vector;
//        }
        Vector remapping = input.vector.clone().subtract(lastVel);
        if(robotContainer.getDriverController().getRightBumper()){

            input.vector = remapping.getMagnitude() > acceleration*zoomyModeMultiple ? remapping.setMagnitude(acceleration*zoomyModeMultiple).add(lastVel) : input.vector;
        }
        else {
            input.vector = remapping.getMagnitude() > acceleration ? remapping.setMagnitude(acceleration).add(lastVel) : input.vector;
        }
        module0.setMotion(input);
        module1.setMotion(input);
        module2.setMotion(input);
        module3.setMotion(input);

        theta = getRotation();
        avgVec.setTheta(avgVec.getTheta() - theta);
        double time = timer.restart();

        positionTracker.updateWithTime(avgVec, time);
        wpiPositionTracker.update(new Rotation2d(-getRotation()), getModuleStateOf(module0), getModuleStateOf(module1), getModuleStateOf(module2), getModuleStateOf(module3));

        velocity.set(0, avgVec.getX(), avgVec.getY());
        SGPositionTracker.update(velocity, time);

        Vector position = positionTracker.getPosition();

        pos.setXY(position.getX(), position.getY());

        for (int i = 0; i < moduleArray.length; i++) {
            Vector vector = moduleArray[i].getTotalVector();
            vector.setTheta(vector.getTheta() - theta);
            xVectorComps[i] = vector.getX();
            yVectorComps[i] = vector.getY();
        }

        xVectorComps[4] = avgVec.getX();
        yVectorComps[4] = avgVec.getY();

        xVectors.setDoubleArray(xVectorComps);
        yVectors.setDoubleArray(yVectorComps);

        posArray[0] = pos.getX();
        posArray[1] = pos.getY();
        posInTable.setDoubleArray(posArray);

        gyroTheta.setDouble(-getRotation());
        lastVel = input.vector;
    }

    public void setPosition(Vector position) {
        positionTracker.setPosition(position);
        pos.set(position);
    }

    public double rotationsToMeters(double rotations) {
        double speed = (rotations / MOTOR_ROTATIONS_PER_WHEEL_ROTATION) * WHEEL_CIRCUMFERENCE;
        return speed;
    }

    public static void main(String[] args) {
        Vector lastVel = new Vector(0.984,0.654);
        Vector vector = new Vector(-0.051,-0.17);
        double acceleration = .1;
        Vector remapping = vector.clone().subtract(lastVel);
        vector = remapping.getMagnitude()>acceleration ? remapping.setMagnitude(acceleration).add(lastVel) : vector;
        System.out.println(vector);
    }

    public void setDesiredAngle(double angle) {
        double currentAngle = Math.toRadians(-gyro.getYawContinuous());
        double basicAngle = ((angle % (2 * Math.PI)) + (2 * Math.PI)) % (2 * Math.PI);

        double nearestMiddle2PIMultiple = currentAngle - (currentAngle % (2 * Math.PI)) - (currentAngle < 0 ? 2 * Math.PI : 0);
        double nearestLow2PIMultiple = nearestMiddle2PIMultiple - (2 * Math.PI);
        double nearestHigh2PIMultiple = nearestMiddle2PIMultiple + (2 * Math.PI);

        double lowOption = nearestLow2PIMultiple + basicAngle;
        double middleOption = nearestMiddle2PIMultiple + basicAngle;
        double highOption = nearestHigh2PIMultiple + basicAngle;

        double lowDiff = Math.abs(lowOption - currentAngle);
        double middleDiff = Math.abs(middleOption - currentAngle);
        double highDiff = Math.abs(highOption - currentAngle);

        boolean useLowNotHigh = lowDiff < highDiff;
        double lowHighDiff = useLowNotHigh ? lowDiff : highDiff;
        double lowHighOption = useLowNotHigh ? lowOption : highOption;

        double setPoint = middleDiff < lowHighDiff ? middleOption : lowHighOption;

        desiredAngle = setPoint;
        rotationPID.setSetPoint(setPoint);

        if (Math.abs(setPoint - currentAngle) > PI) {
        }
    }

    public double getDesiredAngle() {
        return desiredAngle;
    }

    public void enableOverride() {
        setAngleOverride = true;
    }

    public void disableOverride() {
        currentAngle = Math.toRadians(-gyro.getYawContinuous());
        desiredAngle = currentAngle;
        rotationPID.setSetPoint(desiredAngle);
        setAngleOverride = false;
    }

    public SwerveModuleState getModuleStateOf(SwerveModule module) {
        return new SwerveModuleState(rotationsToMeters(module.getTotalVector().getMagnitude()), new Rotation2d(module.getTotalVector().getTheta() - getRotation()));
    }

    public double getRotation() {
        return Math.toRadians(gyro.getYawContinuous());
    }

    public Vector getHubCentricPos() {
        Vector newVector = pos.clone();
        newVector.add(initalOffset);
        return newVector;
    }

    public DPoint getPoint() {
        return new DPoint(pos.getX(), pos.getY());
    }

    public void resetYaw() {
        gyro.zero();
        setDesiredAngle(0);
        setAngleOverride = false;
    }

    public void resetPosition() {
        pos.setXY(0, 0);
        positionTracker.setPosition(pos);
    }

    public void bumpPDown() {
        module0.setP(module0.getP() - .05);
        module1.setP(module1.getP() - .05);
        module2.setP(module2.getP() - .05);
        module3.setP(module3.getP() - .05);
    }

    public void bumpPUp() {
        module0.setP(module0.getP() + .05);
        module1.setP(module1.getP() + .05);
        module2.setP(module2.getP() + .05);
        module3.setP(module3.getP() + .05);
    }

    public void stop() {
        module0.stop();
        module1.stop();
        module2.stop();
        module3.stop();
    }

    public boolean atTargetRotation() {
        return module0.atTargetAngle(.04)
                && module1.atTargetAngle(.04)
                && module2.atTargetAngle(.04)
                && module3.atTargetAngle(.04);
    }

    public double getAverageDesiredAngleError() {
        return (module0.getDesiredAngleError() + module1.getDesiredAngleError() + module2.getDesiredAngleError() + module3.getDesiredAngleError()) / 4.0;
    }

    public double getAverageAngleError() {
        return (module0.getAngleError() + module1.getAngleError() + module2.getAngleError() + module3.getAngleError()) / 4.0;
    }

    public Vector getAverageVector() {
        return module0.getTotalVector().add(module1.getTotalVector()).add(module2.getTotalVector()).add(module3.getTotalVector()).scale(.25);
    }

    public Vector getRotatedAverageVector() {
        Vector vector = getAverageVector();
        vector.setTheta(vector.getTheta() + getRotation());
        return vector;
    }

    public double getAverageAngle() {
        return (module0.getCurrentAngle() + module1.getCurrentAngle() + module2.getCurrentAngle() + module3.getCurrentAngle()) / 4.0;
    }

    public Vector getPosition() {
        return positionTracker.getPosition();
    }

    public void enableModules() {
        module0.setState(SwerveModule.ModuleState.Enabled);
        module1.setState(SwerveModule.ModuleState.Enabled);
        module2.setState(SwerveModule.ModuleState.Enabled);
        module3.setState(SwerveModule.ModuleState.Enabled);
    }

    public void disableModules() {
        module0.setState(SwerveModule.ModuleState.Disabled);
        module1.setState(SwerveModule.ModuleState.Disabled);
        module2.setState(SwerveModule.ModuleState.Disabled);
        module3.setState(SwerveModule.ModuleState.Disabled);
    }

    public void printAllPositions() {
        module0.printPosition();
        module1.printPosition();
        module2.printPosition();
        module3.printPosition();
    }

    public void configure() {
        gyro = new TatorAHRS(SPI.Port.kOnboardCS0);
        gyro.zero();

        CANCoderWrapper encoder0 = new CANCoderWrapper(9,"Default Name");
        CANCoderWrapper encoder1 = new CANCoderWrapper(10,"Default Name");
        CANCoderWrapper encoder2 = new CANCoderWrapper(11,"Default Name");
        CANCoderWrapper encoder3 = new CANCoderWrapper(12,"Default Name");

        try {
            Thread.sleep(2000);
        } catch (Exception ignored) {

        }

        shuffleboardRegister.addData("Encoder 0", () -> encoder0.getAbsolutePosition(), 1, 1, 1, 1);
        shuffleboardRegister.addData("Encoder 1", () -> encoder1.getAbsolutePosition(), 1, 1, 3, 1);
        shuffleboardRegister.addData("Encoder 2", () -> encoder2.getAbsolutePosition(), 1, 1, 5, 1);
        shuffleboardRegister.addData("Encoder 3", () -> encoder3.getAbsolutePosition(), 1, 1, 7, 1);

        module0 = new SwerveModule(0, new TalonFXWrapper(1,"Default Name"), new CANSparkMax(5, MotorType.kBrushless), encoder0);
        module1 = new SwerveModule(1, new TalonFXWrapper(2,"Default Name"), new CANSparkMax(6, MotorType.kBrushless), encoder1);
        module2 = new SwerveModule(2, new TalonFXWrapper(3,"Default Name"), new CANSparkMax(7, MotorType.kBrushless), encoder2);
        module3 = new SwerveModule(3, new TalonFXWrapper(4,"Default Name"), new CANSparkMax(8, MotorType.kBrushless), encoder3);

        moduleArray = new SwerveModule[]{module0, module1, module2, module3};
        gyro.zero();
    }

    public SwerveGyro getGyro() {
        return gyro;
    }

    @Override
    public void reset() {

    }

    @Override
    public ManualTestGroup createManualTests() {
        ManualTestGroup tests = new ManualTestGroup(getName());

        return tests;
    }
}