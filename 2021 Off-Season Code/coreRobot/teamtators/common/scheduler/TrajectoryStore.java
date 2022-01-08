package org.teamtators.common.scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.wpi.first.wpilibj.kinematics.DifferentialDriveKinematics;
import edu.wpi.first.wpilibj.trajectory.Trajectory;
import edu.wpi.first.wpilibj.trajectory.TrajectoryConfig;
import edu.wpi.first.wpilibj.trajectory.TrajectoryGenerator;
import edu.wpi.first.wpilibj.trajectory.constraint.DifferentialDriveKinematicsConstraint;
import edu.wpi.first.wpilibj.util.Units;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamtators.common.TatorRobotBase;
import org.teamtators.common.config.ConfigLoader;
import org.teamtators.common.control.Timer;
import org.teamtators.common.drive.TrajectoryParameterizer;
import org.teamtators.common.drive.TrajectoryPath;
import org.teamtators.common.math.Pose2d;
import org.teamtators.common.math.Rotation;
import org.teamtators.common.math.Translation2d;

import java.util.*;

public class TrajectoryStore {
    protected final Logger logger = LoggerFactory.getLogger(TrajectoryStore.class);
    private Map<String, TrajectoryPath> paths = new HashMap<>();
    private ConfigLoader loader;
    private DifferentialDriveKinematics kinematics;
    private boolean encounteredError;

    public Map<String, TrajectoryPath> getPaths() {
        return paths;
    }

    public TrajectoryStore(ConfigLoader cfgLoader, DifferentialDriveKinematics kinematics) {
        this.loader = cfgLoader;
        this.kinematics = kinematics;
    }

    public void putPath(String name, TrajectoryPath path) {
        path.setName(name);
        paths.put(name, path);
    }

    public TrajectoryPath getPath(String name) {
        TrajectoryPath path = paths.get(name);
        if (path == null)
            throw new IllegalArgumentException("No trajectory with name \"" + name + "\" created.");
        return path;
    }

    public void clearPaths() {
        paths.clear();
    }
    private void reportError () {
        encounteredError = true;
    }
    private void loadPathsFromConfig(ObjectNode cfg) {
        logger.debug("Start loading trajectories");

         TrajectoryGenerator.setErrorHandler((__, ___) -> reportError());

        try {
            logger.info("Getting Fields");
            var fields = cfg.fields();
            while (fields.hasNext()) {
                logger.debug("Getting Next Path");
                var field = fields.next();
                String name = field.getKey();
                logger.debug("Current PathName: " + name);
                logger.debug("Reflection Starting");
                var path = TatorRobotBase.configMapper.treeToValue(field.getValue(), TrajectoryPath.class);
                logger.info("Reflection Complete");
                Trajectory trajectory = path.isCubic ? generateCubicTrajectory(path) : generateQuinticTrajectory(path);
                logger.info("Generation Complete");

//                double startTime = Timer.getTimestamp();
//                logger.info("Beginning Wait");
//                while (true) {
//                    if (Timer.getTimestamp() - startTime >= 5) {
//                        break;
//                    }
//                }

                logger.info("Ending Wait");
                if (encounteredError) {
                    logger.error("Bad trajectory, not generated, named " + name);
                    encounteredError = false;
                    continue;
                }
                logger.info("Registering Path");
                path.setTrajectory(trajectory);
                path.setParameterizer(TrajectoryParameterizer.parameterize(path.getTrajectory()));
                this.putPath(name, path);
            }
        } catch (JsonProcessingException e) {
            logger.error("Exception thrown while configuring TrajectoryPath YAML!", e);
        }

        logger.info("Finished Loading Trajectories");
    }

    public void loadPathsFromConfig(String fileName) {
         loadPathsFromConfig((ObjectNode) loader.load(fileName));
    }

    private TrajectoryConfig generateTrajectoryConfig(TrajectoryPath path) {
        var tconfig = new TrajectoryConfig(
                Units.inchesToMeters(path.getMaxVelocity()),
                Units.inchesToMeters(path.getMaxAcceleration())
        );
        tconfig.addConstraint(//just to make sure I guess
                new DifferentialDriveKinematicsConstraint(
                        kinematics,
                        Units.inchesToMeters(path.getMaxVelocity())
                )
        );
        return tconfig;
    }

    /**
     * @param
     * @return a cubic wpilib trajectory, using only the endpoint headings
     */
    public Trajectory generateCubicTrajectory(TrajectoryPath path) {
        logger.info(">>>>>>>>>creating a cubic trajectory<<<<<<<<<<<<<<<");
        var tconfig = generateTrajectoryConfig(path);
        List<TrajectoryPath.PathPoint> waypoints = path.getWayPoints();
        List<edu.wpi.first.wpilibj.geometry.Translation2d> _waypoints = new ArrayList<>(waypoints.size());
        logger.info("start pose: {}", path.getStart().getPose());
        for (TrajectoryPath.PathPoint p : waypoints)
            _waypoints.add(p.getPoint().toWpiLibTranslation());
//        _waypoints.remove(0);
//        _waypoints.remove(_waypoints.size()-1);
//        for (var p : _waypoints)
//            logger.info("waypoint: {}", p);
        logger.info("end pose: {}", path.getEnd().getPose());
        return TrajectoryGenerator.generateTrajectory(
                path.getStart().getPose().toWpiLibPose(),
                _waypoints,
                path.getEnd().getPose().toWpiLibPose(),
                tconfig
        );
    }

    /**
     * @return a quintic wpilib trajectory
     */
    public Trajectory generateQuinticTrajectory(TrajectoryPath path) {
        var tconfig = generateTrajectoryConfig(path);
        return TrajectoryGenerator.generateTrajectory(path.getPoses(), tconfig);
    }

    // test that can be run with a GRADLE task to print out states of a cubic trajectory
    // this reveals the fixed bug from wpilib 2020 to 2021.2.2
    public static void main (String[] args) {
        var store = new TrajectoryStore(null, new DifferentialDriveKinematics(24.5));
        var path = new TrajectoryPath();
        path.setCubic(true);
        path.setName("testTrajectory");
        path.setPoints(Arrays.asList(
                new TrajectoryPath.PathPoint(new Translation2d(0, 0), new Rotation(0, 1.0)),
                new TrajectoryPath.PathPoint(new Translation2d(0, 10)),
                new TrajectoryPath.PathPoint(new Translation2d(0, 0), new Rotation(0, 1.0))
        ));
        var config = new TrajectoryConfig(
                Units.inchesToMeters(path.getMaxVelocity()),
                Units.inchesToMeters(path.getMaxAcceleration())
        );
        config.addConstraint(//just to make sure I guess
                new DifferentialDriveKinematicsConstraint(
                        store.kinematics,
                        Units.inchesToMeters(path.getMaxVelocity())
                )
        );
        var trajectory = TrajectoryGenerator.generateTrajectory(
                new Pose2d(new Translation2d(0, 0), new Rotation(0, -1.0)).toWpiLibPose(),
                Arrays.asList(
                        new Translation2d(-100, 100).toWpiLibTranslation(),
                        new Translation2d(100, 100).toWpiLibTranslation()
                ),
                new Pose2d(new Translation2d(0, 0), new Rotation(0.0, -1.0)).toWpiLibPose(),
                config
        ); // store.generateCubicTrajectory(path);
        for (var state : trajectory.getStates()) {
            System.out.println(String.format("%.3f\t%.3f", state.poseMeters.getX(), state.poseMeters.getY()));
        }
    }
}
