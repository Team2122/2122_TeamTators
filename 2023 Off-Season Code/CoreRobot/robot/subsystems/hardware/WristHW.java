package frc.robot.subsystems.hardware;

import com.revrobotics.CANSparkMax;
import com.revrobotics.SparkMaxAbsoluteEncoder;
import com.revrobotics.CANSparkMax.ControlType;
import com.revrobotics.CANSparkMax.IdleMode;
import com.revrobotics.CANSparkMaxLowLevel.MotorType;
import com.revrobotics.SparkMaxAbsoluteEncoder.Type;
import com.revrobotics.SparkMaxPIDController.ArbFFUnits;
import com.revrobotics.SparkMaxPIDController;

import edu.wpi.first.math.Matrix;
import edu.wpi.first.math.Nat;
import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.numbers.N1;
import edu.wpi.first.math.numbers.N2;
import edu.wpi.first.math.system.plant.DCMotor;
import edu.wpi.first.wpilibj.DutyCycle;
import edu.wpi.first.wpilibj.DutyCycleEncoder;
import frc.robot.RobotContainer;
import frc.robot.constants.WristConstants;
import frc.robot.constants.WristConstants.WristPositions;

public class WristHW {

    private Matrix<N2, N1> x;
    public final RotationMotor rotationMotor;
    private double angleSetpoint;

    public WristHW() {
        x = new Matrix<>(Nat.N2(), Nat.N1());
        double initialAngle = 0;
        double initialAngularVelocity = 0;
        x.set(0, 0, initialAngle);
        x.set(1, 0, initialAngularVelocity);
        rotationMotor = new RotationMotor();
        angleSetpoint = 0;
    }

	public double getSimulatedAngle() {
		return x.get(0, 0);
	}

    public double getOutputCurrent() {
        return rotationMotor.getOutputCurrent();
    }
    
    public void updateState_(double dt) {
        x = computeChangeOfStateEuler(x, angleSetpoint, dt);
    }

    public void setMotorOutput(double output) {
        rotationMotor.set(output);
    }

    public void updateSetpoint_(double angleSetpoint) {
        this.angleSetpoint = angleSetpoint;
        rotationMotor.setSetpoint_(angleSetpoint);
    }

    public void setP(double p) {
        rotationMotor.setP(p);
    }

    public void printState() {
        System.out.println("wrist angle: " + x.get(0, 0) + "wrist angular velocity: " + x.get(1, 0));
    }

    public Matrix<N2, N1> computeChangeOfStateEuler(Matrix<N2, N1> x, double angleSetpoint,
                                                    double dt) {
        Matrix<N2, N1> out = new Matrix<>(x.getStorage());

        double angle = x.get(0, 0);

        double calculatedVoltageRotation = WristConfig.computeVoltageForRotation(angle, angleSetpoint);

        double appliedTorque = WristConfig.getAppliedAngularTorque(x.get(1, 0), calculatedVoltageRotation);

        // drift of rotation??
        Matrix<N2, N1> drift = VecBuilder.fill(
                0,
                -9.81 * WristConfig.wristMOI * Math.sin(angle) * dt);

        return out.plus(drift).plus(
                VecBuilder.fill(
                        x.get(1, 0) * dt,
                        appliedTorque / WristConfig.wristMOI
                ));

    }

    public class RotationMotor {
        private final CANSparkMax motor;
        private final SparkMaxPIDController motorController;
        private final SparkMaxAbsoluteEncoder rotationEncoder;


        public RotationMotor() {
            motor = new CANSparkMax(WristConstants.kWristMotorID, MotorType.kBrushless);
            motorController = motor.getPIDController();
            motorController.setP(WristConfig.kPRot);
            motorController.setI(WristConfig.kIRot);
            motorController.setD(WristConfig.kDRot);
            rotationEncoder = motor.getAbsoluteEncoder(Type.kDutyCycle);
            motorController.setOutputRange(-0.40, 0.40);
            motorController.setFeedbackDevice(rotationEncoder);
            motor.setIdleMode(IdleMode.kBrake);
            motor.setInverted(true);
        }

        public void set(double speed) {
            motor.set(speed);
        }

        public void setSetpoint_(double position) {
            motorController.setReference(position, ControlType.kPosition);
        }

        public CANSparkMax getWristMotor() {
            return motor;
        }

        public SparkMaxAbsoluteEncoder getRotationEncoder() {
            return rotationEncoder;
        }

        public double getOutputCurrent() {
            return motor.getOutputCurrent();
        }

        public double getAngleDegrees(){
            return rotationEncoder.getPosition();
        }

        public void setP(double p) {
            motorController.setP(p);
        }
    }

    // All of the following units are SI
    public static class WristConfig {
        public static final double wristMOI = 1;

        public static final double kPRot = 1.0;
        public static final double kIRot = 0;
        public static final double kDRot = 0;

        public static final PIDController rotationController = new PIDController(kPRot, kIRot, kDRot);

        public static final double reductionForRotation = 10;

        public static final DCMotor rotationGearbox = DCMotor.getNeo550(1).withReduction(reductionForRotation);

        public static final double gravityAngle = -Math.PI / 2;

        private static double calculateMOI(double massM1, double massM2, double massM3, double lengthL1,
                                           double lengthL2, double lengthL3, double extentionOfCloseStage) {
            // Cylender MOI and parallel axis theorem
            return 1;
        }

        public static double calculateMOI() {
            return wristMOI;
        }
        

        public static double getAppliedAngularTorque(double angularVelocityOfArm, double voltage) {
            var current = rotationGearbox.getCurrent(angularVelocityOfArm, voltage);
            return rotationGearbox.getTorque(current);
        }

        public static double computeVoltageForRotation(double currentAngle, double value) {
            return rotationController.calculate(currentAngle, value);
        }

    }

}
