package org.teamtators.bbt8r.staging;

import org.teamtators.common.control.Timer;

public class ArcLengthTracker {

    private Timer timer;
    private double distance = 0;
    public ArcLengthTracker(){
        timer = new Timer();
        timer.start();
    }

    public void update(double magnitude, double time){
        distance = distance + magnitude*time;
    }

    public double getDistance(){
        return distance;
    }

    public void resetArcLength(){
        distance = 0;
    }

}
