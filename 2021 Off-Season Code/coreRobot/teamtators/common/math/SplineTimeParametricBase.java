package org.teamtators.common.math;

import java.util.ArrayList;

public abstract class SplineTimeParametricBase {
    protected SplineTime x;
    protected SplineTime y;

    protected Point[] xPoints;
    protected Point[] yPoints;

    protected double[] controlTs;

    public double endPoint;

    /**
     * @param t, where t = 0 represents the beginning of the spline, and t = endPoint represents the end
     * @return
     */
    public Point getPointRaw(double t) {
        Point p = new Point(x.getY(t), y.getY(t));
        return p;
    }

    /**
     * Standardization of the spline
     * @param t, where t = 0 represents the beginning of the spline, and t = 1 represents the end
     * @return the point
     */
    public Point getPoint(double t) {
        double tt = t * endPoint;
        Point p = new Point(x.getY(tt), y.getY(tt));
        return p;
    }

    /**
     * Uses Cardano's method
     * @param x
     * @param id
     * @return t given x
     */
    public ArrayList<Double> solveForTX(double x, int id) {
        Spline s = this.x.getSegment(id);
        double[] coefficients = s.getCoefficientsWithoutH();

        return filterSolutions(solveCubic(coefficients[0], coefficients[1], coefficients[2], coefficients[3], x), s);
    }

    public ArrayList<Double> solveForTY(double y, int id) {
        Spline s = this.y.getSegment(id);
        double[] coefficients = s.getCoefficientsWithoutH();

        return filterSolutions(solveCubic(coefficients[0], coefficients[1], coefficients[2], coefficients[3], y), s);
    }

    /**
     * Arc length from tStart to tEnd using trapezoidal approximation to do take the integral of sqrt(dx^2 + dy^2)
     * @param tStart
     * @param tEnd
     * @param numIntervals the number of trapezoids you wanna use to approximate
     * @return arclength
     */
    public double getArcLength(double tStart, double tEnd, double numIntervals) {
        Derivative d = (double x) ->  Math.sqrt(1 + (getDerivative(x) * getDerivative(x)));

        return TrapezoidalApproximation.approximateArea(tStart, tEnd, numIntervals, d);
    }

    /**
     * Finds intercepts between the line and the spline at the specified id
     * @param line
     * @param id
     * @return t value at intercepts
     */
    public ArrayList<Double> getIntercepts(LinearFunction line, int id) {
        ArrayList<Double> intercepts = new ArrayList<>();

        Spline xs = x.getSegment(id);
        Spline ys = y.getSegment(id);

        double[] xc = xs.getCoefficientsWithoutH();
        double[] yc = ys.getCoefficientsWithoutH();

        double a = xc[0];
        double b = xc[1];
        double c = xc[2];
        double d = xc[3];

        double e = yc[0];
        double f = yc[1];
        double g = yc[2];
        double h = yc[3];

        double m = line.m;
        double bbb = line.b;

        double aa = e - (m * a);
        double bb = f - (m * b);
        double cc = g - (m * c);
        double dd = h - (m * d) - bbb;

        return filterSolutions(solveCubic(aa, bb, cc, dd, 0.0), xs);
    }

    public double getDerivative(double t) {
        return y.getDerivative(t) / x.getDerivative(t);
    }

    public double measureDistance(double t1, double t2) {
        return findDistance(getPoint(t1), getPoint(t2));
    }

    public double findDistance(Point p1, Point p2) {
        double xDiff = Math.pow(p1.getX() - p2.getX(), 2);
        double yDiff = Math.pow(p1.getY() - p2.getY(), 2);

        return Math.sqrt(xDiff + yDiff);
    }

    public double getStartT(int id) {
        if(id < 0 || id > xPoints.length - 1) {
            throw new IllegalArgumentException("ID is out of bounds");
        }
        return controlTs[id] / endPoint;
    }

    public double getEndT(int id) {
        if(id < 0 || id > xPoints.length - 1) {
            throw new IllegalArgumentException("ID is out of bounds");
        }
        return controlTs[id + 1] / endPoint;
    }

    private boolean betweenCriticalPoints(double[] minCritT, double[] maxCritT, LinearFunction line) {
        for(double t : maxCritT) {
            if (line.calculate(getPoint(t).getX()) > getPoint(t).getX()) {
                return false;
            }
        }

        for(double t : minCritT) {
            if(line.calculate(getPoint(t).getX()) < getPoint(t).getY()) {
                return false;
            }
        }

        return true;
    }

    private boolean isInBetween(double a, double b, double val) {
        return (a < val && b > val) || (a > val && b < val);
    }

    /**
     * Solves for x in ax^3 + bx^2 + cx + d = val
     * @param a
     * @param b
     * @param c
     * @param d
     * @param val
     * @return x
     */
    private ArrayList<Double> solveCubic(double a, double b, double c, double d, double val) {
        Cubic cubic = new Cubic(a, b, c, d - val);
        return cubic.solve();
    }

    private ArrayList<Double> filterSolutions(ArrayList<Double> plausibleSolutions, Spline s) {
        ArrayList<Double> solutions = new ArrayList<>();

        for(double sol : plausibleSolutions) {
            if (sol > s.getStartPoint().getX() && sol < s.getEndPoint().getX()) {
                solutions.add(sol);
            }
        }

        return solutions;
    }

    protected void initialize(Point[] points) {
        xPoints = new Point[points.length];
        yPoints = new Point[points.length];

        controlTs = new double[points.length];

        double totalDistance = 0.0;

        Point pointX = new Point(totalDistance, points[0].getX());
        Point pointY = new Point(totalDistance, points[0].getY());

        xPoints[0] = pointX;
        yPoints[0] = pointY;

        controlTs[0] = 0.0;

        for(int i = 1; i < points.length; i++) {
            double d = findDistance(points[i - 1], points[i]);

            totalDistance += d;

            Point px = new Point(totalDistance, points[i].getX());
            Point py = new Point(totalDistance, points[i].getY());

            controlTs[i] = totalDistance;

            xPoints[i] = px;
            yPoints[i] = py;
        }

        endPoint = totalDistance;
    }
}
