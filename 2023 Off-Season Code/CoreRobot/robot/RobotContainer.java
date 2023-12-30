// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.ConditionalCommand;
import edu.wpi.first.wpilibj2.command.InstantCommand;
import edu.wpi.first.wpilibj2.command.ParallelCommandGroup;
import frc.robot.subsystems.*;
import frc.robot.subsystems.ArmExtension.ExtensionPosition;
import edu.wpi.first.wpilibj2.command.SequentialCommandGroup;
import edu.wpi.first.wpilibj.Filesystem;
import edu.wpi.first.wpilibj.smartdashboard.Field2d;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.CommandBase;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.button.JoystickButton;
import edu.wpi.first.wpilibj2.command.button.Trigger;
import frc.robot.commands.*;
import frc.robot.constants.OperatorInterfaceConstants;
import frc.robot.constants.PinkarmConstants.ArmExtensionConstants;
import frc.robot.constants.WristConstants.WristPositions;
import frc.robot.subsystems.hardware.PinkarmHW;
import frc.robot.subsystems.ArmRotation.RotationPosition;
import frc.robot.subsystems.Claw.ClawStates;

import static frc.robot.RobotContainer.GamePieceTypes.*;
import common.Controllers.XBOXController;
import common.Tools.tester.ManualTester;
import common.Util.PhoseStuff;
import common.Util.Vision;
import common.teamtators.Subsystem;

import com.pathplanner.lib.auto.PIDConstants;
import com.pathplanner.lib.auto.SwerveAutoBuilder;

import com.pathplanner.lib.PathConstraints;
import com.pathplanner.lib.PathPlanner;
import com.pathplanner.lib.PathPlannerTrajectory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This class is where the bulk of the robot should be declared. Since
 * Command-based is a
 * "declarative" paradigm, very little robot logic should actually be handled in
 * the {@link Robot}
 * periodic methods (other than the scheduler calls). Instead, the structure of
 * the robot (including
 * subsystems, commands, and trigger mappings) should be declared here.
 */
public class RobotContainer {

  public enum PlacePositions {
    LOW,
    MID,
    HIGH,
    AUTO_HIGH
  }

	  public enum GamePieceTypes {
		  UNDEFINED,
		  CUBE,
		  CONE
	  }

  // public final SequentialCommandGroup extendAndRotateWrist;

  private final XBOXController driverController = new XBOXController(
    OperatorInterfaceConstants.kDriverControllerPort);
  private ManualTester manualTester;
  private final XBOXController gunnerController = new XBOXController(
    OperatorInterfaceConstants.kGunnerControllerPort);
  private HashMap<String, CommandBase> commandList;
  private ArrayList<Subsystem> subsystemList = new ArrayList<Subsystem>();
  private final CommandFactory commands;

  public final Command driveCommand;
  public final Rotate180 rotate180;
  // public final PoseStuff poseStuff;
  public final PhoseStuff poseStuff;
  // public final Camera camera;

  // instantiate subsystems here:
  private SwerveDrive swerveDrive;
  private OperatorInterface operatorInterface;
  private Vision vision;
  private ArmRotation armRotation;
  private ArmExtension armExtension;
  private Wrist wrist;
  private Claw claw;
  private PinkarmHW armHW;
  private SequentialCommandGroup armDoThing;
  private static RobotContainer instance;
  public static final Field2d fieldSim = new Field2d();

  public PathPlannerTrajectory traj;

