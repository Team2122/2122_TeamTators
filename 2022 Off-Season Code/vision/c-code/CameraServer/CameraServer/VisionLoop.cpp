#include <iostream>
#include "VisionLib.hpp"
#include "Utilities.h"
#include <math.h>

#include "opencv2/videoio.hpp"
#include <opencv2/opencv.hpp>

#include "AprilTagPipeline.h"

#include "networktables/NetworkTableInstance.h"
#include "networktables/NetworkTableEntry.h"

#include "cameraserver/CameraServer.h"
#include "NetworkTables.h"
#include "SocketServer.h"
#include "Camera.h"

#include "time.h"

#define MAX_JSON_STRING_LENGTH 4096

// int cameraID[2] = { 4, 0 };
// int cameraID[2] = { 2, 0 }; // This is the the Robot Pi 2+
// int cameraID[2] = { 0, 2 }; // This is the the Practice Pi 2

// int cameraID[2] = { 2, 0 };    // This is for Marks Home Pi
int cameraID[1] = { 0 };    // This is for Marks Home Pi

#define countof(array) (sizeof(array) / sizeof(array[0]))

SENSOR_ORIENTATION current_orientation;
typedef struct image_u8 image_u8_t;

// Function Prototypes for dispaying Shuffleboard Images
void grabTurretFeed(Vision_Pipeline* turretVision, Image_Store* turretImage, cs::CvSource* turretFeed, bool monoFeed, bool debugInfoState);
void grabPickerFeed(Vision_Pipeline* groundVision, Image_Store* groundcamImage, cs::CvSource* groundFeed, bool monoFeed, bool debugInfoState);
void loadConfig(PipelineConfig* pipeline_config);

void runSocketServer( int serverPort, PipelineConfig* myPipelineConfig )
{
    // Initiate the socket connection
    SocketServer mySocketTest( serverPort, myPipelineConfig );

    // Assuming all is well and good, run the server
    mySocketTest.runServer();
}

