package SplineGenerator.Follower;

import BSpline.BSplineH;
import BSpline.SplinePoint;
import SplineGenerator.GUI.DisplayGraphics;
import SplineGenerator.GUI.Displayable;
import SplineGenerator.GUI.SplineDisplay;
import SplineGenerator.Splines.PolynomicSpline;
import SplineGenerator.Splines.Spline;
import SplineGenerator.Util.DControlPoint;
import SplineGenerator.Util.DDirection;
import SplineGenerator.Util.DVector;
import SplineGenerator.Util.InterpolationInfo;
import main.LinearlyInterpLUT;
import main.Vector2D;

import java.awt.*;

public class GoodestFollower implements Follower, Displayable {
    Followable followable;
    protected double t = 0;
    protected  Vector2D pos = new Vector2D();
    protected double splineR = 0;
    protected double splineRes = 0;
    protected Waypoints waypoints;
    private BSplineH angleSpline;
    private LinearlyInterpLUT angleTLut;
    private Vector2D followerVel = new Vector2D();
    private double borrowedTime = 0;


    private double toVel = 0;
    private double forVel = 0;

    private Vector2D nearestPos = new Vector2D();
    public GoodestFollower(Followable followable, double splineR, double splineRes, Waypoints waypoints, RequiredFollowerPoints requiredFollowerPoints) {
        waypoints.addWaypoint(new Waypoint(followable.getNumPieces(), () -> {
        }, 0));
        this.followable = followable;
        this.splineR = splineR;
        this.splineRes = splineRes;
        this.waypoints = waypoints;

//        double minOffSum = 99999;
//        double tToVel = 0;
//        forVel = .01;
//        // Running maxToVel / toVelSearchRadius paths
//        for (double j = 0; j < 1/30.0; j += .01) {
//            double offSum = 0;
//            Vector2D pos = new Vector2D();
//            // Trying a new toVel, and resetting the class to run again
//            toVel = j;
//            boolean exit = false;
//            t = 0;
//            // Running a path
//            for (int k = 0; k < 1000 && !finished(); k++) {
//                pos.add(this.get(pos));
//                offSum += pos.clone().subtract(findPosOnSpline(pos)).getMagnitude();
//            }
//            // Updating the local best ratio so far
//            if (offSum < minOffSum) {
//                tToVel = toVel;
//                minOffSum = offSum;
//            }
//        }
//        // Updating the back to the spline velocity scaling to the ideal one found in the simulations
//        toVel = tToVel;
//        t = 0;
        toVel = 1/30.0;

        SplinePoint[] splinePoints =new SplinePoint[requiredFollowerPoints.getRequiredFollowerPoints().length];
        double[] arr = requiredFollowerPoints.getAnglesOrdered();
        double[] arr2 = requiredFollowerPoints.getTOrdered();
        double[] indexArr = new double[splinePoints.length];
        for (int i = 0; i < splinePoints.length; i++) {
            splinePoints[i] = new SplinePoint(new Vector2D(arr2[i],arr[i]), new Vector2D(1,0));
            indexArr[i] = i;
        }
        angleTLut = new LinearlyInterpLUT(arr2,indexArr);
        angleSpline = new BSplineH(.1,.1,splinePoints);
    }

    @Override
    public Vector2D get(Vector2D pos) {
        Vector2D oldFollowervel = followerVel.clone();
        this.pos = pos;
        Vector2D nearestPos = findPosOnSpline(pos);
        Vector2D out = nearestPos.subtract(pos).scale(30);
        this.nearestPos = out.clone().scale(1/30.0);
        out.add(followable.evaluateDerivative(t, 1).toVector2D());
        out.setMagnitude(waypoints.getSpeed(t));
        this.followerVel = out.clone();
        if(oldFollowervel.clone().subtract(followerVel.clone()).getMagnitude() > 1){
            this.t+=.2;
            System.out.println("t: " + t);
            nearestPos = findPosOnSpline(pos);
            out = nearestPos.subtract(pos).scale(30);
            this.nearestPos = out.clone().scale(1/30.0);
            out.add(followable.evaluateDerivative(t, 1).toVector2D());
            out.setMagnitude(waypoints.getSpeed(t));
            this.followerVel = out.clone();
        }
        return out;
    }

    public Vector2D goBack(Vector2D pos) {
        this.pos = pos;
        Vector2D nearestPos = findPosOnSpline(pos);
        Vector2D out = nearestPos.subtract(pos).scale(30);
        this.nearestPos = out.clone().scale(1/30.0);
        out.add(followable.evaluateDerivative(t, 1).toVector2D().scale(-1));
        out.setMagnitude(waypoints.getSpeed(t));
        this.followerVel = out.clone();

        return out;
    }
    /**
     * Finds the nearest t on the spline given a position
     **/
    public double findTOnSpline(Vector2D pos) {
        double distMin = 9e307;
        double tempT = 0;
        for (double j = 0; j < followable.getNumPieces()-.00001; j += splineRes){
            Vector2D temp = followable.get(j).toVector2D();
            if(temp.getDistance(pos) < distMin && (this.t - j) < .1 && (this.t - j) >= 0){
                distMin = temp.getDistance(pos);
                tempT = j;
            }
        }
        this.t = tempT;
        return this.t;
    }

