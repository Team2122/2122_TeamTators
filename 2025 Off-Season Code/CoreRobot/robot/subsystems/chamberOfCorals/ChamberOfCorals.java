package frc.robot.subsystems.chamberOfCorals;

import edu.wpi.first.math.filter.Debouncer;
import edu.wpi.first.math.filter.Debouncer.DebounceType;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.button.Trigger;
import frc.robot.constants.Constants;
import org.littletonrobotics.junction.Logger;
import org.teamtators.tester.ManualTestGroup;
import org.teamtators.util.Subsystem;
import org.teamtators.util.Timer;

public class ChamberOfCorals extends Subsystem {

    public enum ChamberStates {
        INIT,
        IDLE,
        EMPTY_PICKING,
        PANIK,
        PICKED,
        INTERRUPT
    }

    private final ChamberOfCoralsIO io;
    private final ChamberOfCoralsIOInputsAutoLogged inputs = new ChamberOfCoralsIOInputsAutoLogged();

    private ChamberStates currentState = ChamberStates.IDLE;
    private ChamberStates newState = ChamberStates.INIT;

    public final Trigger hasCoral = new Trigger(() -> currentState == ChamberStates.PICKED);

    private final Debouncer debouncer = new Debouncer(0.1);

    public ChamberOfCorals() {
        io =
                switch (Constants.kRobotMedium) {
                    case REAL -> new ChamberOfCoralsIOReal();
                    case SIM -> new ChamberOfCoralsIOSim();
                    default -> new ChamberOfCoralsIO() {};
                };
    }

    @Override
    public void log() {
        io.updateInputs(inputs);
        Logger.processInputs("ChamberOfCorals", inputs);
        Logger.recordOutput("ChamberOfCorals/CurrentState", currentState);
        Logger.recordOutput("ChamberOfCorals/NewState", newState);
    }

    private final Timer jamTimer = new Timer();
    private final Debouncer jamDebounce = new Debouncer(0.1, DebounceType.kRising);
    private final Debouncer pickDebounce = new Debouncer(0.3);

    @Override
    public void doPeriodic() {
        if (currentState != newState) {
            currentState = newState;
            switch (currentState) {
                case INIT, INTERRUPT -> {}
                case IDLE -> {
                    io.setVolts(0.0, 0.0);
                }
                case EMPTY_PICKING -> {
                    io.setVolts(-12.0, -3.0); // Spin motor until coral detected
                    debouncer.calculate(false);
                }
                case PANIK -> {
                    jamTimer.restart();
                    io.setVolts(4.0, 2.0); // Spin motor until coral detected
                }
                case PICKED -> {
                    pickDebounce.calculate(false);
                    io.setVolts(0.0, -1.5); // Stop motor
                }
            }
        }

        switch (currentState) {
            case INIT -> {
                newState = ChamberStates.IDLE;
            }
            case IDLE -> {
                if (inputs.breakbeamTriggered) {
                    newState = ChamberStates.PICKED;
                }
            }
            case EMPTY_PICKING -> {
                if (debouncer.calculate(inputs.breakbeamTriggered)) {
                    newState = ChamberStates.PICKED;
                } else if (jamDebounce.calculate(
                        inputs.minion1StatorCurrent > 40 || inputs.minion2StatorCurrent > 40)) {
                    newState = ChamberStates.PANIK;
                }
            }
            case PANIK -> {
                if (jamTimer.get() > 0.2) {
                    newState = ChamberStates.EMPTY_PICKING;
                }
            }
            case PICKED -> {
                if (pickDebounce.calculate(!inputs.breakbeamTriggered)) {
                    newState = ChamberStates.IDLE;
                }
            }
            case INTERRUPT -> {
                newState = ChamberStates.IDLE;
            }
        }
    }

    public Command pick() {
        return this.runOnce(() -> newState = ChamberStates.EMPTY_PICKING)
                .andThen(Commands.waitUntil(() -> currentState == ChamberStates.PICKED))
                .handleInterrupt(
                        () -> {
                            if (newState == ChamberStates.EMPTY_PICKING || newState == ChamberStates.PANIK) {
                                newState = ChamberStates.IDLE;
                            }
                        })
                .withName("Pick");
    }

    @Override
    public boolean getHealth() {
        return inputs.falconMotorConnected
                && inputs.minion1MotorConnected
                && inputs.minion2MotorConnected;
    }

    @Override
    public ManualTestGroup createManualTests() {
        return io.getManualTests();
    }
}
