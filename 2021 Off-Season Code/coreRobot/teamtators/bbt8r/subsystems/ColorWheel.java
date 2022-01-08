package org.teamtators.bbt8r.subsystems;

import edu.wpi.first.wpilibj.DriverStation;
import org.teamtators.bbt8r.TatorRobot;
import org.teamtators.common.config.helpers.*;
import org.teamtators.common.hw.NEOEncoder;
import org.teamtators.common.hw.RevColorSensor;
import org.teamtators.common.hw.TatorSparkMax;
import org.teamtators.common.scheduler.Subsystem;
import org.teamtators.common.tester.ManualTestGroup;
import org.teamtators.common.tester.components.*;

import java.util.Arrays;
import java.util.List;

public class ColorWheel extends Subsystem{
    RevColorSensor colorSensor;
    TatorSparkMax spinWheelMotor;
    NEOEncoder spinWheelEncoder;
    double spinWheelPower;
    String sensorColor;

    private Config config;

    private boolean localDebug = false;

    public ColorWheel(TatorRobot robot) {
        super("ColorWheel");
    }

    public void setSpinWheelMotor(double power) {
        spinWheelMotor.set(power);
    }

    public List<TatorSparkMax> getSparkMaxes() {
        return Arrays.asList(spinWheelMotor);
    }

    public edu.wpi.first.wpilibj.util.Color getDetectedColor() {
        return colorSensor.getColor(); // (r, g, b)
    }

    // returns red, green, blue, or yellow
    public edu.wpi.first.wpilibj.util.Color getRawSensed() {
        double r = colorSensor.getRawRed();
        double g = colorSensor.getRawGreen();
        double b = colorSensor.getRawBlue();
        double norm = r + g + b;
        return new edu.wpi.first.wpilibj.util.Color(r/norm, g/norm, b/norm);
    }

    public Color getFieldColor() {
        Color color = Color.Unknown;
        sensorColor = DriverStation.getInstance().getGameSpecificMessage();
        if (sensorColor.length() > 0) {
            switch (sensorColor.charAt(0)) {
                case 'B':
                    color = Color.Blue;
                    break;
                case 'G':
                    color = Color.Green;
                    break;
                case 'R':
                    color = Color.Red;
                    break;
                case 'Y':
                    color = Color.Yellow;
                    break;
                default:
                    color = Color.Unknown;
                    break;
            }
        }
        return color;
    }

    public Color getDesiredColor() {
        Color sensorColor = getFieldColor();
        Color color = Color.Unknown;
        switch (sensorColor) {
            case Yellow: {
                color = Color.Green;
                break;
            }
            case Blue: {
                color = Color.Red;
                break;
            }
            case Red: {
                color = Color.Blue;
                break;
            }
            case Green: {
                color = Color.Yellow;
                break;
            }
        }
        return color;
    }

    public double getSpinWheelEncoder(){
        return spinWheelEncoder.getRotations();
    }

    public void configure(ColorWheel.Config config) {
        this.spinWheelMotor = config.spinWheelMotor.create();
        this.spinWheelEncoder = spinWheelMotor.getNeoEncoder();
        this.colorSensor = config.colorSensor.create();
        this.spinWheelPower = spinWheelPower;
        this.config = config;
        localDebug = config.debug;
    }

    @Override
    public ManualTestGroup createManualTests() {
        ManualTestGroup tests = super.createManualTests();
        tests.addTest(new RevColorSensorTest("revColorSensorTest", colorSensor));
        tests.addTest(new SpeedControllerTest("spinWheelMotorTest" , spinWheelMotor));
        tests.addTest(new NEOEncoderTest("spinWheelEncoderTest", spinWheelEncoder));
        return tests;
    }

    public static class Config {
        public SparkMaxConfig spinWheelMotor;
        public RevColorSensorConfig colorSensor;
        public double spinWheelPower;
        public boolean debug;
    }

    public enum Color {
        Red,
        Blue,
        Green,
        Yellow,
        Unknown
    }
}