int legacyCodeView(bool debugInfoState)
{
    bool visionStateLogging = true;

    // Create a network table instance 
    NetworkTables NT_instance;

    Camera* cam0 = NULL;
    Camera* cam1 = NULL;
    Camera* cam2 = NULL;

    // Start off by building a vision pipeline for the Turret Cam
    current_orientation = SENSOR_ORIENTATION::LANDSCAPE;
    Vision_Pipeline turretVision( 0.1, 17, current_orientation, TARGET_DETECTION_TYPE::HEXAGON);

    // Build an AprilTag pipeline for the Turret Cam
    AprilTagPipeline aprilTagPipeLine(tagID::TAG_36H11);

    // Kick off the web-socket server to receive requests from the EWS
    std::thread* socketThread;
    socketThread = new std::thread(runSocketServer, SERVER_PORT, turretVision.pipeline_config );

    std::cout << "\tOpening Turret Camera Stream\n";
    cam0 = new PiCamera( cameraID[0] ) ;
    
    if (cam0->cam != NULL)
    {
        // Configure which settings the camera supports
        turretVision.pipeline_config->setCameraConfiguration(cam0, CAMERA_IN_USE::TURRETCAM);
        turretVision.pipeline_config->setNetworkTableInstance(&NT_instance);

        // Set up the camera
        turretVision.pipeline_config->setCameraCaptureSettings();
        turretVision.pipeline_config->setCameraFieldSettings();

        // Set up Parameters for Detecting the Hub
        turretVision.pipeline_config->setRGB_ClippingLUT(128, 255);
        turretVision.pipeline_config->setHue_ClippingLUT(125, 200);

        turretVision.pipeline_config->setAspectRatioRange(1.0, 5.0);
        turretVision.pipeline_config->setBrightnessRange(75, 100);
        turretVision.pipeline_config->setFullnessRange(40, 100);
        turretVision.pipeline_config->setPercentAreaRange(0.0, 10 );

        turretVision.pipeline_config->setThreshold(0.6);

        turretVision.pipeline_config->setTargetType(TARGET_DETECTION_TYPE::HUB);

        // Checking if there is a config file and loading it if there is one 
        // and using existing values if not present.
        // loadConfig(turretVision.pipeline_config);

    }
    else {
        printf("Could not open Turret Camera Feed\n");
        return EXIT_FAILURE;
    }


    if (debugInfoState) {
        // Figure out which settings the camera supports
        std::cout << "\n";
        std::cout << "\tTurret Camera Settings\n";
        turretVision.pipeline_config->printCameraSupportSettings();
    }
    
    // Start off by building a vision pipeline for the Ground Cam
    Vision_Pipeline groundVision(0.25, 17, SENSOR_ORIENTATION::LANDSCAPE, TARGET_DETECTION_TYPE::BALL);

    // Check to see if the 2nd Camera is enabled
    if (countof(cameraID) >= 2)
    {
        std::cout << "\tOpening Ground Camera Stream\n";
        cam1 = new ELPCamera(cameraID[1]);

        if (cam1->cam != NULL)
        {
            // Configure which settings the camera supports
            groundVision.pipeline_config->setCameraConfiguration(cam1, CAMERA_IN_USE::BALLCAM);
            groundVision.pipeline_config->setNetworkTableInstance(&NT_instance);

            // Hard Code settings for now
            groundVision.pipeline_config->setCameraAutoExposure(true);
            // groundVision.pipeline_config->setCameraManualExposure(1500);
            // groundVision.pipeline_config->setCameraManualExposure(500);
            //groundVision.pipeline_config->setCameraBrightness(100 * 128 / 255.0);
            groundVision.pipeline_config->setCameraBrightness(100 * 32 / 255.0);
            // groundVision.pipeline_config->setCameraContrast(100 * 32 / 127.0);
            groundVision.pipeline_config->setCameraContrast(100 * 48 / 127.0);
            groundVision.pipeline_config->setCameraSaturation(100 * 64 / 127.0);
            groundVision.pipeline_config->setCameraSharpness(100 * 5 / 15.0);
            groundVision.pipeline_config->setSensorOrientation(SENSOR_ORIENTATION::LANDSCAPE);

            // Set up the camera
            groundVision.pipeline_config->setCameraCaptureSettings();
            groundVision.pipeline_config->setCameraFieldSettings();

            groundVision.pipeline_config->setTargetLab(0, 133/2.55, 205-128, 191-128); // Blue Ball
            groundVision.pipeline_config->setTargetLab(1, 94/2.55, 152-128, 65-128);  // Red Ball
            groundVision.pipeline_config->setNumTargetColors(2);
            groundVision.pipeline_config->setThreshold(0.65);

            if (debugInfoState)
            {
                // Figure out which settings the camera supports
                std::cout << "\n";
                std::cout << "\tGround Camera Settings\n";
                groundVision.pipeline_config->printCameraSupportSettings();
            }
        }
    }

    // Start off by building a vision pipeline for the Ground Cam
    Vision_Pipeline pickerVision( 0.25, 17, SENSOR_ORIENTATION::LANDSCAPE, TARGET_DETECTION_TYPE::BALL );

    // Check to see if the 3rd Camera is enabled
    if (countof(cameraID) >= 3)
    {
        std::cout << "\tOpening Picker Camera Stream\n";
        cam2 = new PiCamera(cameraID[2]);

        if (cam2->cam != NULL) {

            // Configure which settings the camera supports
            pickerVision.pipeline_config->setCameraConfiguration(cam2, CAMERA_IN_USE::BALLCAM);
            pickerVision.pipeline_config->setNetworkTableInstance(&NT_instance);

            // Set up the camera
            pickerVision.pipeline_config->setCameraCaptureSettings();
            pickerVision.pipeline_config->setCameraFieldSettings();

            if (debugInfoState)
            {
                // Figure out which settings the camera supports
                std::cout << "\n";
                std::cout << "\tPicker Camera Settings\n";
                pickerVision.pipeline_config->printCameraSupportSettings();
                std::cout << "\n";
            }
        }
    }

 
    std::cout << "\tFinished Creating Camera Streams\n";

    // Initalize the Camera objects
    cv::Mat* rawTurretCam = new cv::Mat();
    cv::Mat* rawGroundCam = new cv::Mat();
    cv::Mat* rawPickerCam = new cv::Mat();

    // Write the current frame to the network table
    cs::CvSource turretFeed;
    cs::CvSource groundFeed;
    cs::CvSource pickerFeed;

    if ((cam0 == NULL) || (cam0->cam == NULL) || !cam0->cam->isOpened()) {
        std::cout << "Turret Camera could not be opened, returning.\n";
        return 1;
    }
    else {
        //Reading the Turret Camera
        std::cout << "Reading from Turret Camera\n";
        // Opening the Turret Camera
        cam0->cam->grab();
        cam0->cam->read(*rawTurretCam);
    }

    if ( (cam1 == NULL) || ( cam1->cam == NULL ) || !cam1->cam->isOpened()) {
        std::cout << "Ground Camera could not be opened, disabling camera feed.\n";
    }
    else {
        std::cout << "Reading from Ground Camera\n";
        // Opening the ground camera
        cam1->cam->grab();
        cam1->cam->read(*rawGroundCam);
    }

    if ( (cam2 == NULL) || (cam2->cam == NULL) || !cam2->cam->isOpened()) {
        std::cout << "Picker Camera could not be opened, disabling camera feed.\n";
    }
    else {
        std::cout << "Reading from Picker Camera\n";
        // Opening the ground camera
        cam2->cam->grab();
        cam2->cam->read(*rawPickerCam);
    }

    int time = 0;

    // Setting up the target vision pipeline
    Image_Store* targetImage = new Image_Store();
    targetImage->set_attributes(rawTurretCam->cols, rawTurretCam->rows, 3, 255, PIXEL_FORMAT::RGB, INTERLEAVE_FORMAT::PIXEL_INTERLEAVED);
    targetImage->image = rawTurretCam->data;

    if (debugInfoState) {
        write_image_store(targetImage, PNM_FORMAT::BINARY, "./Runtime_Capture//Raw_Turret_Cam_Capture.ppm");
    }

    // Run target capture process to initalize the pipeline
    run_idle_process(targetImage, &turretVision);

    if (debugInfoState) {
        write_image_store(targetImage, PNM_FORMAT::BINARY, "./Runtime_Capture//Turret_Cam_Capture.ppm");
        write_image_store(turretVision.scaled_image, PNM_FORMAT::BINARY, "./Runtime_Capture//Turret_Cam_Rotated.ppm");
    }

    if (cam0->cam->isOpened()) 
    {
        // Set up OpenCV output stream of TurretCam for Shuffleboard viewing
        std::cout << "Camera Stream 0 Open\n";
        cv::Mat turretCamOutput = cv::Mat(turretVision.scaled_image->height, turretVision.scaled_image->width, CV_8UC3);

        if ((turretVision.pipeline_config->getSensorOrientation() == SENSOR_ORIENTATION::PORTRAIT) ||
            (turretVision.pipeline_config->getSensorOrientation() == SENSOR_ORIENTATION::PORTRAIT_UD)) {
            // Portrait Camera Feed
            turretFeed = frc::CameraServer::GetInstance()->PutVideo(NT_instance.visionNTEntry, turretCamOutput.rows, turretCamOutput.cols);
        } else {
            // Landscape Camera Feed
            turretFeed = frc::CameraServer::GetInstance()->PutVideo(NT_instance.visionNTEntry, turretCamOutput.cols, turretCamOutput.rows);
        }

    }

    // Needs to be declared outside of the scope because it is used later
    Image_Store* groundImage = new Image_Store();

    if ( (cam1 != NULL) && (cam1->cam != NULL) && cam1->cam->isOpened() )
    {
        // Set up image attributes
        groundImage->set_attributes(rawGroundCam->cols, rawGroundCam->rows, 3, 255, PIXEL_FORMAT::RGB, INTERLEAVE_FORMAT::PIXEL_INTERLEAVED);
        groundImage->image = rawGroundCam->data;

        if (debugInfoState) {
            write_image_store(groundImage, PNM_FORMAT::BINARY, "./Runtime_Capture//Raw_Ground_Cam_Capture.ppm");
        }

        run_idle_process(groundImage, &groundVision);

        std::cout << "Camera Stream 1 Open\n";
        cv::Mat groundCamOutput = cv::Mat(groundVision.scaled_image->height, groundVision.scaled_image->width, CV_8UC3);

        // Set up the network table connection
        groundFeed = frc::CameraServer::GetInstance()->PutVideo(NT_instance.groundNTEntry, groundCamOutput.cols, groundCamOutput.rows);

        if (debugInfoState)
            write_image_store(groundImage, PNM_FORMAT::BINARY, "./Runtime_Capture//Ground_Cam_Capture.ppm");
    }

    // Needs to be declared outside of the scope because it is used later
    Image_Store* pickerImage = new Image_Store();

    if ( (cam2 != NULL) && (cam2->cam != NULL) && cam2->cam->isOpened())
    {
        // Set up image attributes
        pickerImage->set_attributes(rawPickerCam->cols, rawPickerCam->rows, 3, 255, PIXEL_FORMAT::RGB, INTERLEAVE_FORMAT::PIXEL_INTERLEAVED);
        pickerImage->image = rawPickerCam->data;

        if (debugInfoState) {
            write_image_store(pickerImage, PNM_FORMAT::BINARY, "./Runtime_Capture//Raw_Picker_Cam_Capture.ppm");
        }

        run_idle_process(pickerImage, &pickerVision);

        std::cout << "Camera Stream 2 Open\n";
        cv::Mat pickerCamOutput = cv::Mat(pickerVision.scaled_image->height, pickerVision.scaled_image->width, CV_8UC3);

        // Set up the network table connection
        pickerFeed = frc::CameraServer::GetInstance()->PutVideo(NT_instance.pickerNTEntry, pickerCamOutput.cols, pickerCamOutput.rows);

        if (debugInfoState)
            write_image_store(pickerImage, PNM_FORMAT::BINARY, "./Runtime_Capture//Picker_Cam_Capture.ppm");
    }

    // Initialize the Frame Rate timers
    auto frameRateTimer_Start = std::chrono::high_resolution_clock::now();
    auto frameRateTimer_Current = frameRateTimer_Start;
    auto frameRateTimer_Duration = std::chrono::duration_cast<std::chrono::milliseconds>(frameRateTimer_Current - frameRateTimer_Start);

    // Polling Wait time for the NT Server Updates
    auto networkTableTimer_Start = frameRateTimer_Start;
    auto networkTableTimer_Duration = std::chrono::duration_cast<std::chrono::milliseconds>(frameRateTimer_Current - networkTableTimer_Start);
    auto networkTableTimer_WaitTime = std::chrono::milliseconds( 5000 ); // 5 Second poll time

    // Initialize the debug timers
    auto totalStartTime = std::chrono::high_resolution_clock::now();
    auto grabStartTime = totalStartTime;
    auto grabEndTime = totalStartTime;
    auto processStartTime = totalStartTime;
    auto processEndTime = totalStartTime;
    auto streamStartTime = totalStartTime;
    auto streamEndTime = totalStartTime;
    auto networkTablesStartTime = totalStartTime;
    auto networkTablesEndTime = totalStartTime;
    auto totalEndTime = totalStartTime;

    // Initialize the Network Table entries
    NT_instance.setupListenerServices( & NT_instance, turretVision.pipeline_config );

    // Update the shuffleboard STATE
    NT_instance.visionStateMachineEntry.SetString( "SHUFFLEBOARD" );

    // Read from the network tables
    NT_instance.vision_frameRateLimit = NT_instance.frameRateLimitEntry.GetDouble(INITIAL_FRAME_RATE);
    NT_instance.frameRateTimer_waitTime = std::chrono::milliseconds((long)(1000.0 / NT_instance.vision_frameRateLimit));

    // Print out the network Tables
    if (debugInfoState)
        NT_instance.printNetworkTableInfo();

    while (true)
    { 

        // Check to see if the state has changed
        if (NT_instance.vision_trackingState_current != NT_instance.vision_trackingState_new )
        {

            if (debugInfoState) {
                // Code used for debugging
                // printf("NT Instance ptr : %x\n", NT_instance);
                printf("Vision state has changed\n");
                printf("~~~~~~~~~~~~~~~~~~~~~~~~\n");
                printf("Current CameraServer State : %d\n", NT_instance.vision_trackingState_current);
                printf("New CameraServer State     : %d\n", NT_instance.vision_trackingState_new);
                printf("Vision FrameRate NT State  : %g\n", NT_instance.vision_frameRateLimit);

                //printf("Vision Brightness NT State : %g\n", NT_instance.vision_brightnessState);
                //printf("Vision Exposure NT State   : %g\n", NT_instance.vision_exposure);

                NT_instance.printNetworkTableInfo();
            }

            // Swith the video feed based on the state
            switch (NT_instance.vision_trackingState_new)
            {
                case VISION_STATE::THRESHOLDING:
                {
                    // Update the Console
                    std::cout << "Switching to EWS Viewing Mode\n";
                    NT_instance.visionStateMachineEntry.SetString("EWS CONFIGURATION");
                    NT_instance.visionStatusEntry.SetBoolean(false);
                    NT_instance.ballDetectStatusEntry.SetBoolean(false);

                    if (NT_instance.vision_trackingState_current != VISION_STATE::TARGET_DETECTION) {
                        // Reload the configuration
                        loadConfig(turretVision.pipeline_config);
                        // Set up camera for on field viewing
                        LED_Turn_ON();
                    }

                    break;
                }

                case VISION_STATE::SHUFFLEBOARD:
                {
                    // Update the Console
                    std::cout << "Switching to Shuffleboard Viewing Mode\n";
                    NT_instance.visionStateMachineEntry.SetString("SHUFFLEBOARD");
                    NT_instance.visionStatusEntry.SetBoolean(false);
                    NT_instance.ballDetectStatusEntry.SetBoolean(false);

                    if (debugInfoState) {
                        turretVision.pipeline_config->setVisionLibDebugStr("ShuffleBoard");
                        turretVision.pipeline_config->setVisionLibDebugMode(false);
                        turretVision.pipeline_config->setVisionLibCaptureMode(false);
                        turretVision.pipeline_config->setVisionLibCaptureInc(0);
                    }

                    // Set up camera for on field viewing
                    turretVision.pipeline_config->setCameraFieldSettings();
                    turretVision.pipeline_config->setScaleFactor(0.2);
                    LED_Turn_OFF();

                    if (debugInfoState) {
                        turretVision.pipeline_config->printCameraSupportSettings();
                    }

                    if (cam1 != NULL)
                    {
                        // Reset the video frame feed
                        groundVision.driver_station->height = groundVision.scaled_image->height;
                        groundVision.driver_station->width = groundVision.scaled_image->width;
                        groundVision.driver_station->planes = groundVision.scaled_image->planes;
                        groundVision.driver_station->clear_memory();

                        cv::Mat groundCamOutput = cv::Mat(groundVision.driver_station->height, groundVision.driver_station->width, CV_8UC3, groundVision.driver_station->image);

                        // Push the video feed
                        groundFeed.PutFrame(groundCamOutput);
                    }
                    break;
                }

                case VISION_STATE::TARGET_DETECTION:
                {
                    // Update the Console
                    std::cout << "Switching to Vision Viewing Mode\n";
                    NT_instance.visionStateMachineEntry.SetString("TARGET DETECT");

                    // Checking if there is a config file and loading it if there is one 
                    // and using existing values if not present.
                    if (NT_instance.vision_trackingState_current != VISION_STATE::THRESHOLDING) {
                        // Reload the configuration
                        loadConfig(turretVision.pipeline_config);
                        // Set up camera for on field viewing
                        LED_Turn_ON();
                    }

                    turretVision.pipeline_config->setCameraTargetSettings();

                    if (debugInfoState)
                        turretVision.pipeline_config->printCameraSupportSettings();

                    if (debugInfoState) {
                        turretVision.pipeline_config->setVisionLibDebugStr("TargetDetect_");
                        turretVision.pipeline_config->setVisionLibDebugMode(true);
                        turretVision.pipeline_config->setVisionLibCaptureMode(true);
                        turretVision.pipeline_config->setVisionLibCaptureInc(5);
                    }

                    if (cam0 != NULL)
                    {
                        // Reset the video frame feed
                        turretVision.driver_station->height = turretVision.scaled_image->height;
                        turretVision.driver_station->width = turretVision.scaled_image->width;
                        turretVision.driver_station->planes = turretVision.scaled_image->planes;
                        turretVision.driver_station->clear_memory();

                        cv::Mat turretCamOutput = cv::Mat(turretVision.driver_station->height, turretVision.driver_station->width, CV_8UC3, turretVision.driver_station->image);

                        // Push the video feed
                        turretFeed.PutFrame(turretCamOutput);
                    }
                    break;
                }
                
                case VISION_STATE::BALL_DETECTION:
                {
                    if (cam1 != NULL)  // Make sure the Camera exists
                    {
                        // Update the Console
                        std::cout << "Switching to BallCam Viewing Mode\n";
                        NT_instance.visionStateMachineEntry.SetString("BALL DETECT");
                        LED_Turn_OFF();

                        // Set up camera for on ballcam viewing
                        if ( (cam1->cam != NULL) && cam1->cam->isOpened() )
                        {
                            groundVision.pipeline_config->setCameraTargetSettings();

                            if (debugInfoState)
                                groundVision.pipeline_config->printCameraSupportSettings();

                            // Take a picture and run it through the pipeline
                            if (debugInfoState) {
                                groundVision.pipeline_config->setVisionLibDebugStr("BallDetect_");
                                groundVision.pipeline_config->setVisionLibDebugMode(true);
                                groundVision.pipeline_config->setVisionLibCaptureMode(true);
                                groundVision.pipeline_config->setVisionLibCaptureInc(5);
                            }
                            else
                            {
                                // Set-up for intermittent capture
                                groundVision.pipeline_config->setVisionLibDebugStr("BallDetect_");
                                groundVision.pipeline_config->setVisionLibDebugMode(false);
                                groundVision.pipeline_config->setVisionLibCaptureMode(false);
                                groundVision.pipeline_config->setVisionLibCaptureInc(15);
                            }

                            // Reset the video frame feed
                            groundVision.driver_station->height = groundVision.scaled_image->height;
                            groundVision.driver_station->width = groundVision.scaled_image->width;
                            groundVision.driver_station->planes = groundVision.scaled_image->planes;
                            groundVision.driver_station->clear_memory();

                            cv::Mat groundCamOutput = cv::Mat(groundVision.driver_station->height, groundVision.driver_station->width, CV_8UC3, groundVision.driver_station->image);

                            // Push the video feed
                            turretFeed.PutFrame(groundCamOutput);
                        }
                    }
                    else
                    {
                        // Update the Console
                        std::cout << "Ball Camera Not Enabled\nRestarting vision state machine\n";
                        NT_instance.vision_trackingState_new = VISION_STATE::INITIALIZE;
                    }

                    break;
                }

                case VISION_STATE::APRILTAG:
                {
                    if (cam1 != NULL)  // Make sure the Camera exists
                        {
                        // Update the Console
                        std::cout << "Switching to APRILTAG Viewing Mode\n";
                        NT_instance.visionStateMachineEntry.SetString("APRILTAG");
                        LED_Turn_OFF();

                        // Set up camera for on ballcam viewing
                        if ((cam1->cam != NULL) && cam1->cam->isOpened())
                        {
                            turretVision.pipeline_config->setCameraTargetSettings();

                            if (debugInfoState)
                                turretVision.pipeline_config->printCameraSupportSettings();


                            // Reset the video frame feed
                            turretVision.driver_station->height = turretVision.scaled_image->height;
                            turretVision.driver_station->width = turretVision.scaled_image->width;
                            turretVision.driver_station->planes = turretVision.scaled_image->planes;
                            turretVision.driver_station->clear_memory();

                            cv::Mat apirltagCamOutput = cv::Mat(turretVision.driver_station->height, turretVision.driver_station->width, CV_8UC3, turretVision.driver_station->image);

                            // Push the video feed
                            turretFeed.PutFrame(apirltagCamOutput);
                        }
                    }
                    else
                    {
                        // Update the Console
                        std::cout << "Camera Not Enabled\nRestarting vision state machine\n";
                        NT_instance.vision_trackingState_new = VISION_STATE::INITIALIZE;
                    }

                    break;
                }
            }

            // Update the vision state
            NT_instance.vision_trackingState_current = NT_instance.vision_trackingState_new;

        }

        // Get the current timer 
        frameRateTimer_Current     = std::chrono::high_resolution_clock::now();

        frameRateTimer_Duration    = std::chrono::duration_cast<std::chrono::milliseconds>(frameRateTimer_Current - frameRateTimer_Start);
        networkTableTimer_Duration = std::chrono::duration_cast<std::chrono::milliseconds>(frameRateTimer_Current - networkTableTimer_Start);

        // Force an update to poll the NT table server to keep it alive
        if (networkTableTimer_Duration > networkTableTimer_WaitTime)
        {
            bool tempVisionStateEntry = NT_instance.visionStatusEntry.GetBoolean(false);
            bool tempBallCamStateEntry = NT_instance.ballDetectStatusEntry.GetBoolean(false);

            networkTableTimer_Start = std::chrono::high_resolution_clock::now();
        }

        switch ( NT_instance.vision_trackingState_current )
        {
            case VISION_STATE::INITIALIZE:
            {
                //Need to restart the state machine
                NT_instance.visionStateMachineEntry.SetString("SHUFFLEBOARD");
                NT_instance.vision_trackingState_new = VISION_STATE::SHUFFLEBOARD;
                break;
            }
            case VISION_STATE::SHUFFLEBOARD:
            {
                if (frameRateTimer_Duration >= NT_instance.frameRateTimer_waitTime)
                {
                    // Grab the feed from the turret camera
                    grabTurretFeed(&turretVision, targetImage, &turretFeed, true, debugInfoState);
                    // std::thread thread_1 = std::thread(grabTurretFeed, &turretVision, targetImage, &turretFeed, true, debugInfoState);

                    // Grab the feed from the picker camera
                    // grabPickerFeed( &groundVision, groundImage, &groundFeed, true, debugInfoState);
                    // std::thread thread_2 = std::thread(grabPickerFeed, &groundVision, groundImage, &groundFeed, true, debugInfoState);

                    // Reset the start timer 
                    frameRateTimer_Start = std::chrono::high_resolution_clock::now();

                    // printf("Elapsed Processing : %lld ms\n", std::chrono::duration_cast<std::chrono::milliseconds>(frameRateTimer_Start - frameRateTimer_Current) );
                }

                break;
            }
            case VISION_STATE::THRESHOLDING:
            {
                if (cam0->cam->isOpened())
                {

                    // Make sure that the camera is ready
                    if (turretVision.pipeline_config->getCameraState() == false) {
                        if (debugInfoState) {
                            printf("Turret Camera is not Ready\n");
                        }
                        break;
                    }

                    // Grab a frame
                    cam0->cam->grab();
                    cam0->cam->read(*rawTurretCam);
                    targetImage->image = rawTurretCam->data;
                    targetImage->pixel_format = PIXEL_FORMAT::BGR;

                    // Process it through the image processing pipeline
                    run_object_detection_process(targetImage, &turretVision);

                    cv::Mat greyImg = cv::Mat(turretVision.thresholded_out->height, turretVision.thresholded_out->width, CV_8U, turretVision.thresholded_out->image);

                    // Push the video feed
                    turretFeed.PutFrame(greyImg);
                }

                break;
            }
            case VISION_STATE::TARGET_DETECTION:
            {
                // Make sure that the camera is ready
                if (turretVision.pipeline_config->getCameraState() == false) {
                    if (debugInfoState) {
                        printf("Turret Camera is not Ready\n");
                    }
                    break;
                }

                totalStartTime = std::chrono::high_resolution_clock::now();

                cam0->cam->grab();
                cam0->cam->read(*rawTurretCam);
                targetImage->image = rawTurretCam->data;
                targetImage->pixel_format = PIXEL_FORMAT::BGR;

                processStartTime = std::chrono::high_resolution_clock::now();

                if (debugInfoState) {
                    std::chrono::duration <double, std::milli> elapsed_time = processStartTime - totalStartTime;
                    printf( "Grab and Read Time: %g\n", elapsed_time.count() ) ;
                }

                // Mark the frame in count
                NT_instance.currentFrameStartEntry.SetDouble(time);

                // Get the current angle of the Turret and set it 
                NT_instance.initialAngleEntry.SetDouble(NT_instance.currentAngleEntry.GetDouble(-1));

                run_object_detection_process(targetImage, &turretVision);

                networkTablesStartTime = std::chrono::high_resolution_clock::now();

                if (debugInfoState) {
                    std::chrono::duration <double, std::milli> elapsed_time = networkTablesStartTime - processStartTime;
                    printf("Process Time : %g\n", elapsed_time.count() );
                }

                // Setting Object Entries
                // printf("Number of Objects found : %d\n", turretVision.object_location.objects_found);
                NT_instance.numberOfObjectsEntry.SetDouble(turretVision.object_location.objects_found);

                if (turretVision.object_location.objects_found > 0)
                {
                    NT_instance.deltaAngleEntry.SetDouble(turretVision.target_LOC->getTurretAngle());
                    NT_instance.distanceEntry.SetDouble(turretVision.target_LOC->getDistance());

                    size_t array_size = (size_t)turretVision.object_location.objects_found;
                    std::vector <double> obj_locations(array_size * 2);

                    double effSF = 1 / sqrt(turretVision.pipeline_config->getScaleFactor()) ;
                    double count = 0;

                    for (int i = 0; i < array_size; i++)
                    {
                        Location object_location = turretVision.object_location.center_location[i];
                        obj_locations[count++] = object_location.x * effSF;
                        obj_locations[count++] = object_location.y * effSF;
                    }

                    NT_instance.objectCenterEntry.SetDoubleArray(obj_locations);

                    std::vector <double> width(array_size);
                    std::vector <double> height(array_size);
                    std::vector <std::string> color(array_size);

                    for (int i = 0; i < array_size; i++)
                    {
                        char myColor[255] = { 0 };
                        getObjectColorString(turretVision.object_location.object_color[i], myColor);

                        BoundingBox bounding_box = turretVision.object_location.bounding_box[i];
                        color[i] = myColor;
                        height[i] = ( bounding_box.bottom - bounding_box.top ) * effSF;
                        width[i] = ( bounding_box.right - bounding_box.left ) * effSF ;
                    }

                    // Update the NT entries
                    NT_instance.objectHeightEntry.SetDoubleArray(height);
                    NT_instance.objectWidthEntry.SetDoubleArray(width);
                    NT_instance.objectColorEntry.SetStringArray(color);

                }
                else
                {
                    // Since we did not find anything, force the output to 0
                    NT_instance.deltaAngleEntry.SetDouble(0);
                    NT_instance.distanceEntry.SetDouble(0);
                }

                streamStartTime = std::chrono::high_resolution_clock::now();

                if (debugInfoState) {
                    std::chrono::duration <double, std::milli> elapsed_time = streamStartTime - networkTablesStartTime;
                    printf("Network Table Time : %g\n", elapsed_time.count() );
                }

                // Mark the frame out count
                NT_instance.currentFrameEndEntry.SetDouble(time++);

                // Get the current timer
                frameRateTimer_Current = std::chrono::high_resolution_clock::now();
                frameRateTimer_Duration = std::chrono::duration_cast<std::chrono::milliseconds>(frameRateTimer_Current - frameRateTimer_Start);

                if (frameRateTimer_Duration >= NT_instance.frameRateTimer_waitTime)
                {
                    int result;

                    char temp[50] = { 0 };
                    turretVision.pipeline_config->getTargetTypeString(temp);

                    switch (turretVision.pipeline_config->getTargetType())
                    {
                        case TARGET_DETECTION_TYPE::HEXAGON: 
                        {
                            result = annotateDriverStationTargetImage(&turretVision);
                            break;
                        }
                        case TARGET_DETECTION_TYPE::HUB :  
                        {
                            result = annotateDriverStationHubImage(&turretVision);
                            break;
                        }
                        case TARGET_DETECTION_TYPE::BALL:
                        default:
                        {
                            result = annotateDriverStationBallCamImage(&turretVision);
                            break;
                        }
                    }
                    
                    // Create the video frame object
                    cv::Mat turretCamOutput = cv::Mat(turretVision.driver_station->height, turretVision.driver_station->width, CV_8UC3, turretVision.driver_station->image);

                    // Push the video feed
                    turretFeed.PutFrame(turretCamOutput);

                    // Reset the start timer 
                    frameRateTimer_Start = std::chrono::high_resolution_clock::now();
                }

                totalEndTime = std::chrono::high_resolution_clock::now();
                std::chrono::duration <double, std::milli> elapsed_time = totalEndTime - streamStartTime ;
                
                NT_instance.currentFrameRateEntry.SetDouble( 1000 / turretVision.timing_total_processing );

                if (debugInfoState) {
                    printf("Total Time Taken: %g\n", elapsed_time.count() );

                    std::chrono::duration <double, std::milli> elapsed_time = totalEndTime - streamStartTime;
                    printf( "Streaming Time : %g\n", elapsed_time.count() );
                }

                break;
            }
            case VISION_STATE::BALL_DETECTION:
            {
                if ((cam1->cam != NULL) && cam1->cam->isOpened())
                {
                    // Make sure that the camera is ready
                    if (groundVision.pipeline_config->getCameraState() == false) {
                        if (debugInfoState) {
                            printf("Ground Camera is not Ready\n");
                        }
                        break;
                    }

                    // Grab the Camera feed to display on the driver station
                    cam1->cam->grab();
                    cam1->cam->read(*rawGroundCam);

                    groundImage->image = rawGroundCam->data;
                    groundImage->pixel_format = PIXEL_FORMAT::BGR;

                    run_object_detection_process(groundImage, &groundVision);

                    if (groundVision.object_location.objects_found > 0)
                    {
                        // Setting Object Entries
                        NT_instance.numberOfObjectsEntry.SetDouble(turretVision.object_location.objects_found);

                        size_t array_size = (size_t)turretVision.object_location.objects_found;
                        std::vector <double> obj_locations(array_size * 2);

                        double effSF = 1 / sqrt(turretVision.pipeline_config->getScaleFactor());
                        double count = 0;

                        for (int i = 0; i < array_size; i++)
                        {
                            Location object_location = turretVision.object_location.center_location[i];
                            obj_locations[count++] = object_location.x * effSF;
                            obj_locations[count++] = object_location.y * effSF;
                        }

                        NT_instance.objectCenterEntry.SetDoubleArray(obj_locations);

                        std::vector <double> width(array_size);
                        std::vector <double> height(array_size);
                        std::vector <std::string> color(array_size);

                        for (int i = 0; i < array_size; i++)
                        {
                            char myColor[255] = { 0 };
                            getObjectColorString(turretVision.object_location.object_color[i], myColor);

                            BoundingBox bounding_box = turretVision.object_location.bounding_box[i];
                            color[i] = myColor;
                            height[i] = ( bounding_box.bottom - bounding_box.top ) * effSF;
                            width[i] = ( bounding_box.right - bounding_box.left ) * effSF;
                        }

                        NT_instance.objectHeightEntry.SetDoubleArray(height);
                        NT_instance.objectWidthEntry.SetDoubleArray(width);
                        NT_instance.objectColorEntry.SetStringArray(color);

                        // Get the current timer 
                        frameRateTimer_Current = std::chrono::high_resolution_clock::now();
                        frameRateTimer_Duration = std::chrono::duration_cast<std::chrono::milliseconds>(frameRateTimer_Current - frameRateTimer_Start);

                        if (frameRateTimer_Duration >= NT_instance.frameRateTimer_waitTime)
                        {
                            // Display the image on the drivers station
                            int result = annotateDriverStationBallCamImage(&groundVision);

                            // Create the video frame object
                            cv::Mat groundCamOutput = cv::Mat(groundVision.driver_station->height, groundVision.driver_station->width, CV_8UC3, groundVision.driver_station->image);

                            if (debugInfoState) {
                                // Writing out the cameras image
                                write_image_store(groundVision.driver_station, PNM_FORMAT::BINARY, "./Runtime_Capture//Ground_Cam_Annotated_Stream.ppm");
                            }

                            // Push the video feed
                            // groundFeed.PutFrame(groundCamOutput);
                            turretFeed.PutFrame(groundCamOutput);

                        }

                        // Reset the start timer 
                        frameRateTimer_Start = std::chrono::high_resolution_clock::now();
                    }
                    else
                    {
                        NT_instance.numberOfObjectsEntry.SetDouble(0);
                        std::vector <std::string> color(1);
                        std::vector <double> obj_locations(1);
                        obj_locations[0] = 0;

                        NT_instance.objectCenterEntry.SetDoubleArray(obj_locations);
                        NT_instance.objectHeightEntry.SetDoubleArray(obj_locations);
                        NT_instance.objectWidthEntry.SetDoubleArray(obj_locations);
                        NT_instance.objectColorEntry.SetStringArray(color);

                        std::cout << "Ground Camera did not find any balls\n";
                        
                        // Set up the camera feed
                        cv::Mat colorImage = cv::Mat(groundVision.scaled_image->height, groundVision.scaled_image->width, CV_8UC3, groundVision.scaled_image->image);
                        cv::cvtColor(colorImage, colorImage, cv::COLOR_RGB2GRAY);

                        // Push the Color Video feed
                        // groundFeed.PutFrame(colorImage);
                        turretFeed.PutFrame(colorImage);
                    }

                }
                break;
            }

            case VISION_STATE::APRILTAG:
            {
                if ((cam1->cam != NULL) && cam1->cam->isOpened())
                {
                    // Make sure that the camera is ready
                    if (turretVision.pipeline_config->getCameraState() == false) {
                        if (debugInfoState) {
                            printf("Turret Camera is not Ready\n");
                        }
                        break;
                    }

                    // Grab the Camera feed to display on the driver station
                    cam1->cam->grab();
                    cam1->cam->read(*rawTurretCam);

                    targetImage->image = rawTurretCam->data;
                    targetImage->pixel_format = PIXEL_FORMAT::BGR;

                    // Create AprilTag Image Object
                    cv::Mat aprilTagGrayFrame = cv::Mat(targetImage->height, targetImage->width, CV_8UC1 );

                    // Convert the color image to Gray
                    cvtColor( *(targetImage->image), aprilTagGrayFrame, COLOR_BGR2GRAY);

                    // Process the Frame through AprilTag code
                    int numTargets = aprilTagPipeLine.processFrame ( rawTurretCam, &aprilTagGrayFrame );

                    // Objects found
                    if ( numTargets > 0 )
                    {
                        
                        // Get the current timer 
                        frameRateTimer_Current = std::chrono::high_resolution_clock::now();
                        frameRateTimer_Duration = std::chrono::duration_cast<std::chrono::milliseconds>(frameRateTimer_Current - frameRateTimer_Start);

                        if (frameRateTimer_Duration >= NT_instance.frameRateTimer_waitTime)
                        {
                            
                            // Convert the image from BGR to RGB
                            cvtColor(*rawTurretCam, *rawTurretCam, COLOR_BGR2RGB);

                            Image_Store* targetImage = new Image_Store();
                            targetImage->set_attributes(rawTurretCam->cols, rawTurretCam->rows, 3, 255, PIXEL_FORMAT::RGB, INTERLEAVE_FORMAT::PIXEL_INTERLEAVED);
                            targetImage->image = rawTurretCam->data;

                            if (debugInfoState) {
                                // Writing out the cameras image
                                write_image_store(targetImage, PNM_FORMAT::BINARY, "./Runtime_Capture//Ground_Cam_Annotated_Stream.ppm");
                            }

                            // Push the video feed
                            turretFeed.PutFrame(*rawTurretCam);
                        }

                        // Reset the start timer 
                        frameRateTimer_Start = std::chrono::high_resolution_clock::now();
                    }
                    else
                    {
                        std::cout << "Turret Camera did not find any AprilTags\n";

                        // Set up the camera feed
                        cv::Mat colorImage = cv::Mat(turretVision.scaled_image->height, turretVision.scaled_image->width, CV_8UC3, turretVision.scaled_image->image);
                        cv::cvtColor(colorImage, colorImage, cv::COLOR_RGB2GRAY);

                        // Push the Color Video feed
                        turretFeed.PutFrame(colorImage);
                    }

                }
                break;
            }

        }
    }
}


