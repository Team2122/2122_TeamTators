package org.teamtators.common.hw;

import edu.wpi.first.wpilibj.SpeedController;

public interface MotorControllerGroup extends SpeedController {
    void enableFollowerMode();

    void disableFollowerMode();

    SpeedController[] getSpeedControllers();

    double getVelocity();

    SpeedController getMaster();
}
