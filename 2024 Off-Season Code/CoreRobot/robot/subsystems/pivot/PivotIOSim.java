package frc.robot.subsystems.pivot;

import frc.robot.subsystems.pivot.PivotIO.PivotIOInputs;

public class PivotIOSim implements PivotIO {
    private double position;

    @Override
    public void updateInputs(PivotIOInputs inputs) {
        inputs.positionRotations = position;
    }

    @Override
    public void setEncoderPosition(double value) {
        position = value;
    }

    @Override
    public void setSetpoint(double theta) {
        position = theta;
    }
}
