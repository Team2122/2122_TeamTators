package org.teamtators.common.drive;

import org.teamtators.common.math.*;

import java.util.ArrayList;

/**
 * Creates a segment between two poitsn using spline
 */
public class SplineSegment extends DriveSegmentBase {
    private double startT;
    private double endT;
    private int splineSegmentID;
    private SplineTimeParametricBase spline;

    private boolean initializing = true;
    private double tolerance;
    private int numIntervals;
    private double prevNearestT;

    public void setSpline(SplineTimeParametricBase spline) {
        this.spline = spline;
    }

    public void setSplineSegmentID(int id) {
        splineSegmentID = id;
    }

    public void setTolerance(double tolerance) {
        this.tolerance = tolerance;
    }

    public void setNumIntervals(int intervals) {
        numIntervals = intervals;
    }

    public void setStartT(double t) {
        startT = t;
        if(initializing) {
            prevNearestT = t;
            initializing = false;
        }
    }

    public void setEndT(double t) {
        endT = t;
    }

    @Override
    protected Pose2d getNearestPoint(Translation2d point) {
        //the t places where the x value = point.getx
        ArrayList<Double> tx = spline.solveForTX(point.getX(), splineSegmentID);
        //the y places where the y value = point.gety
        ArrayList<Double> ty = spline.solveForTY(point.getY(), splineSegmentID);

        ArrayList<Double> closeTX = findClosestT(tx, prevNearestT);
        ArrayList<Double> closeTY = findClosestT(ty, prevNearestT);

        boolean xCloser = false;
        boolean interpolateTwice = false;
        double estimatedT = 0;

        if(closeTX.size() > 1 && closeTY.size() > 1) {
            //Uhh not supposed to happen...
        } else if(closeTX.size() > 1 && !closeTY.isEmpty()) {
            xCloser = true;
            interpolateTwice = true;
            estimatedT = closeTY.get(0);
        } else if(closeTY.size() > 1 && !closeTX.isEmpty()) {
            xCloser = false;
            interpolateTwice = true;
            estimatedT = closeTX.get(0);
        } else if(closeTX.isEmpty()) {
            estimatedT = findClosestT(spline.solveForTY(closeTY.get(0), splineSegmentID), point).get(0);
        } else if(closeTY.isEmpty()) {
            estimatedT = findClosestT(spline.solveForTX(closeTX.get(0), splineSegmentID), point).get(0);
        } else {
            xCloser = closeTX.get(0) >= closeTY.get(0);

            LinearFunction l = getBisector(closeTX.get(0), closeTY.get(0), point);

            estimatedT = findClosestT(spline.getIntercepts(l, splineSegmentID), point).get(0);
        }

        if(xCloser) {
            estimatedT = interpolateNearest(closeTX.get(0), estimatedT, point);

            if(interpolateTwice) {
                double otherEstimate = interpolateNearest(closeTX.get(1), estimatedT, point);

                Translation2d est1 = new Translation2d(spline.getPoint(estimatedT).getX(), spline.getPoint(estimatedT).getY());
                Translation2d est2 = new Translation2d(spline.getPoint(otherEstimate).getX(), spline.getPoint(otherEstimate).getY());

                if(est1.getDistance(point) > est2.getDistance(point)) {
                    estimatedT = otherEstimate;
                }
            }
        } else {
            interpolateNearest(closeTY.get(0), estimatedT, point);

            if(interpolateTwice) {
                double otherEstimate = interpolateNearest(closeTY.get(1), estimatedT, point);

                Translation2d est1 = new Translation2d(spline.getPoint(estimatedT).getX(), spline.getPoint(estimatedT).getY());
                Translation2d est2 = new Translation2d(spline.getPoint(otherEstimate).getX(), spline.getPoint(otherEstimate).getY());

                if(est1.getDistance(point) > est2.getDistance(point)) {
                    estimatedT = otherEstimate;
                }
            }
        }

        Translation2d trans = new Translation2d(spline.getPoint(estimatedT).getX(), spline.getPoint(estimatedT).getY());

        double angle = Math.atan(spline.getDerivative(estimatedT));
        Rotation yaw = Rotation.fromRadians(angle);

        return new Pose2d(trans, yaw);
    }

