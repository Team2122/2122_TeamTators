package org.teamtators.common.math;

/**
 * Uses trapezoids to approximate the integral
 */
public final class TrapezoidalApproximation {

    public static double approximateArea(double start, double end, double intervals, Derivative derivative) {
        double intervalLength = (end - start) / intervals;

        double baseSums = derivative.derive(start) + derivative.derive(end);

        for(int i = 1; i < intervals; i++) {
            baseSums += derivative.derive(((i * intervalLength) + start) * 2.0);
        }

        return intervalLength * 0.5 * baseSums;
    }
}
