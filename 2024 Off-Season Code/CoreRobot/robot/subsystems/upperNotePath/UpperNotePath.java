package frc.robot.subsystems.upperNotePath;

import frc.robot.Robot;
import frc.robot.subsystems.kingRollers.KingRollers;
import org.littletonrobotics.junction.Logger;
import org.teamtators.Util.Subsystem;
import org.teamtators.tester.ManualTestGroup;

import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.button.Trigger;
import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.constants.GeneralConstants;
import frc.robot.util.AimUtil;

public class UpperNotePath extends Subsystem {
    public enum ShooterSpeeds {
        IDLE(20, 20),
        FIXED_SHOT(50, 35),
        STOPPED(0, 0),
        DROP(-90, 0),
        STOW(-90, 0);

        public final double kLeftRPS, kRightRPS;
        private ShooterSpeeds(double leftRPS, double rightRPS)  {
            kLeftRPS = leftRPS;
            kRightRPS = rightRPS;
        }
    }

    public enum ShotType {
        STATIC,
        DYNAMIC
    }
    private ShotType shotType = ShotType.DYNAMIC;

    public enum UpperNotePathStates {
        INIT,
        IDLE,
        AMP,
        SHOOTING,
        TRAP,
        PREP_EJECT,
        EJECT_DUNKER_PREP,
        EJECT_DUNKER,
        EJECT_SHOOTER,
        INTERRUPT
    }
    private UpperNotePathStates currentState = UpperNotePathStates.INTERRUPT;
    private UpperNotePathStates newState = UpperNotePathStates.INIT;

    public enum AmpStates {
        PREPARE,
        WAIT,
        STOWING,
        STOWED,
        DROP,
        DONE,
        INTERRUPT
    }
    private AmpStates currentAmpState = AmpStates.INTERRUPT;
    private AmpStates newAmpState = AmpStates.PREPARE;

    private Timer timer;
    private Trigger noteOutOfDunker;

    private enum TrapStates {
        PREPARE,
        WAIT,
        STOWING,
        STOWED,
        DROP,
        ALMOST_OUT,
        DONE,
        INTERRUPT
    }
    private TrapStates currentTrapState = TrapStates.INTERRUPT;
    private TrapStates newTrapState = TrapStates.PREPARE;

    private UpperNotePathIO io;
    private UpperNotePathIOInputsAutoLogged inputs;
    private KingRollers kingRollers;

    public final Trigger startable;
    public final Trigger shooting = 
        new Trigger(() -> currentState == UpperNotePathStates.SHOOTING);

    public final Trigger doneWithStartTrap;
    public final Trigger doneWithMidTrap;

    public final Trigger doneWithStartAmp;

    private boolean upToSpeed = false;

    public UpperNotePath() {
        super();

        switch (GeneralConstants.kRobotMedium) {
            case REAL:
                io = new UpperNotePathIOReal();
                //io = new UpperNotePathIO() {};
                break;
            case SIM:
                io = new UpperNotePathIOSim();
                break;
            case REPLAY:
                io = new UpperNotePathIO() {};
                break;
        }
        inputs = new UpperNotePathIOInputsAutoLogged();

        timer = new Timer();
        noteOutOfDunker = new Trigger(() -> !inputs.dunkerSensor)
            .debounce(UpperNotePathConstants.kDunkerDeactivateTimeout);

        startable = new Trigger(this::startable);

        doneWithStartTrap = new Trigger(() -> currentState == UpperNotePathStates.TRAP
                                              && currentTrapState == TrapStates.WAIT);
        doneWithMidTrap = new Trigger(() -> currentState == UpperNotePathStates.TRAP
                                            && currentTrapState == TrapStates.STOWED);

        doneWithStartAmp = new Trigger(() -> currentState == UpperNotePathStates.AMP
                                             && currentAmpState == AmpStates.STOWED);
    }

    @Override
    public void configure() {
        kingRollers = Robot.getKingRollers();
    }

