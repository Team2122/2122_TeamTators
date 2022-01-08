package org.teamtators.common.math;

/**
 * @author Avery Bainbridge
 */
public class Polynomial4 implements DoubleFunction {
    private double a;
    private double b;
    private double c;
    private double d;

    public Polynomial4() {
        this(0.0, 0.0, 0.0, 0.0);
    }

    public Polynomial4(double a, double b, double c, double d) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
    }

    public double getA() {
        return a;
    }

    public void setA(double a) {
        this.a = a;
    }

    public double getB() {
        return b;
    }

    public void setB(double b) {
        this.b = b;
    }

    public double getC() {
        return c;
    }

    public void setC(double c) {
        this.c = c;
    }

    public double getD() {
        return d;
    }

    public void setD(double d) {
        this.d = d;
    }

    public double calculate(double x) {
        return a * x * x * x + b * x * x + c * x + d;
    }
}
