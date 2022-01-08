package org.teamtators.common.drive;

import org.teamtators.common.math.Pose2d;

import java.util.ArrayList;
import java.util.List;

public class DriveSegments {
    private List<DriveSegment> segments = new ArrayList<>();
    private double len = Double.NaN;

    public List<DriveSegment> getSegments() {
        return segments;
    }

    public void addSegment(DriveSegment segment) {
        segments.add(segment);
    }

    public Pose2d getStartPose() {
        if (segments.size() == 0) {
            return null;
        }
        return segments.get(0).getStartPose();
    }

    public Pose2d getEndPose() {
        if (segments.size() == 0) {
            return null;
        }
        return segments.get(segments.size() - 1).getEndPose();
    }

    public double getArcLength() {
        return Double.isNaN(len) ? calcLength() : len;
    }

    public double calcLength() {
        len = segments.stream().mapToDouble(DriveSegment::getArcLength).sum();
        return len;
    }
}
