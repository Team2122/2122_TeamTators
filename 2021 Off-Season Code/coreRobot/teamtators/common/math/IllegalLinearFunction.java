package org.teamtators.common.math;

public class IllegalLinearFunction extends LinearFunction{
    public double x;

    public IllegalLinearFunction(double x) {
        this.x = x;
        this.isLegal = false;
    }

    @Override
    public double calculate(double y) {
        return x;
    }
}
