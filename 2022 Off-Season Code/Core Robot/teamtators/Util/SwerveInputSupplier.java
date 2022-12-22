package org.teamtators.Util;

import frc.robot.subsystems.SwerveInputProxy.SwerveInput;

public interface SwerveInputSupplier {

    SwerveInput get(SwerveInput swerveInput);
}

