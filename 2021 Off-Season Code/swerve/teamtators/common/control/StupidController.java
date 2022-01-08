package org.teamtators.common.control;

import org.teamtators.common.config.Configurable;

/**
 * bang-bang controller
 */
public class StupidController extends AbstractController implements Configurable<StupidController.Config> {
    private double movePower = 0.0;
    private double endRamp;

    private Ramper ramper = new Ramper();
    private boolean moveDir;

    public StupidController(String name) {
        super(name);
    }

    public double getMovePower() {
        return movePower;
    }

    public void setMovePower(double movePower) {
        this.movePower = movePower;
    }

    public double getMaxAcceleration() {
        return ramper.getMaxAcceleration();
    }

    public void setMaxAcceleration(double maxAcceleration) {
        ramper.setMaxAcceleration(maxAcceleration);
    }

    public double getEndRamp() {
        return endRamp;
    }

    public void setEndRamp(double endRamp) {
        this.endRamp = endRamp;
    }

    public boolean getMoveDir() {
        return moveDir;
    }

    @Override
    public synchronized void setSetpoint(double setpoint) {
        super.setSetpoint(setpoint);
        moveDir = setpoint > getInput();
    }

    @Override
    public synchronized void stop() {
        reset();
        super.stop();
    }

    @Override
    protected double computeOutput(double delta) {
        double error = getError();
        double power = movePower * Math.signum(error);
        if (Math.abs(error) <= endRamp) {
            power *= Math.abs(error) / endRamp;
        }
        ramper.setValue(power);
        ramper.update(delta);
        return ramper.getOutput();
    }

    @Override
    public void configure(Config config) {
        super.configure(config);
        this.movePower = config.movePower;
        this.setMaxAcceleration(config.maxAcceleration);
        this.endRamp = config.endRamp;
    }

    public static class Config extends AbstractController.Config {
        public double movePower = 0.0;
        public double maxAcceleration;
        public double endRamp;
    }
}
