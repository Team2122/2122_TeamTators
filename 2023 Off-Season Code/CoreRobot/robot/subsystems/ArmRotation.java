package frc.robot.subsystems;

import common.Tools.tester.ManualTestGroup;
import common.Tools.tester.components.CANEncoderTest;
import common.Tools.tester.components.DigitalSensorTest;
import common.Tools.tester.components.DutyCycleEncoderTest;
import common.Tools.tester.components.SparkMaxAbsoluteEncoderTest;
import common.Tools.tester.components.SpeedControllerTest;
import common.Tools.tester.components.StateMachineTest;
import common.teamtators.Subsystem;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.FunctionalCommand;

// import org.common.teamtators.Subsystem;

import frc.robot.RobotContainer;
import frc.robot.RobotContainer.PlacePositions;
import frc.robot.constants.PinkarmConstants.ArmRotationConstants;
import frc.robot.RobotContainer.GamePieceTypes;
import frc.robot.subsystems.hardware.PinkarmHW;
import frc.robot.constants.PinkarmConstants;

// public class ArmRotation extends Subsystem {
public class ArmRotation extends Subsystem {
	enum SubsystemState {
		INIT,
		IN_POSITION,
		MOVING,
		WAITING
	}

	public enum RotationPosition {
		// each member is associated with a number representing the degrees of
		// that member
		HOME(ArmRotationConstants.idleDegrees, -.02,.01),
		FLOOR_PICK_CUBE(ArmRotationConstants.floorPickCubeDegrees, -.05,.01),
		FLOOR_PICK_CONE(ArmRotationConstants.floorPickConeDegrees, -.05,.01),
		HIGH_PICK(ArmRotationConstants.highPickDegrees,.03),
		HYBRID_PLACE(ArmRotationConstants.hybridPlaceDegrees, -.05, .01),
		MID_PLACE_CONE(ArmRotationConstants.midPlaceConeDegrees,  ArmRotationConstants.midPlaceConeP),
		MID_PLACE_CUBE(ArmRotationConstants.midPlaceCubeDegrees,  ArmRotationConstants.midPlaceCubeP),
		HIGH_PLACE_CONE(ArmRotationConstants.highPlaceConeDegrees,  ArmRotationConstants.highPlaceConeP),
		AUTO_HIGH_PLACE_CONE(ArmRotationConstants.autoHighPlaceConeDegrees,  ArmRotationConstants.highPlaceConeP),
		HIGH_PLACE_CUBE(ArmRotationConstants.highPlaceCubeDegrees,  ArmRotationConstants.highPlaceConeP);

		private double positionDegrees;
		private double holdPower = 0;
		private double kP = 0;

		RotationPosition(double positionDegrees, double kP) {
			this.positionDegrees = positionDegrees;
			this.kP = kP;
		}

		RotationPosition(double positionDegrees, double holdPower, double kP) {
			this.positionDegrees = positionDegrees;
			this.holdPower = holdPower;
			this.kP = kP;
		}


		public double getDegrees() {
			return positionDegrees;
		}

		public double getHoldPower() {
			return holdPower;
		}

		public double getP() {
            return kP;
        }
	
	}

	// need to be different so the switch to INIT code is run
	private SubsystemState currentState = SubsystemState.IN_POSITION;
	private SubsystemState newState = SubsystemState.INIT;

	private RotationPosition currentPosition = RotationPosition.HOME;
	private RotationPosition desiredPosition = RotationPosition.HOME;

	private PinkarmHW armHw;
	private Claw claw;

	private boolean waitingEnabled = PinkarmConstants.waitingEnabled;
	private boolean fromInit = false; // don't do stuff immediately upon startup

	public ArmRotation(RobotContainer robotContainer) {
		super(robotContainer);
		this.armHw = robotContainer.getArmHW();
		this.claw = robotContainer.getClaw();
	}

