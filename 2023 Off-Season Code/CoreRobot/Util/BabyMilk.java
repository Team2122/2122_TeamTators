package common.Util;

import edu.wpi.first.wpilibj.AnalogInput;

public class BabyMilk {

    private AnalogInput analogInput;

    //channel = 4 right now
    public BabyMilk(int channel) {
        analogInput = new AnalogInput(channel);
    }
    public double getRawVoltage() {
        return analogInput.getVoltage();
    }

    public double getDistance(){
        if(getRawVoltage()< .1){
            return 3;
        }
        return 3-(getRawVoltage() / (5/3.0));
}

}
