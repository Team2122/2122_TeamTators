package org.teamtators.bbt8r;

import org.teamtators.bbt8r.subsystems.SwerveInputProxy.SwerveInput;

public interface SwerveInputSupplier {

    SwerveInput get(SwerveInput input);

}