void loadConfig(PipelineConfig* pipeline_config) {

    char config_file[MAX_STRING_LENGTH] = "./pipelineConfig.txt";
    char config_data[MAX_JSON_STRING_LENGTH] = { 0 };

    if (pipeline_config->readFromFile(config_file, config_data, MAX_JSON_STRING_LENGTH) == EXIT_SUCCESS)
    {
        printf("Load Configuration\n");
        pipeline_config->parseJSONCommand(config_data, MAX_JSON_STRING_LENGTH);
    }
    else {
        printf("Unable to open file. Loading default settings \n");
    }
 
}

void grabTurretFeed( Vision_Pipeline* turretVision, Image_Store* targetImage, cs::CvSource* turretFeed, bool monoFeed, bool debugInfoState )
{

    // Make sure that the camera is ready
    if (turretVision->pipeline_config->getCameraState() == false) {
        if (debugInfoState) {
            printf("Turret Camera is not Ready\n");
        }
        return;
    }

    // Set up the pointer to the camera feed
    Camera* cameraFeed = turretVision->pipeline_config->getCameraConfiguration();
 
    if ((cameraFeed != NULL) && (cameraFeed->cam != NULL) && cameraFeed->cam->isOpened())
    {
        // Grab the Camera feed to display on the driver station
        cameraFeed->cam->grab();

        cv::Mat rawTurretCam = cv::Mat();
        bool result = cameraFeed->cam->read(rawTurretCam);

        if (result == false) {
            printf("Unable to read image\n");
            return;
        }

        targetImage->image = rawTurretCam.data;
        targetImage->pixel_format = PIXEL_FORMAT::RGB;

        // printf( "Generate Turret Preview\n");
        run_idle_process( targetImage, turretVision );

        // Set up the camera feed
        cv::Mat colorImage = cv::Mat(turretVision->scaled_image->height, turretVision->scaled_image->width, CV_8UC3, turretVision->scaled_image->image);

        if (monoFeed)
        {
            int center_x = turretVision->scaled_image->width / 2;
            int center_y = turretVision->scaled_image->height / 2;

            cv::Point2i TM_0 = Point(center_x - 1, 0);
            cv::Point2i BM_0 = Point(center_x - 1, turretVision->scaled_image->height);
            cv::Point2i TM_1 = Point(center_x, 0);
            cv::Point2i BM_1 = Point(center_x, turretVision->scaled_image->height);
            cv::Point2i TM_2 = Point(center_x + 1, 0);
            cv::Point2i BM_2 = Point(center_x + 1, turretVision->scaled_image->height);

            cv::Point2i LM_0 = Point(0, center_y - 1 );
            cv::Point2i RM_0 = Point(turretVision->scaled_image->width, center_y - 1);
            cv::Point2i LM_1 = Point(0, center_y );
            cv::Point2i RM_1 = Point(turretVision->scaled_image->width, center_y);
            cv::Point2i LM_2 = Point(0, center_y + 1);
            cv::Point2i RM_2 = Point(turretVision->scaled_image->width, center_y + 1);

            // Convert the Color Image to greyscale
            cv::Mat grayImage = cv::Mat(turretVision->scaled_image->height, turretVision->scaled_image->width, CV_8U);
            cv::cvtColor(colorImage, grayImage, cv::COLOR_BGR2GRAY);

            // Vertical Line
            draw_dashed_line(grayImage, TM_0, BM_0, 6, 4);
            draw_dashed_line(grayImage, TM_1, BM_1, 6, 4);
            // draw_dashed_line(grayImage, TM_2, BM_2, 6, 4);

            // Horizontal Line
            draw_dashed_line(grayImage, LM_0, RM_0, 6, 4);
            draw_dashed_line(grayImage, LM_1, RM_1, 6, 4);
            // draw_dashed_line(grayImage, LM_2, RM_2, 6, 4);

            // Push the video feed
            turretFeed->PutFrame(grayImage);

        } else 
        {
            // Push the Color Video feed
            turretFeed->PutFrame(colorImage);
        }

        if (debugInfoState)
        {
            std::cout << "Pushing Turret Camera Feed\n";
            std::cout << "\tWidth: " << turretVision->scaled_image->width << " Height: " << turretVision->scaled_image->height << "\n";
            write_image_store(turretVision->scaled_image, PNM_FORMAT::BINARY, "./Runtime_Capture//Turret_Cam_Stream.ppm");

            // For Debug
            // write_image_store(targetImage, PNM_FORMAT::BINARY, "./Runtime_Capture//Turret_Cam_Raw.ppm");
        }
    }

}


