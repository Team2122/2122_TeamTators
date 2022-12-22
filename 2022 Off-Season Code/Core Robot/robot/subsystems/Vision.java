package frc.robot.subsystems;

import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import frc.robot.RobotContainer;
import org.teamtators.Util.TatorPigeon;
import org.teamtators.Util.Vector;
import org.teamtators.sassitator.Subsystem;

import java.util.*;

public class Vision extends Subsystem {

    public enum VisionState {
        INITIALIZE,
        ENABLED_BALL,
        ENABLED_TURRET,
        DISABLED
    }

    private final RobotContainer local_robotContainer;

    private VisionState currentSubsystemState = VisionState.INITIALIZE;
    private VisionState newSubsystemState = VisionState.INITIALIZE;

    // Vision storage data
    private ArrayList<VisionObject> visionObjects = new ArrayList<>();

    // Data from Network Tables From here down
    private final String visionTableKey = "visionTable";
    private final String objectTableKey = "Object Detection";
    private final String stateMachineTableKey = "Vision State Machine";

    // General Keys
    private final String deltaAngleKey = "deltaAngle";
    private final String initialAngleKey = "initialAngle";

    private Vector shootFlyVector = new Vector(1, 1);
    private final String distanceKey = "distance";
    private final String currentAngleKey = "currentAngle";
    private final String currentFrameStartKey = "currentFrameStart";
    private final String currentFrameEndKey = "currentFrameEnd";
    private final String ballStateKey = "Ball Detection Status";
    private final String turretStateKey = "Vision Status";
    private final String visionStateMachineKey = "Current State";

    //  Keys for data for visionObjects
    private final String numberOfObjectsKey = "Number Of Objects";
    private final String allHeightsKey = "Height";
    private final String allWidthsKey = "Width";
    private final String allCentersKey = "Center";
    private final String allColorsKey = "Color";

    // General Entries
    private NetworkTableEntry deltaAngleEntry;
    private NetworkTableEntry initialAngleEntry;
    private NetworkTableEntry distanceEntry;
    private NetworkTableEntry currentAngleEntry;
    private NetworkTableEntry currentFrameStartEntry;
    private NetworkTableEntry currentFrameEndEntry;

    // Entries for data for visionObjects
    private NetworkTableEntry numberOfObjectsEntry;
    private NetworkTableEntry allHeightsEntry;
    private NetworkTableEntry allWidthsEntry;
    private NetworkTableEntry allCentersEntry;
    private NetworkTableEntry allColorsEntry;

    private NetworkTableEntry visionStateMachine;
    private NetworkTableEntry turretState;
    private NetworkTableEntry ballState;

    // Private Members of the Class
    private double deltaAngle = -1;
    private double initialAngle = -1;
    private double distance = -1;
    private double currentFrameStartID = -1;
    private double currentFrameEndID = -1;
    private double lastFrameSeen = -1;
    private double updatedDeltaAngle = -1;
    private double updatedDistance = -1;
    private double[] allHeights;
    private double[] allWidths;
    private double[] allCenters;
    private String[] allColors;
    private int numberOfObjects;

    private double correctedRotationAngle = 0;

    public Vision(RobotContainer robotContainer) {
        super(robotContainer);
        this.local_robotContainer = robotContainer;

        initializeAllEntries();

        //Initialize current start and end networktable values
        currentFrameStartID = currentFrameStartEntry.getDouble(-1);
        currentFrameEndID = currentFrameEndEntry.getDouble(-1);
        newSubsystemState = VisionState.ENABLED_TURRET;
    }

