package SplineGenerator.Follower.sim;

import SplineGenerator.Follower.*;
import SplineGenerator.GUI.SplineDisplay;
import SplineGenerator.Splines.PolynomicSpline;
import SplineGenerator.Splines.Spline;
import SplineGenerator.Util.DControlPoint;
import SplineGenerator.Util.DVector;
import SplineGenerator.Util.InterpolationInfo;
import main.Vector2D;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.tools.FileObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class JSONAutoParser {
    enum SplineOrder {
        FIRST, FIFTH,
    }

    private static SplineOrder jeferson = SplineOrder.FIRST;
    private static SplineOrder newJeferson = SplineOrder.FIRST;


    public JSONAutoParser() {

    }

    public static SingleAuto generateAutoFromJSON(String fileName) throws IOException {
        JSONObject json = new JSONObject(new String(Files.readAllBytes(Paths.get(fileName))));
        SingleAuto outPut;
        String autoName = json.getString("Name");
        JSONArray aryControls = json.getJSONArray("ControlPoints");
        JSONArray aryWaypoints = json.getJSONArray("WayPoints");
        JSONArray aryRequiredPoints = json.getJSONArray("RequiredPoints");

        JSONObject controlJSON;
        JSONObject waypointJSON;
        JSONObject requiredJSON;
        DVector a, b;

        ArrayList<PolynomicSpline> splines = new ArrayList<PolynomicSpline>();
        jeferson = SplineOrder.values()[aryControls.getJSONObject(0).getInt("order") == 1 ? 0 : 1];
        System.out.println("jeferson: " + jeferson);
        PolynomicSpline spline = new PolynomicSpline(2, aryControls.getJSONObject(0).getInt("order"));
        int fiveOrderCounter = 0;
        boolean dontEnd = false;

        for (int i = 0; i < aryControls.length(); i++) {

            double x;
            double y;
            switch (jeferson) {
                case FIFTH:
                    controlJSON = aryControls.getJSONObject(i);
                    x = controlJSON.getDouble("X");
                    y = controlJSON.getDouble("Y");
                    double Vx = controlJSON.getDouble("Vx");
                    double Vy = controlJSON.getDouble("Vy");

                    a = new DVector(x, y);
                    b = new DVector(Vx, Vy);
                    boolean isMax = true; // major bug here // actually maybe not
//                    if (!(fiveOrderCounter == 0 || isMax)) {
//                        spline.addControlPoint(new DControlPoint(a, b));
//                    } else {
                    spline.addControlPoint(new DControlPoint(a, b, new DVector(1, 0)));
//                    }

                    fiveOrderCounter++;

                    break;
                case FIRST:
                    controlJSON = aryControls.getJSONObject(i);
                    x = controlJSON.getDouble("X");
                    y = controlJSON.getDouble("Y");
                    a = new DVector(x, y);
                    spline.addControlPoint(new DControlPoint(a));
                    break;
            }
            newJeferson = SplineOrder.values()[aryControls.getJSONObject(i).getInt("order") == 1 ? 0 : 1];
            if (newJeferson != jeferson) {
                switch (newJeferson) {
                    case FIRST:
                        addSplineInterp5Generation(spline);
                        fiveOrderCounter = 0;
                        splines.add(spline);
                        if (i + 1 != aryControls.length()) {
                            spline = new PolynomicSpline(2, 1);

                            controlJSON = aryControls.getJSONObject(i);
                            x = controlJSON.getDouble("X");
                            y = controlJSON.getDouble("Y");
                            a = new DVector(x, y);
                            spline.addControlPoint(new DControlPoint(a));
                        } else {
                            dontEnd = true;
                        }

                        break;

                    case FIFTH:
                        addSplineInterp1Generation(spline);
                        splines.add(spline);
                        if (i + 1 != aryControls.length()) {

                            spline = new PolynomicSpline(2, 5);

                            fiveOrderCounter = 0;

                            controlJSON = aryControls.getJSONObject(i);
                            x = controlJSON.getDouble("X");
                            y = controlJSON.getDouble("Y");
                            double Vx = controlJSON.getDouble("Vx");
                            double Vy = controlJSON.getDouble("Vy");

                            a = new DVector(x, y);
                            b = new DVector(Vx, Vy);
                            boolean isMax = true;
                            if (!(fiveOrderCounter == 0 || isMax)) {
                                spline.addControlPoint(new DControlPoint(a, b));
                            } else {
                                spline.addControlPoint(new DControlPoint(a, b, new DVector(1, 0)));
                            }
                            fiveOrderCounter++;
                        } else {
                            dontEnd = true;
                        }
                        break;
                }
                jeferson = newJeferson;
            }
        }
        if (!dontEnd) {
            if (spline.getPolynomicOrder() == 1) {
                addSplineInterp1Generation(spline);
            } else {
                addSplineInterp5Generation(spline);
            }
            splines.add(spline);
        }

        Followable fullSpline;
        if (splines.size() > 1) {
            Followable[] followables = new Followable[splines.size()];
            splines.toArray(followables);
            fullSpline = new SplineStringer(followables);
        } else {
            fullSpline = splines.get(0);
        }
        Waypoint[] waypoints = new Waypoint[aryWaypoints.length()];

        for (int i = 0; i < waypoints.length; i++) {


            waypointJSON = aryWaypoints.getJSONObject(i);
            double x = waypointJSON.getDouble("X");
            double y = waypointJSON.getDouble("Y");
            double speed = waypointJSON.getDouble("Speed");
            String command = waypointJSON.getString("Command");
            Map<String, Object> argsMap = new HashMap<String, Object>();

            if (waypointJSON.has("args")) {
                argsMap = getJSONMap(waypointJSON.getJSONObject("args"));
            }

            Runnable r = getRunnable(command, argsMap);
            waypoints[i] = new Waypoint(findTOnSpline(new Vector2D(x, y), fullSpline, .001), r, speed);
        }

        RequiredFollowerPoint[] rb = new RequiredFollowerPoint[aryRequiredPoints.length()];
        for (int i = 0; i < rb.length; i++) {
            requiredJSON = aryRequiredPoints.getJSONObject(i);
            double x = requiredJSON.getDouble("X");
            double y = requiredJSON.getDouble("Y");
            double angle = requiredJSON.getJSONObject("args").getDouble("angle");


            rb[i] = new RequiredFollowerPoint(findTOnSpline(new Vector2D(x, y), fullSpline, .01), angle);
        }
        Follower follower = new GoodestFollower(fullSpline, .1, .01, new Waypoints(waypoints), new RequiredFollowerPoints(.1, .01, rb));
        return new SingleAuto(autoName, fullSpline, follower);
    }

    public static Runnable getRunnable(String Command, Map<String, Object> argsMap) {
        Runnable r;
        r = () -> {
        };
        return r;
    }

    public static Map<String, Object> getJSONMap(JSONObject jsonObj) {
        Map<String, Object> map = new HashMap<String, Object>();

        Iterator<?> mapKeys = jsonObj.keys();

        while (mapKeys.hasNext()) {
            String key = (String) mapKeys.next();
            Object value = jsonObj.get(key);
            map.put(key, value);
        }

        return map;
    }

    public static void addSplineInterp5Generation(Spline spline) {
        spline.closed = false;
        InterpolationInfo c1 = new InterpolationInfo();
        c1.interpolationType = Spline.InterpolationType.Linked;
        c1.endBehavior = Spline.EndBehavior.Hermite;
        InterpolationInfo c2 = new InterpolationInfo();
        c2.interpolationType = Spline.InterpolationType.Linked;
        c2.endBehavior = Spline.EndBehavior.Hermite;
        InterpolationInfo c3 = new InterpolationInfo();
        c3.interpolationType = Spline.InterpolationType.Linked;
        c3.endBehavior = Spline.EndBehavior.None;
        InterpolationInfo c4 = new InterpolationInfo();
        c4.interpolationType = Spline.InterpolationType.Linked;
        c4.endBehavior = Spline.EndBehavior.None;
        spline.interpolationTypes.add(c1);
        spline.interpolationTypes.add(c2);
        spline.interpolationTypes.add(c3);
        spline.interpolationTypes.add(c4);
        spline.generate();
        spline.takeNextDerivative();
    }

    public static void addSplineInterp1Generation(Spline spline) {
        spline.closed = false;
        spline.generate();
        spline.takeNextDerivative();
    }

    public static double findTOnSpline(Vector2D pos, Followable followable, double splineRes) {
        double distMin = 9e307;
        double tempT = 3;
        for (double j = 0; j < followable.getNumPieces(); j += splineRes) {
            Vector2D temp = followable.get(j).toVector2D();
            if (temp.clone().subtract(pos.clone()).getMagnitude() < distMin) {
                distMin = temp.clone().subtract(pos.clone()).getMagnitude();
                tempT = j;
//                System.out.println("distMin: " + distMin + " temp: "+ temp + " pos: " + pos + " temp-pos: " +
//                        temp.clone().subtract(pos.clone()) + " tempT: " + tempT + " J: " + j + " spline.get(j): " + followable.get(1));
            }
        }
        return tempT;
    }


    public static void main(String[] args) throws IOException {
        JSONAutoParser jsonAutoParser = new JSONAutoParser();
        SingleAuto singleAuto = jsonAutoParser.generateAutoFromJSON("/home/ibrahim/robotics/TatorEyes2/AutoFollower/src/SplineGenerator/Follower/sim/figure_eight.txt");
        GoodestFollower goodestFollower = (GoodestFollower) singleAuto.getFollower();
        Vector2D pos = new Vector2D(0, 0);

        if (false) {
            SplineDisplay splineDisplay = new SplineDisplay(singleAuto.getSpline(), 0, 1, 1300, 700);
            splineDisplay.displayables.add(goodestFollower);
            splineDisplay.display();
            while (true) {
                if (!goodestFollower.finished()) {
                    pos.add(goodestFollower.get(pos).scale(.01));
//                    System.out.println(pos);
                }
                splineDisplay.repaint();
            }
        } else {
            for (int i = 0; i < 1000 && !goodestFollower.finished(); i++) {
                pos.add(goodestFollower.get(pos).scale(.01));
                System.out.println(pos);
            }
        }
    }


}
