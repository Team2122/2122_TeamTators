package frc.robot.subsystems.picker;

import org.littletonrobotics.junction.AutoLog;
import org.teamtators.tester.ManualTestGroup;

public interface PickerIO {
    @AutoLog
    public class PickerIOInputs {
        protected double supplyCurrent;
        protected double statorCurrent;
        protected double torqueCurrent;
        protected double dutyCycle;
        protected String controlMode;
        protected double velocityRPS;
        protected double tempCelcius;
        protected double appliedVolts;
        protected boolean connected = true;

        protected boolean farEntranceActivated;
        /*protected boolean closeEntranceActivated;*/
        protected boolean chimneySensorActivated;
    }

    public default void updateInputs(PickerIOInputs inputs) {}
    public default void setSpeed(Picker.Speeds speed) {}
    public default ManualTestGroup getManualTest() {
        return new ManualTestGroup("Picker") {};
    }
}