    @Override
    public void doPeriodic() {

        double currentYaw = local_robotContainer.getSubsystems().getSwerveDrive().getGyro().getYawContinuous();
        double currentPot = local_robotContainer.getSubsystems().getShooter().getAnalogPotentiometer().get();

        double currentAngle = currentYaw + currentPot;

        currentAngleEntry.setDouble(currentAngle);

        // Check to see if a state change was requested
        if (currentSubsystemState != newSubsystemState) {

            logger.info("Changing Vision State : " + newSubsystemState);

            switch (newSubsystemState) {
                case INITIALIZE:
                    break;
                case DISABLED:
                    turretState.setBoolean(false);
                    ballState.setBoolean(false);
                    break;
                case ENABLED_BALL:
                    ballState.setBoolean(true);
                    break;
                case ENABLED_TURRET:
                    turretState.setBoolean(true);
                    break;
            }
            currentSubsystemState = newSubsystemState;
        }

        // Act on the current state
        switch (currentSubsystemState) {
            case INITIALIZE:
                newSubsystemState = VisionState.DISABLED;
                break;

            case DISABLED:
                break;
            case ENABLED_BALL:
                break;

            case ENABLED_TURRET:
                // Go and grab the information from the network table
                updateNetworkTableData();

                // Check the validity of the data
                if (checkVisionObjects() == false) {
                    // Invalid data or we have already
                    // seen the frame, should not continue

                    // Force the turrent to not move
                    numberOfObjects = 0 ;
                    break;
                }

                // Now correct for the current gyro angle
                double drift = currentAngle - initialAngle;
                correctedRotationAngle = updatedDeltaAngle - drift;

                if (Math.abs(correctedRotationAngle) < 0.5) {
                    correctedRotationAngle = 0;
                }

                break;
        }


        if (numberOfObjects < 1) {
            // correctedRotationAngle = 0;
        }

        // System.out.println("Vision - Init : " + initialAngle + " Curr : " + currentAngle + " Rot Corr : " + correctedRotationAngle + " Delta Angle : " + updatedDeltaAngle + " Yaw : " + currentYaw + " Pot : " + currentPot );

    }

    @Override
    public void reset() {

    }

    /**
     * Updates all this classes private members to reflect the values in the Network Tables
     */
    private void updateNetworkTableData() {
        // NOTE : The +1 is a correction factor for the FRC Idaho event
        // This tells the shot table that we are 12 inches further away
        // than we actually are.
        currentFrameEndID = currentFrameEndEntry.getDouble(-1);

        distance = distanceEntry.getDouble(-1);
        initialAngle = initialAngleEntry.getDouble(-1);
        deltaAngle = deltaAngleEntry.getDouble(-1);
        allHeights = allHeightsEntry.getDoubleArray(new double[]{});
        allWidths = allWidthsEntry.getDoubleArray(new double[]{});
        allCenters = allCentersEntry.getDoubleArray(new double[]{});
        allColors = allColorsEntry.getStringArray(new String[]{});
        numberOfObjects = (int) numberOfObjectsEntry.getDouble(-1);

        // System.out.println("Delta Angle : " + deltaAngle);

        currentFrameStartID = currentFrameStartEntry.getDouble(-1);
    }

    /**
     * Corrects the distance and deltaAngle if the data is up to date and vision pi identified the hub + objects that
     * aren't a part of the hub
     *
     * @return true means data is up to date and false means data is not up to date
     */
    private boolean checkVisionObjects() {
        if (currentFrameStartID == currentFrameEndID) 
        {
            if (lastFrameSeen == currentFrameEndID) {
                // Already seen this frame
                // Force Update
                // updatedDistance = distance;
                updatedDeltaAngle *= 0.85;
                return false;
            } else {
                // Update the last frame seen
                lastFrameSeen = currentFrameEndID;

                updateVisionObjects();

                // updateVisionObjects() will have visionObjects.size() equal to 0 if the Network Table data is invalid:
                //      The numberOfObjects and the size of the arrays don't match. Meaning that the distance and deltaAngle
                //      are left unchanged because the validation code never ran; 
                // also the validation code shouldn't be run if there is not more than one visionObject
                // because the validation deals with adjacent visionObjects which isn't applicable if
                // there is only one

                if (visionObjects.size() > 1) {
                    // All objects found before validation; validateData() removes objects that aren't a part of the hub
                    ArrayList<VisionObject> allVisionObjects = cloneVisionObjects();
                    boolean isDataValid = false;

                    try {
                        isDataValid = validateData();
                    } catch (Exception e) {
                        logger.info("Trying to add to null Arraylist; validation code not executed");
                        // Force Update
                        e.printStackTrace();
                        updatedDistance = distance;
                        updatedDeltaAngle = deltaAngle;
                        return true;
                    }

                    // Meaning that the hub was identified, but the pi also found additional objects that aren't part of the hub
                    if (allVisionObjects.size() != visionObjects.size() && isDataValid) {
                        logger.info("Previous amount of vision objects : " + allVisionObjects.size());
                        logger.info("Corrected amount vision objects  : " + visionObjects.size());

                        updatedDistance = correctDistance();
                        updatedDeltaAngle = correctDeltaAngle();

                        // double yCenter = (getLeftmostVisionObject().top + getRightmostVisionObject().top) / 2.0;
                        // double xCenter = (getRightmostVisionObject().rightEdge + getLeftmostVisionObject().leftEdge) / 2.0;

                        logger.info("Distance Before : " + distance);
                        logger.info("Distance After : " + updatedDistance);

                        logger.info("Delta Angle Before : " + deltaAngle);
                        logger.info("Delta Angle After : " + updatedDeltaAngle);

                        // logger.info("X Center : " +  xCenter);
                        // logger.info("Y Center : " +  yCenter);
                    } else {
                        // Force Update
                        updatedDistance = distance;
                        updatedDeltaAngle = deltaAngle;
                    }
                } else {
                    // Force Update
                    updatedDistance = distance;
                    updatedDeltaAngle = deltaAngle;
                }

                return true;
            }
        } else {
            // Update the current frame just seen
            lastFrameSeen = currentFrameStartID;
            return false;
        }
    }

