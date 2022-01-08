package org.teamtators.bbt8r.action_commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamtators.bbt8r.TatorRobot;
import org.teamtators.bbt8r.subsystems.Drive;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.drive.DriveTrajectoryFollower;
import org.teamtators.common.drive.TrajectoryPath;
import org.teamtators.common.scheduler.Command;

public class DriveTrajectoryCommand extends Command implements Configurable<DriveTrajectoryCommand.Config> {
    private final Logger logger = LoggerFactory.getLogger(DriveTrajectoryCommand.class);
    private final Drive drive;
    private Config config;
    private DriveTrajectoryFollower follower;
    private TrajectoryPath currentPath;
    private boolean localDebug = false;

    public static DriveTrajectoryCommand create(TatorRobot robot, String pathName, boolean resetPose) {
        var config = new Config();
        config.pathName = pathName;
        config.resetPose = resetPose;
        var command = new DriveTrajectoryCommand(robot);
        command.configure(config);
        return command;
    }

    public DriveTrajectoryCommand(TatorRobot robot) {
        super("DriveTrajectory");
        drive = robot.getSubsystems().getDrive();
        follower = drive.getDriveTrajectoryFollower();
        requires(drive);
    }

    @Override
    protected void initialize() {
        super.initialize(localDebug);
        if (localDebug) {
            logger.info("Starting driving trajectory at " + drive.getPose());
        }
        if (config.resetPose)
            drive.getPoseEstimator().setPose(currentPath.getStart().getPose());
        drive.driveTrajectory(currentPath);
        if (localDebug) {
            logger.info("Total time should be {}", follower.getTimeLeft());
            logger.info("Total time should be {}", follower.getTrajectory().getTotalTimeSeconds());
        }
    }

    @Override
    public boolean step() {
        return follower.isFinished();
    }

    @Override
    protected void finish(boolean interrupted) {
        super.finish(interrupted, localDebug);
        if (localDebug) {
            logger.info((interrupted ? "Interrupted" : "Finished") + " driving path at " + drive.getPose());
        }
        drive.stopTrajectory();
    }

    public void configure(Config config) {
        this.config = config;
        currentPath = drive.getPathByStringName(config.pathName);
        localDebug = config.debug;
    }

    public static class Config {
        public String pathName;
        public boolean resetPose = false;
        public boolean debug;
    }
}
