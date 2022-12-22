package org.teamtators.sassitator;

import java.util.ArrayList;

public class LinearInterpolatedTable {

    private ArrayList<double[]> data;
    private int dependentDimensions;

    public LinearInterpolatedTable(int dependentDimensions) {
        data = new ArrayList<>();
        this.dependentDimensions = dependentDimensions;
    }

    /**
     * @param values The first value (index = 0) is the independent dimension
     */
    public void addPoint(double... values) {
        if (values.length != dependentDimensions + 1) {
            throw new IllegalArgumentException("The number of values passed in must be the same as the number of dependent dimensions plus 1\n" +
                    "The current number of dependent dimensions is: " + dependentDimensions + "\n" +
                    "But you provided: " + (values.length - 1) + " dependent values");
        }
        data.add(values);
    }

    /**
     *
     * @param independentValue is the value we want to use to determine a corresponding value based off of the slope
     *                         between the
     * @param dimension is the index we want from the data arrayList
     * @return
     */
    public double get(double independentValue, int dimension) {
        if (data.size() == 0 || data.size() == 1) {
            return 0;
        }

        int backwardIndex;
        int forwardIndex = 1;

        while (independentValue > data.get(forwardIndex)[0]) {
            forwardIndex++;
            if (forwardIndex >= data.size()) {
                forwardIndex--;
                break;
            }
        }

        backwardIndex = forwardIndex - 1;

        double indChange = data.get(forwardIndex)[0] - data.get(backwardIndex)[0];
        double depChange = data.get(forwardIndex)[dimension] - data.get(backwardIndex)[dimension];

        double slope = depChange / indChange;

        return slope * (independentValue - data.get(forwardIndex)[0]) + data.get(forwardIndex)[dimension];
    }

}
