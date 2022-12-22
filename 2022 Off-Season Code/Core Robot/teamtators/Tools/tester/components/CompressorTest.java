package org.teamtators.Tools.tester.components;

import org.teamtators.Controllers.XBOXController;
import org.teamtators.Tools.tester.ManualTest;

import edu.wpi.first.wpilibj.PneumaticsControlModule;

public class CompressorTest extends ManualTest {
    private final PneumaticsControlModule compressorController;

    public CompressorTest(PneumaticsControlModule compressorController) {
        super("compressor");
        this.compressorController = compressorController;
    }

    @Override
    public void start() {
        super.start();
        printTestInstructions("Press A to start compressor and Press B to stop compressor");
    }

    @Override
    public void stop() {
        super.stop();
    }

    @Override
    public void onButtonDown(XBOXController.Button button) {
        if(button == XBOXController.Button.kA) compressorController.enableCompressorDigital();
        if(button == XBOXController.Button.kB) compressorController.disableCompressor();
    }

}
