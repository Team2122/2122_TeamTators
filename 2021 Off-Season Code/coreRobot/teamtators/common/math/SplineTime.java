package org.teamtators.common.math;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Abby Chen
 * Represents a group of splines to form a path
 */
public class SplineTime {

    protected List<Spline> splines = new ArrayList<Spline>();
    protected double[][] matrix;
    private Point[] points;

    public SplineTime(Point[] points) {
        int numSplines = points.length - 1;

        Point[] ps = points;

        if(numSplines < 2) {
            throw new IllegalStateException("There need to be at least 3 points inputted");
        }

        //Puts points in order
        for(int i = 0; i < ps.length; i++){
            Point p = ps[i];
            for(int j = i; j < ps.length; j++) {
                if(p.getX() > ps[j].getX()) {
                    Point temp = ps[i];
                    ps[i] = ps[j];
                    ps[j] = temp;
                }
            }
            System.out.println(ps[i].getX());
        }

        this.points = ps;

        for (int i = 0; i < numSplines; i++) {
            splines.add(new Spline(ps[i], ps[i + 1]));
        }

        matrix = new double[numSplines * 4][numSplines * 4 + 1];

        solve();
    }

    /**
     * Use this to plug in values
     * @param x the x value
     * @return the y value
     */
    public double getY(double x) {
        if(x < points[0].getX() || x > points[points.length - 1].getX()) {
            throw new IllegalArgumentException("The x value isn't within bounds");
        }

        boolean found = false;
        int i = 0;
        while(!found && i < splines.size()) {
            if(points[i + 1].getX() > x) {
                found = true;
            } else {
                i++;
            }
        }

        if(found) {
            Spline s = splines.get(i);
            double m = (x - s.getStartPoint().getX());
            double a = s.getAValue();
            double b = s.getBValue() * m;
            double c = s.getCValue() * m * m;
            double d = s.getDValue() * m * m * m;
            return a + b + c + d;
        } else {
            throw new IllegalArgumentException("The x value isn't within bounds");
        }
    }

    public Spline getSegment(int id) {
        return splines.get(id);
    }

    public double getDerivative(double x) {
        if(x < points[0].getX() || x > points[points.length - 1].getX()) {
            throw new IllegalArgumentException("The x value isn't within bounds");
        }

        boolean found = false;
        int i = 0;
        while(!found && i < splines.size()) {
            if(points[i + 1].getX() > x) {
                found = true;
            } else {
                i++;
            }
        }

        if(found) {
            Spline s = splines.get(i);
            return s.derive(x);
        } else {
            throw new IllegalArgumentException("The x value isn't within bounds");
        }
    }

    /**
     * Matrix that looks like this--returns the spline in matrix form
     * ---------------SPLINE #0--------------
     * n, n, n, n, n, n, n, n, n, n, n, n, a1 <-- "getAEquation(0)"
     * n, n, n, n, n, n, n, n, n, n, n, n, b1 <-- "getCurvatureEquation(0)"
     * n, n, n, n, n, n, n, n, n, n, n, n, c1 <-- "getSubEquation(0)"
     *                                                                      LOOP 0 - Start Looping below stuff
     * n, n, n, n, n, n, n, n, n, n, n, n, d1 <-- "getSlopeEquation(1)"
     * ---------------SPLINE #1--------------
     * n, n, n, n, n, n, n, n, n, n, n, n, a2 <-- "getAEquation(1)"
     * n, n, n, n, n, n, n, n, n, n, n, n, b2 <-- "getCurvatureEquation(1)"
     * n, n, n, n, n, n, n, n, n, n, n, n, c2 <-- "getSubEquation(1)"
     *                                                                      LOOP 1
     * n, n, n, n, n, n, n, n, n, n, n, n, d2 <-- "getSlopeEquation(2)"
     * ---------------SPLINE #2--------------
     * n, n, n, n, n, n, n, n, n, n, n, n, a3 <-- "getAEquation(2)"
     * n, n, n, n, n, n, n, n, n, n, n, n, b3 <-- "getCurvatureEquation(2)"
     * n, n, n, n, n, n, n, n, n, n, n, n, c3 <-- "getSubEquation(2)"
     *                                                                      END LOOP
     * n, n, n, n, n, n, n, n, n, n, n, n, d3 <-- "getCurvatureEquation(3)"
     * @return a matrix that can be solved
     */
    protected double[][] populateMatrix() {

        matrix[0] = getAEquation(0);
        matrix[1] = getCurvatureEquation(0);
        matrix[2] = getSubEquation(0);

        for(int i = 1; i < splines.size(); i++) {
            matrix[4 * i - 1] = getSlopeEquation(i);
            matrix[4 * i] = getAEquation(i);
            matrix[4 * i + 1] = getCurvatureEquation(i);
            matrix[4 * i + 2] = getSubEquation(i);
        }

        matrix[splines.size() * 4 - 1] = getCurvatureEquation(splines.size());

        return matrix;
    }

