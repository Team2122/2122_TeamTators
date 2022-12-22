package org.teamtators.Util;

import edu.wpi.first.wpilibj.motorcontrol.MotorController;

public interface MotorControllerGroup extends MotorController {
    void enableFollowerMode();

    void disableFollowerMode();

    MotorController[] getSpeedControllers();

    double getVelocity();

    MotorController getMaster();
}
