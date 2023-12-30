package frc.robot.subsystems;

import java.util.function.BooleanSupplier;

import com.revrobotics.CANSparkMax;
import com.revrobotics.RelativeEncoder;
import com.revrobotics.SparkMaxAbsoluteEncoder;

import common.Tools.tester.ManualTestGroup;
import common.Tools.tester.components.CANEncoderTest;
import common.Tools.tester.components.SparkMaxAbsoluteEncoderTest;
import common.Tools.tester.components.SpeedControllerTest;
import common.Tools.tester.components.StateMachineTest;
import common.teamtators.Subsystem;
import frc.robot.RobotContainer.GamePieceTypes;
import edu.wpi.first.wpilibj.RobotBase;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.FunctionalCommand;
import frc.robot.RobotContainer;
import frc.robot.RobotContainer.PlacePositions;
import frc.robot.constants.WristConstants;
import frc.robot.constants.WristConstants.WristPositions;


public class Wrist extends Subsystem {
    public enum WristStates {
        INIT,
        IN_POSITION,
        MOVING,
        WAITING
    }

    private frc.robot.subsystems.hardware.WristHW wristHW = new frc.robot.subsystems.hardware.WristHW();
    private WristStates currentState = WristStates.INIT;
    private WristStates newState = WristStates.INIT;
    private RobotContainer robotContainer;
    private WristPositions currentPosition = WristPositions.TRANSPORT;
    private WristConstants.WristPositions desiredPosition = WristPositions.TRANSPORT;
    private ArmRotation armRotation;
    private ArmExtension armExtension;
    private Claw claw;
    private CANSparkMax rotationMotor;
    private RelativeEncoder relativeEncoder;
    private SparkMaxAbsoluteEncoder absoluteEncoder;
    
    public Wrist(RobotContainer robotContainer) {
        super(robotContainer);
        //rotationEncoder.setInverted(true);
        //rotationEncoder.reset();
        this.robotContainer = robotContainer;
        this.armRotation = robotContainer.getArmRotation();
        this.armExtension = robotContainer.getArmExtension();
        this.rotationMotor = wristHW.rotationMotor.getWristMotor();
        this.relativeEncoder = rotationMotor.getEncoder();
        this.absoluteEncoder = wristHW.rotationMotor.getRotationEncoder();
        this.claw = robotContainer.getClaw();
    }



    @Override
    public void doPeriodic() {
                
        // Set to true to run the wrist simulation
        // if (false) {
        //     wristHW.updateState_(0.02);
        //     // wristHW.printState();
        // }

        if (currentState != newState) {
            System.out.println("The wrist state is in " + currentState);

            switch (newState) {
                case INIT:
                    break;

                case IN_POSITION:
                    currentPosition = desiredPosition;
                    // rotationMotor.set(0.0);
                    rotationMotor.set(currentPosition.getHoldPower());
                    System.out.println("IN_POSITION: Setting hold power to " + currentPosition.getHoldPower());
                    break;

                case MOVING:
                    // if (getCurrentWristPosition() < desiredPosition.getDegrees()) {
                    //     // wristHW.setMotorOutput(WristConstants.kGoingDownOutput);
                    // }
                    // else {
                    //     // wristHW.setMotorOutput(WristConstants.kGoingUpOutput);
                    // }

                    wristHW.setP(desiredPosition.getP());
                    wristHW.updateSetpoint_(desiredPosition.getDegrees());
                    
                    break;

                case WAITING:
                    // Currently this condition is never hit
                    rotationMotor.set(0.0);

                    break;

            }
            currentState = newState;
        }

        // System.out.println("Current State Wrist: " + currentState);
        // System.out.println("Desired Position: " + desiredPosition);
        switch (currentState) {
            case INIT:
                newState = WristStates.IN_POSITION;
                break;

            case IN_POSITION:
                break;

            case MOVING:
                if (inPosition()) {
                    rotationMotor.set(0);
                    newState = WristStates.IN_POSITION;
                }

                break;

            case WAITING:
                // Currently this condition is never hit
                switch (desiredPosition) {
                    case SHELF_PICK:
                        if (armExtension.getCurrentPosition() == ArmExtension.ExtensionPosition.HOME) {
                            newState = WristStates.MOVING;
                        }
                        break;

                    case TRANSPORT:
                        if (armRotation.getCurrentPosition() != ArmRotation.RotationPosition.HOME) {
                            newState = WristStates.MOVING;
                        }
                        break;
                    
                    default:
                        break;
                }
                break;
        }
    }



