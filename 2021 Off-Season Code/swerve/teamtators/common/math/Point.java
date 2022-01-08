package org.teamtators.common.math;

/**
 * @author Abby Chen
 */
public class Point {
    private double x;
    private double y;
    private double slope; //1st derivative
    private double slopeSlope; //2nd derivative

    private boolean slopeSet = false;
    private boolean slopeSlopeSet = false;

    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Point() {
        this(0.0, 0.0);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public void setSlope(double s) {
        slope = s;
        slopeSet = true;
    }

    public double getSlope() {
        if(slopeSet) {
            return slope;
        } else {
            throw new IllegalStateException("Derivative not set yet");
        }
    }

    public void setSlopeSlope(double ss) {
        slopeSlope = ss;
        slopeSlopeSet = true;
    }

    public double getSlopeSlope() {
        if(slopeSlopeSet) {
            return slopeSlope;
        } else {
            throw new IllegalStateException("2nd derivative not set yet");
        }
    }

    public boolean isSlopeSet() {
        return slopeSet;
    }

    public boolean isSlopeSlopeSet() {
        return slopeSlopeSet;
    }
}
