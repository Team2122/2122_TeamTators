package org.teamtators.common.drive;

import org.teamtators.common.control.AbstractUpdatable;

/** Not used but I want to keep it around because it kindof works
 * @author Jacob
 */
public class DistanceEstimator extends AbstractUpdatable {
//    private Drive drive;
    private double currentDistance = 0;
//    public DistanceEstimator (Drive drive) {
//        this.drive = drive;
//    }
    private double getDeltaDistance (double deltaTime) {
        /*
        var difficulty = 3.5;
        var leftTank = scaleInput(leftAxis[1]) * difficulty;
        var rightTank = scaleInput(rightAxis[1]) * difficulty;
        var trackWidth = 26;
        var I = 1/2 * 120 * 15 * 15      * 0.3; // robot radius 15 inches
        var torque = (rightTank - leftTank) * trackWidth; // T = I * a
        var alpha = torque / I; // theta / s ^ 2
        var rotateRadius = trackWidth / (rightTank - leftTank) // in
        var movex = trackWidth * (rightTank + leftTank) * Math.sin(-angle); // Force
        var movey = trackWidth * (rightTank + leftTank) * Math.cos(-angle); // Force
        vx -= movex / 120;//F = ma
        vy -= movey / 120;
        anglespeed += alpha;//
         */
//        double left = drive.getLeftTransmissionRate();
//        double right = drive.getRightTransmissionRate();
//        double trackWidth = drive.getTankKinematics().getEffectiveTrackWidth();
//        return trackWidth * (left + right) * Math.sqrt(2);
        return 0;
    }
    public double getDistance () {
        return currentDistance;
    }
    @Override
    protected void doUpdate(double delta) {
        currentDistance += getDeltaDistance(delta);
    }
}