    @Override
    public void log() {
        io.updateInputs(inputs);
        Logger.processInputs("UpperNotePath", inputs);

        Logger.recordOutput("UpperNotePath/CurrentState", currentState);
        Logger.recordOutput("UpperNotePath/NewState", newState);
        Logger.recordOutput("UpperNotePath/Startable", startable());
        Logger.recordOutput("UpperNotePath/CurrentCommand", this.getPossibleCommand()
            .orElse(Commands.none().withName("nothing"))
            .getName());
        Logger.recordOutput("UpperNotePath/UpToSpeed", upToSpeed);

        Logger.recordOutput("UpperNotePath/Amp/CurrentState", currentAmpState);
        Logger.recordOutput("UpperNotePath/Amp/NewState", newAmpState);

        Logger.recordOutput("UpperNotePath/Trap/CurrentState", currentTrapState);
        Logger.recordOutput("UpperNotePath/Trap/NewState", newTrapState);
    }

    @Override
    public void doPeriodic() {
        io.updateControls();
        if (currentState != newState) {
            switch (newState) {
                case INIT:
                    break;
                case IDLE:
                    io.setDunkerVoltage(0);
                    io.setShooterSpeeds(ShooterSpeeds.IDLE);
                    break;
                case AMP:
                    currentAmpState = AmpStates.INTERRUPT;
                    newAmpState = AmpStates.PREPARE;
                    break;
                case SHOOTING:
                    break;
                case TRAP:
                    currentTrapState = TrapStates.INTERRUPT;
                    newTrapState = TrapStates.PREPARE;
                    break;
                case PREP_EJECT:
                    io.setShooterSpeeds(ShooterSpeeds.IDLE);
                    io.setDunkerVoltage(0);
                    timer.restart();
                    break;
                case EJECT_SHOOTER:
                    io.setShooterSpeeds(ShooterSpeeds.IDLE);
                    io.setDunkerVoltage(0);
                    break;
                case EJECT_DUNKER_PREP:
                    io.setShooterSpeeds(ShooterSpeeds.DROP);
                    io.flipDiverterUp();
                    io.setDunkerVoltage(-12);
                    break;
                case EJECT_DUNKER:
                    io.holdDiverter();
                    break;
                case INTERRUPT:
                    break;
            }

            currentState = newState;
        }

        switch (newState) {
            case INIT:
                newState = UpperNotePathStates.IDLE;
                break;
            case IDLE:
                break;
            case AMP:
                runAmpSubStateMachine();

                if (currentAmpState == AmpStates.DONE) {
                    newState = UpperNotePathStates.IDLE;
                }
                break;
            case SHOOTING:
                //double left = QuickDebug.input("Tuning/Left Shooter", 0.0);
                //double right = QuickDebug.input("Tuning/Right Shooter", 0.0);

                double left, right;

                if (shotType == ShotType.DYNAMIC) {
                    left = AimUtil.getLeftShooterSpeed();
                    right = AimUtil.getRightShooterSpeed();
                } else {
                    left = ShooterSpeeds.FIXED_SHOT.kLeftRPS;
                    right = ShooterSpeeds.FIXED_SHOT.kRightRPS;
                }

                io.setShooterSpeeds(
                    left,
                    right);

                upToSpeed = Math.abs(left - inputs.leftShooterVelocityRPS) < UpperNotePathConstants.kShooterErrorRPS
                    && Math.abs(right - inputs.rightShooterVelocityRPS) < UpperNotePathConstants.kShooterErrorRPS;
                //QuickDebug.output("Tuning/Left Shooter Setpoint", left);
                //QuickDebug.output("Tuning/Right Shooter Setpoint", right);
                break;
            case TRAP:
                runTrapSubStateMachine();

                if (currentTrapState == TrapStates.DONE) {
                    newState = UpperNotePathStates.IDLE;
                }
                break;
            case PREP_EJECT:
                if (timer.get() > UpperNotePathConstants.kEjectPrepTimeout) {
                    if (kingRollers.safetySensor()) {
                        newState = UpperNotePathStates.EJECT_SHOOTER;
                    } else {
                        newState = UpperNotePathStates.EJECT_DUNKER_PREP;
                    }
                }
                break;
            case EJECT_DUNKER_PREP:
                if (inputs.diverterSensor) {
                    newState = UpperNotePathStates.EJECT_DUNKER;
                }
                break;
            case EJECT_DUNKER:
            case EJECT_SHOOTER:
            case INTERRUPT:
                break;
        }
        
    }

