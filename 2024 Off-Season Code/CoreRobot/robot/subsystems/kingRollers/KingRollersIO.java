package frc.robot.subsystems.kingRollers;

import org.littletonrobotics.junction.AutoLog;
import org.teamtators.tester.ManualTestGroup;

public interface KingRollersIO {
    @AutoLog
    public class KingRollersIOInputs {
        protected double supplyCurrent;
        protected double statorCurrent;
        protected double torqueCurrent;
        protected double dutyCycle;
        protected String controlMode;
        protected double velocityRPS;
        protected double tempCelcius;
        protected double appliedVolts;
        protected boolean connected = true;

        protected boolean noteSensor;
        protected boolean safetySensor;
    }

    public default void updateInputs(KingRollersIOInputs inputs) {}
    public default void setSpeed(KingRollers.Speeds speed) {}
    public default ManualTestGroup getManualTest() {
        return new ManualTestGroup("King Rollers") {};
    }
}
