package common.Util;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import edu.wpi.first.cameraserver.CameraServer;
import edu.wpi.first.cscore.CvSink;
import edu.wpi.first.cscore.CvSource;
import edu.wpi.first.cscore.UsbCamera;
import edu.wpi.first.cscore.VideoMode.PixelFormat;

public class Camera implements Runnable {
    private CvSink cvSink;
    private CvSource outputStream;
    // private Timer timer;
    private Mat colorPicture;
    private Mat greyPicture;
    private final int kWidth = 320;
    private final int kHeight = 240;
    private final int cameraFPS = 5;

    public Camera() {

        // Creates a UsbCamera object
        UsbCamera visionCam = new UsbCamera("CamInput", 0);

        // Wrap the camera resolution with a try
        try {
            visionCam.setResolution(kWidth, kHeight);
            // visionCam.setVideoMode(PixelFormat.kGray, kWidth, kHeight, cameraFPS);
        } catch ( Exception e) {
            // Do nothing
        }

        CameraServer.startAutomaticCapture(visionCam);

        // Creates the CvSink and connects it to the UsbCamera
        cvSink = CameraServer.getVideo();
        colorPicture = new Mat();
        greyPicture = new Mat();

        // Creates the CvSource and MjpegServer and connects them
        outputStream = CameraServer.putVideo("CamOutput", kWidth, kHeight);
        // outputStream.setPixelFormat(PixelFormat.kGray);
        
        // timer = new Timer();
        // timer.start();
    }

    public void updateCamera() {
        // if (timer.hasPeriodElapsed(1.0 / cameraFPS)) {
        //     timer.restart();

            if (cvSink.grabFrame(colorPicture) == 0) {
                // Send the output the error.
                outputStream.notifyError(cvSink.getError());
                // skip the rest of the current iteration
            } else {
                // Give the output stream a new image to display
                // Imgproc.cvtColor(colorPicture, greyPicture, Imgproc.COLOR_BGR2GRAY);
                outputStream.putFrame(colorPicture);
            }
        // }
    }

    @Override
    public void run() {

        while(true){
        updateCamera();
        try{
            Thread.sleep(1000/cameraFPS);
        } catch (Exception e){

        }
        
    }
    }

    public void reset() {}
  
}