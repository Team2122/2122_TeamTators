package org.teamtators.sassitator;

import frc.robot.RobotContainer;
import frc.robot.subsystems.OperatorInterface;
import frc.robot.subsystems.SwerveDrive;
import frc.robot.subsystems.SwerveInputProxy;
import frc.robot.subsystems.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class Subsystems {

    private final ArrayList<Subsystem> subsystemList;
    private final Logger logger;
    private OperatorInterface operatorInterface;
    private Shooter shooter;

    private SwerveDrive swerveDrive;

    private SwerveInputProxy swerveInputProxy;

    private Vision vision;
    private Picker picker;
    private BallChannel ballChannel;
    private Dumper dumper;
    private Climber climber;

    public Subsystems() {
        logger = LoggerFactory.getLogger(getClass().getSimpleName());
        subsystemList = new ArrayList<>();
    }

    public void createSubsystems(RobotContainer robotContainer) {
        logger.info("SUBSYSTEMS");


        // Create the new subsystem objects
//        dumper = new Dumper(robotContainer);
        climber = new Climber(robotContainer);
        vision = new Vision(robotContainer);
        // Create the new subsystem objects
        operatorInterface = new OperatorInterface(robotContainer);
        swerveDrive = new SwerveDrive(robotContainer);
        swerveInputProxy = new SwerveInputProxy(robotContainer,swerveDrive,operatorInterface);
        shooter = new Shooter(robotContainer);
        picker = new Picker(robotContainer);
        ballChannel = new BallChannel(robotContainer);

        // Adding the objects to the sub system list
        subsystemList.add(swerveDrive);
//        subsystemList.add(dumper);
        subsystemList.add(shooter);
        subsystemList.add(swerveInputProxy);
        subsystemList.add(picker);
        subsystemList.add(ballChannel);
        subsystemList.add(vision);
        subsystemList.add(operatorInterface);
        subsystemList.add(climber);
        //subsystemList.add(ballCounter);

        // Run through the list and add each sub-system to robot command scheduler
        for (Subsystem subsystem: subsystemList) {
            subsystem.configure(robotContainer);
        }
//        dumper.configure();

    }

    public ArrayList<Subsystem> getSubsystemList() {
        return subsystemList;
    }

    public void add(Subsystem subsystem) {
        subsystemList.add(subsystem);
    }

    public SwerveDrive getSwerveDrive() {
        return swerveDrive;
    }

    public SwerveInputProxy getSwerveInputProxy() {
        return swerveInputProxy;
    }

    public OperatorInterface getOperatorInterface() { return operatorInterface;}


    public Picker getPicker() { return picker;}

    public BallChannel getBallChannel() { return ballChannel;}

    public Shooter getShooter() { return shooter;}

    public Climber getClimber() { return climber;}

    public Vision getVision() {
        return vision;
    }

}
