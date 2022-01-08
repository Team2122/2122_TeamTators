package org.teamtators.common.config.helpers;

import edu.wpi.first.wpilibj.Compressor;

public class CompressorConfig implements ConfigHelper<Compressor> {
    public Integer canID;
    public boolean autoOn;

    @Override
    public Compressor create() {
        if (this.canID == null)
            throw new NullPointerException("Pneumatics CAN ID not specified");
        Compressor c = new Compressor(canID);
        c.setClosedLoopControl(autoOn);
        return c;
    }
}
