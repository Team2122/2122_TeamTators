package org.teamtators.common.drive;

import org.teamtators.common.math.Rotation;
import org.teamtators.common.math.SplineTimeParametricBase;
import org.teamtators.common.math.SplineTimeParametricHermitic;
import org.teamtators.common.math.Translation2d;

import java.util.Collection;
import java.util.List;

public class DrivePathSpline extends DrivePath {
    private Translation2d[] translations;
    private double splineTolerance;
    private double intervalLength;

    public DrivePathSpline(Collection<Point> points, List<Rotation> rotations, double tolerance, double intervalLength) {
        super(points);
        translations = new Translation2d[points.size()];

        this.intervalLength = intervalLength;

        splineTolerance = tolerance;

        for (int i = 0; i < points.size(); i++) {
            translations[i] = rotations.get(i).toTranslation(1.0);
        }
    }


    @Override
    public DriveSegments toSegments() {
        double startSpeed = 0.0, lastSpeed = 0.0;
        int numPoints = points.size();

        if(numPoints != translations.length) {
            throw new IllegalStateException("Number of rotations not equal number of points provided.");
        }

        DriveSegments segments = new DriveSegments();

        org.teamtators.common.math.Point[] pts = new org.teamtators.common.math.Point[numPoints];
        for(int i = 0; i < numPoints; i++) {
            pts[i] = new org.teamtators.common.math.Point(points.get(i).getX(), points.get(i).getY());
        }

        SplineTimeParametricBase spline = new SplineTimeParametricHermitic(pts, translations);

        for(int i = 0; i < numPoints - 1; i++) {
            Point point1 = points.get(i);
            Point point2 = points.get(i + 1);

            SplineSegment seg = new SplineSegment();
            seg.setSpline(spline);
            seg.setTolerance(splineTolerance);
            seg.setNumIntervals((int)((spline.getStartT(i) - spline.getEndT(i)) / intervalLength));
            seg.setStartT(spline.getStartT(i));
            seg.setEndT(spline.getEndT(i));

            seg.setStartSpeed(startSpeed);
            seg.setTravelSpeed(point1.getSpeed());

            if(i < numPoints - 2) {
                seg.setEndSpeed(point2.getSpeed());
                startSpeed = point2.getSpeed();
            } else {
                seg.setEndSpeed(lastSpeed);
            }

            if(i >= numPoints - 2) {
                seg.setEndSpeed(0.0);
            }
            seg.setEndSpeed(point2.getSpeed());

            segments.addSegment(seg);
        }

        return segments;
    }

}
