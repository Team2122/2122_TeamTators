package org.teamtators.Tools.tester.components;

import edu.wpi.first.wpilibj.PWM;
import org.teamtators.Controllers.XBOXController;
import org.teamtators.Tools.tester.ManualTest;

import edu.wpi.first.wpilibj.Servo;

public class ServoTest extends ManualTest{
    private Servo servo;
    private double servoValue;

    public ServoTest(String name, Servo servo) {
        super(name);
        this.servo = servo;
        servoValue = 0;
        servo.setPeriodMultiplier(PWM.PeriodMultiplier.k1X);
//        servo.setPeriodMultiplier(PWM.PeriodMultiplier.k2X);
//        servo.setPeriodMultiplier(PWM.PeriodMultiplier.k4X);


    }

    @Override
    public void start() {
        printTestInstructions("Press 'A' to display the current position, 'B' to go to 0");
        printTestInstructions("Press 'X' to set the servo to 0.5, 'Y' to set the servo to 1");
    }

    @Override
    public void onButtonDown(XBOXController.Button button) {
        if (button == XBOXController.Button.kA) {
            printTestInfo("Servo Position: " + servo.get() + " Servo Channel: " + servo.getChannel() + " ServoPWM: " + servo.getRaw() + "Bounds: " + servo.getRawBounds().min + " " + servo.getRawBounds().max);
        }
        else if (button == XBOXController.Button.kB) {
            servoValue = 0;
            servo.set(servoValue);
        }
        else if (button == XBOXController.Button.kX) {
            servoValue = 0.5;
            servo.set(servoValue);
        }
        else if (button == XBOXController.Button.kY) {
            servoValue = 1;
            servo.set(servoValue);
        }
    }
}

