package org.teamtators.common.math;

/**
 * Represents a complex number z
 */
public class ComplexNumber extends Object {

    private double r;
    private double s;

    /**
     * Constructs the complex number z = r + i*s
     *
     * @param r Real part
     * @param s Imaginary part
     */
    public ComplexNumber(double r, double s) {
        this.r = r;
        this.s = s;
    }

    /**
     * @return the mod
     */
    public double mod() {
        return Math.sqrt(r*r + s*s);
    }

    public double getReal() {
        return r;
    }

    public double getImaginary() {
        return s;
    }

    /**
     * @return r - si
     */
    public ComplexNumber getMultiplicativeInverse() {
        return new ComplexNumber(r, -s);
    }

    /**
     * @param c complex number
     * @return z+c where z is this ComplexNumber number.
     */
    public ComplexNumber add(ComplexNumber c) {
        return new ComplexNumber(r + c.getReal(), s + c.getImaginary());
    }

    /**
     * @param c complex number
     * @return z+c where z is this ComplexNumber number.
     */
    public ComplexNumber add(double c) {
        return new ComplexNumber(r + c, s);
    }

    /**
     * @param c complext number
     * @return z - c
     */
    public ComplexNumber substract(ComplexNumber c) {
        return new ComplexNumber(r - c.getReal(), s - c.getImaginary());
    }

    /**
     * @param c complext number
     * @return z - c
     */
    public ComplexNumber substract(double c) {
        return new ComplexNumber(r - c, s);
    }


    /**
     * @param c
     * @return c*z
     */
    public ComplexNumber multiply(ComplexNumber c) {
        return new ComplexNumber(r * c.getReal() - s * c.getImaginary(), r * c.getImaginary() + s * c.getReal());
    }

    /**
     * @param c
     * @return c*z
     */
    public ComplexNumber multiply(double c) {
        return new ComplexNumber(r * c, s * c);
    }

    /**
     * @param c
     * @return z / c
     */
    public ComplexNumber div(ComplexNumber c) {
        ComplexNumber n = multiply(c.getMultiplicativeInverse());
        double denominator = (c.r * c.r) + (c.s * c.s);
        ComplexNumber m = new ComplexNumber(n.r / denominator, n.s / denominator);
        return m;
    }

    /**
     * @param c
     * @return z / c
     */
    public ComplexNumber div(double c) {
        ComplexNumber m = new ComplexNumber(r / c, s / c);
        return m;
    }

    /**
     * Use DeMoivre's formula
     * @return cube root of this complex number
     */
    public ComplexNumber cbrt() {
        double theta = Math.atan(s / r);
        double modr = Math.cbrt(mod());

        return new ComplexNumber(modr * Math.cos(theta / 3.0), modr * Math.sin(theta / 3.0));
    }

    public boolean isReal() {
        return Epsilon.isEpsilonZero(s);
    }

    public double toReal() {
        return s + r;
    }
}
