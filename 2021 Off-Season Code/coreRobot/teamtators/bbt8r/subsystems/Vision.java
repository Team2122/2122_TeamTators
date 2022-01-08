package org.teamtators.bbt8r.subsystems;

import edu.wpi.first.networktables.EntryListenerFlags;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import org.teamtators.common.control.Timer;
import org.teamtators.common.scheduler.Subsystem;
import java.util.concurrent.atomic.AtomicReference;

public class Vision extends Subsystem {

    private Config config;
    private final String visionTableKey = "visionTable";
    private final String deltaAngleKey = "deltaAngle";
    private final String initialAngleKey = "initialAngle";
    private final String distanceKey = "distance";
    private final String timeReceivedKey = "timeReceived";
    private final String currentAngleKey = "currentAngle";
    private final String visionStatusKey = "visionStatus";
    private final String visionBrightnessKey = "visionBrightness";
    private final String visionExposureKey = "visionExposure" ;

    private NetworkTableEntry deltaAngleEntry;
    private NetworkTableEntry initialAngleEntry;
    private NetworkTableEntry distanceEntry;
    private NetworkTableEntry timeReceivedEntry;
    private NetworkTableEntry currentAngleEntry;
    private NetworkTableEntry visionStatusEntry;
    private NetworkTableEntry visionBrightnessEntry;
    private NetworkTableEntry visionExposureEntry;
    private AtomicReference<VisionData> visionData;

    private boolean localDebug = false;

    public Vision() {

        super("Vision");

        visionData = new AtomicReference<>(new VisionData());

        deltaAngleEntry = NetworkTableInstance.getDefault().getTable(visionTableKey).getEntry(deltaAngleKey);
        initialAngleEntry = NetworkTableInstance.getDefault().getTable(visionTableKey).getEntry(initialAngleKey);
        distanceEntry = NetworkTableInstance.getDefault().getTable(visionTableKey).getEntry(distanceKey);
        timeReceivedEntry = NetworkTableInstance.getDefault().getTable(visionTableKey).getEntry(timeReceivedKey);
        visionStatusEntry = NetworkTableInstance.getDefault().getTable(visionTableKey).getEntry(visionStatusKey);
        visionExposureEntry = NetworkTableInstance.getDefault().getTable(visionTableKey).getEntry(visionExposureKey);
        currentAngleEntry = NetworkTableInstance.getDefault().getTable(visionTableKey).getEntry(currentAngleKey);
        visionBrightnessEntry = NetworkTableInstance.getDefault().getTable(visionTableKey).getEntry(visionBrightnessKey);

        timeReceivedEntry.addListener(timeReceivedListener -> {
            VisionData newData = new VisionData();
            if ( localDebug )
                System.out.println("New VisionData Received!");
            newData.deltaAngle = deltaAngleEntry.getDouble(0);
            newData.initialAngle = initialAngleEntry.getDouble(-1);
            newData.distance = distanceEntry.getDouble(0);
            newData.timeReceived = timeReceivedEntry.getDouble(0);
            newData.visionBrightness = visionBrightnessEntry.getDouble(0);
            newData.checkValidity();
            visionData.set(newData);
        }, EntryListenerFlags.kUpdate | EntryListenerFlags.kImmediate);
    }

    public void setVisionStatus(boolean state) { 
        // Sets the vision status to the argument state
        visionStatusEntry.setBoolean(state);
    }

    public boolean getVisionStatus() { 
        // Gets the current vision status
        return visionStatusEntry.getBoolean(false);
    }

    public void toggleVisionStatus() { 
        // Changes the current status of vision
        visionStatusEntry.setBoolean(!visionStatusEntry.getBoolean(true));
    }

    public class VisionData { 
        // A class for holding vision data
        public boolean valid = true; // A boolean for if the data is valid or not

        public double deltaAngle = 0; // The change in angle necessary
        public double initialAngle = 140; // The current angle at the time the photo was captured
        public double distance = 0; // The distance from the target
        public double timeReceived = 0; // The time vision took the photo
        public double visionBrightness = 0; // The current brightness of the visionLight

        public boolean checkValidity() { // Returns true if the data is valid
            if (timeReceived == -1) {
                // logger.warn("Vision has notified invalid data!");
                return false;
            } else {
                return true;
            }
        }
    }

    public void sendData(double angle) { 
        // Sends the data from the robot required by vision
        if ( localDebug )
            logger.info("Sending Data: " + angle);
        
        currentAngleEntry.setDouble(angle);
    }

    public VisionData getData() { 
        // Gets the most recent visionData
        return visionData.get();
    }

    public double distanceToFlywheelSpeed(double distance) { 
        // Returns the flywheel speed for the current distance
        return 0;
    }

    public boolean checkPiStatus() { 
        // Checks if we are connected to the pi
        return checkPiStatus(getData());
    }

    private boolean checkPiStatus(VisionData data) {
        // Checks if we are connected to the pi
        if (Timer.getTimestamp() - data.timeReceived < config.staleTime) {
            return true;
        } else {
            // logger.warn("Stale Data From PI");
            return false;
        }
    }

    public void configure(Config config) {
        this.config = config;
        localDebug = config.debug;
    }

    public static class Config {
        public double staleTime;
        public boolean debug;
    }

}