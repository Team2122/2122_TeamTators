package frc.robot.subsystems.swerve;

import com.ctre.phoenix6.StatusSignal;
import frc.robot.constants.Constants;
import frc.robot.subsystems.swerve.GyroIO.GyroIOInputs;
import org.teamtators.util.TatorPigeon;

public class GyroIOReal implements GyroIO {
    private TatorPigeon gyro;

    public GyroIOReal() {
        gyro = new TatorPigeon(0, Constants.kCanivoreBusName);
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

        inputs.yawDegreesPerSec = (inputs.yawDegreesRaw - prevYaw) / Constants.kTickPeriod;
        inputs.rollDegreesPerSec = (inputs.rollDegrees - prevRoll) / Constants.kTickPeriod;
        inputs.pitchDegreesPerSec = (inputs.pitchDegrees - prevPitch) / Constants.kTickPeriod;

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
}
