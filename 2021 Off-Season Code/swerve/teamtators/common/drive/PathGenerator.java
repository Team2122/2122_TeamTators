package org.teamtators.common.drive;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.teamtators.common.config.ConfigLoader;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PathGenerator {
    private Map<String, DriveSegments> pathMap = new HashMap<>();
    public DriveSegments createPath(List<DrivePath.Point> path, double radius, double speed, double arcSpeed, boolean reverse) {
        DrivePath drivePath = new DrivePath();
        for (int i = 0; i < path.size(); i++) {
            DrivePath.Point point = path.get(i);
            boolean isLast = i == path.size() - 1;
            if (Double.isNaN(point.getRadius())) {
                point.setRadius(radius);
            }
            if (Double.isNaN(point.getSpeed())) {
                if (isLast) {
                    point.setSpeed(0);
                } else {
                    point.setSpeed(speed);
                }
            }
            if (Double.isNaN(point.getArcSpeed())) {
                if (isLast) {
                    point.setArcSpeed(0);
                } else {
                    point.setArcSpeed(arcSpeed);
                }
            }
            if (point.isReverse() == null) {
                point.setReverse(reverse);
            }
            drivePath.addPoint(point);
        }
        return drivePath.toSegments();
    }

    public void createBakedPath(String key, List<DrivePath.Point> path, double radius, double speed, double arcSpeed, boolean reverse) {
        pathMap.put(key, createPath(path, radius, speed, arcSpeed, reverse));
    }

    public DriveSegments getBakedPath(String key) {
        return pathMap.get(key);
    }



    public void initialize(Logger logger, ConfigLoader loader) {
        ObjectMapper m = new ObjectMapper();
        ObjectNode configNode = (ObjectNode) loader.load("BakedPaths.yaml");
        if(configNode != null)
        configNode.fields().forEachRemaining((entry -> {
            try {
                logger.info("Baking path {}", entry.getKey());
                BakedPath p = m.treeAsTokens(entry.getValue()).readValueAs(BakedPath.class);
                createBakedPath(entry.getKey(), p.path, p.radius, p.speed, p.arcSpeed, p.reverse);
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }));
    }

    private static class BakedPath {
        public double speed;
        public double arcSpeed;
        public double radius;
        public boolean reverse = false;
        public List<DrivePath.Point> path;
    }
}
