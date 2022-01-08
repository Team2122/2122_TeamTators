package org.teamtators.bbt8r;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamtators.common.commands.ConditionalCommand;
import org.teamtators.common.config.ConfigCommandStore;
import org.teamtators.common.datalogging.Disable;
import org.teamtators.common.scheduler.*;
import org.teamtators.bbt8r.TatorRobot;
import org.teamtators.bbt8r.subsystems.*;
import org.teamtators.bbt8r.action_commands.*;
import org.teamtators.bbt8r.continuous_commands.*;

import java.util.EnumSet;
import java.util.function.Function;

public class CommandRegistrar {
    private final TatorRobot robot;
    private final Logger logger = LoggerFactory.getLogger(CommandRegistrar.class);

    private SuperStructure superStructure;
    private Turret turret;
    private Vision vision;
    private Climber climber;
    private GroundPicker picker;
    private ColorWheel colorWheel;
    private Drive drive;

    public CommandRegistrar(TatorRobot robot) {
        this.robot = robot;
        // turret = robot.getSubsystems().getTurret(); NOT ALLOWED EVER
        // vision = robot.getSubsystems().getVision(); NOT ALLOWED EVER
    }

    /**
     * @param commandStore An interpreter that reads YAML and turns it into Java that we can consume
     */
    public void register(ConfigCommandStore commandStore) {
        // todo: organize the commands in this method (subsystem labels please)

        Subsystems subsystems = robot.getSubsystems();
        superStructure = subsystems.getSuperStructure();
        turret = subsystems.getTurret();
        vision = subsystems.getVision();
        climber = subsystems.getClimber();
        picker = subsystems.getGroundPicker();
        colorWheel = subsystems.getColorWheel();
        drive = subsystems.getDrive();

        // putCommand: for use when the intent of the command is ONLY to be USED in yaml configuration
        // registerCommand: for use when the intent of the command is to be CONSTRUCTED AND CONFIGURED (potentially multiple times) in yaml configuration. The lambda (Supplier<Command>) gives us the ability to construct multiple instances of this command.

        commandStore.registerCommand("Wait", () -> new Wait());

        // Drive
        commandStore.registerCommand("DriveTank", () -> new DriveTank(robot));
        commandStore.registerCommand("GearShift", () -> new GearShift(robot));
        commandStore.registerCommand("DriveBack", () -> new DriveBack(robot));
        commandStore.registerCommand("RotateRobot", () -> new RotateRobot(robot));
        commandStore.registerCommand("DriveTrajectoryCommand", () -> new DriveTrajectoryCommand(robot));
        commandStore.putCommand("CalibrateGyro", Commands.instant(drive::calibrateGyro));

        // Multi-Action Command :
        commandStore.registerCommand("LTB_Sequence", () -> new LTB_Sequence(robot));

        // Vision Commands:
        commandStore.registerCommand("Acquire", () -> new Acquire(robot));
        commandStore.registerCommand("VisionTracking", () -> new VisionTracking(robot));
        commandStore.registerCommand("VisionShoot", () -> new VisionTracking(robot));
        commandStore.putCommand("ToggleVisionStatus", Commands.instant(vision::toggleVisionStatus));

        // SuperStructure Commands:
        commandStore.putCommand("SuperStructureContinuous", new SuperStructureContinuous(robot));
        commandStore.registerCommand("SuperStructureSpit", () -> new SuperStructureSpit(robot));

        // BallChannel Commands:
        commandStore.putCommand("PrintSuperStructureState", Commands.instant(superStructure::printSuperStructureState));
        commandStore.putCommand("Pulling", new Pulling(robot));

        // Turret Commands:
        commandStore.registerCommand("ManualShot", () -> new ManualShot(robot));
        commandStore.registerCommand("TimedManualShot", () -> new TimedManualShot(robot));
        commandStore.registerCommand("TurretToAngle", () -> new TurretToAngle(robot));
        commandStore.registerCommand("TurretBasePointInitial", () -> new TurretBasePointInitial(robot));
        commandStore.putCommand("UpdateBasePoint", Commands.instant(turret::updateBasePoint));
        commandStore.registerCommand("BumpFlywheelTargetSpeed", () -> new BumpFlywheelTargetSpeed(turret));
        commandStore.registerCommand("HoodHome", () -> new HoodHome(robot));
        commandStore.registerCommand("HoodExtend", () -> new HoodExtend(robot));
        commandStore.putCommand("PrintTurretState", Commands.instant(turret::printTurretState));
        commandStore.putCommand("PrintTurretSpeed", Commands.instant(turret::printTurretSpeed));
        commandStore.putCommand("PrintTurretTargetSpeed", Commands.instant(turret::printTurretTargetSpeed));
        commandStore.putCommand("PrintTurretAngle", Commands.instant(turret::printTurretAngle));
        commandStore.putCommand("PrintTurretRotations", Commands.instant(turret::printTurretRotations));
        commandStore.putCommand("PrintTurretTargetAngle", Commands.instant(turret::printTurretTargetAngle));
        commandStore.putCommand("PrintTurretEncoderCount", Commands.instant(turret::printTurretEncoderCount));
        commandStore.putCommand("PrintHoodEncoderCount", Commands.instant(turret::printHoodEncoderCount));
        commandStore.putCommand("PrintHoodExtension", Commands.instant(turret::printHoodExtension));
        commandStore.putCommand("PrintHoodTargetExtension", Commands.instant(turret::printHoodTargetExtension));
        commandStore.putCommand("PrintIsHoodAtExtension", Commands.instant(turret::printIsHoodAtExtension));

        // Picker Commands:
        commandStore.registerCommand("PickerPick", () -> new PickerPick(robot));
        commandStore.registerCommand("SpitButton", () -> new SpitButton(robot));
        commandStore.registerCommand("SuperStructureClear", () -> new SuperStructureClear(robot));
        commandStore.putCommand("TeleClear", new TeleClear(robot));
        commandStore.registerCommand("PickHomingSensor", () -> new PickHomingSensor(robot));
        commandStore.putCommand("PrintFlipMotorRotations", Commands.instant(picker::printFlipMotorRotations));
        commandStore.registerCommand("WallExtend", () -> new WallExtend(robot));
        commandStore.registerCommand("RunFluffer", () -> new RunFluffer(robot));
        commandStore.putCommand("SetIdling", Commands.instant(picker::setIdling));

        // Scheduler Commands
        commandStore.putCommand("PrintRunningCommands", Commands.instant(robot.getScheduler()::printRunningCommands));
        commandStore.registerCommand("TestCommand", () -> new TestCommand());
        commandStore.putCommand("HackerServer", new HackerServer(robot));

        // Climber
        commandStore.registerCommand("RaiseArm", () -> new RaiseArm(robot));
        commandStore.registerCommand("LiftToPosition", () -> new LiftToPosition(robot));
        commandStore.registerCommand("HookToBar", () -> new HookToBar(robot));
        commandStore.putCommand("PrintLiftEncoderRotations", Commands.instant(climber::printLiftEncoderRotations));
        commandStore.registerCommand("ActivateRatchet", () -> new ActivateRatchet(robot));
        commandStore.registerCommand("LowerArm", () -> new LowerArm(robot));
        commandStore.registerCommand("ClimbBump", () -> new ClimbBump(robot));


    }

    /**
     * Polymorphic interface to create Tunable Commands.
     * Provide getConfigured() that returns a configured Tunable (usually a Command)
     * Intended for single-use only, in this class
     * @see CommandRegistrar#register(ConfigCommandStore)
     * @see Tunable
     * @param <T>
     */
    interface TunableRegistrar<T extends Tunable<?>> {
        public final static Logger logger = LoggerFactory.getLogger("Tunable");
        String DECREASE_COMMAND = "DecreaseTuner";
        String INCREASE_COMMAND = "IncreaseTuner";

        T getConfigured();
    }
}
