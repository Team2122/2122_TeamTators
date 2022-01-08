package org.teamtators.common.math;

/**
 * Represents a coefficient--the value is the "value" of the coefficient, and the "coefficient" is the number in front
 * of the coefficient
 */
public class Coefficient {
    private double value;
    private boolean isSet = false;
    private double coefficient = 1.0;

    public void setCoefficient(double coefficient) {
        this.coefficient = coefficient;
    }

    public void setValue(double value) {
        isSet = true;
        this.value = value;
    }

    public double getCoefficient() {
        return coefficient;
    }

    public double getValue() {
        if(isSet) {
            return value;
        } else {
            throw new IllegalStateException();
        }
    }

    public boolean isSet() {
        return isSet;
    }
}
