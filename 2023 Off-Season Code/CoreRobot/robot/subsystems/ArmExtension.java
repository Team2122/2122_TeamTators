package frc.robot.subsystems;

import javax.management.RuntimeErrorException;

import com.ctre.phoenix6.signals.MotionMagicIsRunningValue;

import ch.qos.logback.core.joran.conditional.ElseAction;
import common.Tools.tester.ManualTestGroup;
import common.Tools.tester.components.CANEncoderTest;
import common.Tools.tester.components.DigitalSensorTest;
import common.Tools.tester.components.SpeedControllerTest;
import common.Tools.tester.components.StateMachineTest;
import common.teamtators.Subsystem;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.FunctionalCommand;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

// import org.common.teamtators.Subsystem;

import frc.robot.RobotContainer;
import frc.robot.RobotContainer.PlacePositions;
import frc.robot.constants.PinkarmConstants.ArmExtensionConstants;
import frc.robot.constants.WristConstants.WristPositions;
import frc.robot.RobotContainer.GamePieceTypes;
import frc.robot.subsystems.hardware.PinkarmHW;
import frc.robot.constants.PinkarmConstants;

// public class ArmExtension extends Subsystem {
public class ArmExtension extends Subsystem {
	public enum SubsystemState {
		INIT,
		IN_POSITION,
		MOVING,
		WAITING
	}

	public enum ExtensionPosition {
		// each member is associated with a number representing the motor rotations of
		// that member
		// the extensions are for each INDIVIDUAL motor, so just take that into account
		// because there are two motors total
		HOME(ArmExtensionConstants.idleInches, ArmExtensionConstants.defaultP),
		FLOOR_PICK_CUBE(ArmExtensionConstants.floorPickCubeInches, ArmExtensionConstants.defaultP),
		FLOOR_PICK_CONE(ArmExtensionConstants.floorPickConeInches, ArmExtensionConstants.defaultP),
		HIGH_PICK(ArmExtensionConstants.highPickInches, ArmExtensionConstants.defaultP),
		HYBRID_PLACE(ArmExtensionConstants.hybridPlaceInches, ArmExtensionConstants.defaultP),
		MID_PLACE_CONE(ArmExtensionConstants.midPlaceConeInches, ArmExtensionConstants.midPlaceConeP),
		MID_PLACE_CUBE(ArmExtensionConstants.midPlaceCubeInches, ArmExtensionConstants.midPlaceCubeP),
		HIGH_PLACE_CONE(ArmExtensionConstants.highPlaceConeInches, ArmExtensionConstants.highPlaceConeP),
		HIGH_PLACE_CUBE(ArmExtensionConstants.highPlaceCubeInches, ArmExtensionConstants.highPlaceCubeP);

		private double positionInches;
		private double kP;

		ExtensionPosition(double positionInches, double kP) {
			this.positionInches = positionInches;
			this.kP = kP;
		}

		public double getInches() {
			return positionInches;
		}

		public double getP() {
			return kP;
		}
	}

	// need to be different so the switch to INIT code is run
	private SubsystemState currentState = SubsystemState.IN_POSITION;
	private SubsystemState newState = SubsystemState.INIT;

	private ExtensionPosition currentPosition = ExtensionPosition.HOME;
	private ExtensionPosition desiredPosition = ExtensionPosition.HOME;

	private PinkarmHW armHw;
	private Wrist wrist;
	private Claw claw;

	private boolean waitingEnabled = PinkarmConstants.waitingEnabled;
	private boolean fromInit = false; // to make sure we don't just start doing stuff immediately

	public ArmExtension(RobotContainer robotContainer) {
		super(robotContainer);
		this.armHw = robotContainer.getArmHW();
		this.claw = robotContainer.getClaw();
	}