  /**
   * The container for the robot. Contains subsystems, OI devices, and commands.
   */
  public RobotContainer(Robot robot) {
    subsystemList = new ArrayList<>();
    // create subsystems:
    claw = new Claw(this);
    swerveDrive = new SwerveDrive(this);
    operatorInterface = new OperatorInterface(this);
    vision = new Vision();
    armHW = new PinkarmHW(); // NOT A SUBSYSTEM
    armRotation = new ArmRotation(this);
    armExtension = new ArmExtension(this);
    wrist = new Wrist(this);
    armExtension.giveWrist(wrist);

    poseStuff = new PhoseStuff(swerveDrive, vision);
    // camera = new Camera();
    // poseStuff = new PoseStuff(swerveDrive, vision);

    subsystemList.add(claw);
    subsystemList.add(armRotation);
    subsystemList.add(operatorInterface);
    subsystemList.add(swerveDrive);
    // subsystemList.add(vision);
    subsystemList.add(armExtension);
    subsystemList.add(wrist);

    // Configure the trigger bindings
    commandList = new HashMap<String, CommandBase>();

    manualTester = new ManualTester(this);
    // ExecutorService e = Executors.newFixedThreadPool(1);

    driveCommand = swerveDrive.drive(operatorInterface::get);
    rotate180 = new Rotate180(swerveDrive, operatorInterface);
    Robot.getInstance().addPeriodic(poseStuff, 0.01);
    // e.submit(camera);
    // Robot.getInstance().addPeriodic(camera, 0.01);
    commands = new CommandFactory(this);

    configureDriverButtonBindings();
    configureGunnerButtonBindings();
    instance = this;
  }

  // Auto Stuff
  SendableChooser<List<PathPlannerTrajectory>> autoChooser = new SendableChooser<>();
  // SendableChooser<SequentialCommandGroup> autoChooserForNonPathPlanner = new
  // SendableChooser<>();
  HashMap<String, edu.wpi.first.wpilibj2.command.Command> eventMap = new HashMap<>();

  public void autoChooser() {

    File pathplannerDirectory = new File(Filesystem.getDeployDirectory(), "pathplanner");
    String[] pathNames = pathplannerDirectory.list();

    if (pathNames != null) {
      for (String pathName : pathNames) {
        System.out.println(pathName);
        if (pathName.endsWith(".path")) {
          String path = pathName.substring(0, pathName.length() - 5);
          autoChooser.addOption(path, PathPlanner.loadPathGroup(path, false, new PathConstraints(1.75,.75
          )));
        }
      }
    }
    SmartDashboard.clearPersistent("Auto Path");
    // autoChooserForNonPathPlanner.addOption("SitPretty", highPlace);
    SmartDashboard.putData("Auto Path", autoChooser);

  }

  /*
   * Builds the commands for the auto paths.
   * JORGE: EDIT THIS TO FIT CORRECT COMMANDS
   */
  public void buildCommands() {
    eventMap.put("FirstPickCommand", commands.floorPick(CUBE));

    eventMap.put("FirstPlaceCommand",
      claw.runOnce(() -> claw.setGamePieceType(CONE))
        .andThen(commands.place(PlacePositions.AUTO_HIGH))
        .andThen(claw.spit()));

    eventMap.put("Retract",
      wrist.toPosition(WristPositions.TRANSPORT)
        .alongWith(armExtension.toPosition(ExtensionPosition.HOME)
          .andThen(armRotation.toPosition(RotationPosition.HOME))));

    eventMap.put("ThrowPiece",
      Commands.print("Throw Piece in auto Scheduled")
        .andThen(wrist.toNode(PlacePositions.LOW))
        .andThen(claw.suck(CONE)));

    eventMap.put("FloorPickCube", commands.floorPick(CUBE));

    eventMap.put("DonePickingCube", commands.goToTransport());

    eventMap.put("GoToTransport", commands.goToTransport());

    eventMap.put("PrintTwoPieceCable", Commands.print("Cable_Cone1_Forward Scheduled"));
    eventMap.put("PrintTwoPieceNonCable", Commands.print("NonCable_Cone1_Forward Scheduled"));
    eventMap.put("PrintBalance", Commands.print("Station_Cone1_Forward Scheduled"));
    eventMap.put("PrintPretty", Commands.print("Be_Pretty Scheduled"));
  }