void grabPickerFeed( Vision_Pipeline* groundVision, Image_Store* groundImage, cs::CvSource* groundFeed, bool monoFeed, bool debugInfoState )
{
    // Make sure that the camera is ready
    if (groundVision->pipeline_config->getCameraState() == false) {
        if (debugInfoState) {
            printf("Ground Camera is not Ready\n");
        }
        return;
    }
    
    // Set up the pointer to the camera feed
    Camera* cameraFeed = groundVision->pipeline_config->getCameraConfiguration();

    if ((cameraFeed != NULL) && (cameraFeed->cam != NULL) && cameraFeed->cam->isOpened())
    {
        // Grab the Camera feed to display on the driver station
        cameraFeed->cam->grab();

        cv::Mat rawGroundCam = cv::Mat();
        bool result = cameraFeed->cam->read(rawGroundCam);

        if (result == false) {
            printf("Unable to read image\n");
            return;
        }

        groundImage->image = rawGroundCam.data;
        groundImage->pixel_format = PIXEL_FORMAT::RGB;

        // printf("Generate Ground Preview\n");
        run_idle_process( groundImage, groundVision);

        // Set up the camera feed
        cv::Mat colorImage = cv::Mat(groundVision->scaled_image->height, groundVision->scaled_image->width, CV_8UC3, groundVision->scaled_image->image);

        if (monoFeed)
        {
            // Convert the Color Image to Gray
            cv::Mat grayImage = cv::Mat(groundVision->scaled_image->height, groundVision->scaled_image->width, CV_8U);
            cv::cvtColor(colorImage, grayImage, cv::COLOR_BGR2GRAY);

            // Push the Mono Video feed
            groundFeed->PutFrame(grayImage);
        }
        else
        {
            // Push the Color Video feed
            groundFeed->PutFrame(colorImage);
        }

        if (debugInfoState)
        {
            // std::cout << "Pushing Ground Camera Feed\n";
            // std::cout << "\tWidth: " << groundVision->scaled_image->width << " Height: " << groundVision->scaled_image->height << "\n";
            write_image_store(groundVision->scaled_image, PNM_FORMAT::BINARY, "./Runtime_Capture//Ground_Cam_Stream.ppm");

            // For Debug
            // write_image_store(groundImage, PNM_FORMAT::BINARY, "./Runtime_Capture//Ground_Cam_Raw.ppm");
        }
    }
}
