package org.teamtators.common.drive;

import edu.wpi.first.wpilibj.trajectory.Trajectory;
import org.teamtators.common.math.Translation2d;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class  TrajectoryParameterizer {
    private static final double MAX_UPDATE_ITERS = 10;
    private List<ParameterPoint> stamps; // < distance, time >
    private List<Translation2d> points; // states but only the points
    private List<Double> times; // states but collect the times
    private double completeDistance; // total distance of the trajectory
    private int currentDistanceIndex; // closest distance >= current distance

    public static TrajectoryParameterizer parameterize(Trajectory trajectory) {
        var ptzer = new TrajectoryParameterizer(trajectory);
        return ptzer;
    }

    private TrajectoryParameterizer(Trajectory trajectory) {
        var states = trajectory.getStates();
        stamps = new ArrayList<>(states.size());
        currentDistanceIndex = 1;
        points = states
                .stream()
                .map(state ->
                        Translation2d.fromWpiLibTranslation(
                                state.poseMeters.getTranslation()
                        )
                )
                .collect(Collectors.toList());
        times = states
                .stream()
                .map(state -> state.timeSeconds)
                .collect(Collectors.toList());
        parameterize();
    }

    public void reset() {
        currentDistanceIndex = 1;
    }

    public void updateDistance(double currentDistance) {
        for(int i = 0; i<MAX_UPDATE_ITERS; i++) {
            if (currentDistance > getNextDistance())
                currentDistanceIndex ++;
            else break;
        }
    }

    // get data
    private ParameterPoint getNextStamp() {
        return stamps.get(currentDistanceIndex);
    }
    private ParameterPoint getPreviousStamp() {
        return stamps.get(currentDistanceIndex - 1);
    }
    private double getNextDistance() {
        return getNextStamp().getDistance();
    }
    private double getPreviousDistance() {
        return getPreviousStamp().getDistance();
    }
    private double getNextTime() {
        return getNextStamp().getTime();
    }
    private double getPreviousTime() {
        return getPreviousStamp().getTime();
    }
    private Translation2d getNextPoint() {
        return points.get(currentDistanceIndex);
    }
    private Translation2d getPreviousPoint() {
        return points.get(currentDistanceIndex - 1);
    }

    public Translation2d getEstimatedGoalPoint (double currentDistance) {
        return getPreviousPoint().interpolate(getNextPoint(), unmix(currentDistance, getPreviousDistance(), getNextDistance()));
    }

    public double getEstimatedTime (double currentDistance) {
        return mix(getPreviousTime(), getNextTime(), unmix(currentDistance, getPreviousDistance(), getNextDistance()));
    }

    private void parameterize() {
        double integratedDistance = 0;
        stamps.add(new ParameterPoint(0, 0)); // the first state is always at a distance of 0
        for (int i = 1; i < points.size(); i++) {
            Translation2d currentPoint = points.get(i);
            integratedDistance += currentPoint.getDistance(points.get(i-1));
            stamps.add(new ParameterPoint(integratedDistance, times.get(i)));
        }
        completeDistance = integratedDistance;
    }

    public double getCompleteDistance() {
        return completeDistance;
    }

    private static class ParameterPoint {
        private double distance, time;
        public ParameterPoint(double distance, double time) {
            this.distance = distance;
            this.time = time;
        }

        public double getDistance() {
            return distance;
        }

        public double getTime() {
            return time;
        }
    }

    private static double mix(double a, double b, double t) {
        //0 <= t <= 1
        return a + (b-a) * t;
    }
    private static double unmix(double v, double a, double b) {
        //returns a value between 0 and 1: t, given that a!=b
        return (v-a)/(b-a);
    }
}
