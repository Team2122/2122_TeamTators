package org.teamtators.common.config.helpers;

import edu.wpi.first.wpilibj.BuiltInAccelerometer;
import edu.wpi.first.wpilibj.interfaces.Accelerometer;

public class BuiltInAccelerometerConfig implements ConfigHelper<BuiltInAccelerometer> {
    private BuiltInAccelerometer.Range range = Accelerometer.Range.k8G;

    public BuiltInAccelerometer.Range getRange() {
        return range;
    }

    public void setRange(BuiltInAccelerometer.Range range) {
        this.range = range;
    }

    @Override
    public BuiltInAccelerometer create() {
        return new BuiltInAccelerometer(range);
    }
}