  /**
   * Builds the auto command from the path group.
   * 
   * @param pathGroup The path group to build the auto command from.
   * @return The auto command.
   */
  public edu.wpi.first.wpilibj2.command.Command buildAuto(List<PathPlannerTrajectory> pathGroup) {
    buildCommands();

    // for (int i = 0; i < pathGroup.`; i++) {

    // }

    SwerveAutoBuilder autoBuilder = new SwerveAutoBuilder(
      poseStuff::get, // Pose2d supplier
      poseStuff::reset, // Pose2d consumer, used to reset odometry at the beginning of auto
      new PIDConstants(1.8, 0.0, 0.28575), // PID constants to correct for translation error (used to create the X
      // and Y PID controllers)
      new PIDConstants(2.5, 0.0, 0.0), // PID constants to correct for rotation error (used to create the
      // rotation controller)
      swerveDrive::accept, // Module states consumer used to output to the drive subsystem
      eventMap,
      true, // Should the path be automatically mirrored depending on alliance color.
      // Optional, defaults to true
      swerveDrive // The drive subsystem. Used to properly set the requirements of path following
      // commands
    );

    edu.wpi.first.wpilibj2.command.Command fullAuto = autoBuilder.fullAuto(pathGroup);
    traj = pathGroup.get(0);
    // holDrive = new HolonomicDriveCommand(swerveDrive, traj, poseStuff);
    return fullAuto;
  }

  /**
   * Returns the auto command selected by the auto chooser.
   * Used by Robot.java to run the auto command.
   * 
   * @return The auto command selected by the auto chooser.
   */
  public edu.wpi.first.wpilibj2.command.Command getAutonomousCommand() {
    return buildAuto(autoChooser.getSelected());
  }

  // Auto Stuff

  private void configureDriverButtonBindings() {
    Trigger buttonA = new JoystickButton(driverController, XBOXController.Button.kA.value);
    Trigger buttonB = new JoystickButton(driverController, XBOXController.Button.kB.value);

    Trigger buttonX = new JoystickButton(driverController, XBOXController.Button.kX.value);
    Trigger buttonY = new JoystickButton(driverController, XBOXController.Button.kY.value);
    Trigger POV_DOWN = new JoystickButton(driverController, XBOXController.Button.kPOV_DOWN.value);
    Trigger POV_UP = new JoystickButton(driverController, XBOXController.Button.kPOV_UP.value);
    Trigger POV_LEFT = new JoystickButton(driverController, XBOXController.Button.kPOV_LEFT.value);
    Trigger POV_RIGHT = new JoystickButton(driverController, XBOXController.Button.kPOV_RIGHT.value);
    Trigger START = new JoystickButton(driverController, XBOXController.Button.kSTART.value);
    Trigger BACK = new JoystickButton(driverController, XBOXController.Button.kBACK.value);
    Trigger BUMPER_LEFT = new JoystickButton(driverController, XBOXController.Button.kBUMPER_LEFT.value);
    Trigger BUMPER_RIGHT = new JoystickButton(driverController, XBOXController.Button.kBUMPER_RIGHT.value);
    Trigger TRIGGER_LEFT = new JoystickButton(driverController, XBOXController.Button.kTRIGGER_LEFT.value);
    Trigger TRIGGER_RIGHT = new JoystickButton(driverController, XBOXController.Button.kTRIGGER_RIGHT.value);

    // pick human feed cube

    TRIGGER_RIGHT.onTrue(commands.floorPick(CONE));
    TRIGGER_RIGHT.onFalse(commands.goToTransport());
  
    TRIGGER_LEFT.onTrue(commands.floorPick(CUBE));
    TRIGGER_LEFT.onFalse(commands.goToTransport());
  
    BUMPER_RIGHT.onTrue(commands.shelfPick(CONE));
    BUMPER_RIGHT.onFalse(commands.goToTransport());
  
    BUMPER_LEFT.onTrue(commands.shelfPick(CUBE));
    BUMPER_LEFT.onFalse(commands.goToTransport());

    BACK.onTrue(new InstantCommand(() -> swerveDrive.getGyro().zero()));

    buttonA.onTrue(commands.place(PlacePositions.LOW));
    buttonB.onTrue(rotate180);

    buttonX.onTrue(
      claw.spit()
        .andThen(commands.goToTransport()));

    // bump the arm up/down
    // START.onTrue(new InstantCommand(armRotation::bumpUp));
    // START.onFalse(new InstantCommand(armRotation::thouShallHalt));   
  }

