package org.teamtators.common.SG;

import SplineGenerator.GUI.Display;
import SplineGenerator.Util.DPoint;
import SplineGenerator.Util.DVector;
import SplineGenerator.Util.Extrema;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import org.teamtators.bbt8r.staging.Vector;

public class PositionViewer {

    private final String server = "10.21.22.2";
    private NetworkTableInstance inst;

    private final String tableKey = "PositionTable";
    private final String xVectorsKey = "XVectors";
    private final String yVectorsKey = "YVectors";
    private final String posKey = "posInTable";
    private final String gyroThetaKey = "gyroTheta";

    private NetworkTableEntry xVectors;
    private NetworkTableEntry yVectors;
    private NetworkTableEntry position;
    private NetworkTableEntry gyroTheta;

    private double[] vectorXComps;
    private double[] vectorYComps;
    private double[] points;

    private DVector[] moduleStates;
    private DPoint point;

    private Display display;

    private SwerveDisplayable swerveDisplayable;
    private double distMag = 1;

    public static void main(String... args) {
        new PositionViewer();
    }

    public PositionViewer() {

        connectToNetworkTables();

        xVectors = inst.getTable(tableKey).getEntry(xVectorsKey);
        yVectors = inst.getTable(tableKey).getEntry(yVectorsKey);
        position = inst.getTable(tableKey).getEntry(posKey);
        gyroTheta = inst.getTable(tableKey).getEntry(gyroThetaKey);

        vectorXComps = new double[5];
        vectorYComps = new double[5];
        points = new double[2];

        moduleStates = new DVector[5];
        for (int i = 0; i < 5; i++) {
            moduleStates[i] = new DVector(0, 0);
        }
        point = new DPoint(2);

        Extrema extrema = new Extrema(new DPoint(-20, -20), new DPoint(20, 20));
        display = new Display(2, extrema, 0, 1, 1600, 700);
        display.setTitle("Position Viewer");
        swerveDisplayable = new SwerveDisplayable(new DPoint(0, 0), new Vector(-distMag, distMag), new Vector(-distMag, -distMag), new Vector(distMag, -distMag), new Vector(distMag, distMag));

        display.displayables.add(swerveDisplayable);

        display.display();

        while (true) {
            updateSwerveInfo();
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

    public void updateSwerveInfo() {
        // Update position and angle for robot
        points = position.getDoubleArray(points);
        point.set(0, points[0], points[1]);
        swerveDisplayable.setLocations(point, gyroTheta.getDouble(0));

        // Update the status of each module
        vectorXComps = xVectors.getDoubleArray(vectorXComps);
        vectorYComps = yVectors.getDoubleArray(vectorYComps);
        for (int i = 0; i < vectorXComps.length && i < vectorYComps.length; i++) {
            moduleStates[i].set(0, vectorXComps[i], vectorYComps[i]);
        }
        swerveDisplayable.setModuleStates(moduleStates);
    }

}
