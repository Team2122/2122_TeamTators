package common.Util;

import java.util.List;
import org.photonvision.PhotonCamera;
import org.photonvision.targeting.PhotonPipelineResult;
import org.photonvision.targeting.PhotonTrackedTarget;

public class Vision {
    private PhotonCamera photonCamera;

    public Vision() {
        // Change the name of your camera here to whatever it is in the PhotonVision UI.
        photonCamera = new PhotonCamera(VisionConstants.cameraName);
    }

    public PhotonPipelineResult getResult(){
        return photonCamera.getLatestResult();
    }

    public List<PhotonTrackedTarget> getTargets() {
        var result = photonCamera.getLatestResult();
        return result.getTargets();
    }

    public double getLatencySeconds(){
        var result = photonCamera.getLatestResult();
        return result.getLatencyMillis();
    }


    public static List<PhotonTrackedTarget> getTargets(PhotonPipelineResult result) {
        // var result = photonCamera.getLatestResult();
        return result.getTargets();
    }

    public static double getLatencySeconds(PhotonPipelineResult result){
        // var result = photonCamera.getLatestResult();
        return result.getLatencyMillis();
    }

}