  private void configureGunnerButtonBindings() {
    Trigger buttonA = new JoystickButton(gunnerController, XBOXController.Button.kA.value);
    Trigger buttonB = new JoystickButton(gunnerController, XBOXController.Button.kB.value);
    Trigger buttonX = new JoystickButton(gunnerController, XBOXController.Button.kX.value);
    Trigger buttonY = new JoystickButton(gunnerController, XBOXController.Button.kY.value);
    Trigger START = new JoystickButton(gunnerController, XBOXController.Button.kSTART.value);
    Trigger BACK = new JoystickButton(gunnerController, XBOXController.Button.kBACK.value);
    Trigger POV_DOWN = new JoystickButton(driverController, XBOXController.Button.kPOV_DOWN.value);
    Trigger POV_UP = new JoystickButton(driverController, XBOXController.Button.kPOV_UP.value);
    Trigger POV_LEFT = new JoystickButton(driverController, XBOXController.Button.kPOV_LEFT.value);
    Trigger POV_RIGHT = new JoystickButton(driverController, XBOXController.Button.kPOV_RIGHT.value);
    Trigger BUMPER_RIGHT = new JoystickButton(gunnerController, XBOXController.Button.kBUMPER_RIGHT.value);
    Trigger BUMPER_LEFT = new JoystickButton(gunnerController, XBOXController.Button.kBUMPER_LEFT.value);
    Trigger TRIGGER_LEFT = new JoystickButton(gunnerController, XBOXController.Button.kTRIGGER_LEFT.value);
    Trigger TRIGGER_RIGHT = new JoystickButton(gunnerController, XBOXController.Button.kTRIGGER_RIGHT.value);

    buttonA.onTrue(commands.place(PlacePositions.LOW));

    buttonB.onTrue(commands.place(PlacePositions.MID));

    buttonY.onTrue(commands.place(PlacePositions.HIGH));

    BUMPER_LEFT.onTrue(armRotation.toPosition(RotationPosition.HIGH_PLACE_CONE));
    // BACK.onTrue(new InstantCommand(armRotation::bumpUp));
    // BACK.onFalse(new InstantCommand(armRotation::thouShallHalt));
    // // START.onTrue(new InstantCommand(armRotation::bumpDown));
    // START.onFalse(new InstantCommand(armRotation::thouShallHalt));



  }

  private void createCommands() {
  
  }

  public static RobotContainer getInstance() {
    return instance;
  }

  public XBOXController getDriverController() {
    return driverController;
  }

  public XBOXController getGunnerController() {
    return gunnerController;
  }

  public ManualTester getTester() {
    return manualTester;
  }

  public ArrayList<Subsystem> getSubsystemList() {
    return subsystemList;
  }

  public SwerveDrive getSwerveDrive() {
    return swerveDrive;
  }

  public OperatorInterface getOperatorInterface() {
    return operatorInterface;
  }

  public Vision getVision() {
    return vision;
  }

  public PinkarmHW getArmHW() {
    return armHW;
  }

  public ArmExtension getArmExtension() {
    return armExtension;
  }

  public ArmRotation getArmRotation() {
    return armRotation;
  }

  public Wrist getWrist() {
    return wrist;
  }

  public Claw getClaw() {
    return claw;
  }

}
