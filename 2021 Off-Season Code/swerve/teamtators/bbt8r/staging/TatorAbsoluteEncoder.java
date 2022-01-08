package org.teamtators.bbt8r.staging;

import edu.wpi.first.wpilibj.DutyCycleEncoder;

public class TatorAbsoluteEncoder extends DutyCycleEncoder {
    double factor = 1;

    public TatorAbsoluteEncoder(int channel) {
        super(channel);
    }

    public void setInverted() {
        factor = -1;
    }

    public void setInverted(boolean inverted) {
        if (inverted) {
            factor = -1;
        } else {
            factor = 1;
        }
    }

    public double getDistance() {
        return super.getDistance() * factor;
    }

    public static class Config {
        public int channel;

        public TatorAbsoluteEncoder create() {
            return new TatorAbsoluteEncoder(channel);
        }
    }
}
