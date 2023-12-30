package frc.robot.subsystems.hardware;

import edu.wpi.first.math.Matrix;
import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.math.numbers.N1;
import edu.wpi.first.math.numbers.N4;
import edu.wpi.first.math.system.plant.DCMotor;

public class PinkarmSim {


	public static class ArmConfig {
		public static double mass1 = 1;
		public static double mass2 = 8;
		public static double length1 = 1;
		public static double length2 = 10;
		public static double motorRotationsPerArmRotation = 100;

	}

	private VariableLengthArmSim armRotationSim;
	private ExtensionSim armExtensionSim;
	private double j;
	private double extension = 0;


	public PinkarmSim() {
		j = 1.0 / 3.0 * (ArmConfig.mass1 * ArmConfig.length1 * ArmConfig.length1) + 1.0 / 3.0 * (ArmConfig.mass2 * ArmConfig.length2 * ArmConfig.length2);
		armRotationSim = new VariableLengthArmSim(DCMotor.getNEO(1), ArmConfig.motorRotationsPerArmRotation, j, ArmConfig.length1, 0, Math.PI / 2, ArmConfig.mass1 + ArmConfig.mass2, true);
		armExtensionSim = new ExtensionSim(DCMotor.getNEO(1), ArmConfig.mass2, ArmConfig.length1, ArmConfig.length1 + ArmConfig.length2, -Math.PI/2, true);

		/*
		 * States are as follows:
		 * 0: rotation position
		 * 1: extension position
		 * 2: rotation velocity
		 * 3: extension velocity
		 */
		Matrix<N4, N1> x = VecBuilder.fill(Math.PI/3, 0, 0, 0);
		armRotationSim.setState(VecBuilder.fill(x.get(0, 0), x.get(2, 0)));

		armExtensionSim.setState(VecBuilder.fill(x.get(1, 0), x.get(3, 0)));
	}

	public void periodic(double rotationInput, double extensionInput) {
		// double numberOfIterations = 1;
		// for (int i = 0; i < numberOfIterations; i++) {
		// 	iterate(.02/numberOfIterations, rotationInput, extensionInput);
		// }

		// double fullLength = armExtensionSim.getTotalLength();

	}

	public String getDataToWrite(){
		return armExtensionSim.getTotalLength() * Math.cos(armRotationSim.getAngleRads()) + ", " + armExtensionSim.getTotalLength() * Math.sin(armRotationSim.getAngleRads()) + " ,";
	}

	private void iterate (double dt, double rotationInput, double extensionInput) {
		armRotationSim.setInput(rotationInput);
		armExtensionSim.setInput(extensionInput);
		extension = armExtensionSim.getExtension();
		j = 1.0 / 3.0 * (ArmConfig.mass1 * ArmConfig.length1 * ArmConfig.length1) + 1.0 / 3.0 * (ArmConfig.mass2 * ArmConfig.length2 * ArmConfig.length2) + ArmConfig.mass2 * extension * extension;
		armRotationSim.setCGRadius((ArmConfig.mass1 * ArmConfig.length1 / 2.0) + (ArmConfig.mass2) * (extension + ArmConfig.length2 / 2.0));
		armRotationSim.setMOI(j);
		armRotationSim.setLength(armExtensionSim.getTotalLength());

		armRotationSim.update(dt);
		armExtensionSim.update(dt);
	}

	public void setArmRotationVoltage(double voltage) {
		armRotationSim.setInputVoltage(voltage);
	}
	public void setArmExtensionVoltage(double voltage) {
		armExtensionSim.setInputVoltage(voltage);
	}

	public void setVoltages(double angularVoltage, double rotationalVoltage) {
		armRotationSim.setInputVoltage(angularVoltage);
		armExtensionSim.setInputVoltage(rotationalVoltage);
	}

	public void setVoltages(double[] voltages){
		setVoltages(voltages[0], voltages[1]);
	}

	// public double getLength() {
	// 	return armExtensionSim.getTotalLength();
	// }

	public double getExtension() {
		return armExtensionSim.getExtension();
	}

	public double getRawExtension() {
		return armExtensionSim.getTotalLength();
	}

	public double getVelocityExtension() {
		return armExtensionSim.getVelocity();
	}

	public double getAngleRads() {
		return armRotationSim.getAngleRads();
	}

	public double getVelocityRads() {
		return armRotationSim.getVelocityRadPerSec();
	}
}
