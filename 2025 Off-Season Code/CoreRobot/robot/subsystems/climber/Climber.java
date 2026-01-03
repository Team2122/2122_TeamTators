package frc.robot.subsystems.climber;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import frc.robot.constants.Constants;
import frc.robot.subsystems.climber.ClimberConstants.ClimberPositions;
import org.littletonrobotics.junction.AutoLogOutput;
import org.littletonrobotics.junction.Logger;
import org.teamtators.tester.ManualTestGroup;
import org.teamtators.util.Subsystem;

public class Climber extends Subsystem {
    public enum ClimberStates {
        INIT,
        IDLE,
        MOVING,
        INTERRUPT
    }

    @AutoLogOutput private ClimberStates currentState = ClimberStates.INTERRUPT;
    @AutoLogOutput private ClimberStates newState = ClimberStates.INIT;
    @AutoLogOutput private ClimberPositions desiredPosition = ClimberPositions.STOWED;

    @Override
    public void doPeriodic() {
        if (currentState != newState) {
            currentState = newState;
            switch (currentState) {
                case INIT -> {
                    io.calibrateEncoder();
                    newState = ClimberStates.IDLE;
                }
                case IDLE -> {
                    io.setVolts(0);
                }
                case MOVING -> {
                    io.setVolts(12);
                }
                case INTERRUPT -> {}
            }
        }
        switch (currentState) {
            case INIT -> {}
            case IDLE -> {}
            case MOVING -> {
                if (inputs.currentPosition > desiredPosition.rotations) {
                    newState = ClimberStates.IDLE;
                }
            }
            case INTERRUPT -> {}
        }
    }

    private ClimberIO io;
    private ClimberIOInputsAutoLogged inputs;

    public Climber() {
        io = new ClimberIO() {};
        inputs = new ClimberIOInputsAutoLogged();
        io =
                switch (Constants.kRobotMedium) {
                    case SIM -> new ClimberIOSim();
                    case REAL -> new ClimberIOReal();
                    default -> new ClimberIO() {};
                };
    }

    @Override
    public void log() {
        io.updateInputs(inputs);
        Logger.processInputs("Climber", inputs);
    }

    @Override
    public boolean getHealth() {
        // to already did
        return inputs.motorConnected;
    }

    public ClimberStates getState() {
        return currentState;
    }

    @Override
    public ManualTestGroup createManualTests() {
        return io.getManualTests();
    }

    public Command goTo(ClimberPositions pos) {
        return this.runOnce(
                        () -> {
                            desiredPosition = pos;
                            currentState = ClimberStates.INTERRUPT;
                            newState = ClimberStates.MOVING;
                        })
                .andThen(
                        Commands.waitUntil(
                                () -> currentState != ClimberStates.MOVING && newState != ClimberStates.MOVING));
    }

    public ClimberPositions getDesiredPosition() {
        return desiredPosition;
    }
}
