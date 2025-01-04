package frc.robot.subsystems.swerve;

import com.ctre.phoenix6.BaseStatusSignal;
import com.ctre.phoenix6.StatusSignal;
import com.ctre.phoenix6.hardware.Pigeon2;

import org.teamtators.Util.TatorPigeon;
import frc.robot.constants.GeneralConstants;
import frc.robot.subsystems.swerve.GyroIO.GyroIOInputs;

public class GyroIOReal implements GyroIO {
    private TatorPigeon gyro;

    public GyroIOReal() {
        gyro = new TatorPigeon(0, GeneralConstants.kCanivoreBusName);
        System.out.println("Boot gyro angle: " + gyro.getYaw());
    }

    public void updateInputs(GyroIOInputs inputs) {
        double prevYaw = inputs.yawDegreesRaw;
        double prevRoll = inputs.rollDegrees;
        double prevPitch = inputs.pitchDegrees;

        inputs.yawDegreesRaw = gyro.getYaw().getValueAsDouble();
        inputs.yawDegrees = gyro.getYawContinuous();
        inputs.rollDegrees = gyro.getRoll().getValueAsDouble();
        inputs.pitchDegrees = gyro.getPitch().getValueAsDouble();

        inputs.yawDegreesPerSec = (inputs.yawDegreesRaw - prevYaw) / .02;
        inputs.rollDegreesPerSec = (inputs.rollDegrees - prevRoll) / .02;
        inputs.pitchDegreesPerSec = (inputs.pitchDegrees - prevPitch) / .02;

        inputs.connected = StatusSignal.isAllGood(gyro.getYaw());
    }

    public void zero() {
        gyro.zero();
    }

    public void setCurrentAngle(double angle) {
        gyro.setCurrentAngle(angle);
    }

    public void changeOffset(double offset) {
        gyro.changeOffset(offset);
    }

    public TatorPigeon getM_gyro() {
        return gyro;
    }
}
