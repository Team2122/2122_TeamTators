package org.teamtators.bbt8r.subsystems;

import SplineGenerator.Util.DPoint;
import SplineGenerator.Util.DVector;
import com.kauailabs.navx.frc.AHRS;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.I2C;
import edu.wpi.first.wpilibj.geometry.Pose2d;
import edu.wpi.first.wpilibj.geometry.Rotation2d;
import edu.wpi.first.wpilibj.geometry.Translation2d;
import edu.wpi.first.wpilibj.kinematics.SwerveDriveKinematics;
import edu.wpi.first.wpilibj.kinematics.SwerveDriveOdometry;
import org.teamtators.bbt8r.staging.ArcLengthTracker;
import org.teamtators.bbt8r.staging.GeneralPIDController;
import org.teamtators.bbt8r.staging.PositionTracker;
import org.teamtators.bbt8r.staging.Vector;
import org.teamtators.bbt8r.subsystems.SwerveInputProxy.SwerveInput;
import org.teamtators.common.control.Timer;
import org.teamtators.common.scheduler.Subsystem;


public class SwerveDrive extends Subsystem {

    private SwerveModule[] moduleArray;
    private SwerveModule module0;           // The four swerve modules
    private SwerveModule module1;
    private SwerveModule module2;
    private SwerveModule module3;
    private AHRS gyro;
    private ArcLengthTracker arcLengthTracker;

    private Vector pos = new Vector();
    private final double PI = Math.PI;

    private Timer timer;

    // PID Controllers
    private GeneralPIDController velocityXPID = new GeneralPIDController(.5, 0, 0, 0, 1);
    private GeneralPIDController velocityYPID = new GeneralPIDController(.5, 0, 0, 0, 1);
    private GeneralPIDController rotationPID = new GeneralPIDController(8000, 0, 0, 0, 1);
    private double desiredAngle;

    private DVector velocity;
    private PositionTracker positionTracker;
    private SplineGenerator.Applied.PositionTracker SGPositionTracker;

    private SwerveDriveKinematics swerveDriveKinematics;
    private SwerveDriveOdometry swerveDriveOdometry;
    private double theta;
    private double currentAngle;


    private Config config;

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
    private Timer timer2 = new Timer();

    private double[] posArray = new double[2];
    private double[] xVectorComps;
    private double[] yVectorComps;

    public SwerveDrive() {
        super("SwerveDrive");
        positionTracker = new PositionTracker();
        SGPositionTracker = new SplineGenerator.Applied.PositionTracker(new DPoint(0, 0));
        velocity = new DVector(0, 0);
        timer = new Timer();
        timer.start();

        inst = NetworkTableInstance.getDefault();
        logger.info("Getting Entries");
        xVectors = inst.getTable(tableKey).getEntry(xVectorsKey);
        yVectors = inst.getTable(tableKey).getEntry(yVectorsKey);
        posInTable = inst.getTable(tableKey).getEntry(posInTableKey);
        gyroTheta = inst.getTable(tableKey).getEntry(gyroKey);

        xVectorComps = new double[5];
        yVectorComps = new double[5];

        arcLengthTracker = new ArcLengthTracker();

        swerveDriveKinematics = new SwerveDriveKinematics(new Translation2d(.3048, .3048),
                new Translation2d(-.3048, .3048), new Translation2d(-.3048, -.3048), new Translation2d(.3048, -.3048));
        swerveDriveOdometry = new SwerveDriveOdometry(swerveDriveKinematics, new Rotation2d(0));
    }

    public void updateModules(SwerveInput input) {
        if(counter == 0){
            timer2.start();
            counter = 10;
        }

        Vector avgVec = getAverageVector();

        velocityXPID.setCurrentState(avgVec.getX());
        velocityYPID.setCurrentState(avgVec.getY());

        // Update Translation PID Targets
        velocityXPID.setSetPoint(input.vector.getX());
        velocityYPID.setSetPoint(input.vector.getY());
        rotationPID.setCurrentState(-getRotation());

        theta = -getRotation() - PI/2;
        rotationPID.setCurrentState(theta);
        if (input.rotationScalar == 0 && !(input.vector.getX() == 0 && input.vector.getY() == 0)) {
            input.rotationScalar = -rotationPID.getOutput();
        } else {
            currentAngle = theta;
            desiredAngle = currentAngle;
            rotationPID.setSetPoint(desiredAngle);
        }

        input.vector.setTheta(-(input.vector.getTheta() - PI/2) - getRotation());

        module0.setMotion(input);
        module1.setMotion(input);
        module2.setMotion(input);
        module3.setMotion(input);

        theta = getRotation();
        avgVec.setTheta(avgVec.getTheta() - theta);
        double time = timer.restart();

        arcLengthTracker.update(avgVec.getMagnitude(),time);
        positionTracker.updateWithTime(avgVec, time);
        System.out.println(arcLengthTracker.getDistance() + " timer: " + timer2.get());

        velocity.set(0, avgVec.getX(), avgVec.getY());
        SGPositionTracker.update(velocity, time);

        Vector position = positionTracker.getPosition();
        pos.setXY(position.getX(), position.getY());
        pos.setTheta(position.getTheta()-PI/2);

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

    }

