package frc.robot.subsystems;

import javax.management.RuntimeErrorException;

import common.Tools.tester.ManualTestGroup;
import common.Tools.tester.components.AnalogInputSensorTest;
import common.Tools.tester.components.DigitalSensorTest;
import common.Tools.tester.components.SolenoidTest;
import common.Tools.tester.components.SpeedControllerTest;
import common.Tools.tester.components.TimeOfFlightTest;
import common.Util.DigitalSensor;
import common.Util.Timer;
import common.teamtators.Subsystem;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.FunctionalCommand;
import edu.wpi.first.hal.simulation.SPIDataJNI;
import frc.robot.RobotContainer;
import frc.robot.RobotContainer.GamePieceTypes;
import frc.robot.RobotContainer.PlacePositions;
import frc.robot.constants.WristConstants.WristPositions;
import frc.robot.subsystems.hardware.ClawHW;
import frc.robot.subsystems.hardware.ClawHW.ClawConfig;

public class Claw extends Subsystem {
	public enum ClawStates {
		INIT,
		IDLE,
		CONE_SUCK, // pulling cone in
		CUBE_SUCK, // pulling cube in
		SPIT_OUT // pushing gamepiece out
	}

	// TODO Need to call constants loading uponInit

	// TODO initialize hardware
	private Timer clawTimer;
	private ClawStates currentState = ClawStates.INIT;
	private ClawStates newState = ClawStates.INIT;
	private GamePieceTypes gamePiece = GamePieceTypes.UNDEFINED;
	private RobotContainer robotContainer;
	private ClawHW clawHW;
	private NetworkTable clawTable;

	public Claw(RobotContainer robotContainer) {
		super(robotContainer);
		this.robotContainer = robotContainer;
		clawHW = new ClawHW();
		clawTable = NetworkTableInstance.getDefault().getTable("Claw");
	}

	@Override
	public void reset() {
		// newState = ClawStates.INIT ;
	}

	@Override
	public void doPeriodic() {

		// if(Math.random() > .99) {
		// 	System.out.println("The game piece type as observed by claw is " + gamePiece);
		// }

		if (currentState != newState) {
			System.out.println("New CLAW state: " + newState);
			switch (newState) {
				case INIT:

					break;

				case IDLE:					
					clawTable.getEntry("state").setString("idle");
					System.out.println("CLAW IS IDLE");
			
					switch(gamePiece) {
						case UNDEFINED:
							if(clawHW.getBreakBeamSensor().get()) {
								clawHW.updateSetpoint_(ClawConfig.kHoldCubePower);
								gamePiece = GamePieceTypes.CUBE;
							} else if (clawHW.getTimeOfFlightSensorDistance() < ClawConfig.kTimeOfFlightConeInThingThreshold) {
								clawHW.updateSetpoint_(ClawConfig.kHoldConePower);
								gamePiece = GamePieceTypes.CONE;
							} else {
								clawHW.updateSetpoint_(0);
							}
							break;
						case CONE:
							clawHW.updateSetpoint_(ClawConfig.kHoldConePower);
							break;
						case CUBE:
							clawHW.updateSetpoint_(ClawConfig.kHoldCubePower);
							break;
					}
					break;
				case CONE_SUCK:
					clawHW.updateSetpoint_(ClawConfig.kSuckConeInSpeed);
					clawTable.getEntry("state").setString("sucking cone");
					System.out.println("CLAW IS SUCKING CONE");
					break;
				case CUBE_SUCK:
					clawHW.updateSetpoint_(ClawConfig.kSuckCubeInSpeed);
					clawTable.getEntry("state").setString("sucking cube");
					System.out.println("CLAW IS SUCKING CUBE");
					break;
				case SPIT_OUT:
					clawHW.updateSetpoint_(gamePiece == GamePieceTypes.CONE ? ClawConfig.kSpitOutSpeed : -ClawConfig.kSpitOutSpeed);
					clawTable.getEntry("state").setString("spitting");
					System.out.println("CLAW IS SPITTING");
					clawTimer = new Timer();
					break;
			}
			currentState = newState;
		}

		switch (currentState) {
			case INIT:
				newState = ClawStates.IDLE;
				break;

			case IDLE: // can be set externally from command
				break;
			case CONE_SUCK: // set externally from command
				if (clawHW.getConeSensedCurrent() && getConeDistance() < ClawConfig.kTimeOfFlightConeInThingThreshold) {
					gamePiece = GamePieceTypes.CONE;
					newState = ClawStates.IDLE;
				}
				break;
			case CUBE_SUCK:
				if(clawHW.breakBeamTriggered()) {
					gamePiece = GamePieceTypes.CUBE;
					newState = ClawStates.IDLE;
				}
				break;
			case SPIT_OUT:
			// System.out.println(clawTimer.get());
				if (!clawTimer.isRunning()) {
					clawTimer.start();
				} else if (clawTimer.get() > .75) {
					clawHW.clearSim();
					newState = ClawStates.IDLE;
					gamePiece = GamePieceTypes.UNDEFINED;
				}
				
				break;
		}

		// clawHW.update_();
		// clawTable.getEntry("gamePiece").setString(gamePiece.toString());
		// clawTable.getEntry("hasGamePiece").setBoolean(hasGamePiece());
		// clawTable.getEntry("coneDistance").setDouble(getConeDistance());
		// clawTable.getEntry("motorCurrent").setDouble(clawHW.getMotorSpeed());
	}

