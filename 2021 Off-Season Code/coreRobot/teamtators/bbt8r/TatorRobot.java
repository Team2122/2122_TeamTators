package org.teamtators.bbt8r;

import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import org.teamtators.common.SubsystemsBase;
import org.teamtators.common.TatorRobotBase;
import org.teamtators.common.config.ConfigCommandStore;
import org.teamtators.common.scheduler.Command;
import org.teamtators.bbt8r.CommandRegistrar;
import org.teamtators.bbt8r.subsystems.Subsystems;

public class TatorRobot extends TatorRobotBase {
    private Subsystems subsystems;
    private final CommandRegistrar registrar;
    private SendableChooser<String> autoChooser;

    public TatorRobot(String configDir) {
        super(configDir);
        registrar = new CommandRegistrar(this);
        subsystems = new Subsystems(this);
    }

    @Override
    public SubsystemsBase getSubsystemsBase() {
        return subsystems;
    }

    public Subsystems getSubsystems() {
        return subsystems;
    }

    @Override
    @SuppressWarnings("removal")
    protected void registerCommands(ConfigCommandStore commandStore) {
        super.registerCommands(commandStore);
        registrar.register(commandStore);

        autoChooser = new SendableChooser<>();
        autoChooser.setDefaultOption("NoAuto", "NoAuto");
        autoChooser.addOption("Basic3Ball", "$Basic3Ball");
        autoChooser.addOption("BasicBack3Ball", "$BasicBack3Ball");
        // autoChooser.addOption("StraightTest", "$StraightTest");
        autoChooser.addOption("Straight", "$Straight");
        autoChooser.addOption("ReverseStraight", "$ReverseStraight");
        autoChooser.addOption("Center6Ball_Trench", "$Center6Ball_Trench");
        autoChooser.addOption("Center6Ball_OverBar", "$Center6Ball_OverBar");

        autoChooser.addOption("Hood80", "$Hood80");
        autoChooser.addOption("Hood70", "$Hood70");
        autoChooser.addOption("Hood60", "$Hood60");

        autoChooser.setSubsystem("TatorRobot");
        autoChooser.setName("autoChooser");
        SmartDashboard.putData(autoChooser);
    }

    @Override
    public Command getAutoCommand() {
        return commandStore.getCommand(autoChooser.getSelected());
    }
}
