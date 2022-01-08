package org.teamtators.bbt8r.subsystems;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.teamtators.common.SubsystemsBase;
import org.teamtators.common.config.ConfigException;
import org.teamtators.common.config.ConfigLoader;
import org.teamtators.common.control.FaultChecker;
import org.teamtators.common.control.Updatable;
import org.teamtators.common.controllers.Controller;
import org.teamtators.common.controllers.LogitechF310;
import org.teamtators.common.hw.TatorSparkMax;
import org.teamtators.common.scheduler.Subsystem;
import org.teamtators.bbt8r.TatorRobot;

import java.util.ArrayList;
import java.util.List;

public class Subsystems extends SubsystemsBase {
    private static final String SUBSYSTEMS_CONFIG = "Subsystems.yaml";
    private List<Subsystem> subsystems = new ArrayList<>();
    private List<Updatable> updatables;

    private Drive drive;
    private GroundPicker groundPicker;
    private Climber climber;
    private List<TatorSparkMax> sparks;

    private OperatorInterface operatorInterface;

    private BallChannel ballChannel;

    private Turret turret;
    private Config config;
    private SuperStructure superStructure;
    private Vision vision;
    private ColorWheel colorWheel;

    private FaultChecker checker = new FaultChecker();

    private boolean localDebug = false;

    public Subsystems(TatorRobot robot) {
        updatables = new ArrayList<>();
        drive = new Drive(robot);
        operatorInterface = new OperatorInterface();
        ballChannel = new BallChannel(robot);
        groundPicker = new GroundPicker();
        turret = new Turret();
        climber = new Climber(robot);
        superStructure = new SuperStructure(ballChannel, groundPicker, turret, climber);
        vision = new Vision();
        colorWheel = new ColorWheel(robot);

        subsystems.add(groundPicker);
        subsystems.add(drive);
        subsystems.add(operatorInterface);
        subsystems.add(climber);
        subsystems.add(turret);
        subsystems.add(ballChannel);
        subsystems.add(vision);
        subsystems.add(colorWheel);
        subsystems.add(superStructure);
        updatables.addAll(drive.getUpdatables());
        updatables.add(checker);
    }

    @Override
    public List<Subsystem> getSubsystemList() {
        return subsystems;
    }

    public Drive getDrive() {
        return drive;
    }

    public BallChannel getBallChannel() {
        return ballChannel;
    }

    public GroundPicker getGroundPicker() {
        return groundPicker;
    }

    public Turret getTurret() {
        return turret;
    }

    public SuperStructure getSuperStructure() {
        return superStructure;
    }

    public Vision getVision() {
        return vision;
    }

    public Climber getClimber() {
        return climber;
    }

    public ColorWheel getColorWheel() {
        return colorWheel;
    }

    public OperatorInterface getOi() {
        return operatorInterface;
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
        drive.configure(config.drive);
        operatorInterface.configure(config.operatorInterface);
        climber.configure(config.climber);
        groundPicker.configure(config.groundPicker);
        turret.configure(config.turret);
        sparks = new ArrayList<>();
        sparks.addAll(drive.getSparkMaxes());

        ballChannel.configure(config.ballChannel);
        superStructure.configure(config.superStructure);
        vision.configure(config.vision);
        colorWheel.configure(config.colorWheel);
        localDebug = config.debug;
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
        return operatorInterface.getDriverJoystick(); // Use operator interface, refer to darthtator2
    }

    @Override
    public void deconfigure() {
    }

    public static class Config {
        public OperatorInterface.Config operatorInterface;
        public Drive.Config drive;
        public BallChannel.Config ballChannel;
        public Climber.Config climber;
        public GroundPicker.Config groundPicker;
        public Turret.Config turret;
        public SuperStructure.Config superStructure;
        public Vision.Config vision;
        public ColorWheel.Config colorWheel;
        public boolean debug;
    }
}
