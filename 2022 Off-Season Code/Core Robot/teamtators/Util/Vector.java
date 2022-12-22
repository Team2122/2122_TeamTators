package org.teamtators.Util;

import main.Vector2D;

// Class for handleing all forms of vector math
public class Vector {

    // Data in this class is stored in cartesian coordinates
    double x;
    double y;

    // Nothing gets passed into constructor
    public Vector() {

    }

    public Vector subtract(Vector v){
        return new Vector(x-v.x,y-v.y);
    }

    public Vector(double x, double y) {
        setXY(x, y);
    }

    public Vector(Vector v1, Vector v2) {
        setX(v2.getX() - v1.getX());
        setY(v2.getY() - v1.getY());
    }

    public static Vector fromPolar(double theta, double r) {
        Vector vector = new Vector();
        vector.setPolar(theta, r);
        return vector;
    }

    // Setting x and Y through cartesian coordinates
    public Vector setXY(double x, double y) {
        this.x = x;
        this.y = y;
        return this;
    }


    public Vector setX(double x) {
        this.x = x;
        return this;
    }

    public Vector setY(double y) {
        this.y = y;
        return this;
    }

    public Vector set(Vector vector) {
        x = vector.getX();
        y = vector.getY();
        return this;
    }

    // Adds this vector to vector "that" in the cartesian space
    public Vector add(Vector that) {
        return addSelf(that);
//        Vector out = new Vector();
//        out.setXY(x+that.getX(),y+that.getY());
    }

    public Vector addSelf(Vector that) {
        this.setXY(x + that.getX(), y + that.getY());
        return this;
    }

    // Sets x and Y using in terms of theta and magnitude
    public Vector setPolar(double theta, double magnitude) {
        this.x = magnitude * Math.cos(theta);
        this.y = magnitude * Math.sin(theta);
        return this;
    }

    public Vector setTheta(double theta) {
        setPolar(theta, getMagnitude());
        return this;
    }

    public Vector addTheta(double theta) {
        setTheta(this.getTheta() + theta);
        return this;
    }

    public Vector setMagnitude(double magnitude) {
        double oldMag = getMagnitude();
        if (oldMag == 0) {
            return this;
        } else if (magnitude == 0) {
            x = 0;
            y = 0;
        }

        x *= (magnitude / oldMag);
        y *= (magnitude / oldMag);

        return this;
    }

    // Scales the vector using a double scalar
    public Vector scale(double scalar) {
        scaleX(scalar);
        scaleY(scalar);
        return this;
    }

    // Scales X
    public void scaleX(double scalar) {
        x *= scalar;
    }

    // Scales Y
    public void scaleY(double scalar) {
        y *= scalar;
    }

    // Gets X
    public double getX() {
        return x;
    }

    // Gets Y
    public double getY() {
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
        return Math.sqrt(a * a + b * b);
    }

    public double getDistance(Vector vector) {
        return pythagorean(x - vector.x, y - vector.y);
    }

    private double minusPiTo2Pi(double theta) {
        if (theta < 0)
            return theta + 2 * 3.1416;
        return theta;
    }

    public double dotProduct(Vector v){
        return x * v.getX() + y * v.getY();
    }

    public Vector project(Vector v){
        double dot = this.dotProduct(v);
        Vector out = v.clone();
        out.setMagnitude(dot/v.getMagnitude());
        return out;
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

    public static void main(String[] args) {
        Vector v = new Vector(1,17);
        Vector j = new Vector(2,6);
        System.out.println(v.project(j));
    }
}
