package frc.robot.subsystems.climber;

import org.littletonrobotics.junction.Logger;

import org.teamtators.Util.Subsystem;
import org.teamtators.tester.ManualTestGroup;

import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.button.Trigger;

import frc.robot.subsystems.climber.ClimberIO.ClimberIOInputs;

import frc.robot.constants.GeneralConstants;

public class Climber extends Subsystem {
    public enum Position {
        HOME(0, 0),
        PRE_CLIMB(123.4, 12), // this is not a placeholder, it is really 123.4
        CLIMB(-24.9, -5);

        private final double kRotations;
        private final double kBangBangVoltage;
        private final boolean kUseBangBang;

        Position(double rotations) {
            this.kRotations = rotations;
            this.kBangBangVoltage = 0;
            this.kUseBangBang = false;
        }

        Position(double rotations, double bangVoltage) {
            this.kRotations = rotations;
            this.kBangBangVoltage = bangVoltage;
            this.kUseBangBang = true;
        }

        public double getRotations() {
            return kRotations;
        }
    }

    public enum ClimberStates {
        INIT,
        IN_POSITION,
        MOVING,
        INTERRUPT
    }

    private ClimberStates currentState = ClimberStates.IN_POSITION;
    private ClimberStates newState = ClimberStates.INIT;
    private Position desiredPosition = Position.HOME;

    private ClimberIO io;
    private ClimberIOInputsAutoLogged inputs;

    private Timer initTimer;

    public Climber() {
        super();

        switch (GeneralConstants.kRobotMedium) {
            case REAL:
                io = new ClimberIOReal();
                // io = new ClimberIO() {};
                break;
            case SIM:
                io = new ClimberIOSim();
                break;
            case REPLAY:
                io = new ClimberIO() {};
        }
        
        inputs = new ClimberIOInputsAutoLogged();

        initTimer = new Timer();
    }

    @Override
    public void log() {
        io.updateInputs(inputs);
        Logger.processInputs("Climber", inputs);
        Logger.recordOutput("Climber/CurrentState", currentState);
        Logger.recordOutput("Climber/NewState", newState);
        Logger.recordOutput("Climber/DesiredPosition", desiredPosition);
        Logger.recordOutput("Climber/CurrentCommand", this.getPossibleCommand()
            .orElse(Commands.none().withName("nothing"))
            .getName());
    }

    @Override
    public void doPeriodic() {
        if (currentState != newState) {
            switch (newState) {
                case INIT:
                    io.setEncoderPosition(0);
                    newState = ClimberStates.IN_POSITION;
                    break;
                case IN_POSITION:
                    if (desiredPosition.kUseBangBang) {
                        io.setVoltage(0);
                    } else {
                        io.setSetpoint(desiredPosition);
                    }
                    break;
                case MOVING:
                    if (desiredPosition.kUseBangBang) {
                        io.setVoltage(desiredPosition.kBangBangVoltage);
                    } else {
                        io.setSetpoint(desiredPosition);
                    }
                case INTERRUPT:
                    break;
            }

            currentState = newState;
        }

        switch (currentState) {
            case INIT:
                break;
            case IN_POSITION:
                if (!inPosition()) {
                    newState = ClimberStates.MOVING;
                }
                break;
            case MOVING:
                if (inPosition()) {
                    newState = ClimberStates.IN_POSITION;
                }
                break;
            case INTERRUPT:
                break;
        }
    }

    private boolean inPosition() {
        if (desiredPosition.kUseBangBang) {
            return (
                desiredPosition.kRotations - inputs.motorPositionRotations
            ) * Math.signum(desiredPosition.kBangBangVoltage) < ClimberConstants.kErrorDegrees;
        } else {
            double error = inputs.motorPositionRotations - desiredPosition.kRotations;
            return Math.abs(error) < ClimberConstants.kErrorDegrees;
        }
    }

    @Override
    public void reset() {
        newState = ClimberStates.INIT;
    }

    @Override
    public ManualTestGroup createManualTests() {
        return io.getManualTest();
    }

    public Command goTo(Position position) {
        return this.runOnce(() -> {
            currentState = ClimberStates.INTERRUPT;
            newState = ClimberStates.MOVING;
            desiredPosition = position;
        }).andThen(Commands.waitUntil(() -> newState != ClimberStates.MOVING))
            .withName("GoTo");
    }

    @Override
    public boolean getHealth() {
        return inputs.connected;
    }
}