    private void initializeAllEntries() {

        // Definition Of Object Table
        numberOfObjectsEntry = NetworkTableInstance.getDefault().getTable(objectTableKey).getEntry(numberOfObjectsKey);
        allHeightsEntry = NetworkTableInstance.getDefault().getTable(objectTableKey).getEntry(allHeightsKey);
        allWidthsEntry = NetworkTableInstance.getDefault().getTable(objectTableKey).getEntry(allWidthsKey);
        allCentersEntry = NetworkTableInstance.getDefault().getTable(objectTableKey).getEntry(allCentersKey);
        allColorsEntry = NetworkTableInstance.getDefault().getTable(objectTableKey).getEntry(allColorsKey);

        // Definition Of Vision Table
        deltaAngleEntry = NetworkTableInstance.getDefault().getTable(visionTableKey).getEntry(deltaAngleKey);
        initialAngleEntry = NetworkTableInstance.getDefault().getTable(visionTableKey).getEntry(initialAngleKey);
        distanceEntry = NetworkTableInstance.getDefault().getTable(visionTableKey).getEntry(distanceKey);
        currentAngleEntry = NetworkTableInstance.getDefault().getTable(visionTableKey).getEntry(currentAngleKey);
        currentFrameEndEntry = NetworkTableInstance.getDefault().getTable(visionTableKey).getEntry(currentFrameEndKey);
        currentFrameStartEntry = NetworkTableInstance.getDefault().getTable(visionTableKey).getEntry(currentFrameStartKey);

        // Definition Of State Machine Table
        turretState = NetworkTableInstance.getDefault().getTable(stateMachineTableKey).getEntry(turretStateKey);
        turretState.setBoolean(false);
        ballState = NetworkTableInstance.getDefault().getTable(stateMachineTableKey).getEntry(ballStateKey);

        visionStateMachine = NetworkTableInstance.getDefault().getTable(stateMachineTableKey).getEntry(visionStateMachineKey);

    }

    /**
     * Updates the vision objects to reflect the values in the Network Tables
     */
    private void updateVisionObjects() {
        // Resets the data inside visionObjects, so new data only will be kept
        visionObjects = new ArrayList<>();

        // Meaning that the data from the network tables is invalid since the length of the arrays is not equal to the
        // Number of objects; also after returning the visionObjects.size() is equal to 0, so it just keeps the values
        // From the pi since the validation never gets triggered
        if (allHeights.length != numberOfObjects || allWidths.length != numberOfObjects ||
                allCenters.length != numberOfObjects * 2 || allColors.length != numberOfObjects) {

            // logger.info("Invalid Network Table Data");
            return;
        }

        for (var i = 0; i < numberOfObjects; i++) {
            double height = allHeights[i];
            double width = allWidths[i];

//          Have to double it because the first entry is xCenter and the second entry is yCenter, so it is double the size
            double xCenter = allCenters[i * 2];
            double yCenter = allCenters[i * 2 + 1];

            String color = allColors[i];

            visionObjects.add(new VisionObject(height, width, xCenter, yCenter, color));
        }
    }

