package frc.robot.subsystems.coralPicker;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.button.Trigger;
import frc.robot.constants.Constants;
import frc.robot.subsystems.coralPicker.CoralPickerConstants.CoralPickerPositions;
import org.littletonrobotics.junction.Logger;
import org.teamtators.tester.ManualTestGroup;
import org.teamtators.util.Subsystem;

public class CoralPicker extends Subsystem {

    public enum CoralPickerStates {
        INIT,
        IN_POSITION,
        MOVING,
        INTERRUPT
    }

    private CoralPickerIO io;
    private CoralPickerIOInputsAutoLogged inputs = new CoralPickerIOInputsAutoLogged();

    private CoralPickerStates currentState = CoralPickerStates.INTERRUPT;
    private CoralPickerStates newState = CoralPickerStates.INIT;

    private CoralPickerPositions desiredPosition = CoralPickerPositions.STOWED;

    public final Trigger inPosition = new Trigger(this::inPosition);

    public CoralPicker() {
        io =
                switch (Constants.kRobotMedium) {
                    case REAL -> new CoralPickerIOReal();
                    case SIM -> new CoralPickerIOSim();
                    default -> new CoralPickerIO() {};
                };
    }

    public void reset() {}

    @Override
    public void log() {
        io.updateInputs(inputs);
        Logger.processInputs("CoralPicker", inputs);
        Logger.recordOutput("CoralPicker/CurrentState", currentState);
        Logger.recordOutput("CoralPicker/NewState", newState);
        Logger.recordOutput("CoralPicker/DesiredPosition", desiredPosition);
    }

    @Override
    public void doPeriodic() {
        if (currentState != newState) {
            currentState = newState;
            switch (currentState) {
                case INIT -> {
                    newState = CoralPickerStates.IN_POSITION;
                    // old homing code - use if implementing a failsafe
                    // io.setVolts(CoralPickerConstants.STOW_VOLTAGE);
                }
                case IN_POSITION -> {
                    if (desiredPosition.holdPower != 0) {
                        io.setVolts(desiredPosition.holdPower); // hold power would be the same as setSetpoint
                    }
                }
                case MOVING -> {
                    io.setSetpoint(desiredPosition.rotations);
                }
                case INTERRUPT -> {}
            }
        }

        switch (currentState) {
            case INIT -> {}
            case IN_POSITION -> {
                if (!inPosition()) {
                    newState = CoralPickerStates.MOVING;
                }
            }
            case MOVING -> {
                if (inPosition()) {
                    newState = CoralPickerStates.IN_POSITION;
                }
            }
            case INTERRUPT -> {}
        }
    }

    private boolean inPosition() {

        if (desiredPosition.holdPower != 0) {
            // using hold power instead of just maintaining setpoint
            double error =
                    (inputs.currentPosition - desiredPosition.rotations)
                            * Math.signum(desiredPosition.holdPower);
            return error > -CoralPickerConstants.ALLOWED_ERROR;
        } else {
            double error = Math.abs(inputs.currentPosition - desiredPosition.rotations);
            return error < CoralPickerConstants.ALLOWED_ERROR;
        }
    }

    private void goToInternal(CoralPickerPositions pos) {
        desiredPosition = pos;
        currentState = CoralPickerStates.INTERRUPT;
        newState = CoralPickerStates.MOVING;
    }

    public Command deploy() {
        return this.runOnce(() -> goToInternal(CoralPickerPositions.DEPLOYED))
                .andThen(Commands.idle()) // force the caller to decide when the picker should stow
                .finallyDo(() -> goToInternal(CoralPickerPositions.STOWED));
    }

    public Command stow() {
        return this.runOnce(() -> goToInternal(CoralPickerPositions.STOWED))
                .andThen(Commands.waitUntil(inPosition));
    }

    public Command completeStow() {
        return this.runOnce(() -> goToInternal(CoralPickerPositions.COMPLETE_STOWED))
                .andThen(Commands.idle())
                .finallyDo(() -> goToInternal(CoralPickerPositions.STOWED));
    }

    public Command goTo(CoralPickerPositions pos) {
        return this.runOnce(() -> goToInternal(pos)).andThen(Commands.waitUntil(inPosition));
    }

    @Override
    public boolean getHealth() {
        return inputs.motorConnected;
    }

    @Override
    public ManualTestGroup createManualTests() {
        return io.getManualTests();
    }

    public CoralPickerStates getCurrentState() {
        return currentState;
    }

    public CoralPickerPositions getDesiredPosition() {
        return desiredPosition;
    }
}