    @Override
    public Pose2d getStartPose() {
        Point p = spline.getPoint(startT);
        Translation2d trans = new Translation2d(p.getX(), p.getY());
        double angle = Math.atan(spline.getDerivative(startT));
        Rotation yaw = Rotation.fromRadians(angle);
        return new Pose2d(trans, yaw);
    }

    @Override
    public Pose2d getEndPose() {
        Point p = spline.getPoint(endT);
        Translation2d trans = new Translation2d(p.getX(), p.getY());
        double angle = Math.atan(spline.getDerivative(endT));
        Rotation yaw = Rotation.fromRadians(angle);
        return new Pose2d(trans, yaw);
    }

    @Override
    public double getArcLength() {
        return spline.getArcLength(startT, endT, numIntervals);
    }

    //TODO: find a better way
    @Override
    public Pose2d getLookAhead(Pose2d nearestPoint, double distance) {
        boolean overshot = false;
        ArrayList<Double> tx = spline.solveForTX(nearestPoint.getX(), splineSegmentID);
        ArrayList<Double> ty = spline.solveForTY(nearestPoint.getY(), splineSegmentID);

        double t = compare(tx, ty);

        prevNearestT = t;

        Translation2d point = nearestPoint.getTranslation();

        while (!overshot) {
            Point a = spline.getPoint(t);
            Translation2d aa = new Translation2d(a.getX(), a.getY());

            Point b = spline.getPoint(t + tolerance);
            Translation2d bb = new Translation2d(b.getX(), b.getY());

            if(point.getDistance(aa) > point.getDistance(bb)) {
                t += tolerance;
            } else {
                overshot = true;
            }
        }

        Translation2d trans = new Translation2d(spline.getPoint(t).getX(), spline.getPoint(t).getY());
        Rotation yaw = trans.sub(nearestPoint.getTranslation()).getDirection();

        /*
        double slope = (trans.getY() - nearestPoint.getY()) / (trans.getX() - nearestPoint.getX());
        double angle = Math.atan(slope);
        Rotation yaw = new Rotation(trans.getX())

        Point nearest = spline.getPoint(prevNearestT);
        double angle = Math.atan(spline.getDerivative(startT));
        return new Pose2d(new Translation2d(nearest.getX(), nearest.getY()), Rotation.fromRadians(angle));

        double t = spline.solveForTX(nearestPoint.getX());
        t = t + distance;
        Point p = spline.getPoint(t);
        double angle = Math.atan(spline.getDerivative(endT));
        Rotation yaw = Rotation.fromRadians(angle)
        */;
        Pose2d pose = new Pose2d(trans, yaw);
        return pose;
    }

    /*
    a + bt + ct2= + dt3 = x
    bt + cu + dv = x - a
    t2 - u  + 0v = 0
    t3 - 0u + v  = 0
     */
    @Override
    protected double getTraveledDistance(Translation2d point) {
        ArrayList<Double> tx = spline.solveForTX(point.getX(), splineSegmentID);
        ArrayList<Double> ty = spline.solveForTY(point.getY(), splineSegmentID);
        double t = compare(tx, ty);
        //double tt = findLargerClosestT(t, prevNearestT);
        System.out.println("getTraveledDIstance: ");
        return spline.getArcLength(startT, t, numIntervals);
    }

    @Override
    protected double getRemainingDistance(Translation2d point) {
        ArrayList<Double> tx = spline.solveForTX(point.getX(), splineSegmentID);
        ArrayList<Double> ty = spline.solveForTY(point.getY(), splineSegmentID);
        double t = compare(tx, ty);
        //double tt = findLargerClosestT(t, prevNearestT);
        System.out.println("GetRemainingDistance: ");
        return spline.getArcLength(t, endT, numIntervals);
    }