    private void runAmpSubStateMachine() {
        if (currentAmpState != newAmpState) {
            switch (newAmpState) {
                case PREPARE:
                    io.setShooterSpeeds(ShooterSpeeds.STOW);
                    io.setDunkerVoltage(UpperNotePathConstants.kDunkerStowVoltage);
                    io.flipDiverterUp();
                    break;
                case WAIT:
                    io.holdDiverter();
                    break;
                case STOWING:
                    io.holdDiverter();
                    timer.restart();
                    break;
                case STOWED:
                    io.setShooterSpeeds(ShooterSpeeds.STOPPED);
                    io.holdDiverter();
                    io.setDunkerVoltage(0);
                    break;
                case DROP:
                    io.setShooterSpeeds(ShooterSpeeds.DROP);
                    io.setDunkerVoltage(UpperNotePathConstants.kDunkerDropVoltage);
                    timer.restart();
                    break;
                case DONE:
                    break;
                case INTERRUPT:
                    break;
            }

            currentAmpState = newAmpState;
        }

        switch (currentAmpState) {
            case PREPARE:
                if (inputs.diverterSensor) {
                    newAmpState = AmpStates.WAIT;
                }
                break;
            case WAIT:
                // do nothing
                // transition handled by command
                break;
            case STOWING:
                if (timer.get() > UpperNotePathConstants.kAmpStowingTimeout
                    || inputs.dunkerSensor)
                {
                    newAmpState = AmpStates.STOWED;
                }
                break;
            case STOWED:
                // do nothing like the wait state
                break;
            case DROP:
                if (timer.get() > UpperNotePathConstants.kAmpDropTimeout
                    || !inputs.dunkerSensor)
                {
                    newAmpState = AmpStates.DONE;
                }
                break;
            case DONE:
                break;
            case INTERRUPT:
                break;
        }
    }

    private void runTrapSubStateMachine() {
        if (currentTrapState != newTrapState) {
            switch (newTrapState) {
                case PREPARE:
                    io.flipDiverterUp();
                    break;
                case WAIT:
                    io.holdDiverter();
                    break;
                case STOWING:
                    io.setShooterSpeeds(ShooterSpeeds.DROP);
                    break;
                case STOWED:
                    io.setShooterSpeeds(ShooterSpeeds.STOPPED);
                    break;
                case DROP:
                    io.setDunkerVoltage(UpperNotePathConstants.kDunkerDropVoltage);
                    timer.restart();
                    break;
                case ALMOST_OUT:
                    timer.restart();
                    break;
                case DONE:
                    // do nothing
                    break;
                case INTERRUPT:
                    break;
            }

            currentTrapState = newTrapState;
        }

        switch (currentTrapState) {
            case PREPARE:
                if (inputs.diverterSensor) {
                    newTrapState = TrapStates.WAIT;
                }
                break;
            case WAIT:
                // do nothing
                break;
            case STOWING:
                if (inputs.dunkerSensor || timer.get() > UpperNotePathConstants.kAmpStowingTimeout) {
                    newTrapState = TrapStates.STOWED;
                }
                break;
            case STOWED:
                break;
            case DROP:
                if (inputs.dunkerSensor
                    || timer.get() > UpperNotePathConstants.kTrapDropTimeout)
                {
                    newTrapState = TrapStates.ALMOST_OUT;
                }
                break;
            case ALMOST_OUT:
                if (noteOutOfDunker.getAsBoolean()
                    || timer.get() > UpperNotePathConstants.kTrapAlmostOutTimeout)
                {
                    newTrapState = TrapStates.DONE;
                }
                break;
            case DONE:
                break;
            case INTERRUPT:
                break;
        }
    }

