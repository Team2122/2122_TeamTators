package org.teamtators.bbt8r;

public interface Module {

    // Initialization step to assign all necessary info
    public void initialize();

    // Resets internal information, such as recalibrating a gyro
    public void calibrate();

    // Bring back to predefined state, allowed to use hw but not necessary
    public void reset();

    // Cleanly exits the module
    public void stop();
}
