package org.teamtators.common.math;

public class SplineTimeParametricHermitic extends SplineTimeParametricBase {
    private int numPoints;

    public SplineTimeParametricHermitic(Point[] points, Translation2d[] translations) {
        initialize(points);

        numPoints = points.length;

        if(translations.length != numPoints) {
            throw new IllegalArgumentException("The number of translations needs to be equal to the number of points provided.");
        }

        setPointSlopes(translations);

        x = new HermiteSplineTime(xPoints);
        y = new HermiteSplineTime(yPoints);
    }

    private void setPointSlopes(Translation2d[] translations) {
        for(int i = 0; i < numPoints - 1; i++) {
            Point xp1 = xPoints[i];
            Point xp2 = xPoints[i + 1];

            double dtdx = (xp1.getY() - xp1.getX()) / (xp2.getY() - xp2.getX());

            Point yp1 = yPoints[i];
            Point yp2 = yPoints[i + 1];

            double dtdy = (yp1.getY() - yp1.getX()) / (yp2.getY() - yp1.getX());

            xPoints[i].setSlope(dtdx * translations[i].getX());
            yPoints[i].setSlope(dtdy * translations[i].getY());

            if(i == numPoints - 2) {
                xPoints[i + 1].setSlope(dtdx * translations[i + 1].getX());
                yPoints[i + 1].setSlope(dtdy * translations[i + 1].getY());

            }
        }
    }
}

