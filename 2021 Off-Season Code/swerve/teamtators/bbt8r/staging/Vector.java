package org.teamtators.bbt8r.staging;

// Class for handleing all forms of vector math
public class Vector {

    // Data in this class is stored in cartesian coordinates
    double x;
    double y;

    // Nothing gets passed into constructor
    public Vector(){

    }

    public Vector(double x, double y){
        setXY(x, y);
    }

    // Setting x and Y through cartesian coordinates
    public Vector setXY (double x, double y){
        this.x = x;
        this.y = y;
        return this;
    }

    // Adds this vector to vector "that" in the cartesian space
    public Vector add(Vector that) {
        return addSelf(that);
//        Vector out = new Vector();
//        out.setXY(x+that.getX(),y+that.getY());
    }

    public Vector addSelf (Vector that){
        this.setXY(x+that.getX(),y+that.getY());
        return this;
    }

    // Sets x and Y using in terms of theta and magnitude
    public Vector setPolar(double theta, double magnitude){
        this.x = magnitude*Math.cos(theta);
        this.y = magnitude*Math.sin(theta);
        return this;
    }

    public Vector setTheta(double theta){
        setPolar(theta,getMagnitude());
        return this;
    }

    public Vector addTheta(double theta){
        setTheta(this.getTheta() + theta);
        return this;
    }

    public Vector setMagnitude(double magnitude) {
        double oldMag = getMagnitude();
        x *= (magnitude / oldMag);
        y *= (magnitude / oldMag);
        return this;
    }

    // Scales the vector using a double scalar
    public Vector scale(double scalar){
        scaleX(scalar);
        scaleY(scalar);
        return this;
    }

    // Scales X
    public void scaleX(double scalar) {
        x*=scalar;
    }

    // Scales Y
    public void scaleY(double scalar) {
        y*=scalar;
    }

    // Gets X
    public double getX(){
        return x;
    }

    // Gets Y
    public double getY(){
        return y;
    }

    // Gets theta
    public double getTheta() {
        return minusPiTo2Pi(Math.atan2(y, x));
    }

    public double getATAN2() {
        return Math.atan2(y, x);
    }

    // Gets magnitude
    public double getMagnitude() {
        return pythagorean(x, y);
    }

    private double pythagorean(double a, double b) {
        return Math.sqrt(a*a + b*b);
    }

    private double minusPiTo2Pi (double theta){
        if(theta < 0)
            return theta + 2 * 3.1416;
        return theta;
    }

    public Vector clone() {
        return new Vector(getX(), getY());
    }

    @Override
    public String toString() {
        return "Vector{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }
}
