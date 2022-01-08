package org.teamtators.common.hw;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.IFollower;
import com.ctre.phoenix.motorcontrol.can.BaseMotorController;
import com.ctre.phoenix.motorcontrol.can.WPI_TalonSRX;
import edu.wpi.first.wpilibj.SpeedController;

public class CtreMotorControllerGroup implements MotorControllerGroup {
    private BaseMotorController[] controllers;
    private BaseMotorController master;
    private double currentOutput;
    private boolean inverted;

    public CtreMotorControllerGroup(BaseMotorController... controllers) {
        master = controllers[0];
        this.controllers = controllers;
        enableFollowerMode();
    }

    public void enableFollowerMode() {
        for (IFollower motor : controllers) {
            if (master == motor) {
                continue;
            }
            motor.follow(master);
        }
    }

    public void disableFollowerMode() {
        for (BaseMotorController controller : controllers) {
            controller.set(ControlMode.Disabled, 0);
        }
    }

    public SpeedController[] getSpeedControllers() {
        return (SpeedController[]) controllers;
    }

    @Override
    public void set(double speed) {
        speed *= inverted ? -1 : 1;
        master.set(ControlMode.PercentOutput, speed);
        this.currentOutput = speed;
    }

    @Override
    public double get() {
        return currentOutput;
    }

    @Override
    public void setInverted(boolean isInverted) {
        this.inverted = isInverted;
    }

    @Override
    public boolean getInverted() {
        return inverted;
    }

    @Override
    public void disable() {
        master.set(ControlMode.Disabled, 0);
    }

    @Override
    public void stopMotor() {
        disable();
    }

    @Override
    public void pidWrite(double output) {
        set(output);
    }

    public WPI_TalonSRX getMaster() {
        return (WPI_TalonSRX) master;
    }

    public double getVelocity() {
        return getMaster().getSelectedSensorVelocity(0);
    }
}
