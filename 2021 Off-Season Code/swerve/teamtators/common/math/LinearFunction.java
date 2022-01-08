package org.teamtators.common.math;

public class LinearFunction implements DoubleFunction {
    public boolean isLegal = true;
    public double m;
    public double b;

    public void setPointSlopeForm(double b, double x, double y) {
        this.b = b;
        m = y - (b * x);
    }

    @Override
    public double calculate(double x) {
        return m * x + b;
    }
}
