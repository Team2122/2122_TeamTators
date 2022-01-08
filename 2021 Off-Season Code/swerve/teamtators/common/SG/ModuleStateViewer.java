package org.teamtators.common.SG;

import SplineGenerator.GUI.Display;
import SplineGenerator.GUI.DisplayGraphics;
import SplineGenerator.GUI.Displayable;
import SplineGenerator.GUI.SplineDisplay;
import SplineGenerator.Splines.PolynomicSpline;
import SplineGenerator.Splines.Spline;
import SplineGenerator.Util.*;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;

public class ModuleStateViewer implements Displayable {

    private final String server = "10.21.22.2";
    private NetworkTableInstance inst;

    private final String tableKey = "PositionTable";
    private final String xVectorsKey = "XVectors";
    private final String yVectorsKey = "YVectors";
    private final String xPointsKey = "XPoints";
    private final String yPointsKey = "YPoints";
    private final String posInTableKey = "posInTable";

    private NetworkTableEntry xVectors;
    private NetworkTableEntry yVectors;
    private NetworkTableEntry xPoints;
    private NetworkTableEntry yPoints;

    private double[] defaultVectorXComps;
    private double[] defaultVectorYComps;
    private double[] defaultPointXComps;
    private double[] defaultPointYComps;

    private Display display;

    private boolean allowPositionOfVectors = false;

    private DPoint[] defaultPoints;

    public static void main(String... args) {
        new ModuleStateViewer();
    }

    public ModuleStateViewer() {
        connectToNetworkTables();

        System.out.println("Getting Entries");
        xVectors = inst.getTable(tableKey).getEntry(xVectorsKey);
        yVectors = inst.getTable(tableKey).getEntry(yVectorsKey);
        xPoints = inst.getTable(tableKey).getEntry(xPointsKey);
        yPoints = inst.getTable(tableKey).getEntry(yPointsKey);

        System.out.println("Creating Arrays");
        defaultVectorXComps = new double[0];
        defaultVectorYComps = new double[0];
        defaultPointXComps = new double[0];
        defaultPointYComps = new double[0];
        defaultPoints = new DPoint[]{new DPoint(-1, 1), new DPoint(-1, -1), new DPoint(1, -1), new DPoint(1, 1), new DPoint(0, 0)};

        System.out.println("Creating Window");
        Extrema extrema = new Extrema(new DPoint(-3, -3), new DPoint(3, 3));
        display = new Display(2, extrema, 0, 1, 1600, 700);
        display.setTitle("Swerve Motion Viewer");
        display.displayables.add(this);

        display.display();

        while (true) {
            display.repaint();
        }
    }

    public void connectToNetworkTables() {

        System.out.println("Connecting To NetworkTables Server: " + server);

        inst = NetworkTableInstance.getDefault();
        inst.setServer(server);
        inst.startClientTeam(2122);

        long startTime = System.currentTimeMillis();
        long timeout = 10 * 1000;

        while (true) {
            if (inst.isConnected()) {
                System.out.println("Successfully Connected To NetworkTables Server");
                break;
            } else if (System.currentTimeMillis() - startTime > timeout) {
                System.out.println("Connecting To NetworkTables Server Timed Out");
                System.exit(1);
            }
        }
    }

    @Override
    public void display(DisplayGraphics displayGraphics) {

        double[] realXVectorComps = xVectors.getDoubleArray(defaultVectorXComps);
        double[] realYVectorComps = yVectors.getDoubleArray(defaultVectorYComps);

        if (allowPositionOfVectors) {
            double[] realXPointComps = xPoints.getDoubleArray(defaultPointXComps);
            double[] realYPointComps = yPoints.getDoubleArray(defaultPointYComps);

            for (int i = 0; i < realXVectorComps.length && i < realYVectorComps.length && i < realXPointComps.length && i < realYPointComps.length; i++) {
                displayGraphics.paintVector(new DPosVector(new DPoint(realXPointComps[i], realYPointComps[i]), new DVector(realXVectorComps[i], realYVectorComps[i])));
            }
        } else {
            for (int i = 0; i < realXVectorComps.length && i < realYVectorComps.length; i++) {
                DVector vector = new DVector(realXVectorComps[i], realYVectorComps[i]);
                displayGraphics.getGraphics().drawString("Module " + i + ": " + vector, 100, 100 + 50 * i);
                displayGraphics.paintVector(new DPosVector(defaultPoints[i].clone(), vector));
            }
        }

    }

}
