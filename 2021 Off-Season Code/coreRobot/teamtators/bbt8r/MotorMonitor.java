package org.teamtators.bbt8r;

import org.teamtators.common.hw.NEOEncoder;
import org.teamtators.common.hw.TatorSparkMax;

public class MotorMonitor {

    private TatorSparkMax motor;
    private NEOEncoder encoder;

    private int dataSize;
    private int currentIndex = 0;
    private double outputThresh;
    private double minAvgVel;

    private double[] output; // Percent Output [-1, 1]
    private double[] position; // Motor Rotations
    private double[] velocity; // Motor Rotation Velocity
    private double[] acceleration; // Motor Rotation Acceleration


    public MotorMonitor(TatorSparkMax motor, int dataSize, double outputThresh, double minAvgVel) {
        this.motor = motor;
        encoder = motor.getNeoEncoder();

        this.dataSize = dataSize;
        this.outputThresh = outputThresh;
        this.minAvgVel = minAvgVel;

        output = new double[dataSize];
        position = new double[dataSize];
        velocity = new double[dataSize];
        acceleration = new double[dataSize];
    }

    public void run() {
        update();
        if (inBadState()) {
            System.out.println("Motor Monitor: Bad Stuff Is Happening!");
            motor.stopMotor();
        }
    }

    public void update() {
        incrementIndex();
        output[currentIndex] = motor.getAppliedOutput();
        position[currentIndex] = encoder.getRotations();
        velocity[currentIndex] = position[currentIndex] - getOffCurrentIndex(-1, position);
        acceleration[currentIndex] = velocity[currentIndex] - getOffCurrentIndex(-1, velocity);
    }

    /**
     * A method for checking if the motor is in some sort of unsafe state
     * @return false if the state is okay, true if something has gone wrong
     */
    public boolean inBadState() {
        double avgOutput = getTotal(output) / dataSize;
        double avgVel = getTotal(velocity) / dataSize;

        return avgOutput > outputThresh && avgVel < minAvgVel;
    }

    public double getOffCurrentIndex(int change, double[] data) {
        int newIndex = currentIndex + change;
        if (newIndex >= dataSize) {
            newIndex %= dataSize;
        } else if (newIndex < 0) {
            newIndex = dataSize + change;
        }
        return data[newIndex];
    }

    public double getTotal(double[] data) {
        double total = 0;
        for (int i = 0; i < dataSize; i++) {
            total += data[i];
        }

        return total;
    }

    public double incrementIndex() {
        currentIndex++;
        if (currentIndex >= dataSize) {
            currentIndex = 0;
        }

        return currentIndex;
    }
}
