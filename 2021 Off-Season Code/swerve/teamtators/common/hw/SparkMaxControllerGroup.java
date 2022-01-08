package org.teamtators.common.hw;

import com.revrobotics.CANSparkMax;
import edu.wpi.first.wpilibj.SpeedController;

public class SparkMaxControllerGroup implements MotorControllerGroup {
    private TatorSparkMax[] controllers;
    private TatorSparkMax master;
    private double currentOutput;
    private boolean inverted;

    private boolean isEmulatingFollowerMode = true;

    public SparkMaxControllerGroup(TatorSparkMax... controllers) {
        master = controllers[0];
        this.controllers = controllers;
        enableFollowerMode();
    }
    public void enableFollowerMode() {
//        for(TatorSparkMax motor: controllers){
//            if(master == motor){
//                continue;
//            }
//            motor.follow(master);
//        }
        isEmulatingFollowerMode = true;
    }

    public void disableFollowerMode(){
        for(TatorSparkMax controller : controllers){
            controller.follow(CANSparkMax.ExternalFollower.kFollowerDisabled, 0);
        }
        isEmulatingFollowerMode = false;
    }

    public SpeedController[] getSpeedControllers(){
        return controllers;
    }

    public TatorSparkMax[] getSparkMaxes() {
        return controllers;
    }

    public TatorSparkMax getMaster() {
        return master;
    }
    @Override
    public void set(double speed) {
        speed*= inverted ? -1 : 1;
//        master.set(speed);
        for (TatorSparkMax controller : controllers) {
            controller.set(speed);
        }
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
//        master.disable();
        set(0);
    }

    @Override
    public void stopMotor() {
        disable();
    }

    @Override
    public void pidWrite(double output) {
        set(output);
    }

    @Override
    public double getVelocity() {
        return 0;
    }
}
