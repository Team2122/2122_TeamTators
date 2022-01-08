package org.teamtators.bbt8r.subsystems;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.teamtators.bbt8r.TatorRobot;
import org.teamtators.common.SubsystemsBase;
import org.teamtators.common.config.ConfigException;
import org.teamtators.common.config.ConfigLoader;
import org.teamtators.common.control.FaultChecker;
import org.teamtators.common.control.Updatable;
import org.teamtators.common.controllers.Controller;
import org.teamtators.common.controllers.LogitechF310;
import org.teamtators.common.hw.TatorSparkMax;
import org.teamtators.common.scheduler.Subsystem;

import java.util.ArrayList;
import java.util.List;

public class Subsystems extends SubsystemsBase {
    private static final String SUBSYSTEMS_CONFIG = "Subsystems.yaml";
    private List<Subsystem> subsystems = new ArrayList<>();
    private List<Updatable> updatables;

    private List<TatorSparkMax> sparks;

    private OperatorInterface operatorInterface;
    private SwerveDrive swerveDrive;
    private SwerveInputProxy inputProxy;

    private Config config;

    private FaultChecker checker = new FaultChecker();

    public Subsystems(TatorRobot robot) {
        updatables = new ArrayList<>();
        swerveDrive = new SwerveDrive();
        operatorInterface = new OperatorInterface();
        inputProxy = new SwerveInputProxy(robot, swerveDrive, operatorInterface); // MUST BE AFTER SWERVE DRIVE
        subsystems.add(operatorInterface);
        updatables.add(checker);
    }

    @Override
    public List<Subsystem> getSubsystemList() {
        return subsystems;
    }

    @Override
    public void configure(ConfigLoader configLoader) {
        try {
            ObjectNode configNode = (ObjectNode) configLoader.load(SUBSYSTEMS_CONFIG);
            Config config = configLoader.getObjectMapper().treeToValue(configNode, Config.class);
            configure(config);
        } catch (Throwable e) {
            throw new ConfigException("Error while configuring subsystems: ", e);
        }
    }

    public void configure(Config config) {
        swerveDrive.configure(config.swerveDrive);
        operatorInterface.configure(config.operatorInterface);
        inputProxy.configure(config.swerveInputProxy);
        sparks = new ArrayList<>();
//        sparks.addAll(drive.getSparkMaxes());
    }

    public List<TatorSparkMax> getSparks() {
        return sparks;
    }

    @Override
    public List<Updatable> getUpdatables() {
        return updatables;
    }

    @Override
    public List<Controller<?, ?>> getControllers() {
        return operatorInterface.getAllControllers();
    }

    @Override
    public LogitechF310 getTestModeController() {
        return operatorInterface.getDriverJoystick(); //Use operator interface, refer to darthtator2
    }

    public OperatorInterface getOi() {
        return operatorInterface;
    }

    public SwerveDrive getSwerveDrive() {
        return swerveDrive;
    }

    public SwerveInputProxy getInputProxy() {
        return inputProxy;
    }

    @Override
    public void deconfigure() {

    }

    public static class Config {
        public OperatorInterface.Config operatorInterface;
        public SwerveDrive.Config swerveDrive;
        public SwerveInputProxy.Config swerveInputProxy;
    }
}
