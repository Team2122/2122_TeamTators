package org.teamtators.common.tester.components;

import org.teamtators.common.controllers.LogitechF310;
import org.teamtators.common.hw.TatorADXRS450;
import org.teamtators.common.tester.ManualTest;

public class ADXRS450Test extends ManualTest {
    private final TatorADXRS450 gyro;

    public ADXRS450Test(TatorADXRS450 gyro) {
        super("gyro");
        this.gyro = gyro;
    }

    @Override
    public void onButtonDown(LogitechF310.Button button) {
        printTestInfo("Angle: {}, Rate: {}", gyro.getAngle(), gyro.getRate());
    }
}

