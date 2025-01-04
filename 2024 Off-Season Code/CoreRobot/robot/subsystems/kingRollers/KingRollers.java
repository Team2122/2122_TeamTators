package frc.robot.subsystems.kingRollers;

import org.littletonrobotics.junction.Logger;
import org.teamtators.Util.Subsystem;
import org.teamtators.tester.*;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.button.Trigger;
import frc.robot.Robot;
import frc.robot.constants.GeneralConstants;
import frc.robot.subsystems.picker.Picker;
import frc.robot.subsystems.picker.Picker.PickerStates;

public class KingRollers extends Subsystem {
    protected enum Speeds {
        IDLE(0),
        PICK(12),
        SHOOT(12),
        REVERSE(-12),
        REVERSE_SLOW(-2),
        FEED(12);

        final double kVolts;
        Speeds(double volts) {
            this.kVolts = volts;
        }
    }

    public enum KingRollerStates {
        INIT,
        EMPTY,
        PULLING,
        READY,
        FEED,
        FEED_UNCONDITIONALLY,
        REVERSE,
        NOTE_REPOSITIONING,
        INTERRUPT
    }
    private KingRollerStates currentState = KingRollerStates.EMPTY;
    private KingRollerStates newState = KingRollerStates.INIT;

    private KingRollersIO io;
    private KingRollersIOInputsAutoLogged inputs;

    public final Trigger noteOutOfKingRollers;

    private Picker picker;

    public KingRollers() {
        super();

        switch (GeneralConstants.kRobotMedium) {
            case REAL:
                io = new KingRollersIOReal();
                //io = new KingRollersIO() {};
                break;
            case SIM:
                io = new KingRollersIOSim();
                break;
            case REPLAY:
                io = new KingRollersIO() {};
                break;
        }
        inputs = new KingRollersIOInputsAutoLogged();

        noteOutOfKingRollers = new Trigger(() -> !inputs.noteSensor)
            .debounce(.5);
    }

    @Override
    public void configure() {
        picker = Robot.getPicker();
    }

    @Override
    public void log() {
        io.updateInputs(inputs);
        Logger.processInputs("King Rollers", inputs);
        Logger.recordOutput("King Rollers/Current State", currentState);
        Logger.recordOutput("King Rollers/New State", newState);
        Logger.recordOutput("King Rollers/Current Command", getPossibleCommand()
            .orElse(Commands.none().withName("nothing"))
            .getName());
    }
    
    @Override
    public void doPeriodic() {
        if (currentState != newState) {
            switch (newState) {
                case INIT:
                    newState = KingRollerStates.EMPTY;
                    break;
                case EMPTY:
                    io.setSpeed(Speeds.IDLE);
                    break;
                case PULLING:
                    io.setSpeed(Speeds.PICK);
                    break;
                case READY:
                    io.setSpeed(Speeds.IDLE);
                    break;
                case NOTE_REPOSITIONING:
                    io.setSpeed(Speeds.REVERSE_SLOW);
                    break;
                case FEED_UNCONDITIONALLY:
                case FEED:
                    io.setSpeed(Speeds.FEED);
                    break;
                case REVERSE:
                    io.setSpeed(Speeds.REVERSE);
                    break;
                case INTERRUPT:
                    break;
            }

            currentState = newState;
        }

        switch (currentState) {
            case INIT:
                // do nothing
                break;
            case EMPTY:
                if (inputs.noteSensor) {
                    newState = KingRollerStates.READY;
                } else if (picker.getChimneySensor()) {
                    newState = KingRollerStates.PULLING;
                }
                break;
            case PULLING:
                if (inputs.noteSensor) {
                    newState = KingRollerStates.READY;
                }
                break;
            case READY:
                if (inputs.safetySensor) {
                    newState = KingRollerStates.NOTE_REPOSITIONING;
                }

                if (!inputs.noteSensor) {
                    newState = KingRollerStates.EMPTY;
                }
                break;
            case NOTE_REPOSITIONING:
                if (!inputs.safetySensor) {
                    newState = KingRollerStates.READY;
                }
                break;
            case FEED:
                if (noteOutOfKingRollers.getAsBoolean()
                    && picker.getState() != PickerStates.LEAVING)
                {
                    newState = KingRollerStates.EMPTY;
                }
                break;
            case REVERSE:
            case FEED_UNCONDITIONALLY:
            case INTERRUPT:
                break;
        }
    }

    public KingRollerStates getState() {
        return currentState;
    }

    public boolean noteSensed() {
        return inputs.noteSensor;
    }

    public boolean safetySensor() {
        return inputs.safetySensor;
    }

    @Override
    public void reset() {
        newState = KingRollerStates.INIT;
    }

    @Override
    public ManualTestGroup createManualTests() {
        return io.getManualTest();
    }

    public Command feed() {
        return Commands.waitUntil(() -> currentState == KingRollerStates.READY)
            .andThen(this.runOnce(() -> {
                currentState = KingRollerStates.INTERRUPT;
                newState = KingRollerStates.FEED;
            })).andThen(Commands.waitUntil(() -> newState != KingRollerStates.FEED))
            .withName("Feed");
    }

    public Command feedUnconditionally() {
        return Commands.runOnce(() -> {
            currentState = KingRollerStates.INTERRUPT;
            newState = KingRollerStates.FEED_UNCONDITIONALLY;
        }).andThen(Commands.waitUntil(() -> newState != KingRollerStates.FEED_UNCONDITIONALLY))
            .finallyDo(() -> {
                currentState = KingRollerStates.INTERRUPT;
                newState = KingRollerStates.EMPTY;
            })
            .withName("FeedUnconditionally");
    }

    public Command reverse() {
        return this.runOnce(() -> {
            currentState = KingRollerStates.INTERRUPT;
            newState = KingRollerStates.REVERSE;
        }).andThen(Commands.idle())
            .finallyDo(() -> newState = KingRollerStates.EMPTY)
            .withName("Reverse");
    }

    @Override
    public boolean getHealth() {
        return inputs.connected;
    }
}