    // REMOVED DUE TO SAFTEY
    // public void setRotation(States state) {
    //     newState = state;
    // }

    // Gives you your current wrist position in degrees
    private double getCurrentWristPosition() {
        return RobotBase.isReal() ? absoluteEncoder.getPosition() : wristHW.getSimulatedAngle();
    }

    public WristPositions getDesiredPosition() {
        return desiredPosition;
    }

    public void setDesiredWristRotation(WristPositions position) {
        newState = WristStates.MOVING;
        desiredPosition = position;
        wristHW.updateSetpoint_(desiredPosition.getDegrees());
    }

    public WristStates getCurrentState() {
        return currentState;
    }

	public boolean inPosition() {
		return Math.abs(getCurrentWristPosition()-desiredPosition.getDegrees()) <= WristConstants.kDesiredPositionErrorDegrees;
	}

    // Unnecessary for now 09/16/2023
    // public boolean isSafeToRotateWrist() {
    //     // Code to verify the wrist is safe to rotate
    //     if (robotContainer.getClaw().getClawSensorState() == true) {
    //         // if ( ( robotContainer.getSubsystems().getArmRotation().getArmTheta() > -(Math.PI/6) ) && 
    //         //      ( robotContainer.getSubsystems().getArmRotation().getArmTheta() < (Math.PI/6) ) ) {
    //         //     return true ;
    //         // } else {
    //         //     System.out.println( "WRIST ERROR : Object in Claw, not safe to rotate wrist");
    //         //     return false ;
    //         // }
    //         return true;

    //     } else {
    //         return true;
    //     }
    // }


    @Override
    public ManualTestGroup createManualTests() {
        ManualTestGroup tests = new ManualTestGroup(getName());
        tests.addTest(new StateMachineTest<>("Wrist Position Test", WristPositions.class, this, this::setDesiredWristRotation));
        tests.addTest(new SpeedControllerTest("Rotation Motor test: ", rotationMotor, 0.35));
        tests.addTest(new CANEncoderTest("Rotation relative encoder test", relativeEncoder));
        tests.addTest(new SparkMaxAbsoluteEncoderTest("Rotation Absolute Encoder Test", absoluteEncoder));
        return tests;
    }

    public void reset() {
        currentState = WristStates.IN_POSITION;
        newState = WristStates.IN_POSITION;

        desiredPosition = WristPositions.TRANSPORT;
    }

    
	public WristPositions getPlacePosition(PlacePositions placePosition) {
		switch(placePosition) {
			case HIGH:
			return claw.getGamePiece() == GamePieceTypes.CONE ? WristPositions.PLACE_HIGH_CONE : WristPositions.PLACE_HIGH_CUBE;
			
			case MID:
			return claw.getGamePiece() == GamePieceTypes.CONE ? WristPositions.PLACE_MID_CONE : WristPositions.PLACE_MID_CUBE;
		
			case LOW:
			return claw.getGamePiece() == GamePieceTypes.CONE ? WristPositions.PLACE_LOW_CONE : WristPositions.PLACE_LOW_CUBE;
			
            case AUTO_HIGH:
            return claw.getGamePiece() == GamePieceTypes.CONE ? WristPositions.PLACE_HIGH_CONE : WristPositions.PLACE_HIGH_CUBE;
			
            default:
			return WristPositions.TRANSPORT; // this is impossible to reach, but i have to put because java is dumb
		}
	}

    // Command Factories
    public Command toPosition(WristPositions wristPosition) {
        return new FunctionalCommand(
            () -> setDesiredWristRotation(wristPosition), // onInit,
            () -> {}, // onExecute,
            bool -> {}, // onEnd,
            this::inPosition, // isFinished,
            this // requirements
        );
    }

    public Command toNode(PlacePositions position) {
        return new FunctionalCommand(
            () -> setDesiredWristRotation(getPlacePosition(position)), // onInit,
            () -> {}, // onExecute,
            bool -> {}, // onEnd,
            this::inPosition, // isFinished,
            this // requirements
        );
    }

}
