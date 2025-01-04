package frc.robot.subsystems.picker;

import org.littletonrobotics.junction.Logger;
import org.teamtators.Util.Subsystem;
import org.teamtators.tester.ManualTestGroup;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.button.Trigger;

import frc.robot.constants.GeneralConstants;
import frc.robot.subsystems.kingRollers.KingRollers;
import frc.robot.subsystems.kingRollers.KingRollers.KingRollerStates;
import frc.robot.Robot;

public class Picker extends Subsystem {
    protected enum Speeds {
        IDLE(0),
        PICKING(12),
        PULLING(12),
        REVERSE(-9);

        double volts;
        Speeds(double volts) {
            this.volts = volts;
        }
    }

    public enum PickerStates {
        INIT,
        IDLE,
        PICKING,
        PULLING,
        LEAVING,
        PANIC,
        PICK_UNCONDITIONALLY,
        INTERRUPT
    }
    private PickerStates currentState = PickerStates.IDLE;
    private PickerStates newState = PickerStates.INIT;

    private PickerIO io;
    private PickerIOInputsAutoLogged inputs;

    private KingRollers kingRollers;

    public Picker() {
        super();

        inputs = new PickerIOInputsAutoLogged();
        switch(GeneralConstants.kRobotMedium) {
            case REAL:
                io = new PickerIOReal();
                //io = new PickerIO() {};
                break;
            case SIM:
                io = new PickerIOSim();
                break;
            default:
                io = new PickerIO() {};
                break;
        }

    }

    @Override
    public void configure() {
        kingRollers = Robot.getKingRollers();
    }

    @Override
    public void log() {
        io.updateInputs(inputs);
        Logger.processInputs("Picker", inputs);
        Logger.recordOutput("Picker/NewState", newState);
        Logger.recordOutput("Picker/CurrentState", currentState);
        Logger.recordOutput("Picker/CurrentCommand", getPossibleCommand()
            .orElse(Commands.none().withName("nothing"))
            .getName());
    }

    @Override
    public void doPeriodic() {
        if (currentState != newState) {
            switch (newState) {
                case INIT:
                    newState = PickerStates.IDLE;
                    break;
                case IDLE:
                    io.setSpeed(Speeds.IDLE);
                    break;
                case PICKING:
                    io.setSpeed(Speeds.PICKING);
                    break;
                case PULLING:
                    io.setSpeed(Speeds.PULLING);
                    break;
                case LEAVING:
                    // theres a note already in the king rollers, don't feed
                    if (kingRollers.getState() == KingRollerStates.READY) {
                        io.setSpeed(Speeds.IDLE);
                    }
                    break;
                case PANIC:
                    io.setSpeed(Speeds.REVERSE);
                    break;
                case PICK_UNCONDITIONALLY:
                    io.setSpeed(Speeds.PULLING);
                    break;
                case INTERRUPT:
                    break;
            }

            currentState = newState;
        }

        switch (currentState) {
            case INIT:
            case IDLE:
                // do nothing
                break;
            case PICKING:
                if (inputs.chimneySensorActivated
                    || inputs.farEntranceActivated)
                {
                    newState = PickerStates.PULLING;
                }
                break;
            case PULLING:
                if (inputs.chimneySensorActivated) {
                    newState = PickerStates.LEAVING;
                }
                break;
            case LEAVING:
                // READY means ready to shoot/amp, so not ready
                // to take another note
                if (kingRollers.getState() == KingRollerStates.EMPTY
                    || kingRollers.getState() == KingRollerStates.PULLING
                    || kingRollers.getState() == KingRollerStates.FEED)
                {
                    io.setSpeed(Speeds.PULLING);
                } else {
                    io.setSpeed(Speeds.IDLE);
                }

                if (!inputs.chimneySensorActivated) {
                    newState = PickerStates.IDLE;
                }
                break;
            case PICK_UNCONDITIONALLY:
            case PANIC:
                // let the driver take us out via button press
                break;
            case INTERRUPT:
                break;
        }
    }

    public PickerStates getState() {
        return currentState;
    }

    public boolean getChimneySensor() {
        return inputs.chimneySensorActivated;
    }

    public boolean getFarSensor() {
        return inputs.farEntranceActivated;
    }

    @Override
    public void reset() {
        newState = PickerStates.INIT;
    }

    @Override
    public ManualTestGroup createManualTests() {
        return io.getManualTest();
    }

    public Command pick() {
        return this.runOnce(() -> {
            currentState = PickerStates.INTERRUPT;
            newState = PickerStates.PICKING;   
        }).andThen(Commands.waitUntil(() -> newState != PickerStates.PICKING))
            .withName("Pick");
    }

    public Command idle() {
        return this.runOnce(() -> {
            currentState = PickerStates.INTERRUPT;
            newState = PickerStates.IDLE;   
        }).withName("Idle");
    }

    public Command panic() {
        return this.runOnce(() -> {
            currentState = PickerStates.INTERRUPT;
            newState = PickerStates.PANIC;
        }).andThen(Commands.waitUntil(() -> newState != PickerStates.PANIC))
            .finallyDo(() -> newState = PickerStates.IDLE)
            .withName("Panic");
    }

    public Command pickUnconditionally() {
        return this.runOnce(() -> {
            currentState = PickerStates.INTERRUPT;
            newState = PickerStates.PICK_UNCONDITIONALLY;
        }).andThen(() -> Commands.waitUntil(() -> newState != PickerStates.PICK_UNCONDITIONALLY))
            .withName("PickUnconditionally");
    }

    @Override
    public boolean getHealth() {
        return inputs.connected;
    }
}
