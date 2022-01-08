package org.teamtators.common.tester.components;

import edu.wpi.first.wpilibj.SpeedController;
import org.teamtators.common.characterization.CharacterizationRunner;
import org.teamtators.common.control.Timer;
import org.teamtators.common.controllers.LogitechF310;
import org.teamtators.common.datalogging.DataCollector;
import org.teamtators.common.datalogging.LogDataProvider;
import org.teamtators.common.hw.NEOEncoder;
import org.teamtators.common.tester.ManualTest;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class SparkMaxAutomatedPositionCalibrationTest extends ManualTest {
    private final SpeedController follower;
    private final NEOEncoder encoder;
//    private final TatorSparkMax motor;

    private boolean run;
    private double power;
    private double position;
    private double velocity;
    private boolean finished;

    private double currentTarget;

    private double forwardTarget; //+ power, >=
    private double reverseTarget; //- power, <=

    private double[] powers;
    private int pIdx;

    private int directoryNum;
    private final DataCollector dataCollector;
    private Timer delayTimer = new Timer();
    private double delayTime;
    private double delayTimeProcess;
    private LogDataProvider logDataProvider = new LogDataProvider() {
        @Override
        public String getName() {
            return SparkMaxAutomatedPositionCalibrationTest.this.getName();
        }

        @Override
        public List<Object> getKeys() {
            return Arrays.asList("power", "position", "velocity");
        }

        @Override
        public List<Object> getValues() {
            return Arrays.asList(power, position, velocity);
        }
    };

    public SparkMaxAutomatedPositionCalibrationTest(String name, NEOEncoder encoder, SpeedController controller, double forwardTarget, double reverseTarget,
                                                    double delayTime, double delayProcessTime, double[] powers) {
        super(name);
        this.follower = controller;
        this.encoder = encoder;
        this.forwardTarget = forwardTarget;
        this.reverseTarget = reverseTarget;
        this.delayTime = delayTime;
        this.delayTimeProcess = delayProcessTime;
        this.powers = powers;
        dataCollector = DataCollector.getDataCollector();
    }

    @Override
    public void start() {
        run = false;
        finished = false;
        logger.info("Press START to begin automated calibration sequence. Press any other button to cancel");
    }

    @Override
    public void update(double delta) {
        position = encoder.getRotations();
        velocity = encoder.getRate();

        if (run && !finished) {
            if (Math.signum(power) == 1 ? position >= currentTarget : position <= currentTarget || delayTimer.isRunning()) {
                if (pIdx == powers.length - 1) {
                    finished = true;
                    power = 0;
                    follower.set(power);
                    dataCollector.stopProvider(logDataProvider);
                    logger.info("Run complete. Beginning processing.");
                    return;
                } else {
                    if (!delayTimer.isRunning()) {
                        logger.info("Current move finished. Waiting for next move.");
                        delayTimer.start();
                        power = 0;
                        dataCollector.stopProvider(logDataProvider);
                        return;
                    }
                    if (delayTimer.hasPeriodElapsed(delayTime)) {
                        logger.info("[{}] Moving to {} at a power of {}", ++pIdx, currentTarget, powers[pIdx]);
                        power = powers[pIdx];
                        dataCollector.startProvider(getAutocalibDir(), logDataProvider);
                        currentTarget = Math.signum(power) == 1 ? forwardTarget : reverseTarget;
                        delayTimer.stop();
                    }
                }
            }
            follower.set(power);
        } else if (run && finished) {
            if (!delayTimer.isRunning()) {
                logger.info("Waiting for FS writes to finish before processing...");
                delayTimer.start();
            }
            if (delayTimer.hasPeriodElapsed(delayTimeProcess)) {
                delayTimer.stop();
                finished = false;
                run = false;
                CharacterizationRunner.characterizeSmartMotion(dataCollector.getSubdirectory(getAutocalibDir()));
            }
            follower.set(0); //just for safety
        } else {
            follower.set(0);
        }
    }

    private String getAutocalibDir() {
        return String.format("autocalib-%d", directoryNum);
    }

    @Override
    public void onButtonDown(LogitechF310.Button button) {
        switch (button) {
            case START:
                if (Math.abs(encoder.getDistance()) >= 0.5) {
                    logger.error("START TEST AT ZERO!!!!!!!!!!!! {}/{}", encoder.getDistance(), position);
                    return;
                }
                logger.info("Beginning calibration sequence");
                run = true;
                finished = false;

                directoryNum = new Random().nextInt(65535);
                dataCollector.createOrEmptySubdirectory(getAutocalibDir());
                pIdx = 0;
                power = powers[pIdx];
                currentTarget = Math.signum(power) == 1 ? forwardTarget : reverseTarget;
                logger.info("[{}] Moving to {} at a power of {}", pIdx, currentTarget, power);
                dataCollector.startProvider(getAutocalibDir(), logDataProvider);
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
        follower.set(0);
    }
}
