package org.teamtators.common.math;

/**
 * Makes two splineTimes to model a spline parametrically
 * t = 0 represents the beginning point, and t =  represents the end point
 */
public class SplineTimeParametric extends SplineTimeParametricBase {

    /**
     * 1. find the distances between each point
     * 2. Use the distances to make more points
     * 3. Put the new points into the splinetime
     * 4. Return points
     * @param points the points
     */
    public SplineTimeParametric(Point[] points) {
        initialize(points);

        x = new SplineTime(xPoints);
        y = new SplineTime(yPoints);
    }
}