    /**
     * @param spline the spline to get the "a" equation from
     * @return an array that represents an equation that directly equals "A"
     * e.g. [1, 0, 0, 0, 0, 0, 0, 0, 3] if A = 3, and the spline in question is the first one
     */
    protected double[] getAEquation(int spline) {
        if(spline >= splines.size()) {
            throw new IllegalArgumentException("Invalid spline");
        } else {

            double[] arr = new double[splines.size() * 4 + 1];

            arr[4 * spline] = 1.0;
            arr[splines.size() * 4] = splines.get(spline).getAValue();
            return arr;
        }
    }

    /**
     * @param spline the spline
     * @return the curvature equation of the spline in question and it's starting point
     * If the spline is the first one, then it should equal 0, if not, we need to take into account the spline before it
     */
    private double[] getCurvatureEquation(int spline) {
        if(spline > splines.size() || spline < 0) {
            throw new IllegalArgumentException("Invalid Spline in getCurvatureEquation");
        } else {
            double[] arr = new double[splines.size() * 4 + 1];

            Coefficient[] coes1;
            Coefficient[] coes2;

            if(spline < splines.size()) {
                coes1 = splines.get(spline).getCoefficientsFromDerivationAndSubstitution(true, false);
                populateArray(arr, coes1, spline * 4, 4, false);
            }

            if (spline > 0) {
                coes2 = splines.get(spline - 1).getCoefficientsFromDerivationAndSubstitution(false, false);
                populateArray(arr, coes2, (spline - 1) * 4, 4, true);
            }

            return arr;
        }
    }

    /**
     * @param spline the spline
     * @return the slope equation of the spline in question and it's starting point
     * If the spline is the first one, then it should equal 0, if not, we need to take into account the spline before it
     */
    protected double[] getSlopeEquation(int spline) {
        if(spline >= splines.size() || spline <= 0) {
            throw new IllegalArgumentException("Invalid Spline in getSlopeEquation");
        } else {
            double[] arr = new double[splines.size() * 4 + 1];

            Coefficient[] coes1;
            Coefficient[] coes2;

            coes1 = splines.get(spline).getCoefficientsFromDerivationAndSubstitution(true, true);
            populateArray(arr, coes1, spline * 4, 4, false);

            coes2 = splines.get(spline - 1).getCoefficientsFromDerivationAndSubstitution(false, true);
            populateArray(arr, coes2, (spline - 1) * 4, 4, true);

            return arr;
        }
    }

    /**
     * 
     * @param spline the spline
     * @return the equation that results from substituting the point in front of the specified spline into the spline equation
     */
    protected double[] getSubEquation(int spline) {
        if(spline < 0 || spline >= splines.size()) {
            throw new IllegalArgumentException("Invalid spline in getSubEquation");
        } else {
            double[] arr = new double[splines.size() * 4 + 1];

            Coefficient[] coes = splines.get(spline).substituteEndpointWithoutDeriving();

            populateArray(arr, coes,spline * 4, 4, false);

            arr[splines.size() * 4] = splines.get(spline).getEndPoint().getY();

            return arr;
        }
    }

    /**
     * Populates an array with another array of coefficients
     * @param array the array that's going to be populated
     * @param coefficients the populator
     * @param startPoint the index to start populating at
     * @param numAdditions the number of values transferred
     * @param invert whether or not to put in the negative version of the numbers
     */
    protected void populateArray(double[] array, Coefficient[] coefficients, int startPoint, int numAdditions, boolean invert) {
        for(int i = 0; i < numAdditions; i++) {
            double c = coefficients[i].getCoefficient();

            if(invert) {
                c = -c;
            }

            array[startPoint + i] =  c;
        }
    }

    /**
     * Populates the matrix, and then solves it, and then puts the solutions into the splines
     */
    private void solve() {
        Matrix m = new Matrix(populateMatrix());
        m.solve();
        double[] solutions = m.getSolutions();
        for(int i = 0; i < solutions.length; i+=4) {
            splines.get(i / 4).setAValue(solutions[i]);
            splines.get(i / 4).setBValue(solutions[i+1]);
            splines.get(i / 4).setCValue(solutions[i+2]);
            splines.get(i / 4).setDValue(solutions[i+3]);
        }
    }

}
