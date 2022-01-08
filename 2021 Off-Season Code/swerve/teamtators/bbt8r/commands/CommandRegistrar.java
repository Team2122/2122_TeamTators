package org.teamtators.bbt8r.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamtators.bbt8r.TatorRobot;
import org.teamtators.bbt8r.subsystems.SwerveDrive;
import org.teamtators.common.SG.SGControllerRegistry;
import org.teamtators.common.config.ConfigCommandStore;
import org.teamtators.common.scheduler.Commands;
import org.teamtators.common.scheduler.Tunable;

public class CommandRegistrar {

    private final TatorRobot robot;
    private final Logger logger = LoggerFactory.getLogger(CommandRegistrar.class);
    private SGControllerRegistry controllerRegistry;
    private SwerveDrive swerveDrive;

    public CommandRegistrar(TatorRobot robot) {
        this.robot = robot;
        controllerRegistry = robot.getSGControllerRegistry();
    }

    /**
     * @param commandStore An interpreter that reads YAML and turns it into Java that we can consume
     */
    public void register(ConfigCommandStore commandStore) {

        swerveDrive = robot.getSubsystems().getSwerveDrive();

        // putCommand: for use when the intent of the command is ONLY to be USED in yaml configuration
        // registerCommand: for use when the intent of the command is to be CONSTRUCTED AND CONFIGURED (potentially multiple times) in yaml configuration. The lambda (Supplier<Command>) gives us the ability to construct multiple instances of this command.

        commandStore.putCommand("SwerveDriveContinuous", new SwerveDriveContinuous(robot));
        commandStore.putCommand("PrintRunningCommands", Commands.instant(robot.getScheduler()::printRunningCommands));
        commandStore.putCommand("HackerServer", new HackerServer(robot));
        commandStore.registerCommand("SetDriveComputerInput", () -> new SetDriveComputerInput(robot));
        commandStore.putCommand("ResetYaw", Commands.instant(swerveDrive::resetYaw));
        commandStore.putCommand("ResetPosition", Commands.instant(swerveDrive::resetPosition));
        commandStore.putCommand("bumpDown", Commands.instant(swerveDrive::bumpPDown));
        commandStore.putCommand("bumpUp", Commands.instant(swerveDrive::bumpPUp));


    }

    /**
     * Polymorphic interface to create Tunable Commands.
     * Provide getConfigured() that returns a configured Tunable (usually a Command)
     * Intended for single-use only, in this class
     *
     * @param <T>
     * @see CommandRegistrar#register(ConfigCommandStore)
     * @see Tunable
     */
    interface TunableRegistrar<T extends Tunable<?>> {
        public final static Logger logger = LoggerFactory.getLogger("Tunable");
        String DECREASE_COMMAND = "DecreaseTuner";
        String INCREASE_COMMAND = "IncreaseTuner";

        T getConfigured();
    }
}
