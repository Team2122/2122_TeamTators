package org.teamtators.common.SG;

import SplineGenerator.GUI.DisplayGraphics;
import SplineGenerator.GUI.Displayable;
import SplineGenerator.Util.DPoint;
import SplineGenerator.Util.DPosVector;
import SplineGenerator.Util.DVector;
import org.teamtators.bbt8r.staging.Vector;

import java.awt.*;

public class SwerveDisplayable implements Displayable {

    private DPoint position;

    private Vector[] theta0Positions; // The original position of the modules relative to the center of the swerve bot
    private DVector[] currentPositions; // The current position of the modules relative to the position

    private DVector[] moduleStates;

    private DVector avgVec;

    public SwerveDisplayable(DPoint startingPoint, Vector... theta0Positions) {
        avgVec = new DVector(2);
        position = startingPoint;
        this.theta0Positions = theta0Positions;
        currentPositions = new DVector[theta0Positions.length];
        moduleStates = new DVector[theta0Positions.length];
        for (int i = 0; i < theta0Positions.length; i++) {
            currentPositions[i] = new DVector(0, 0);
            moduleStates[i] = new DVector(0, 0);
        }
    }

    public void setLocations(DPoint position, double theta) {
        this.position.set(position);

        for (int i = 0; i < currentPositions.length; i++) {
            Vector vector = theta0Positions[i].clone();
            vector.setTheta(vector.getTheta() + theta);
            currentPositions[i] = vectorToDVector(vector);
        }
    }

    public DPoint getLocation(int moduleIndex) {
        return position.clone().add(currentPositions[moduleIndex]);
    }

    public void setModuleStates(DVector... moduleStates) {
        for (int i = 0; i < this.moduleStates.length && i < moduleStates.length; i++) {
            this.moduleStates[i].set(moduleStates[i]);
        }

        avgVec.set(moduleStates[4]);
    }

    @Override
    public void display(DisplayGraphics displayGraphics) {
        Color color = new Color(255, 255, 255);

        displayGraphics.paintLine(getLocation(0), getLocation(1), 3, color, 0, 1);
        displayGraphics.paintLine(getLocation(1), getLocation(2), 3, color, 0, 1);
        displayGraphics.paintLine(getLocation(2), getLocation(3), 3, color, 0, 1);
        displayGraphics.paintLine(getLocation(3), getLocation(0), 3, color, 0, 1);

        for (int i = 0; i < moduleStates.length; i++) {
            displayGraphics.paintVector(new DPosVector(getLocation(i), moduleStates[i]));
        }

        displayGraphics.paintVector(position.clone(), avgVec);

    }

    public static DVector vectorToDVector(Vector vector) {
        return new DVector(vector.getX(), vector.getY());
    }

}
