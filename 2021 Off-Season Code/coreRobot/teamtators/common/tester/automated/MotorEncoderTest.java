package org.teamtators.common.tester.automated;

import org.teamtators.common.config.Configurable;
import org.teamtators.common.control.Timer;
import org.teamtators.common.tester.AutomatedTest;
import org.teamtators.common.tester.AutomatedTestMessage;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

public class MotorEncoderTest extends AutomatedTest implements Configurable<MotorEncoderTest.Config> {
    private Config config;
    private DoubleConsumer motor;
    private DoubleSupplier encoder;
    private Runnable before;
    private Timer beforeTimer;
    private double waitBefore;
    private boolean ready;
    private Timer timer;
    private double speedSum;
    private int steps;
    private double encoderStartingValue;
    private double encoderFinishValue;

    public MotorEncoderTest(String name, DoubleConsumer motor, DoubleSupplier encoder, Runnable before, double waitBefore) {
        super(name);
        this.motor = motor;
        this.encoder = encoder;
        this.before = before;
        this.beforeTimer = new Timer();
        this.waitBefore = waitBefore;
        this.timer = new Timer();

    }

    public MotorEncoderTest(String name, DoubleConsumer motor, DoubleSupplier encoder) {
        this(name, motor, encoder, null, 0.0);
    }

    @Override
    protected void initialize() {
        super.initialize();
        if (before != null) {
            beforeTimer.start();
            before.run();
            ready = false;
        } else {
            setup();
            ready = true;
        }
        steps = 0;
    }

    private void setup() {
        motor.accept(config.power);
        encoderStartingValue = encoder.getAsDouble();
        timer.start();
    }

    @Override
    public boolean step() {
        if (before != null && !ready) {
            if (beforeTimer.hasPeriodElapsed(waitBefore)) {
                setup();
                ready = true;
            } else {
                return false;
            }
        }
        if (!ready) return false;
        steps++;
//        sendMessage("Speed: " + speed, AutomatedTestMessage.Level.INFO);

        if (timer.hasPeriodElapsed(config.timeout)) {
            encoderFinishValue = encoder.getAsDouble();
            double delta = Math.abs(encoderFinishValue - encoderStartingValue);
            if (delta < config.desiredDelta) {
                sendMessage("System did not surpass desired delta " + config.desiredDelta + ", actual " + delta, AutomatedTestMessage.Level.ERROR);
            }
            return true;
        }
        return false;
    }


    @Override
    protected void finish(boolean interrupted) {
        super.finish(interrupted);
        motor.accept(0.0);
    }

    public void configure(Config config) {
        this.config = config;
    }

    public static class Config {
        public double power;
        public double desiredDelta;
        public double tolerance = 0.1;
        public double timeout;
    }
}
