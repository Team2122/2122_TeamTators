package frc.robot.subsystems.hardware;

import com.revrobotics.CANSparkMax;
import com.revrobotics.SparkMaxAbsoluteEncoder;
import com.revrobotics.SparkMaxLimitSwitch;
import com.revrobotics.SparkMaxPIDController;
import com.revrobotics.CANSparkMax.ControlType;
import com.revrobotics.CANSparkMax.IdleMode;
import com.revrobotics.CANSparkMaxLowLevel.MotorType;
import com.revrobotics.SparkMaxAbsoluteEncoder.Type;

import common.Util.DigitalSensor;
import edu.wpi.first.math.Matrix;
import edu.wpi.first.math.Nat;
import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.numbers.N1;
import edu.wpi.first.math.numbers.N4;
import edu.wpi.first.math.system.plant.DCMotor;
import edu.wpi.first.wpilibj.DutyCycleEncoder;
import edu.wpi.first.wpilibj.RobotBase;
import edu.wpi.first.wpilibj.smartdashboard.Mechanism2d;
import edu.wpi.first.wpilibj.smartdashboard.MechanismLigament2d;
import edu.wpi.first.wpilibj.smartdashboard.MechanismRoot2d;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import frc.robot.constants.PinkarmConstants;
import frc.robot.constants.PinkarmConstants.ArmExtensionConstants;
import frc.robot.constants.PinkarmConstants.ArmHWConstants;
import frc.robot.constants.PinkarmConstants.ArmRotationConstants;
import frc.robot.subsystems.ArmExtension;
import frc.robot.RobotContainer;

public class PinkarmHW {
	private Matrix<N4, N1> x; // state vector
	private PinkarmSim simulator;
	public final ExtensionMotors extensionMotors;
	public final RotationMotors rotationMotors;
	private double positionSetpoint;
	public double angleSetpoint;
	private final Mechanism2d m_mech2d;
	private final MechanismRoot2d m_armPivot;
	private final MechanismLigament2d m_armTower;
	private boolean out = false;

	public PinkarmHW() {
		simulator = new PinkarmSim();
		x = new Matrix<>(Nat.N4(), Nat.N1());
		double initialAngle = 0;
		double initialExtension = 0;
		double initialAngularVelocity = 0;
		double initialLinearVelocity = 0;
		x.set(0, 0, initialAngle);
		x.set(1, 0, initialExtension);
		x.set(2, 0, initialAngularVelocity);
		x.set(3, 0, initialLinearVelocity);
		extensionMotors = new ExtensionMotors();
		rotationMotors = new RotationMotors();
		positionSetpoint = 0;
		angleSetpoint = 0;
		
		m_mech2d = new Mechanism2d(1, 1);
		m_armPivot = m_mech2d.getRoot("ArmPivot", 0, 0);
		m_armTower = m_armPivot.append(new MechanismLigament2d("ArmTower", 1, 69.1));
		SmartDashboard.putData("Arm Tower", m_mech2d);
	}

	public void disableExtensionLimitSwitch() {
		extensionMotors.disableLimitSwitch_();
	}

	public void enableExtensionLimitSwitch() {
		extensionMotors.enableLimitSwitch_();
	}

	public void setExtensionP(double kP) {
		extensionMotors.leaderController.setP(kP);
	}

	public void updatePhysical_(){
		
		
		

		// rotationMotors.leaderController.
		// TODO IMPL
		// if(out){
		// 	if(extensionMotors.getInches() < ArmConstants.ArmHWConstants.thresholdForNewConstants){
		// 		out = false;
		// 		rotationMotors.configurePID();
		// 		extensionMotors.configurePID();
		// 	}
		// } else {
		// 	if(extensionMotors.getInches() > ArmConstants.ArmHWConstants.thresholdForNewConstants){
		// 		out = true;
		// 		rotationMotors.configurePID();
		// 		extensionMotors.configurePID();
		// 	}
		// }
	}

	public void update_(double dt) {
		simulator.periodic(
			PinkarmConfig.computeVoltageForRotation(
				rotationMotors.degreesToMotorRotations(Math.toDegrees(simulator.getAngleRads())),
				rotationMotors.degreesToMotorRotations(angleSetpoint)
			),
			PinkarmConfig.computeVoltageForExtension(
				extensionMotors.inchesToMotorRotations(simulator.getExtension()),
				extensionMotors.inchesToMotorRotations(positionSetpoint)
			)
		);
		m_armTower.setAngle(Math.toDegrees(simulator.getAngleRads()));
		m_armTower.setLength(simulator.getRawExtension());
		SmartDashboard.putData("Arm Tower", m_mech2d);
		x = computeChangeOfStateEuler();
	}

	public void setExtensionPercent(double percent) {
		extensionMotors.set(percent);
	}

	public void setRotationProportion(double percent) {
		rotationMotors.setProportion(percent);
	}

	public void bumpRotation(double degrees) {
		setSetpointsAngle_(angleSetpoint + degrees);
	}

	public double getAppliedRotationCurrent() {
		return rotationMotors.getAppliedOutput();
	}

