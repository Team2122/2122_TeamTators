package org.teamtators.Util;

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
    private double currentState;

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

    public void setkP(double kP){
        this.kP = kP;
    }

    private void flush(){
        errorSum = 0;
    }

    public void setCurrentState(double currentState){
        timerSplit();
        this.currentState = currentState;
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

    public double getSetPoint() {
        return setPoint;
    }

    private void timerSplit(){
        timerDelta = timer.get();
        timer.restart();
    }

    public double getOutput(){
        double output  = (kP * error) + (errorSum * kI) + kD * ( (error - lastError) / timerDelta) + (kF * setPoint);
        output = (Double.isNaN( output) ? 0 : output);
        return output;
    }


    public void reset(){
        this.timerDelta = 0;
        this.error  = 0;
        this.currentState = 0;
        this.setPoint = 0;
        this.lastError = 0;
        this.timer.restart();
        this.errorSum = 0;
    }

    public double getError() {
        return error;
    }

    public double getErrorChange() {
        return error - lastError;
    }

    public double getErrorSum() {
        return errorSum;
    }

    public double getCurrentState(){
        return currentState;
    }

    public void debug(){
//        System.out.println("Error: " + getError() + " Setpoint: " + getSetPoint() + " Output: " + getOutput() + " Current State: " + getCurrentState());
    }

}