	public void setNewState(ClawStates state) {
		newState = state;
	}

	public double getConeDistance() {
		return clawHW.getTimeOfFlightSensorDistance();
	}

	public void setGamePieceType(GamePieceTypes type) {
		gamePiece = type;
	}

	public ClawStates getCurrentState() {
		return currentState;
	}

	public GamePieceTypes getGamePiece() {
		return gamePiece;
	}

	public boolean hasGamePiece() {
		return gamePiece == GamePieceTypes.CONE || gamePiece == GamePieceTypes.CUBE;
	}

	public WristPositions getPlacePosition(PlacePositions placePosition) {
		switch(placePosition) {
			case HIGH:
			return gamePiece == GamePieceTypes.CONE ? WristPositions.PLACE_LOW_CONE : WristPositions.PLACE_LOW_CUBE;
			
			case MID:
			return gamePiece == GamePieceTypes.CONE ? WristPositions.PLACE_MID_CONE : WristPositions.PLACE_MID_CUBE;
		
			case LOW:
			return gamePiece == GamePieceTypes.CONE ? WristPositions.PLACE_LOW_CONE : WristPositions.PLACE_LOW_CUBE;
			
			default:
			return WristPositions.TRANSPORT; // this is impossible to reach, but i have to put because java is dumb
		}
	}

	@Override
	public ManualTestGroup createManualTests() {
		ManualTestGroup tests = new ManualTestGroup(getName());
		tests.addTest(new DigitalSensorTest("Break beam sensor test", clawHW.getBreakBeamSensor()));
		tests.addTest(new TimeOfFlightTest("Time of flight Sensor Test", clawHW.getTimeOfFlightSensor()));
		tests.addTest(new SpeedControllerTest("Claw Motor Test", clawHW.getClawMotor(), 0.5));
		return tests;
	}

    // Command Factories
    public Command suck(GamePieceTypes piece) {
        if(piece.equals(GamePieceTypes.UNDEFINED)) {
            throw new RuntimeException("You should not use claw.suck(UNDEFINED).\nPlease use an explicitly defined piece for this command");
            // throwing an exception is fine because piece is only defined explicitly, not dynamically by some indeterminant function
            // so we will never crash when it's important, just when a person is doing something stupid when testing
        }
        
        return new FunctionalCommand(
            () ->
                setNewState(piece.equals(GamePieceTypes.CONE)
                    ? ClawStates.CONE_SUCK
                    : ClawStates.CUBE_SUCK), // onInit
            () -> {}, // onExecute,
            bool -> {}, // onEnd,
            this::hasGamePiece, // isFinished,
            this // requirements
        );
    }

    public Command spit() {
        return new FunctionalCommand(
            () -> setNewState(ClawStates.SPIT_OUT),
            () -> {},
            bool -> {},
            () -> !hasGamePiece(),
            this
        );
    }

    public Command stop() {
        return this.runOnce(() -> setNewState(ClawStates.IDLE));
    }
}