	@Override
	public void doPeriodic() {
		if (currentState != newState) {
			switch (newState) {
				case INIT:
					fromInit = false;
					newState = SubsystemState.IN_POSITION;
					break;

				case IN_POSITION:
					System.out.println("ARM EXTENSION IN POSITION");
					// armHw.stopExtension();
					WristPositions wristPosition;
					if (wrist != null)
						wristPosition = wrist.getDesiredPosition();
					else
						wristPosition = null;

					if (desiredPosition.positionInches < PinkarmConstants.ArmExtensionConstants.idleInches
					 + PinkarmConstants.ArmExtensionConstants.error*1.2
					   && (wristPosition == WristPositions.FLOOR_PICK || wristPosition == WristPositions.TRANSPORT)) {
						// set the hold power to keep the extension in
						System.out.println("THE ARM EXTENSION IS HOLDING RETRACT POWER");
						armHw.disableExtensionLimitSwitch();
						armHw.setHoldPowerRetract();
						System.out.println("Arm Extension limit switch is now disabled");
					} else {
						// set the hold power to keep the extension where it is
						armHw.enableExtensionLimitSwitch();
						armHw.setHoldExtention();
						System.out.println("Arm Extension limit switch is now enabled");
					}

					currentPosition = desiredPosition;
					break;

				case MOVING:
					System.out.println("ARM EXTENSION MOVING TO " + desiredPosition);
					armHw.setSetpointsExtension_(desiredPosition.getInches());
					break;

				case WAITING:
					System.out.println("ARM EXTENSION WAITING");
					// armHw.stopExtension();
					armHw.setHoldExtention();

					break;
			}
			currentState = newState;
		}

		switch (currentState) {
				case INIT:
					// everything taken care of during the change to INIT. this block will never be
					// run.
					break;

				case IN_POSITION:
					break;

				case MOVING:
				armHw.applyPowerLimitsOnExtension_();

					if (!isSafe() && waitingEnabled){
						newState = SubsystemState.WAITING;}

					// prioritize being in position over going to waiting
					if (inPosition()){
						newState = SubsystemState.IN_POSITION;}
					break;

				case WAITING:
					if (isSafe())
						newState = SubsystemState.MOVING;
					break;
		}
	}

	public void reset() {
		newState = SubsystemState.INIT;
	}

	/* Helper Functions */
	public ExtensionPosition getCurrentPosition() {
		return currentPosition;
	}

	public ExtensionPosition getDesiredPosition() {
		return desiredPosition;
	}

	public void setDesiredPosition(ExtensionPosition position) {
		fromInit = false;
		this.desiredPosition = position;
		armHw.setExtensionP(desiredPosition.kP);
		if(currentState == SubsystemState.MOVING)
			armHw.setSetpointsExtension_(desiredPosition.getInches());
		newState = SubsystemState.MOVING;
	}

	public SubsystemState getCurrentState() {
		return currentState;
	}

	public SubsystemState getNewState() {
		return newState;
	}

	public boolean inPosition() {
		// System.out.println("Arm extension difference: " + (armHw.getExtensionInches() - desiredPosition.getInches()));
		return Math.abs(armHw.getExtensionInches() - desiredPosition.getInches()) < ArmExtensionConstants.error;
	}

	public boolean isSafe() {
		// FIXME this should check extension + wrist rotation
		return true;
	}

	// wrist is instantiated after arm, but wrist is dependent on arm stuff,
	// so we just supply the wrist externally
	public void giveWrist(Wrist wrist) {
		this.wrist = wrist;
	}

	@Override
	public ManualTestGroup createManualTests() {
		return new ManualTestGroup(
				"Arm Extension",
				new SpeedControllerTest("Arm Extension Motor Test", armHw.extensionMotors.getLeader(), .3),
				new StateMachineTest<>("Arm Extension Position Test", ExtensionPosition.class, this,
						this::setDesiredPosition),
				new CANEncoderTest("Arm Rotation Leader Relative Encoder Test",
						armHw.extensionMotors.getLeader().getEncoder()),
				new CANEncoderTest("Arm Rotation Follwer Relative Encoder Test",
						armHw.extensionMotors.getFollower().getEncoder())

		);
	}

	public ExtensionPosition getPlacePosition(PlacePositions placePosition) {
		switch (placePosition) {
			case HIGH:
				return claw.getGamePiece() == GamePieceTypes.CONE ? ExtensionPosition.HIGH_PLACE_CONE
						: ExtensionPosition.HIGH_PLACE_CUBE;

			case MID:
				return claw.getGamePiece() == GamePieceTypes.CONE ? ExtensionPosition.MID_PLACE_CONE
						: ExtensionPosition.MID_PLACE_CUBE;

			case LOW:
				return ExtensionPosition.HYBRID_PLACE;
			
			case AUTO_HIGH:
            return claw.getGamePiece() == GamePieceTypes.CONE ? ExtensionPosition.HIGH_PLACE_CONE
						: ExtensionPosition.HIGH_PLACE_CUBE;
			

			default:
				return ExtensionPosition.HOME; // this is impossible to reach, but i have to put because java is dumb
		}
	}

    // Command Factories
    public Command toPosition(ExtensionPosition position) {
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