	public double getAppliedExtensionCurrent() {
		return extensionMotors.getAppliedOutput();
	}

	public void setSetpointsAngle_(double angleSetpoint) {
		this.angleSetpoint = angleSetpoint;
		rotationMotors.setSetpoint_(angleSetpoint);
	}

	public void setRotationP(double kP) {
		rotationMotors.configureP(kP);
	}

	public double getRotationAngleDegrees() {
		return rotationMotors.getDegrees();
	}

	public double getRotationAngleRadians() {
		return Math.toRadians(rotationMotors.getDegrees());
	}

	public void setHoldExtention(){
		var linearlyInterpolatedAngle = (getRotationAngleDegrees()) / 40;
		extensionMotors.leader.set(PinkarmConstants.ArmExtensionConstants.holdPower * linearlyInterpolatedAngle);
	}

	public void setHoldPowerRetract(){
		extensionMotors.leader.set(PinkarmConstants.ArmExtensionConstants.holdPowerRetact);
	}

	/* Extension Functions */
	public void stopExtension() {
		extensionMotors.stop_();
	}

	public void stopRotation() {
		rotationMotors.stop();
	}

	public double getExtensionMotorRotations() {
		return extensionMotors.getMotorRotations();
	}

	public double getExtensionInches() {
		return extensionMotors.getInches();
	}

	public void setSetpointsExtension_(double positionSetpoint) {
		this.positionSetpoint = positionSetpoint;
		extensionMotors.setSetpoint_(positionSetpoint);
	}

	private Matrix<N4, N1> computeChangeOfStateEuler() {
		return VecBuilder.fill(
			simulator.getAngleRads(),
			simulator.getExtension(),
			simulator.getVelocityRads(),
			simulator.getVelocityExtension()
		);
	}

	public void applyPowerLimitsOnExtension_(){
		extensionMotors.applyPowerLimits_();
	}

	public class ExtensionMotors {
		private final CANSparkMax leader;
		private final CANSparkMax follower;
		private final SparkMaxPIDController leaderController;

		public ExtensionMotors() {
			leader = new CANSparkMax(ArmHWConstants.extensionLeaderID, MotorType.kBrushless);
			follower = new CANSparkMax(ArmHWConstants.extensionFollowerID, MotorType.kBrushless);
			leader.setInverted(true);
			follower.follow(leader, true);
			leaderController = leader.getPIDController();
			leaderController.setOutputRange(-ArmExtensionConstants.kMaxOutput, ArmExtensionConstants.kMaxOutput); //0.3
			leaderController.setP(3.2);
			leaderController.setI(0);
			leaderController.setD(12);
			leader.getEncoder().setPosition(0);
			follower.getEncoder().setPosition(0);
		} 

		private void setSetpoint_(double position) {
			// leaderController.setReference(inchesToMotorRotations(position), ControlType.kPosition,0);
			
			leaderController.setReference(inchesToMotorRotations(position), ControlType.kPosition);

			PinkarmConfig.extentionController.setSetpoint(position);
		}

		private void set(double percent) {
			leader.set(percent);
		}

		private void applyPowerLimits_(){
			if(getInches() < 2){
				leaderController.setOutputRange(-ArmExtensionConstants.kMaxOutputSlow, ArmExtensionConstants.kMaxOutputSlow);
			} else {
				leaderController.setOutputRange(-ArmExtensionConstants.kMaxOutput, ArmExtensionConstants.kMaxOutput); 
			}
		}

		private void disableLimitSwitch_() {
			leader.getForwardLimitSwitch(SparkMaxLimitSwitch.Type.kNormallyOpen).enableLimitSwitch(false);
		}

		private void enableLimitSwitch_() {
			leader.getForwardLimitSwitch(SparkMaxLimitSwitch.Type.kNormallyOpen).enableLimitSwitch(true);
		}

		private void stop_() {
			leader.stopMotor();
			follower.stopMotor();
		}

		public double getAppliedOutput() {
			return leader.getOutputCurrent();
		}

		public double getMotorRotations() {
			return leader.getEncoder().getPosition();
		}

		public double getInches() {
			return RobotBase.isReal() ? motorRotationsToInches(getMotorRotations()) : x.get(1, 0);
		}

		public double inchesToMotorRotations(double inches) {
			return inches * ArmHWConstants.MOTOR_ROTATIONS_PER_INCH;
		}

		public double motorRotationsToInches(double motRots) {
			return motRots * (1/ArmHWConstants.MOTOR_ROTATIONS_PER_INCH);
		}

		public CANSparkMax getLeader() {
			return leader;
		}

		public CANSparkMax getFollower() {
			return follower;
		}
	}

	public class RotationMotors {
		private final CANSparkMax leader;
		private final CANSparkMax follower;
		private final SparkMaxPIDController leaderController;
		private final SparkMaxAbsoluteEncoder absoluteEncoder;
		private final DigitalSensor limitRotationSensor;

