package org.teamtators.common.config.helpers;

import com.ctre.phoenix.motorcontrol.LimitSwitchNormal;
import com.ctre.phoenix.motorcontrol.LimitSwitchSource;
import com.ctre.phoenix.motorcontrol.can.WPI_TalonSRX;
import org.teamtators.common.hw.LazyTalonSRX;

public class TalonSRXConfig extends CtreMotorControllerConfig implements ConfigHelper<WPI_TalonSRX> {
    public static int REQUIRED_FIRMWARE = 0x0308; // TalonSRX firmware 3.8
    public LimitSwitchSource forwardLimitSwitchSource = LimitSwitchSource.Deactivated;
    public LimitSwitchSource reverseLimitSwitchSource = LimitSwitchSource.Deactivated;
    public LimitSwitchNormal forwardLimitSwitchNormal = LimitSwitchNormal.Disabled;
    public LimitSwitchNormal reverseLimitSwitchNormal = LimitSwitchNormal.Disabled;
    public boolean limitCurrent = false;
    public int continuousCurrentLimit = 0;
    public int peakCurrentLimit = 0;
    public int peakCurrentDurationMillis = 0;

    public WPI_TalonSRX create() {
        super.validate();
        LazyTalonSRX motor = new LazyTalonSRX(id);
        motor.enableCurrentLimit(false);
        super.configure(motor);
        super.checkVersion(motor, REQUIRED_FIRMWARE);
        motor.configForwardLimitSwitchSource(forwardLimitSwitchSource, forwardLimitSwitchNormal, CONFIG_TIMEOUT);
        motor.configReverseLimitSwitchSource(reverseLimitSwitchSource, reverseLimitSwitchNormal, CONFIG_TIMEOUT);
        motor.configForwardSoftLimitEnable(false, CONFIG_TIMEOUT);
        motor.configReverseSoftLimitEnable(false, CONFIG_TIMEOUT);
        motor.configContinuousCurrentLimit(continuousCurrentLimit, CONFIG_TIMEOUT);
        motor.configPeakCurrentLimit(peakCurrentLimit, CONFIG_TIMEOUT);
        motor.configPeakCurrentDuration(peakCurrentDurationMillis, CONFIG_TIMEOUT);
        motor.enableCurrentLimit(limitCurrent);

        return motor;
    }
}
