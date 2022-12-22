package org.teamtators.Util;

import com.revrobotics.CANSparkMax;

public class SparkMaxControllerGroup implements MotorControllerGroup{
    private CANSparkMax[] controllers;
    private CANSparkMax master;
    private double currentOutput;
    private boolean inverted;

    private boolean isEmulatingFollowerMode = true;

    public SparkMaxControllerGroup(CANSparkMax...canSparkMaxs) {
        master = canSparkMaxs[0];
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
        for(CANSparkMax controller : controllers){
            controller.follow(CANSparkMax.ExternalFollower.kFollowerDisabled, 0);
        }
        isEmulatingFollowerMode = false;
    }

    public CANSparkMax[] getSpeedControllers(){
        return controllers;
    }

    public CANSparkMax[] getSparkMaxes() {
        return controllers;
    }

    public CANSparkMax getMaster() {
        return master;
    }
    @Override
    public void set(double speed) {
        speed*= inverted ? -1 : 1;
//        master.set(speed);
        for (CANSparkMax controller : controllers) {
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
    public double getVelocity() {
        return 0;
    }
}