    @Override
    public void reset(Vector2D pos) {
        this.t = 0;
        this.pos = pos;
    }

    public double getT() {
        return t;
    }

    @Override
    public Waypoints getWaypoints() {
        return waypoints;
    }

    /**
     * Finds the nearest position on the spline given a position
     **/
    public Vector2D findPosOnSpline(Vector2D pos) {
        Vector2D min = new Vector2D(9e307,9e307);
        double distMin = 9e307;
        double tempT = 0;
        for (double j = 0; j < followable.getNumPieces(); j += splineRes){
            Vector2D temp = followable.get(j).toVector2D();
            if(temp.getDistance(pos) < distMin && Math.abs(this.t - j) <= 0.1 ){
                distMin = temp.getDistance(pos);
                min = temp.clone();
                tempT = j;
            }
        }
        this.t = tempT;
        return min;
    }
    private double tSpinMap(double t){
        return angleTLut.get(t);
    }
    public double getSpin(Vector2D pos){
        double tempT = findTOnSpline(pos);
        return angleSpline.evaluatePos(tSpinMap(tempT)).getY();
    }

    public boolean finished() {
        return t >= followable.getNumPieces() - .001;
    }

    public static void main(String[] args) {
        PolynomicSpline spline = new PolynomicSpline(2);
        spline.setPolynomicOrder(5);
        byte i = 1;

        spline.addControlPoint(new DControlPoint(new DVector[]{new DVector(new double[]{0,0}), new DDirection(new double[]{0, -1}), new DDirection(new double[]{0.0, 0.0})}));
        spline.addControlPoint(new DControlPoint(new DVector[]{new DVector(new double[]{0, -1.2}), new DDirection(new double[]{-.1, -1})}));
        spline.addControlPoint(new DControlPoint(new DVector[]{new DVector(new double[]{-2.54,-1.2}), new DDirection(new double[]{1, 0}), new DDirection(new double[]{0.0, 0.0})}));


        spline.closed = false;
        InterpolationInfo c1 = new InterpolationInfo();
        c1.interpolationType = Spline.InterpolationType.Linked;
        c1.endBehavior = Spline.EndBehavior.Hermite;
        spline.interpolationTypes.add(c1);

        InterpolationInfo c2 = new InterpolationInfo();
        c2.interpolationType = Spline.InterpolationType.Linked;
        c2.endBehavior = Spline.EndBehavior.Hermite;
        spline.interpolationTypes.add(c2);

        InterpolationInfo c3 = new InterpolationInfo();
        c3.interpolationType = Spline.InterpolationType.Linked;
        c3.endBehavior = Spline.EndBehavior.None;
        spline.interpolationTypes.add(c3);

        InterpolationInfo c4 = new InterpolationInfo();
        c4.interpolationType = Spline.InterpolationType.Linked;
        c4.endBehavior = Spline.EndBehavior.None;
        spline.interpolationTypes.add(c4);
        spline.generate();
        spline.takeNextDerivative();


        Waypoints w = new Waypoints(new Waypoint(0, () -> {
        }, .4), new Waypoint(3, () -> {
        }, .2));
        RequiredFollowerPoint[] r = new RequiredFollowerPoint[]{new RequiredFollowerPoint(0,0),new RequiredFollowerPoint(3,0)};
        GoodestFollower goodestFollower = new GoodestFollower(spline,.1,.01,w,new RequiredFollowerPoints(.1,.0,r));
        Vector2D pos = new Vector2D(-1,2);
        SplineDisplay splineDisplay = new SplineDisplay(spline,0,1,1300,700);
        splineDisplay.displayables.add(goodestFollower);
        splineDisplay.display();
        while (true){
            if(!goodestFollower.finished()){
            pos.add(goodestFollower.get(pos).scale(.0001));
            System.out.println(pos);}
            splineDisplay.repaint();
        }
    }

    @Override
    public void display(DisplayGraphics graphics) {
        graphics.paintPoint(pos.toDVector(),0,1,new Color(255, 226, 255, 255));
        graphics.paintVector(pos.toDVector(),followerVel.toDVector(),new Color(255, 226, 0, 255));
        if(nearestPos.getMagnitude() > .05){
        graphics.paintVector(pos.toDVector(),nearestPos.toDVector(),new Color(0, 255, 210, 255));}
        Vector2D angle = new Vector2D();
        angle.setPolar(getSpin(pos),1);
        graphics.paintVector(pos.toDVector(),angle.toDVector(),new Color(255,255,255));
    }
}