    /**
     * Stores the data for the bounding box of a thing that Visio Pi saw
     */
    private class VisionObject {
        public final double leftEdge; // Top Left Corner
        public final double top; // Top Left Corner
        public final double height;
        public final double width;
        public final String color;
        public final double xCenter;
        public final double yCenter;
        public final double bottom;
        public final double rightEdge;
        public final double widthToHeightRatio;

        public VisionObject(double height, double width, double xCenter, double yCenter, String color) {
            // Values from the NetworkTables
            this.height = height;
            this.width = width;
            this.xCenter = xCenter;
            this.yCenter = yCenter;
            this.color = color;

            // Calculated values using math
            top = yCenter - (height / 2);
            leftEdge = xCenter - (width / 2);

            bottom = top + height;
            rightEdge = leftEdge + width;

            widthToHeightRatio = width / height;
        }

        @Override
        public String toString() {
            return "leftEdge" + leftEdge + "top" + top + "width" + width + "height" + height;
        }
    }

    /**
     * @return the current vision objects; used so the changes to vision objects doesn't affect the clone
     */
    private ArrayList<VisionObject> cloneVisionObjects() {
        ArrayList<VisionObject> clone = new ArrayList<>();

        for (var visionObject : visionObjects) {
            clone.add(visionObject);
        }
        return clone;
    }

    /**
     * Keeps only the vision objects that are part of the hub
     *
     * @return whether the hub was identified (therefore valid data)
     */
    private boolean validateData() {
        keepOnlyCoplanarObjects();

        // The hub should have at least two pieces of tape visible
        return visionObjects.size() >= 2;
    }

    public VisionObject getLeftmostVisionObject() {
        return visionObjects.get(0);
    }

    private VisionObject getRightmostVisionObject() {
        return visionObjects.get(visionObjects.size() - 1);
    }

    /**
     * Corrects the distance from the hub when vision pi has identified an object that isn't part of the hub
     */
    private double correctDistance() {
        // Storing this value for logging
        double corrected_distance = (getLeftmostVisionObject().top + getRightmostVisionObject().top) / 2.0;

        // New Calibration curve for Sassitator 10/27/2022 
        return ((1.8856e-4 * corrected_distance * corrected_distance) - (1.415e-2 * corrected_distance) + 8.4486);
    }


    /**
     * @param want                the value that is wanted
     * @param got                 the value that is being tested to see if it is
     *                            similar to 'want'
     * @param deviationAcceptable the amount that is acceptable for 'got' to be
     *                            off of 'want'
     * @return if got is similar to want
     */
    private boolean isSimilar(double want, double got, double deviationAcceptable) {
        double upperBound = want + deviationAcceptable;
        double lowerBound = want - deviationAcceptable;

        return got >= lowerBound && got <= upperBound;
    }

    /**
     * @param number              the number that will be used to find the index of the item in the list that is similar to it
     * @param list                the items in the list will be compared to see if they are similar
     * @param deviationAcceptable the amount that 'number' can differ from an item in the list to be considered similar
     * @return the number in the parameter 'list' that is similar the parameter 'number'
     * @return the index of the similar item in the list if an item is similar otherwise -1
     */
    private int getIndexOfSimilarNumber(double number, ArrayList<Double> list, double deviationAcceptable) {
        int returnValue = -1;

        for (var i = 0; i < list.size(); i++) {
            if (isSimilar(number, list.get(i), deviationAcceptable)) {
                returnValue = i;
            }
        }
        return returnValue;
    }

