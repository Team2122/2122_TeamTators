package org.teamtators.common.tester.automated;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.can.BaseMotorController;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.control.Timer;
import org.teamtators.common.hw.CtreMotorControllerGroup;
import org.teamtators.common.tester.AutomatedTest;
import org.teamtators.common.tester.AutomatedTestMessage;

import java.util.function.DoubleSupplier;

public class MotorGroupEncoderAutoTest extends AutomatedTest implements Configurable<MotorGroupEncoderAutoTest.Config> {

    private CtreMotorControllerGroup motors;
    private Timer timer;
    private Timer beforeTimer;
    private DoubleSupplier encoder;
    private boolean ready;
    private boolean haventCalledSetup;
    private BaseMotorController controller;
    private int nextMotor;
    private Runnable before;
    private double waitBefore;
    private double encoderStartingValue;
    private double encoderFinishValue;
    private Config config;
    private String name;

    public MotorGroupEncoderAutoTest(String name, CtreMotorControllerGroup motors, DoubleSupplier encoder, Runnable before, double waitBefore) {
        super(name);
        this.motors = motors;
        this.encoder = encoder;
        this.before = before;
        this.waitBefore = waitBefore;

        encoderStartingValue = encoder.getAsDouble();

        timer = new Timer();
        beforeTimer = new Timer();
        this.name = name;
    }

    public MotorGroupEncoderAutoTest(String name, CtreMotorControllerGroup motors, DoubleSupplier encoder) {
        super(name);
        this.motors = motors;
        this.encoder = encoder;

        encoderStartingValue = encoder.getAsDouble();

        timer = new Timer();
        this.name = name;
    }


    private void setUp() {
        //beforeTimer.stop();
        timer.start();
        motors.disableFollowerMode();
        cycleMotors(0);
    }

    private void cycleMotors(int nextMotor) {
        if (this.nextMotor != nextMotor) {
            timer.restart();
            controller.set(ControlMode.PercentOutput, 0.0);
            encoderStartingValue = encoder.getAsDouble();
        }
        controller = (BaseMotorController) motors.getSpeedControllers()[nextMotor];
        this.nextMotor = nextMotor;
    }

    @Override
    protected void initialize() {
        super.initialize();
        if (before != null) {
            beforeTimer.start();
            before.run();
            ready = false;
            haventCalledSetup = true;
        } else {
            setUp();
            ready = true;
            haventCalledSetup = false;
        }
    }

    @Override
    public boolean step() {
        if (before != null && !ready) {
            if (beforeTimer.hasPeriodElapsed(waitBefore)) {
                setUp();
                ready = true;
                haventCalledSetup = false;
            } else {
                return false;
            }
        }

        if (ready && haventCalledSetup) {
            setUp();
            haventCalledSetup = false;
        }

        if (ready && !haventCalledSetup) {
            double delta = encoder.getAsDouble() - encoderStartingValue;
            if (delta > config.desiredDelta) {
                sendMessage(nextMotor + " PASSED.", AutomatedTestMessage.Level.INFO);
                nextMotor++;
                if (nextMotor == motors.getSpeedControllers().length) {
                    return true;
                }
            }

            if (timer.hasPeriodElapsed(config.timeout)) {
                sendMessage(String.format("%d FAILED. Desired delta %.3f, actual %.3f", nextMotor, config.desiredDelta, delta), AutomatedTestMessage.Level.ERROR);
                nextMotor++;
                if (nextMotor == motors.getSpeedControllers().length) {
                    return true;
                }
            }

            cycleMotors(nextMotor);
            controller.set(ControlMode.PercentOutput, config.power);
        }
        return false;
    }

    @Override
    protected void finish(boolean interrupted) {
        motors.enableFollowerMode();
        super.finish(interrupted);
        timer.stop();
        motors.set(0.0);
    }

    @Override
    public void configure(Config config) {
        this.config = config;
    }

    public static class Config {
        public double timeout;
        public double power;
        public double desiredDelta;
    }
}
