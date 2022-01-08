package org.teamtators.common.drive;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import edu.wpi.first.wpilibj.kinematics.DifferentialDriveKinematics;
import org.teamtators.common.config.ConfigLoader;
import org.teamtators.common.scheduler.TrajectoryStore;

public class TrajectoryTest {
    public static void main(String[] args) {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        ConfigLoader loader = new ConfigLoader("config", mapper);
        TrajectoryStore store = new TrajectoryStore(loader, new DifferentialDriveKinematics(30.0/254.0));

        store.loadPathsFromConfig("Trajectories.yaml");
    }
}
