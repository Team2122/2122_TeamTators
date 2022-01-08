package org.teamtators.common.control;

import com.revrobotics.CANPIDController;
import com.revrobotics.ControlType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.hw.TatorSparkMax;

public class SparkMaxPIDController extends CANPIDController implements Configurable<SparkMaxPIDController.Config> {
    private static final Logger logger = LoggerFactory.getLogger(SparkMaxPIDController.class);
    private String name;
    private TatorSparkMax sparkMax;
    private int ticksCount;
    private int slot = 0;

    public SparkMaxPIDController(TatorSparkMax device, String name) {
        super(device);
        this.name = name;
        this.sparkMax = device;
        this.ticksCount = sparkMax.getEncoder().getCountsPerRevolution();
    }
    
    public SparkMaxPIDController(TatorSparkMax device, String name, int slot) {
        this(device, name);
        this.slot = -1;
    }

    public TatorSparkMax getSparkMax() {
        return sparkMax;
    }

    public String getName() {
        return name;
    }

    private double floor(double rotations) {
        return ((int)(ticksCount / rotations)) * (double)ticksCount;
    }

    public void moveToPosition(double deltaPos) {
        this.setReference(deltaPos, ControlType.kSmartMotion);
    }

    public void setPositionSetpoint(double setpoint) {
        this.setReference(floor(setpoint), ControlType.kPosition);
    }

    public void setVelocitySetpoint(double setpoint) {
        this.setReference(setpoint, ControlType.kVelocity);
    }

    public void stop() {
        this.setReference(0, ControlType.kDutyCycle);
    }

    public void setPower(double power) {
        power = power > 1 ? 1 : power;
        power = power < -1 ? -1 : power;
        this.setReference(power, ControlType.kDutyCycle);
    }

    public void configure(Config config) {
        if(Math.signum(config.P) == -1 || Math.signum(config.I) == -1 || Math.signum(config.IZone) == -1 || Math.signum(config.D) == -1) {
            logger.warn("Spark Max PID gains must be positive!");
            config.P = Math.abs(config.P);
            config.I = Math.abs(config.I);
            config.D = Math.abs(config.D);
            config.IZone = Math.abs(config.IZone);
        }

        this.setP(config.P, slot);
        this.setI(config.I, slot);
        this.setIZone(config.IZone, slot);
        this.setD(config.D, slot);
        this.setFF(config.F, slot);
        this.setOutputRange(config.minOutputPID, config.maxOutputPID, slot);
        this.setSmartMotionMinOutputVelocity(config.minVelocity, slot);
        this.setSmartMotionMaxVelocity(config.travelVelocity, slot);
        this.setSmartMotionMaxAccel(config.maxAcceleration, slot);
        this.setSmartMotionAllowedClosedLoopError(config.maxAllowedError, slot);
        this.setSmartMotionAccelStrategy(config.strategy, slot);
    }

    public static class Config {
        //Note: Internally, smart motion (and velocity) deal with units of RPM
        public double P;
        public double I;
        public double IZone;
        public double D;
        public double F;

        public double minOutputPID = -1;
        public double maxOutputPID = 1;

        public double minVelocity;
        public double travelVelocity; //travel velocity
        public double maxAcceleration; //acceleration
        public double maxAllowedError = 0;

        public AccelStrategy strategy = AccelStrategy.kTrapezoidal;
    }
}
