package org.teamtators.common.tester.components;

import org.teamtators.common.characterization.CharacterizationRunner;
import org.teamtators.common.control.Timer;
import org.teamtators.common.controllers.LogitechF310;
import org.teamtators.common.datalogging.DataCollector;
import org.teamtators.common.datalogging.LogDataProvider;
import org.teamtators.common.tester.ManualTest;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

public class SparkMaxAutomatedVelocityCalibrationTest extends ManualTest {
    private final DoubleConsumer follower;
    private final DoubleSupplier encoder;
//    private final TatorSparkMax motor;

    private boolean run;
    private double power;
    private double position;
    private double velocity;
    private boolean finished;

    private double[] powers;
    private int pIdx;

    private int directoryNum;
    private final DataCollector dataCollector;
    private Timer timer = new Timer();
    private Timer waitTimer = new Timer();
    private boolean waitForNext = false;
    private double delayTime;
    private double delayTimeProcess;
    private double runTime;
    private boolean startedFinalWait = false;
    private LogDataProvider logDataProvider = new LogDataProvider() {
        @Override
        public String getName() {
            return SparkMaxAutomatedVelocityCalibrationTest.this.getName();
        }

        @Override
        public List<Object> getKeys() {
            return Arrays.asList("power", "position", "velocity");
        }

        @Override
        public List<Object> getValues() {
            return Arrays.asList(power, 0, velocity);
        }
    };

    public SparkMaxAutomatedVelocityCalibrationTest(String name, DoubleSupplier encoder, DoubleConsumer controller,
                                                    double runTime, double delayTime, double delayProcessTime, double[] powers) {
        super(name);
        this.follower = controller;
        this.encoder = encoder;
        this.delayTime = delayTime;
        this.delayTimeProcess = delayProcessTime;
        this.powers = powers;
        this.runTime = runTime;
        dataCollector = DataCollector.getDataCollector();
    }

    @Override
    public void start() {
        startedFinalWait = false;
        run = false;
        finished = false;
        logger.info("Press START to begin automated calibration sequence. Press any other button to cancel");
    }

    @Override
    public void update(double delta) {
        velocity = encoder.getAsDouble();

        if (run && !finished) {
            if (timer.isRunning() && timer.hasPeriodElapsed(runTime)) {
                if (pIdx == powers.length - 1) {
                    finished = true;
                    power = 0;
                    follower.accept(power);
                    dataCollector.stopProvider(logDataProvider);
                    logger.info("Run complete. Beginning processing.");
                    return;
                } else {
                    if (!waitForNext) {
                        logger.info("Current move finished. Waiting for next move.");
                        waitTimer.start();
                        power = 0;
                        follower.accept(0);
                        dataCollector.stopProvider(logDataProvider);
                        waitForNext = true;
                        return;
                    }
                    if (waitTimer.hasPeriodElapsed(delayTime)) {
                        logger.info("[{}] Running at a power of {}", ++pIdx, powers[pIdx]);
                        power = powers[pIdx];
                        follower.accept(power);
                        dataCollector.startProvider(getAutocalibDir(), logDataProvider);
                        waitForNext = false;
                        timer.start();
                    }
                }
            }
            follower.accept(power);
        } else if (run) {
            if (!startedFinalWait) {
                logger.info("Waiting for FS writes to finish before processing...");
                timer.restart();
                startedFinalWait = true;
            }
            if (timer.hasPeriodElapsed(delayTimeProcess)) {
                timer.stop();
                finished = false;
                run = false;
                CharacterizationRunner.characterizeSmartMotion(dataCollector.getSubdirectory(getAutocalibDir()));
            }
            follower.accept(0); //just for safety
        } else {
            follower.accept(0);
        }
    }

    private String getAutocalibDir() {
        return String.format("autocalib-%d", directoryNum);
    }

    @Override
    public void onButtonDown(LogitechF310.Button button) {
        switch (button) {
            case START:
                logger.info("Beginning calibration sequence");
                run = true;
                finished = false;
                directoryNum = new Random().nextInt(65535);
                dataCollector.createOrEmptySubdirectory(getAutocalibDir());
                pIdx = 0;
                power = powers[pIdx];
                logger.info("[{}] Running at a power of {}", pIdx, power);
                dataCollector.startProvider(getAutocalibDir(), logDataProvider);
                timer.start();
                break;
            default:
                logger.info("Cancelling calibration sequence");
                run = false;
                finished = false;
                break;
        }
    }

    @Override
    public void stop() {
        run = false;
        finished = false;
        dataCollector.stopProvider(logDataProvider);
        follower.accept(0);
    }
}
