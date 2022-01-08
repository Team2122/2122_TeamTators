package org.teamtators.common.tester.components;

import edu.wpi.first.wpilibj.Compressor;
import org.teamtators.common.tester.ManualTest;

public class CompressorTest extends ManualTest {
    private final Compressor compressor;

    public CompressorTest(Compressor compressor) {
        super("compressor");
        this.compressor = compressor;
    }

    @Override
    public void start() {
        super.start();
        compressor.start();
    }


    @Override
    public void stop() {
        super.stop();
        compressor.stop();
    }
}