	@Override
	public void doPeriodic() {

		// if(Math.random() > .9){
		// 	System.out.println("Arm State: " + currentState + " Applied setPoint: "+ armHw.angleSetpoint);
		// }

		if (currentState != newState) {
			switch (newState) {
				case INIT:
					// fromInit = true;
					newState = SubsystemState.IN_POSITION;
					break;
				case IN_POSITION:
					System.out.println("ARM ROTATION IS NOW IN POSITION");
					// armHw.stopRotation();
					currentPosition = desiredPosition;
					// if (desiredPosition.kP == 0) {
					if((currentPosition.holdPower)!=0){
						armHw.setRotationProportion(currentPosition.holdPower);
					}
					// 	// throw new RuntimeException("" + currentPosition.holdPower);
					// }
					// else {
					// 	armHw.setRotationP(currentPosition.kP);
					// 	armHw.setSetpointsAngle_(currentPosition.getDegrees());
					// }
					break;
				case MOVING:
					System.out.println("ARM ROTATION IS NOW MOVING TO " + desiredPosition);
					armHw.setRotationP(desiredPosition.kP);
					armHw.setSetpointsAngle_(desiredPosition.getDegrees());
					break;
				case WAITING:
					// armHw
					System.out.println("ARM ROTATION IS NOW WAITING");
					// armHw.stopRotation();
					break;
			}
			currentState = newState;
		} else {
			switch (currentState) {
				case INIT:
					// everything taken care of during the change to INIT. this block will never be
					// run.
					break;
				case IN_POSITION:
					break;
				case MOVING:
					if(!isSafe() && waitingEnabled)
						newState = SubsystemState.WAITING;

					// prioritize being IN_POSITION over going to WAITING
					if (inPosition()) {
						newState = SubsystemState.IN_POSITION;
					}
					break;
				case WAITING:
					if(isSafe())
						newState = SubsystemState.MOVING;
					break;
			}
		}

		// you may notice this is not in the extension subsystem.
		// not a problem. this should only be called once per tick,
		// so calling from extension would be useless
		// armHw.update_(.02);
	}

	public void reset() {
		newState = SubsystemState.INIT;
	}

	/**
	 * Move the arm up a little bit
	 */
	public void bumpUp() {
		armHw.setRotationProportion(.219);
	}

	/**
	 * Move the arm down a little bit
	 */
	public void bumpDown() {
		armHw.setRotationProportion(.03);
	}

	public void thouShallHalt() {
		armHw.setRotationProportion(.08);
	}

	public double getArmThetaDegrees() {
		return armHw.getRotationAngleDegrees();
	}

	/* Helper Functions */
	public RotationPosition getCurrentPosition() {
		return currentPosition;
	}

	public RotationPosition getDesiredPosition() {
		return desiredPosition;
	}

	public void setDesiredPosition(RotationPosition position) {
		fromInit = false;
		this.desiredPosition = position;
		newState = SubsystemState.MOVING;

		// HACK
		if(currentState == SubsystemState.MOVING){
			armHw.setSetpointsAngle_(desiredPosition.getDegrees());
		}
	}

	public SubsystemState getCurrentState() {
		return currentState;
	}

	public SubsystemState getNewState() {
		return newState;
	}

	public boolean inPosition() {
		return Math.abs(armHw.getRotationAngleDegrees() - desiredPosition.positionDegrees) < ArmRotationConstants.error;
	}

	public boolean isSafe() {
		// FIXME this should check extension + wrist rotation
		return true;
	}

	@Override
	public ManualTestGroup createManualTests() {
		return new ManualTestGroup(
			"Arm Rotation",
			new DigitalSensorTest("Arm Rotation Limit Sensor Test", armHw.rotationMotors.getLimitSensor()),
			new StateMachineTest<>("Arm Rotation Position Test", RotationPosition.class, this, this::setDesiredPosition),
			new SpeedControllerTest("Arm Rotation Motor Test", armHw.rotationMotors.getLeader(), 10),
			new SparkMaxAbsoluteEncoderTest("Arm Rotation Throughbore Test", armHw.rotationMotors.getAbsoluteEncoder()),
			new CANEncoderTest("Arm Rotation Leader Relative Encoder Test", armHw.rotationMotors.getLeader().getEncoder()),
			new CANEncoderTest("Arm Rotation Follwer Relative Encoder Test", armHw.rotationMotors.getFollower().getEncoder())
		);
	}

	public RotationPosition getPlacePosition(PlacePositions placePosition) {
		switch(placePosition) {
			case HIGH:
			return claw.getGamePiece() == GamePieceTypes.CONE ? RotationPosition.HIGH_PLACE_CONE : RotationPosition.HIGH_PLACE_CUBE;
			
			case MID:
			return claw.getGamePiece() == GamePieceTypes.CONE ? RotationPosition.MID_PLACE_CONE : RotationPosition.MID_PLACE_CUBE;
		
			case LOW:
			return RotationPosition.HYBRID_PLACE;

			case AUTO_HIGH:
            return claw.getGamePiece() == GamePieceTypes.CONE ? RotationPosition.AUTO_HIGH_PLACE_CONE
						: RotationPosition.HIGH_PLACE_CUBE;
			
			default:
			return RotationPosition.HOME; // this is impossible to reach, but i have to put because java is dumb
		}
	}

    // Command Factories
    public Command toPosition(RotationPosition position) {
        return new FunctionalCommand(
            () -> setDesiredPosition(position), // onInit,
            () -> {}, // onExecute,
            bool -> {}, // onEnd,
            this::inPosition, // isFinished,
            this // requirements
        );
    }

    public Command toNode(PlacePositions position) {
        return new FunctionalCommand(
            () -> setDesiredPosition(getPlacePosition(position)), // onInit,
            () -> {}, // onExecute,
            bool -> {}, // onEnd,
            this::inPosition, // isFinished,
            this // requirements
        );
    }

}
