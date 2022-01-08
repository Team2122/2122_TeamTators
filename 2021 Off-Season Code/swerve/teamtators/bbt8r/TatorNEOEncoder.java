package org.teamtators.bbt8r;

import com.revrobotics.*;
import com.revrobotics.jni.CANSparkMaxJNI;

import java.util.concurrent.atomic.AtomicBoolean;

public class TatorNEOEncoder extends CANEncoder {

    public TatorNEOEncoder(CANSparkMax device, EncoderType sensorType, int counts_per_rev) {
        super(device, sensorType, counts_per_rev);
    }

    @Override
    public int getCountsPerRevolution() {
        return 42;
    }

}