    public double getRotation() {
        return Math.toRadians(gyro.getYaw());
    }

    public DPoint getPoint() {
        return new DPoint(pos.getX(), pos.getY());
    }


    public void resetYaw() {
        gyro.zeroYaw();
    }

    public void resetPosition() {
        pos.setXY(0, 0);
        swerveDriveOdometry.resetPosition(new Pose2d(), new Rotation2d(getRotation()));
        System.out.println("Before: " +positionTracker.getPosition());
        positionTracker.setPosition(pos);
        System.out.println("After: " + positionTracker.getPosition());
    }

    public double getCurrentAngle() {
        return Math.toRadians(gyro.getAngle() + 90);
    }

    public void bumpPDown (){
        module0.setP(module0.getP()-.05);
        module1.setP(module1.getP()-.05);
        module2.setP(module2.getP()-.05);
        module3.setP(module3.getP()-.05);
        System.out.println(module3.getP());
    }


    public void bumpPUp (){
        module0.setP(module0.getP()+.05);
        module1.setP(module1.getP()+.05);
        module2.setP(module2.getP()+.05);
        module3.setP(module3.getP()+.05);
        System.out.println(module3.getP());
    }

    public void stop() {
        module0.stop();
        module1.stop();
        module2.stop();
        module3.stop();
    }

    public boolean atTargetRotation() {
        return module0.atTargetAngle(config.maxRotationError)
                && module1.atTargetAngle(config.maxRotationError)
                && module2.atTargetAngle(config.maxRotationError)
                && module3.atTargetAngle(config.maxRotationError);
    }


    public double getAverageAngleError() {
        return (module0.getAngleError() + module1.getAngleError() + module2.getAngleError() + module3.getAngleError()) / 4.0;
    }

    public Vector getAverageVector() {
        return module0.getTotalVector().add(module1.getTotalVector()).add(module2.getTotalVector()).add(module3.getTotalVector()).scale(.25);
    }

    public double getAverageAngle() {
        return (module0.getCurrentAngle() + module1.getCurrentAngle() + module2.getCurrentAngle() + module3.getCurrentAngle()) / 4.0;
    }

    public Vector getPosition() {
        return positionTracker.getPosition();
    }

    public double getArcLength(){
        return arcLengthTracker.getDistance();
    }

    public void configure(Config config) {
        this.config = config;

        gyro = new AHRS(I2C.Port.kOnboard);
        gyro.calibrate();
        gyro.zeroYaw();
        gyro.setAngleAdjustment(90.0);

        module0 = config.swerveModuleConfig0.create();
        module1 = config.swerveModuleConfig1.create();
        module2 = config.swerveModuleConfig2.create();
        module3 = config.swerveModuleConfig3.create();

        moduleArray = new SwerveModule[]{module0, module1, module2, module3};

        config.wheelCircumference = 2 * PI * config.wheelRadius;
        gyro.zeroYaw();
    }

    public double RPMtoIPM(double RPM) {
        return RPM * config.wheelCircumference;
    }

    public double IPMtoRPM(double IPS) {
        return IPS / config.wheelCircumference;
    }

    public AHRS getGyro() {
        return gyro;
    }


    public static class Config {
        public SwerveModule.Config swerveModuleConfig0;
        public SwerveModule.Config swerveModuleConfig1;
        public SwerveModule.Config swerveModuleConfig2;
        public SwerveModule.Config swerveModuleConfig3;

        public double maxRotationError = .04;

        public double wheelRadius = 4.5;
        protected double wheelCircumference = wheelRadius * 2 * Math.PI;

    }
}
