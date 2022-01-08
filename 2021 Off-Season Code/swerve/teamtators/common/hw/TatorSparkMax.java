package org.teamtators.common.hw;

import com.revrobotics.*;
import edu.wpi.first.wpilibj.DutyCycleEncoder;
import org.teamtators.common.datalogging.Disable;

public class TatorSparkMax extends CANSparkMax {
    private CANDigitalInput forwardLimit;
    private CANDigitalInput reverseLimit;
    private NEOEncoder.Config encoderConfig;
    private CANEncoder baseEncoder;
    private NEOEncoder neoEncoder;
    private CANPIDController baseController;

    public TatorSparkMax(int deviceID, MotorType type) {
        super(deviceID, type);
        set(0);
        Disable.warnOnce(() -> {
            // todo: how to disable CANSparkMax errors during simulation?
            setControlFramePeriodMs(0);
        });
    }

    public CANError configForwardLimit(CANDigitalInput.LimitSwitchPolarity polarity, boolean enabled) {
        forwardLimit = this.getForwardLimitSwitch(polarity);
        return forwardLimit.enableLimitSwitch(enabled);
    }

    public CANError configReverseLimit(CANDigitalInput.LimitSwitchPolarity polarity, boolean enabled) {
        reverseLimit = this.getReverseLimitSwitch(polarity);
        return reverseLimit.enableLimitSwitch(enabled);
    }

    public CANError follow(CANSparkMax max) {
        return follow(max, getInverted());
    }

    public boolean getForwardLimit() {
        return forwardLimit.get();
    }

    public boolean getReverseLimit() {
        return reverseLimit.get();
    }

    public void setEncoderConfig(NEOEncoder.Config encoderConfig) {
        this.encoderConfig = encoderConfig;
    }

    public NEOEncoder.Config getEncoderConfig() {
        return this.encoderConfig;
    }

    public NEOEncoder getNeoEncoder() {
        if (neoEncoder == null) {
            neoEncoder = new NEOEncoder(this);
        }
        return neoEncoder;
    }

    @Override
    public CANEncoder getEncoder() {
        if (this.baseEncoder == null)
            baseEncoder = super.getEncoder(); //getEncoder creates a new instance on every call!
        return baseEncoder;
    }

    @Override
    public CANPIDController getPIDController() {
        if (this.baseController == null)
            baseController = super.getPIDController();
        return baseController;
    }

    public void moveToTargetAngle(double targetAngle, DutyCycleEncoder encoder, double maxError, double speed) {

        double currentRotationPos = encoder.getDistance() % (2*3.1416);

        if (Math.abs( currentRotationPos - targetAngle % (2*3.1416)) < maxError ) {
            stopMotor();
        } else {
            set(speed);
        }
    }




}