    /**
     * visionObjects must have at least one object in it otherwise this will throw an error
     *
     * @return whether testedVisionObjects have the valid distances between adjacent objects to be the tape on the hub
     */
    private boolean distancesAreValid(ArrayList<VisionObject> testedVisionObjects) {
        boolean distancesBetweenObjectsIsValid = true;
        sortVisionObjects();
        VisionObject prevVisionObject = testedVisionObjects.get(0);
        ArrayList<Double> distances = new ArrayList<>();

        for (var i = 1; i < testedVisionObjects.size(); i++) {
            double distance = testedVisionObjects.get(i).leftEdge - prevVisionObject.rightEdge;
            distances.add(distance);
            double distanceToLengthRatio = distance / testedVisionObjects.get(i).width;

            // The last Vision Object (Retroreflective tape) could be smaller because the camera only caught
            // the end tape because of the curvature of the hub; so this object won't be taken into account
            if (i == testedVisionObjects.size() - 1) {
                continue;
            }

            double actualDistanceToLengthRatio = 1.1;
            if (!isSimilar(actualDistanceToLengthRatio, distanceToLengthRatio, 4)) {
                distancesBetweenObjectsIsValid = false;
                break;
            }

            prevVisionObject = visionObjects.get(i);
        }

        return distancesBetweenObjectsIsValid && numbersAreSimilar(distances, 100);
    }

    /**
     * @return whether the difference between the maximum number and minimum number is within 'deviationAcceptable'
     * This being a metric if all the numbers are similar
     */
    private boolean numbersAreSimilar(ArrayList<Double> numbers, int deviationAcceptable) {
        // So the min and max start out as a valid value
        if (numbers.size() == 0) {
            return false;
        }
        double min = numbers.get(0);
        double max = numbers.get(0);

        for (double number : numbers) {
            if (number < min) {
                min = number;
            }

            if (number > max) {
                max = number;
            }
        }
        double difference = max - min;

        // This is the percentage off minimum is from the maximum
        double percentOff = (difference / max) * 100;

        return percentOff <= deviationAcceptable;
    }

    /**
     * Corrects the deltaAngle to the hub when vision pi has identified an object that isn't part of the hub
     */
    private double correctDeltaAngle() {
        double image_width = 640;        // Number of pixels in image
        double FOV_angle = 75.76079874010732;
        double optical_correction = (640 / 2) - 336;
        double centerX = (getRightmostVisionObject().rightEdge + getLeftmostVisionObject().leftEdge) / 2.0;

        double centerLocationPoint = centerX + optical_correction;

        // Now compute the angular offset
        double turretToAngleRotate = (centerLocationPoint - image_width / 2) * (FOV_angle / image_width);

        if (turretToAngleRotate > 90) {
            turretToAngleRotate = turretToAngleRotate - 90;
        }

        return turretToAngleRotate;

    }

    /**
     * Sorts the visionObjects from smallest to biggest leftEdge
     */
    private void sortVisionObjects() {
        Comparator<VisionObject> comparator = (a, b) -> (int) (a.leftEdge - b.leftEdge);
        Collections.sort(visionObjects, comparator);
    }

    /**
     * @return a HashMap with [top, bottom] for the key and visionObjects on
     * that plane for the value
     */
    private HashMap<String, ArrayList<VisionObject>> getPlaneToObjectsOnPlane() {
        HashMap<String, ArrayList<VisionObject>> planeToObjectsOnPlane = new HashMap<>();
        ArrayList<Double> allTops = new ArrayList<>();
        ArrayList<Double> allBottoms = new ArrayList<>();

        for (VisionObject visionObject : visionObjects) {
            double deviationAcceptable = visionObject.height * 5;
            int topIndex = getIndexOfSimilarNumber(visionObject.top, allTops, deviationAcceptable);
            int bottomIndex = getIndexOfSimilarNumber(visionObject.bottom, allBottoms, deviationAcceptable);

            Double[] coordinates = {visionObject.top, visionObject.bottom};

            // getIndexOfSimilarNumber() returns -1 if no item in the list matches
            if (topIndex == -1 || bottomIndex == -1) {
                // Have to initialize the ArrayList for those coordinates
                planeToObjectsOnPlane.put(Arrays.toString(coordinates), new ArrayList<>());
                allTops.add(visionObject.top);
                allBottoms.add(visionObject.bottom);
            } else {
                changeKey(Arrays.toString(new Double[]{allTops.get(topIndex), allBottoms.get(bottomIndex)}),
                        Arrays.toString(coordinates), planeToObjectsOnPlane);

                allTops.set(topIndex, visionObject.top);
                allBottoms.set(bottomIndex, visionObject.bottom);

            }
            planeToObjectsOnPlane.get(Arrays.toString(coordinates)).add(visionObject);

        }
        return planeToObjectsOnPlane;
    }

