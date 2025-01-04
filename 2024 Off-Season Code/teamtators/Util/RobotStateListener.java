package org.teamtators.Util;

import frc.robot.Robot.RobotControlMode;

public interface RobotStateListener {
    void onEnterRobotState(RobotControlMode state);
}