		public RotationMotors() {
			leader = new CANSparkMax(ArmHWConstants.rotationLeaderID, MotorType.kBrushless);
			follower = new CANSparkMax(ArmHWConstants.rotationFollowerID, MotorType.kBrushless);
			follower.follow(leader, true);
			leaderController = leader.getPIDController();
			leaderController.setOutputRange(-ArmRotationConstants.kMaxOutput, ArmRotationConstants.kMaxOutput); // -0.2, 0.2
			leaderController.setP(.025);
			leaderController.setI(0);
			leaderController.setD(0);
			absoluteEncoder = leader.getAbsoluteEncoder(Type.kDutyCycle);
			limitRotationSensor = new DigitalSensor(ArmHWConstants.kLimitSensorID);
			// absoluteEncoder.setZeroOffset(angleSetpoint)
			leaderController.setFeedbackDevice(absoluteEncoder);
			/// 38.78947864
			leader.setIdleMode(IdleMode.kBrake);
			follower.setIdleMode(IdleMode.kBrake);
		}

		public void configureP(double kP) {
			leaderController.setP(kP);
		}

		public void setProportion(double percent) {
			leader.set(percent);
		}

		public void setSetpoint_(double position) {

			double modifiedPosition = position - PinkarmConstants.ArmRotationConstants.angleOffset;
			// System.out.println("ModifiedPosition: " + modifiedPosition);
			// // while(true){
				
			// // }
			// if(true){
			// 	throw new RuntimeException(modifiedPosition + " <- modified position " + position + " <-  position");
			// }
			leaderController.setReference(modifiedPosition, ControlType.kPosition);
			PinkarmConfig.rotationController.setSetpoint(modifiedPosition);
		}

		public void stop() {
			leader.stopMotor();
			follower.stopMotor();
		}

		// getters

		private double getDegrees() {
			return RobotBase.isReal() ? absoluteEncoder.getPosition() + PinkarmConstants.ArmRotationConstants.angleOffset : x.get(0, 0);
		}

		private double getAppliedVoltage(){
			return leader.getBusVoltage();
		}

		public double degreesToMotorRotations(double degrees) {
			return degrees * ArmHWConstants.MOTOR_ROTATIONS_PER_DEGREE;
		}

		public double motorRotationsToDegrees(double motRots) {
			return motRots * (1 / ArmHWConstants.MOTOR_ROTATIONS_PER_DEGREE);
		}

		public CANSparkMax getLeader() {
			return leader;
		}

		public CANSparkMax getFollower() {
			return follower;
		}

		public SparkMaxAbsoluteEncoder getAbsoluteEncoder() {
			return absoluteEncoder;
		}

		public DigitalSensor getLimitSensor() {
			return limitRotationSensor;
		}

		public double getAppliedOutput() {
			return leader.getOutputCurrent();
		}
	}

	public static class PinkarmConfig {
		public static final PIDController extentionController = new PIDController(ArmHWConstants.kPExt, ArmHWConstants.kIExt, ArmHWConstants.kDExt);
		public static final PIDController rotationController = new PIDController(ArmHWConstants.kPRot, ArmHWConstants.kIRot, ArmHWConstants.kDRot);

		public static final DCMotor rotationGearbox = DCMotor.getNEO(2).withReduction(ArmHWConstants.reductionForRotation);
		public static final DCMotor extentionGearbox = DCMotor.getNEO(2).withReduction(ArmHWConstants.reductionForExtension);

		private static double calculateMOI(double massM1, double massM2, double massM3, double lengthL1,
				double lengthL2, double lengthL3, double extentionOfCloseStage) {
			// Cylender MOI and parallel axis theorem
			return (massM1 * lengthL1 * lengthL1 +
					massM2 * lengthL2 * lengthL2 +
					massM3 * lengthL3 * lengthL3) / 3.0 +
					massM2 * extentionOfCloseStage * extentionOfCloseStage +
					4 * massM3 * extentionOfCloseStage * extentionOfCloseStage;
		}

		public static double calculateMOI(double extensionOfClosestStage) {
			return calculateMOI(ArmHWConstants.massM1, ArmHWConstants.massM2, ArmHWConstants.massM3, ArmHWConstants.lengthL1, ArmHWConstants.lengthL2, ArmHWConstants.lengthL3, extensionOfClosestStage);
		}

		public static double getMassForExtension() {
			return ArmHWConstants.massM2 + ArmHWConstants.massM3;
		}

		public static double getEndOfArmFromExtension(double extension) {
			return 2 * extension + ArmHWConstants.lengthL1;
		}

		public static double getAppliedForce(double currentSpeedOfMiddleStage, double voltage) {
			var current = rotationGearbox.getCurrent(currentSpeedOfMiddleStage * ArmHWConstants.rotationsPerMeter, voltage);
			return rotationGearbox.getTorque(current);
		}

		public static double getAppliedAngularTorque(double angularVelocityOfArm, double voltage) {
			var current = rotationGearbox.getCurrent(angularVelocityOfArm, voltage);
			return rotationGearbox.getTorque(current);
		}

		public static double computeVoltageForExtension(double currentExtension, double value) {
			return extentionController.calculate(currentExtension, value);
		}

		public static double computeVoltageForRotation(double currentAngle, double value) {
			return rotationController.calculate(currentAngle, value);
		}
	}

}
