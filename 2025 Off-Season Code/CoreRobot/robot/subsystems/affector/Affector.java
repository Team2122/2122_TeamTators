package frc.robot.subsystems.affector;

import edu.wpi.first.math.filter.Debouncer;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.button.Trigger;
import frc.robot.constants.Constants;
import org.littletonrobotics.junction.AutoLogOutput;
import org.littletonrobotics.junction.Logger;
import org.teamtators.tester.ManualTestGroup;
import org.teamtators.util.Subsystem;

public class Affector extends Subsystem {
    private AffectorIOInputsAutoLogged inputs;
    private AffectorIO io;
    @AutoLogOutput private AffectorStates currentState = AffectorStates.INTERRUPT;
    @AutoLogOutput private AffectorStates newState = AffectorStates.INIT;

    private final Debouncer debouncer = new Debouncer(0.25);
    private final Timer timer = new Timer();

    public final Trigger hasCoral = new Trigger(() -> currentState == AffectorStates.PICKED_CORAL);
    public final Trigger hasAlgae = new Trigger(() -> currentState == AffectorStates.PICKED_ALGAE);
    public final Trigger hasNothing = new Trigger(() -> currentState == AffectorStates.IDLE);

    public enum AffectorStates {
        INIT,
        IDLE,
        INTERRUPT,
        PICKING_CORAL,
        PICKING_ALGAE,
        PICKED_CORAL,
        PICKED_ALGAE,
        EJECTING_L1,
        EJECTING_BARGE,
        CHECK_CORAL,
        ;
    }

    public Affector() {
        io =
                switch (Constants.kRobotMedium) {
                    case REAL -> io = new AffectorIOReal();
                    case SIM -> io = new AffectorIOSim();
                    default -> io = new AffectorIO() {};
                };
        inputs = new AffectorIOInputsAutoLogged();
    }

    public void doPeriodic() {
        if (currentState != newState) {
            switch (newState) {
                case INTERRUPT -> {}
                case INIT -> newState = AffectorStates.IDLE;
                case IDLE, PICKED_CORAL -> {
                    debouncer.calculate(false);
                    io.setVoltage(0);
                }
                case PICKED_ALGAE -> {
                    io.setVoltage(1);
                }
                case PICKING_CORAL -> {
                    debouncer.calculate(false);
                    io.setVoltage(6);
                }
                case PICKING_ALGAE -> {
                    debouncer.calculate(false);
                    io.setVoltage(8);
                }
                case CHECK_CORAL -> {
                    timer.restart();
                    io.setVoltage(6);
                }
                case EJECTING_L1 -> {
                    timer.restart();
                    io.setVoltage(-3);
                }
                case EJECTING_BARGE -> {
                    io.setVoltage(-8);
                    timer.restart();
                }
            }
            currentState = newState;
        }

        switch (currentState) {
            case INIT, INTERRUPT, PICKED_CORAL, PICKED_ALGAE -> {}
            case IDLE -> {}
            case PICKING_CORAL -> {
                if (debouncer.calculate(inputs.statorCurrent > 15)) {
                    newState = AffectorStates.PICKED_CORAL;
                }
            }
            case PICKING_ALGAE -> {
                if (debouncer.calculate(inputs.statorCurrent > 20)) {
                    newState = AffectorStates.PICKED_ALGAE;
                }
            }
            case EJECTING_BARGE, EJECTING_L1 -> {
                if (timer.get() > AffectorConstants.EJECT_TIMEOUT) {
                    newState = AffectorStates.IDLE;
                }
            }
            case CHECK_CORAL -> {
                if (timer.get() > AffectorConstants.CHECK_CORAL_TIMEOUT && inputs.statorCurrent > 15) {
                    newState = AffectorStates.PICKED_CORAL;
                } else if (timer.get() > AffectorConstants.CHECK_CORAL_TIMEOUT) {
                    newState = AffectorStates.IDLE;
                }
            }
        }
    }

    @Override
    public ManualTestGroup createManualTests() {
        return io.getManualTests();
    }

    @Override
    public void log() {
        io.updateInputs(inputs);

        Logger.processInputs("Affector", inputs);
        Logger.recordOutput(
                "Affector/CurrentCommand", getPossibleCommand().map(Command::getName).orElse("none"));
    }

    @Override
    public boolean getHealth() {
        return inputs.connected;
    }

    private Command setState(AffectorStates state) {
        return this.runOnce(
                () -> {
                    currentState = AffectorStates.INTERRUPT;
                    newState = state;
                });
    }

    private Command setStateAndWait(AffectorStates state) {
        return setState(state).andThen(Commands.waitUntil(() -> newState != state));
    }

    public Command pickCoral() {
        return setStateAndWait(AffectorStates.PICKING_CORAL)
                .handleInterrupt(() -> newState = AffectorStates.IDLE);
    }

    public Command pickAlgae() {
        return setStateAndWait(AffectorStates.PICKING_ALGAE)
                .handleInterrupt(() -> newState = AffectorStates.IDLE);
    }

    public Command checkCoral() {
        return setStateAndWait(AffectorStates.CHECK_CORAL);
    }

    public Command ejectAlgae() {
        return setStateAndWait(AffectorStates.EJECTING_BARGE);
    }

    public Command ejectL1() {
        return setStateAndWait(AffectorStates.EJECTING_L1);
    }

    public AffectorStates getState() {
        return newState;
    }
}
