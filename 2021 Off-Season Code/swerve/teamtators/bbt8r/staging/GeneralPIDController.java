package org.teamtators.bbt8r.staging;

import org.teamtators.common.control.Timer;

public class GeneralPIDController{

    private double output;
    private double timerDelta;
    private double error;
    private double errorSum;
    private Timer timer;
    private double kP;
    private double kI;
    private double kD;
    private double kF;
    private double iZone;
    private double setPoint;
    private double lastError;

    public GeneralPIDController(){
        timer = new Timer();
        timer.start();
    }

    public GeneralPIDController(double kP, double kI, double kD, double kF, double iZone){
        this();
        setPIDF(kP, kI, kD, kF, iZone);
    }

    public void setPIDF(double kP, double kI, double kD, double kF, double iZone){
        this.kP = kP;
        this.kI = kI;
        this.kD = kD;
        this.kF = kF;
    }

    private void flush(){
        errorSum = 0;
    }

    public void setCurrentState(double currentState){
        timerSplit();
        lastError = error;
        error = setPoint - currentState;
        errorSum = errorSum > iZone ? error * timerDelta : errorSum + error * timerDelta;
    }

    public void setSetPoint(double setPoint){
        if(this.setPoint != setPoint){
            flush();
        }
        this.setPoint = setPoint;
    }

    private void timerSplit(){
        timerDelta = timer.get();
        timer.restart();
    }

    public double getOutput(){
        return (kP * error) + (errorSum * kI) + ( (error - lastError) * kD/ timerDelta) + (kF * setPoint);
    }

}