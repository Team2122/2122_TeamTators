package org.teamtators.common.tester.automated;

import org.teamtators.common.config.Configurable;
import org.teamtators.common.control.Timer;
import org.teamtators.common.tester.AutomatedTest;
import org.teamtators.common.tester.AutomatedTestMessage;

import java.util.function.BooleanSupplier;
import java.util.function.DoubleConsumer;


public class MotorDigitalSensorAutoTest extends AutomatedTest implements Configurable<MotorDigitalSensorAutoTest.Config> {
    private Config config;
    private DoubleConsumer motor;
    private BooleanSupplier digitalSensor;
    private boolean digitalSensorFinishValue;
    private double encoderStartingValue;
    private double encoderFinishValue;
    private boolean startValue;
    private boolean hasStartedToMoveBack = false;
    private Timer timer;
    private int steps;
    private String name;

    public MotorDigitalSensorAutoTest(String name, DoubleConsumer motor, BooleanSupplier digitalSensor) {
        super(name);
        this.digitalSensor = digitalSensor;
        this.motor = motor;
        timer = new Timer();
        this.name = name;
    }

    @Override
    protected void initialize() {
        super.initialize();
        timer.start();
        startValue = digitalSensor.getAsBoolean();
        if (startValue != config.digitalSensorStartValue) {
            sendMessage("Started with incorrect value of " + startValue, AutomatedTestMessage.Level.ERROR);
        }
        steps = 0;
    }

    private void setup() {
        motor.accept(config.power);
        timer.start();
    }

    @Override
    public boolean step() {
        setup();
        steps++;
        if (timer.hasPeriodElapsed(config.timeout)) {
            double delta = Math.abs(encoderFinishValue - encoderStartingValue);
            if (delta < config.desiredDelta) {
                sendMessage("System did not surpass desired delta " + config.desiredDelta + ", actual " + delta, AutomatedTestMessage.Level.ERROR);
            }
        }
        timer.restart();
        if (timer.hasPeriodElapsed(config.timeout)) {
            if (digitalSensor.getAsBoolean() == startValue) {
                sendMessage("Digital sensor value didn't change, test skipped", AutomatedTestMessage.Level.ERROR);
            }
            moveBack();
        }
        if(hasStartedToMoveBack) {
            if (timer.hasPeriodElapsed(config.moveBackTime)) {
        } else {
                if (timer.hasPeriodElapsed(config.timeout)) {
                    if (digitalSensor.getAsBoolean() == startValue) {
                        sendMessage("Digital sensor value didn't change, test skipped", AutomatedTestMessage.Level.ERROR);
                    } else {
                        sendMessage(name + " PASSED", AutomatedTestMessage.Level.INFO);
                    }
                    return true;
                }
            }


            if (digitalSensor.getAsBoolean() != startValue) {
                timer.stop();


                if (config.moveBack) {
                    timer.restart();
                    hasStartedToMoveBack = true;
                    moveBack();
                } else {
                    sendMessage(name +" PASSED", AutomatedTestMessage.Level.INFO);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected void finish(boolean interrupted) {
        super.finish(interrupted);
        timer.stop();
        motor.accept(0.0);
    }

    private void moveBack() {
        motor.accept(-config.power);
    }

    public void configure(Config config) {
        this.config = config;
    }

    public static class Config {
        public boolean moveBack = true;
        public double moveBackTime = 0;
        public double power;
        public double timeout;
        public boolean digitalSensorStartValue;
        public double desiredDelta =.5;
    }
}