    private ArrayList<Double> findClosestT(ArrayList<Double> tees, double controlT) {
        ArrayList<Double> closests = new ArrayList<>();
        closests.add(tees.get(0));

        for(double t : tees) {
            double d1 = spline.measureDistance(closests.get(0), controlT);
            double d2 = spline.measureDistance(t, controlT);

            if((d1 - tolerance < d2) && (d1 + tolerance > d2)) {
                if(closests.size() == 1) {
                    closests.add(t);
                } else if(closests.size() == 2) {
                    closests.set(1, t);
                }
            } else if(d1 > d2) {
                closests.set(0, t);
                if(closests.size() > 1) {
                    closests.remove(1);
                }
            }
        }
        return closests;
    }

    private ArrayList<Double> findClosestT(ArrayList<Double> tees, Translation2d controlPoint) {
        ArrayList<Double>  closests = new ArrayList<>();
        closests.add(tees.get(0));

        Point p = new Point(controlPoint.getX(), controlPoint.getY());

        for(double t : tees) {
            double d1 = spline.findDistance(spline.getPoint(closests.get(0)), p);
            double d2 = spline.findDistance(spline.getPoint(t), p);

            if((d1 - tolerance < d2) && (d1 + tolerance > d2)) {
                if(closests.size() == 1) {
                    closests.add(t);
                } else if(closests.size() == 2) {
                    closests.set(1, t);
                }
            } else if(d1 > d2) {
                closests.set(0, t);
                if(closests.size() > 1) {
                    closests.remove(1);
                }
            }
        }
        return closests;
    }

    //TODO: do the tolerance incrementing stuff better
    private double interpolateNearest(double endT1, double endT2, Translation2d controlPoint) {

        if(endT1 > endT2) {
            double temp = endT1;
            endT1 = endT2;
            endT2 = temp;
        }

        double nearestT = endT1;
        double count = endT1;
        boolean decreasingDist = true;

        while(count <= endT2 && decreasingDist) {
            Translation2d a = new Translation2d(spline.getPoint(nearestT).getX(), spline.getPoint(nearestT).getY());

            Translation2d b = new Translation2d(spline.getPoint(count).getX(), spline.getPoint(count).getY());

            if(controlPoint.getDistance(a) < controlPoint.getDistance(b)) {
                decreasingDist = false;
            } else {
                nearestT = count;
            }
            count += tolerance;
        }

        return nearestT;
    }

    /**
     * Bisects the angle between the point at t = tx, vertex, and t = ty and gets the line that does that
     * @param tx
     * @param ty
     * @param vertex
     * @return the line that bisects the angle tx - vertex - ty
     */
    private LinearFunction getBisector(double tx, double ty, Translation2d vertex) {
        double x = spline.getPoint(tx).getY();
        double y = spline.getPoint(ty).getX();

        double slope = -(vertex.getX() - x) / (vertex.getY() - y);

        LinearFunction l = new LinearFunction();
        l.setPointSlopeForm(slope, vertex.getX(), vertex.getY());

        return new LinearFunction();
    }

    /**
     * Out of the t's in tees, find the closest t that is greater than controlT
     * @param tees
     * @param controlT
     * @return the closest t that's greater than controlT
     */
    double findLargerClosestT(ArrayList<Double> tees, double controlT) {
        double closest = tees.get(0);

        for(double t : tees) {
            double d1 = spline.measureDistance(closest, controlT);
            double d2 = spline.measureDistance(t, controlT);

            if(d1 > d2 && d2 > 0.0) {
                closest = t;
            }
        }
        return closest;
    }

    private double compare(ArrayList<Double> x, ArrayList<Double> y) {
        for(int i = 0; i < x.size(); i++) {
            for(int j = 0; j < y.size(); j++) {
                if(Epsilon.isEpsilonEqual(x.get(i), y.get(j))) {
                    return x.get(i);
                }
            }
        }
        throw new IllegalStateException("This is not a valid point.");
    }

}
