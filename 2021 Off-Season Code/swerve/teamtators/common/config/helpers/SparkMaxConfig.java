package org.teamtators.common.config.helpers;

import com.revrobotics.CANDigitalInput;
import com.revrobotics.CANError;
import com.revrobotics.CANSparkMax;
import com.revrobotics.CANSparkMaxLowLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamtators.common.Robot;
import org.teamtators.common.hw.NEOEncoder;
import org.teamtators.common.hw.TatorSparkMax;

public class SparkMaxConfig implements ConfigHelper<CANSparkMax> {
    private static final Logger logger = LoggerFactory.getLogger(SparkMaxConfig.class);
    private static final String REQUIRED_VERSION = "v1.5.2";

    public int id = -1;
    public CANSparkMaxLowLevel.MotorType motorType = CANSparkMaxLowLevel.MotorType.kBrushless;
    public boolean inverted = false;
    public CANSparkMax.IdleMode neutralMode = CANSparkMax.IdleMode.kCoast;
    public int smartCurrentStallLimit = 80;
    public int smartCurrentFreeLimit = 20;
    public int smartCurrentLimitRPM = 20000;
    public double openLoopRampRate = 0;
    public double closedLoopRampRate = 0;
    public int timeoutMilliseconds = 100;
    public CANSparkMax.SoftLimitDirection setDirection = CANSparkMax.SoftLimitDirection.kForward;
    public float softLimit = 1000;
    public CANSparkMax.SoftLimitDirection enableDirection = CANSparkMax.SoftLimitDirection.kForward;
    public boolean enableSoftLimit = false;
    public CANDigitalInput.LimitSwitchPolarity forwardPolarity = CANDigitalInput.LimitSwitchPolarity.kNormallyOpen;
    public boolean forwardEnable = false;
    public CANDigitalInput.LimitSwitchPolarity reversePolarity = CANDigitalInput.LimitSwitchPolarity.kNormallyOpen;
    public boolean reverseEnable = false;

    public NEOEncoder.Config encoderConfig;

    public int Status0Period = 100; //Applied Output, Faults, Sticky Faults, Is Follower. Default = 10ms
    public int Status1Period = 500; //Motor Velocity, Motor Temperature, Motor Voltage, Motor Current. Default = 20ms
    public int Status2Period = 500; //Motor Position. Default = 20ms

    private static final boolean BURN_FLASH = false;

    @Override
    public TatorSparkMax create() {
        delay(250);
        TatorSparkMax sparkMax = new TatorSparkMax(id, motorType);
        if(Robot.isReal()) cfg(sparkMax);
        sparkMax.setEncoderConfig(encoderConfig);
        sparkMax.getEncoder().setPosition(0);
        return sparkMax;
    }

    private void cfg(TatorSparkMax sparkMax) {
        sparkMax.setInverted(inverted);
        delay(25);
        check(sparkMax.setCANTimeout(timeoutMilliseconds));
        delay(25);
        check(sparkMax.setIdleMode(neutralMode));
        delay(25);
        check(sparkMax.setSmartCurrentLimit(smartCurrentStallLimit, smartCurrentFreeLimit, smartCurrentLimitRPM));
        delay(25);
//        check(sparkMax.setSecondaryCurrentLimit(secondaryCurrentlimit, secondaryChopCycles));
//        check(sparkMax.setOpenLoopRampRate(openLoopRampRate));
//        check(sparkMax.setClosedLoopRampRate(closedLoopRampRate));
//        check(sparkMax.setSoftLimit(setDirection, softLimit));
//        check(sparkMax.enableSoftLimit(enableDirection, enableSoftLimit));
        check(sparkMax.configForwardLimit(forwardPolarity, forwardEnable));
        delay(25);
        check(sparkMax.configReverseLimit(reversePolarity, reverseEnable));
        delay(25);
        check(sparkMax.setPeriodicFramePeriod(CANSparkMaxLowLevel.PeriodicFrame.kStatus0, Status0Period));
        delay(25);
        check(sparkMax.setPeriodicFramePeriod(CANSparkMaxLowLevel.PeriodicFrame.kStatus1, Status1Period));
        delay(25);
        check(sparkMax.setPeriodicFramePeriod(CANSparkMaxLowLevel.PeriodicFrame.kStatus2, Status2Period));
        delay(25);
        if(BURN_FLASH) {
            logger.debug("Burning!");
            check(sparkMax.burnFlash());
        }
        delay(20);
        sparkMax.clearFaults();
        String version = sparkMax.getFirmwareString();
        if(!version.equals(REQUIRED_VERSION)) {
            logger.warn("SPARK MAX (id: {}) does not have the required firmware version {} (read: {})",
                    sparkMax.getDeviceId(), REQUIRED_VERSION, version);
        }
    }

    private void check(CANError error) {
        if (error == CANError.kOk) {
            return;
        }
        Robot.logger.error("Error while configuring SPARK MAX (id {}), {}", id, error.toString());
    }

    private void delay(long ms) {
        try {
            Thread.sleep(ms);
        } catch (Exception ignored) {
        }
    }
}
