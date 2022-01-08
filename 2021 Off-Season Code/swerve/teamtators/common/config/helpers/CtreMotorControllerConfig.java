package org.teamtators.common.config.helpers;

import com.ctre.phoenix.motorcontrol.*;
import org.teamtators.common.config.ConfigException;

public class CtreMotorControllerConfig {
    public static int CONFIG_TIMEOUT = 500;

    public static int FOLLOWER_GENERAL = 1000;
    public static int FOLLOWER_FEEDBACK = 1000;
    public static int FOLLOWER_CONTROL = 100;
    public static int FOLLOWER_QUAD = 1000;
    public static int FOLLOWER_PULSEWIDTH = 1000;
    public static int FOLLOWER_AINTEMPVBAT = 1000;

    public int id = -1;
    public boolean inverted = false;
    public NeutralMode neutralMode = NeutralMode.EEPROMSetting;
    public double neutralDeadband = 0.04; // factory default
    public double openLoopRamp = 0.0; // # of seconds from 0 to full output, or 0 to disable
    public double voltageCompensationSaturation = Double.NaN;
    public boolean logTiming = false;
    public FeedbackDevice feedbackDevice = FeedbackDevice.RemoteSensor0;
    public double neutralToFullTime = 0;
    public int generalPeriodMs = 10;
    public int feedback0PeriodMs = 100;
    public int quadPeriodMs = 100;   //not the same as feedback0
    public int ainTempVbatPeriodMs = 100;
    public int controlPeriodMs = 5;
    public int pulseWidthPeriodMs = 100;
    public boolean useDefaultFollowerFrames = false;

    protected void validate() {
        if (id == -1) {
            throw new ConfigException("Must set id on CtreMotorControllerConfig");
        }
    }

    protected void configure(com.ctre.phoenix.motorcontrol.can.BaseMotorController motor) {
        if(useDefaultFollowerFrames) {
            generalPeriodMs = FOLLOWER_GENERAL;
            feedback0PeriodMs = FOLLOWER_FEEDBACK;
            quadPeriodMs = FOLLOWER_QUAD;
            ainTempVbatPeriodMs = FOLLOWER_AINTEMPVBAT;
            controlPeriodMs = FOLLOWER_CONTROL;
            pulseWidthPeriodMs = FOLLOWER_PULSEWIDTH;
        }
        motor.setInverted(this.inverted);
        motor.setNeutralMode(this.neutralMode);
        motor.configNeutralDeadband(this.neutralDeadband, CONFIG_TIMEOUT);
        motor.configOpenloopRamp(this.openLoopRamp, CONFIG_TIMEOUT);
        motor.configVoltageCompSaturation(this.openLoopRamp, CONFIG_TIMEOUT);
        if (!Double.isNaN(this.voltageCompensationSaturation)) {
            motor.configVoltageCompSaturation(this.voltageCompensationSaturation, CONFIG_TIMEOUT);
            motor.enableVoltageCompensation(true);
        } else {
            motor.enableVoltageCompensation(false);
        }
        motor.configSelectedFeedbackSensor(feedbackDevice, 0, CONFIG_TIMEOUT);
        motor.configForwardLimitSwitchSource(RemoteLimitSwitchSource.Deactivated, LimitSwitchNormal.Disabled, 0, CONFIG_TIMEOUT);
        motor.configReverseLimitSwitchSource(RemoteLimitSwitchSource.Deactivated, LimitSwitchNormal.Disabled, 0, CONFIG_TIMEOUT);
        motor.configForwardSoftLimitEnable(false, CONFIG_TIMEOUT);
        motor.configReverseSoftLimitEnable(false, CONFIG_TIMEOUT);
        motor.configOpenloopRamp(neutralToFullTime, CONFIG_TIMEOUT);

        motor.setStatusFramePeriod(StatusFrameEnhanced.Status_1_General.value, generalPeriodMs, CONFIG_TIMEOUT);
        motor.setStatusFramePeriod(StatusFrameEnhanced.Status_2_Feedback0.value, feedback0PeriodMs, CONFIG_TIMEOUT);
        motor.setStatusFramePeriod(StatusFrameEnhanced.Status_3_Quadrature.value, quadPeriodMs, CONFIG_TIMEOUT);
        motor.setStatusFramePeriod(StatusFrameEnhanced.Status_4_AinTempVbat.value, ainTempVbatPeriodMs, CONFIG_TIMEOUT);
        motor.setStatusFramePeriod(StatusFrameEnhanced.Status_8_PulseWidth.value, pulseWidthPeriodMs, CONFIG_TIMEOUT);
        motor.setControlFramePeriod(ControlFrame.Control_3_General, controlPeriodMs);

    }

    protected void checkVersion(com.ctre.phoenix.motorcontrol.can.BaseMotorController motor, int requiredVersion) {
        int firmwareVersion = motor.getFirmwareVersion();
        if (firmwareVersion != requiredVersion) {
//            Robot.logger.warn(String.format("%s (id %d) has wrong firmware version: %d.%d",
//                    motor.getClass().getSimpleName(), id, firmwareVersion >> 2, firmwareVersion % 0xff));
        }
    }
}