    @Override
    public void reset() {
        newState = UpperNotePathStates.INIT;
        newAmpState = AmpStates.PREPARE;
        newTrapState = TrapStates.PREPARE;
    }

    @Override
    public ManualTestGroup createManualTests() {
        return io.getManualTests();
    }

    public boolean upToSpeed() {
        return upToSpeed;
    }

    public boolean getDunkerSensor() {
        return inputs.dunkerSensor;
    }

    public UpperNotePathStates getState() {
        return currentState;
    }

    public boolean startable() {
        boolean safeInTrap = currentState == UpperNotePathStates.TRAP
                             && (currentTrapState == TrapStates.PREPARE
                                 || currentTrapState == TrapStates.WAIT
                                 || currentTrapState == TrapStates.STOWING
                                 || currentTrapState == TrapStates.STOWED);

        boolean safeInAmp = currentState == UpperNotePathStates.AMP
                            && (currentAmpState == AmpStates.PREPARE);

        return currentState == UpperNotePathStates.IDLE
            || currentState == UpperNotePathStates.SHOOTING
            || safeInTrap || safeInAmp;
    }

    public Command startAmp() {
        return Commands.runOnce(() -> {
            currentState = UpperNotePathStates.INTERRUPT;
            newState = UpperNotePathStates.AMP;
        }).andThen(Commands.waitUntil(() -> currentAmpState == AmpStates.WAIT))
            .withName("StartAmp");
    }

    public Command midAmp() {
        return Commands.runOnce(() -> {
            currentAmpState = AmpStates.INTERRUPT;
            newAmpState = AmpStates.STOWING;
        }).andThen(Commands.waitUntil(() -> currentAmpState == AmpStates.STOWED))
            .withName("MidAmp");
    }

    public Command endAmp() {
        return Commands.runOnce(() -> {
            currentAmpState = AmpStates.INTERRUPT;
            newAmpState = AmpStates.DROP;
        }).andThen(Commands.waitUntil(() -> currentState == UpperNotePathStates.IDLE))
            .withName("EndAmp");
    }

    public Command shoot(ShotType shotType) {
        return Commands.runOnce(() -> {
            this.shotType = shotType;
            currentState = UpperNotePathStates.INTERRUPT;
            newState = UpperNotePathStates.SHOOTING;
        }).andThen(Commands.waitUntil(() -> newState != UpperNotePathStates.SHOOTING))
            .withName("Shoot");
    }

    public Command startTrap() {
        return Commands.runOnce(() -> {
            currentState = UpperNotePathStates.INTERRUPT;
            newState = UpperNotePathStates.TRAP;
        }).andThen(Commands.waitUntil(() -> currentTrapState == TrapStates.WAIT))
            .withName("StartTrap");
    }

    public Command midTrap() {
        return Commands.runOnce(() -> {
            currentTrapState = TrapStates.INTERRUPT;
            newTrapState = TrapStates.STOWING;
        }).andThen(Commands.waitUntil(() -> currentTrapState == TrapStates.STOWED))
            .withName("MidTrap");
    }

    public Command endTrap() {
        return Commands.runOnce(() -> {
            currentTrapState = TrapStates.INTERRUPT;
            newTrapState = TrapStates.DROP;
        }).andThen(Commands.waitUntil(() -> currentState == UpperNotePathStates.IDLE))
            .withName("EndTrap");
    }

    public Command idle() {
        return Commands.runOnce(() -> {
            currentState = UpperNotePathStates.INTERRUPT;
            newState = UpperNotePathStates.IDLE;   
        }).withName("Idle");
    }

    public Command eject() {
        return Commands.runOnce(() -> {
            currentState = UpperNotePathStates.INTERRUPT;
            newState = UpperNotePathStates.PREP_EJECT;
        }).andThen(Commands.waitUntil(() -> newState == UpperNotePathStates.EJECT_SHOOTER
                || newState == UpperNotePathStates.EJECT_DUNKER))
            .withName("Eject");
    }

    @Override
    public boolean getHealth() {
        return inputs.leftShooterConnected && inputs.rightShooterConnected;
    }
}