    /**
     * Modifies the property visionObjects so that only objects with roughly
     * the same top and bottom (on the same plane) remain and
     */
    private void keepOnlyCoplanarObjects() {
        // Sorts the vision objects from smallest to largest left edge
        sortVisionObjects();
        // Plane [top, bottom] is the key and the value is the visionObjects on
        // that plane
        HashMap<String, ArrayList<VisionObject>> planeToObjectsOnPlane = getPlaneToObjectsOnPlane();
        // Decided by which one has the most amount of objects on the same plane
        ArrayList<VisionObject> coplanarVisionObjects = new ArrayList<>();

        for (ArrayList<VisionObject> objectsOnPlane : planeToObjectsOnPlane.values()) {
            if (objectsOnPlane.size() > coplanarVisionObjects.size() && distancesAreValid(objectsOnPlane)) {
                coplanarVisionObjects = objectsOnPlane;
            }
        }
        visionObjects = coplanarVisionObjects;
    }

    public double getCorrectedRotationAngle() {
        double[] xyz = new double[3];
        ((TatorPigeon) local_robotContainer.getSubsystems().getSwerveDrive().getGyro()).getRawGyro(xyz);
        return correctedRotationAngle;

    }

    public double getShootOnFlyAngle() {
        SwerveDrive swerveDrive = local_robotContainer.getSubsystems().getSwerveDrive();
        shootFlyVector = shootingOnFlyCompensation(Math.toRadians(correctedRotationAngle),
                distance, swerveDrive.getAverageVector().scale(39.37 / 12.0).addTheta(-swerveDrive.getRotation()));


        //w = (w * v/(||v||^2) * )v
        return Math.toDegrees(shootFlyVector.getTheta() > Math.PI ? shootFlyVector.getTheta() - 2 * Math.PI : shootFlyVector.getTheta());
    }

    public double getShootFlyDistance() {
        SwerveDrive swerveDrive = local_robotContainer.getSubsystems().getSwerveDrive();
        shootFlyVector = shootingOnFlyCompensation(Math.toRadians(correctedRotationAngle),
                distance, swerveDrive.getAverageVector().scale(39.37 / 12.0).addTheta(-swerveDrive.getRotation()));
        if (shootFlyVector.getMagnitude() < 22) {
            return shootFlyVector.getMagnitude();
        } else {
            return 22;
        }
    }

    //velocity.addTheta(-swerveDrive.getRotation)
    public Vector shootingOnFlyCompensation(double theta, double distance, Vector velocity) {
//        Vector urade = new Vector();
//        Vector uorth = new Vector();
//        urade.setPolar(theta + 3.1415 / 2.0, 1);
//        uorth.setPolar(theta, 1);
//        double time = 1.5;
//        Vector rade = velocity.project(urade);
//        Vector rade = velocity.project(urade);
//        Vector orth = velocity.project(uorth);
//
//        double distanceOnFly = rade.getMagnitude() * time + distance;
//
        if (visionObjects.size() < 1) {
            return new Vector(0, 0);
        }
        double time = 0;
        Vector out = new Vector();
        out.setPolar(theta, distance);
        out.add(velocity.scale(time));
        return out;
    }

    /**
     * Changes the hash map's old key to the new key while keeping the same value
     *
     * @param oldKey  the key that will be replaced by the new key
     * @param newKey  the key that will replace the old key
     * @param hashMap the hash map with the keys and values
     */
    private void changeKey(String oldKey, String newKey, HashMap<String, ArrayList<VisionObject>> hashMap) {
        ArrayList<VisionObject> value = hashMap.get(oldKey);
        hashMap.remove(oldKey);
        hashMap.put(newKey, value);
    }

    public double getDistance() {
        return distance;
    }

    public NetworkTableEntry getTurretState() {
        return turretState;
    }
}


