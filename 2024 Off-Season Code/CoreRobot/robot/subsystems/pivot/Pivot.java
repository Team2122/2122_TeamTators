package frc.robot.subsystems.pivot;

import org.littletonrobotics.junction.Logger;
import org.teamtators.Util.Subsystem;
import org.teamtators.Util.QuickDebug;
import org.teamtators.tester.ManualTestGroup;

import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import frc.robot.constants.GeneralConstants;
import frc.robot.subsystems.pivot.PivotConstants.PivotPositions;
import frc.robot.Robot;
import frc.robot.util.AimUtil;

public class Pivot extends Subsystem {
    public enum PivotStates {
        INIT,
        IN_POSITION,
        MOVING,
        SPECIAL_AIMING,
        INTERRUPT
    }
    private PivotStates currentState = PivotStates.IN_POSITION;
    private PivotStates newState = PivotStates.INIT;
    private PivotPositions desiredPosition = PivotPositions.HOME;

    private PivotIO io;
    private PivotIOInputsAutoLogged inputs;

    private Timer initTimer;

    private double specialAimSetpoint = 0;

    public Pivot() {
        super();

        inputs = new PivotIOInputsAutoLogged();
        switch(GeneralConstants.kRobotMedium) {
            case REAL:
                io = new PivotIOReal();
                //io = new PivotIO() {};
                break;
            case SIM:
                io = new PivotIOSim();
                break;
            default:
                io = new PivotIO() {};
                break;
        }

        initTimer = new Timer();
    }

    @Override
    public void log() {
        io.updateInputs(inputs);
        Logger.processInputs("Pivot", inputs);
        Logger.recordOutput("Pivot/NewState", newState);
        Logger.recordOutput("Pivot/CurrentState", currentState);
        Logger.recordOutput("Pivot/DesiredPosition", desiredPosition);
        Logger.recordOutput("Pivot/CurrentCommand", getPossibleCommand()
            .orElse(Commands.none().withName("nothing"))
            .getName());
        Logger.recordOutput("Pivot/InPosition", inPosition());
    }

    @Override
    public void doPeriodic() {
        io.updateControls();

        if (currentState != newState) {
            switch (newState) {
                case INIT:
                    initTimer.restart();
                    io.updateInputs(inputs);
                    break;
                case IN_POSITION: 
                    if (desiredPosition == PivotPositions.HOME) {
                        io.setVolts(PivotConstants.homeHoldPower);
                        break;
                    } /* else fallthru */
                case MOVING:
                    //io.setSetpoint(QuickDebug.input("Tuning/Pivot Angle", 0.0) + PivotPositions.HOME.kDegrees);
                    io.setSetpoint(desiredPosition.kDegrees);
                    break;
                case SPECIAL_AIMING:
                    /* do stuff in the periodic */
                    break;
                case INTERRUPT:
                    break;
            }

            currentState = newState;
        }

        switch (currentState) {
            case INIT:
                // sometimes the encoder can fail to initialize
                // so we make sure that it is correct before moving on
                // if it takes an entire 0.5 seconds to boot that thang up,
                // it's probably failed and we should assume it's down
                double time = initTimer.get();
                if ((inputs.absoluteEncoderPosition > 0
                     && inputs.absoluteEncoderPosition <= 1.0)
                    || time > PivotConstants.kInitTimeout)
                {
                    newState = PivotStates.IN_POSITION;

                    double absEncoderPos;
                    if (time > PivotConstants.kInitTimeout) {
                        absEncoderPos = PivotConstants.kFallbackEncoderValue;
                        System.out.println("WARNING: Pivot absolute encoder has FAILED in " + time + " seconds");
                        SmartDashboard.putBoolean(GeneralConstants.kNTRobotStatusKey, false);
                    } else {
                        absEncoderPos = inputs.absoluteEncoderPosition;
                        System.out.println("Pivot absolute encoder has initialized in " + time + " seconds");
                    }
                    double convertedEncoderPos = PivotConstants
                        .absoluteEncoderToRelativeEncoder(absEncoderPos);
                    io.setEncoderPosition(convertedEncoderPos);
                }

                break;
            case IN_POSITION:
                if (!inPosition()) {
                    newState = PivotStates.MOVING;
                }
                break;
            case MOVING:
                if (inPosition()) {
                    newState = PivotStates.IN_POSITION;
                }
                break;

            case SPECIAL_AIMING:
                /*if (AimUtil.getTarget() == AimUtil.AimTargets.AMP) {
                    currentState = PivotStates.INTERRUPT;
                    newState = PivotStates.MOVING;
                    desiredPosition = PivotPositions.HOME;
                } else {*/
                    specialAimSetpoint = AimUtil.getPivotAngle();
                    io.setSetpoint(specialAimSetpoint);
                    //QuickDebug.output("Tuning/Pivot Setpoint", angle);
                    //io.setSetpoint(QuickDebug.input("Tuning/Pivot Angle", 0.0) + PivotPositions.HOME.kDegrees);
                //}
                break;
            case INTERRUPT:
                break;
        }

        //io.setSetpoint(QuickDebug.input("Tuning/Pivot Angle", 0.0) + PivotPositions.HOME.kDegrees);
    }

    public PivotPositions getDesiredPosition() {
        return desiredPosition;
    }

    public boolean inPosition() {
        double currentPosition = PivotConstants.rotationsToDegrees(inputs.positionRotations);
        
        if (currentState == PivotStates.SPECIAL_AIMING) {
            return Math.abs(specialAimSetpoint - currentPosition) < PivotConstants.kErrorDegrees;
        } else {
            if (desiredPosition == PivotPositions.HOME) {
                return currentPosition < desiredPosition.kDegrees + PivotConstants.kErrorDegrees;
            } else {
                double setpoint;
                if (currentState == PivotStates.SPECIAL_AIMING) {
                    setpoint = specialAimSetpoint;
                } else {
                    setpoint = desiredPosition.kDegrees;
                }
                return Math.abs(setpoint - currentPosition) < PivotConstants.kErrorDegrees;
            }
        }
    }

    public PivotStates getState() {
        return currentState;
    }

    @Override
    public void reset() {
        newState = PivotStates.INIT;
    }

    @Override
    public ManualTestGroup createManualTests() {
        return io.getManualTest();
    }

    public Command goTo(PivotPositions position) {
        return this.runOnce(() -> {
            currentState = PivotStates.INTERRUPT;
            newState = PivotStates.MOVING;
            desiredPosition = position;
        }).andThen(Commands.waitUntil(() -> newState != PivotStates.MOVING))
            .withName("GoTo");
    }

    public Command specialAim() {
        return this.runOnce(() -> {
            currentState = PivotStates.INTERRUPT;
            newState = PivotStates.SPECIAL_AIMING;   
        }).andThen(Commands.waitUntil(() -> newState != PivotStates.SPECIAL_AIMING))
            .withName("Special Aim");
    }

    @Override
    public boolean getHealth() {
        return inputs.connected;
    }
}
