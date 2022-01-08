package org.teamtators.common.math;

public class HermiteSplineTime extends SplineTime {

    public HermiteSplineTime(Point[] points) {
        super(points);
    }

    /**
     * Matrix that looks like this--returns the spline in matrix form
     * ---------------SPLINE #0--------------
     * n, n, n, n, n, n, n, n, n, n, n, n, a1 <-- "getAEquation(0)"  **
     * n, n, n, n, n, n, n, n, n, n, n, n, b1 <-- "getSlopeEquation(0)" xx
     * n, n, n, n, n, n, n, n, n, n, n, n, c1 <-- "getSlopeEquation(1)"
     * n, n, n, n, n, n, n, n, n, n, n, n, d1 <-- "getSubEquation(0)"  **
     * ---------------SPLINE #1--------------
     * n, n, n, n, n, n, n, n, n, n, n, n, a2 <-- "getAEquation(1)"  **
     * n, n, n, n, n, n, n, n, n, n, n, n, b2 <-- "getSlopeEquation(2)"
     * n, n, n, n, n, n, n, n, n, n, n, n, c2 <-- "getSlopeEquation(3)"
     * n, n, n, n, n, n, n, n, n, n, n, n, d2 <-- "getSubEquation(1)"  **
     * ---------------SPLINE #2--------------
     * n, n, n, n, n, n, n, n, n, n, n, n, a3 <-- "getAEquation(2)"  **
     * n, n, n, n, n, n, n, n, n, n, n, n, b3 <-- "getSlopeEquation(4)"
     * n, n, n, n, n, n, n, n, n, n, n, n, c3 <-- "getSlopeEquation(5)"
     * n, n, n, n, n, n, n, n, n, n, n, n, d3 <-- "getSubEquation(2)"  **
     * @return a matrix that can be solved
     */
    @Override
    protected double[][] populateMatrix() {

        for(int i = 0; i < splines.size(); i++) {
            matrix[4 * i] = getAEquation(i);
            matrix[4 * i + 1] = getSlopeEquation(2 * i);
            matrix[4 * i + 2] = getSlopeEquation(2 * i + 1);
            matrix[4 * i + 3] = getSubEquation(i);
        }

        return matrix;
    }

    @Override
    protected double[] getSlopeEquation(int spline) {
        if(spline >= splines.size() * 2 + 2 || spline < 0) {
            throw new IllegalArgumentException("Invalid Spline in getSlopeEquation");
        } else {
            boolean startPoint = (spline % 2) == 0;
            int s = spline / 2;

            double[] arr = new double[splines.size() * 4 + 1];

            Coefficient[] coes1;

            coes1 = splines.get(s).getCoefficientsFromDerivationAndSubstitution(startPoint, true);
            populateArray(arr, coes1, s * 4, 4, false);

            if(startPoint) {
                arr[splines.size() * 4] = splines.get(s).getStartPoint().getSlope();
            } else {
                arr[splines.size() * 4] = splines.get(s).getEndPoint().getSlope();
            }

            return arr;
        }
    }
}

