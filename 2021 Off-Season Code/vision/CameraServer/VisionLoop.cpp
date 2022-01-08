#include <iostream>
#include "VisionLib.hpp"
#include "Utilities.h"
#include <math.h>

#include "opencv2/videoio.hpp"

#include "networktables/NetworkTableInstance.h"
#include "networktables/NetworkTableEntry.h"

#include "cameraserver/CameraServer.h"
#include "NetworkTables.h"
#include "SocketServer.h"


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

    // Start off by building a vision pipeline for the Turret Cam
    Vision_Pipeline turretVision(1.0, 17, SENSOR_ORIENTATION::PORTRAIT, TARGET_DETECTION_TYPE::TARGET);

    // Kick off the web-socket server to receive requests from the EWS
    std::thread* socketThread;
    socketThread = new std::thread(runSocketServer, SERVER_PORT, turretVision.pipeline_config );

    std::cout << "\tOpening Turret Camera Stream\n";
    cv::VideoCapture* cam0 = new cv::VideoCapture(0, cv::CAP_V4L2);
    
    // Configure which settings the camera supports
    turretVision.pipeline_config->setCameraConfiguration(cam0, CAMERA_IN_USE::TURRETCAM);
    turretVision.pipeline_config->setCameraFieldSettings();
    turretVision.pipeline_config->setNetworkTableInstance( &NT_instance );

    // Start off by building a vision pipeline for the Ground Cam
    Vision_Pipeline groundVision(0.5, 17, SENSOR_ORIENTATION::LANDSCAPE, TARGET_DETECTION_TYPE::BALL);

    std::cout << "\tOpening Ground Camera Stream\n";
    cv::VideoCapture* cam1 = new cv::VideoCapture(1, cv::CAP_V4L2);

    // Configure which settings the camera supports
    groundVision.pipeline_config->setCameraConfiguration(cam1, CAMERA_IN_USE::BALLCAM);
    groundVision.pipeline_config->setCameraFieldSettings();
    groundVision.pipeline_config->setNetworkTableInstance( &NT_instance );

    if (debugInfoState)
    {
        // Figure out which settings the camera supports
        std::cout << "\n";
        std::cout << "\tTurret Camera Settings\n";
        turretVision.pipeline_config->printCameraSupportSettings();
        std::cout << "\n";
        std::cout << "\tGround Camera Settings\n";
        groundVision.pipeline_config->printCameraSupportSettings();
        std::cout << "\n";
    }

    std::cout << "\tFinished Creating Camera Streams\n";

    // Initalize the Camera objects
    cv::Mat* rawTurretCam = new cv::Mat();
    cv::Mat* rawGroundCam = new cv::Mat();
    cv::Mat* turretCamOutput = NULL;
    cv::Mat* groundCamOutput = NULL;

    // Write the current frame to the network table
    cs::CvSource turretFeed;
    cs::CvSource groundFeed;

    if (!cam0->isOpened()) {
        std::cout << "Turret Camera could not be opened, returning.\n";
        return 1;
    }
    else {
        //Reading the Turret Camera
        std::cout << "Reading from Turret Camera\n";
        // Opening the Turret Camera
        // cam0->open(0);
        cam0->grab();
        cam0->read(*rawTurretCam);
    }

    if (!cam1->isOpened()) {
        std::cout << "Ground Camera could not be opened, disabling camera feed.\n";
    }
    else {
        std::cout << "Reading from Ground Camera\n";
        // Opening the ground camera
        // cam1->open(1);
        cam1->grab();
        cam1->read(*rawGroundCam);
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

    if (cam0->isOpened()) {
        // Set up OpenCV output stream of TurretCam for Shuffleboard viewing
        turretCamOutput = new cv::Mat(turretVision.scaled_image->height, turretVision.scaled_image->width, CV_8UC3);
        std::cout << "Camera Stream 0 Open\n";
        turretFeed = frc::CameraServer::GetInstance()->PutVideo( NT_instance.visionNTEntry, turretCamOutput->rows, turretCamOutput->cols);
    }

    // Needs to be declared outside of the scope because it is used later
    Image_Store* groundImage = new Image_Store();

    if (cam1->isOpened())
    {
        // Set up image attributes
        groundImage->set_attributes(rawGroundCam->cols, rawGroundCam->rows, 3, 255, PIXEL_FORMAT::RGB, INTERLEAVE_FORMAT::PIXEL_INTERLEAVED);
        groundImage->image = rawGroundCam->data;

        if (debugInfoState) {
            write_image_store(groundImage, PNM_FORMAT::BINARY, "./Runtime_Capture//Raw_Ground_Cam_Capture.ppm");
        }

        run_idle_process(groundImage, &groundVision);

        groundCamOutput = new cv::Mat(groundVision.scaled_image->height, groundVision.scaled_image->width, CV_8UC3);
        std::cout << "Camera Stream 1 Open\n";
        // Set up the network table connection
        groundFeed = frc::CameraServer::GetInstance()->PutVideo(NT_instance.groundNTEntry, groundCamOutput->cols, groundCamOutput->rows);

        if (debugInfoState)
            write_image_store(groundImage, PNM_FORMAT::BINARY, "./Runtime_Capture//Ground_Cam_Capture.ppm");
    }

    // Initialize the Frame Rate timers
    auto frameRateTimer_Start = std::chrono::high_resolution_clock::now();
    auto frameRateTimer_Current = frameRateTimer_Start;
    auto frameRateTimer_Duration = std::chrono::duration_cast<std::chrono::milliseconds>(frameRateTimer_Current - frameRateTimer_Start);

    // Initialize the debug timers
    auto totalStartTime = std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count();
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
    NT_instance.setupListenerServices( & NT_instance );

    // Update the shuffleboard STATE
    NT_instance.visionStateEntry.SetString( "SHUFFLEBOARD" );

    // Read from the network tables
    NT_instance.vision_brightnessState = NT_instance.visionBrightnessEntry.GetDouble(INITIAL_LED_BRIGHTNESS);
    NT_instance.vision_exposure = NT_instance.visionExposureEntry.GetDouble(INITIAL_VISION_EXPOSURE);
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
                printf("Vision Brightness NT State : %g\n", NT_instance.vision_brightnessState);
                printf("Vision Exposure NT State   : %g\n", NT_instance.vision_exposure);
                printf("Vision FrameRate NT State  : %g\n", NT_instance.vision_frameRateLimit);

                NT_instance.printNetworkTableInfo();
            }

            // Update the vision state
            NT_instance.vision_trackingState_current = NT_instance.vision_trackingState_new;

            // Swith the video feed based on the state
            switch (NT_instance.vision_trackingState_current)
            {
                case VISION_STATE::SHUFFLEBOARD:
                    // Update the Console
                    std::cout << "Switching to Shuffleboard Viewing Mode\n";
                    NT_instance.visionStateEntry.SetString("SHUFFLEBOARD");
                    NT_instance.visionStateEntry.SetBoolean(false);
                    NT_instance.ballCamStateEntry.SetBoolean(false);

                    // Set up camera for on field viewing
                    turretVision.pipeline_config->setCameraFieldSettings();

                    if (debugInfoState)
                        turretVision.pipeline_config->printCameraSupportSettings();

                    if (cam1->isOpened())
                    {
                        groundVision.pipeline_config->setCameraFieldSettings();
                        if (debugInfoState)
                            groundVision.pipeline_config->printCameraSupportSettings();
                    }
                    break;

                case VISION_STATE::TARGET_DETECTION:
                    // Update the Console
                    std::cout << "Switching to Vision Viewing Mode\n";
                    NT_instance.visionStateEntry.SetString("TARGET DETECT");

                    // Set up camera for on target viewing
                    // values :
                    //     2^6 = 64         2^10 = 1024
                    //     2^7 = 128        2^11 = 2048
                    //     2^8 = 256        2^12 = 4096
                    //     2^9 = 512        2^13 = 8192
                    turretVision.pipeline_config->setCameraVisionSettings( pow( 2.0 , NT_instance.vision_exposure ) );

                    if (debugInfoState)
                        turretVision.pipeline_config->printCameraSupportSettings( );

                    break;

                case VISION_STATE::BALL_DETECTION:
                    // Update the Console
                    std::cout << "Switching to BallCam Viewing Mode\n";
                    NT_instance.visionStateEntry.SetString("BALL DETECT");

                    // Set up camera for on ballcam viewing
                    if (cam1->isOpened())
                    {
                        groundVision.pipeline_config->setCameraVisionSettings();
                        if (debugInfoState)
                            groundVision.pipeline_config->printCameraSupportSettings();
                    }

                    break;
            }
        }

        switch (NT_instance.vision_trackingState_current )
        {

            case VISION_STATE::SHUFFLEBOARD:
            
                if (debugInfoState) {
                    turretVision.pipeline_config->setVisionLibDebugStr("ShuffleBoard");
                    turretVision.pipeline_config->setVisionLibDebugMode(false);
                    turretVision.pipeline_config->setVisionLibCaptureMode(false);
                    turretVision.pipeline_config->setVisionLibCaptureInc(0);
                }

                // Get the current timer 
                frameRateTimer_Current = std::chrono::high_resolution_clock::now();
                frameRateTimer_Duration = std::chrono::duration_cast<std::chrono::milliseconds>(frameRateTimer_Current - frameRateTimer_Start);

                if (frameRateTimer_Duration >= NT_instance.frameRateTimer_waitTime )
                {
                    // Grab the Camera feed to display on the driver station
                    cam0->grab();
                    cam0->read(*rawTurretCam);
                    targetImage->image = rawTurretCam->data;
                    targetImage->pixel_format = PIXEL_FORMAT::RGB;

                    run_idle_process(targetImage, &turretVision);

                    turretCamOutput->data = turretVision.scaled_image->image;

                    // Push the video feed
                    turretFeed.PutFrame(*turretCamOutput);

                    if (debugInfoState)
                    {
                        //std::cout << "Pushing Turret Camera Feed\n";
                        //std::cout << "\tWidth: " << turretVision.scaled_image->width << " Height: " << turretVision.scaled_image->height << "\n";
                        write_image_store(turretVision.scaled_image, PNM_FORMAT::BINARY, "./Runtime_Capture//Turret_Cam_Stream.ppm");

                        // For Debug
                        // write_image_store(targetImage, PNM_FORMAT::BINARY, "./Runtime_Capture//Turret_Cam_Raw.ppm");
                    }

                    if (cam1->isOpened())
                    {
                        // Grab the Camera feed to display on the driver station
                        cam1->grab();
                        cam1->read(*rawGroundCam);
                        groundImage->image = rawGroundCam->data;
                        groundImage->pixel_format = PIXEL_FORMAT::RGB;

                        run_idle_process(groundImage, &groundVision);

                        groundCamOutput->data = groundVision.scaled_image->image;

                        // Push the video feed
                        groundFeed.PutFrame(*groundCamOutput);

                        if (debugInfoState)
                        {
                            // std::cout << "Pushing Ground Camera Feed\n";
                            // std::cout << "\tWidth: " << groundVision.scaled_image->width << " Height: " << groundVision.scaled_image->height << "\n";
                            write_image_store(groundVision.scaled_image, PNM_FORMAT::BINARY, "./Runtime_Capture//Ground_Cam_Stream.ppm");

                            // For Debug
                            // write_image_store(groundImage, PNM_FORMAT::BINARY, "./Runtime_Capture//Ground_Cam_Raw.ppm");
                        }
                    }

                    // Reset the start timer 
                    frameRateTimer_Start = std::chrono::high_resolution_clock::now();
                }
            
                break;

            case VISION_STATE::TARGET_DETECTION:

                if (debugInfoState) {
                    turretVision.pipeline_config->setVisionLibDebugStr("TargetDetect_");
                    turretVision.pipeline_config->setVisionLibDebugMode(true);
                    turretVision.pipeline_config->setVisionLibCaptureMode(true);
                    turretVision.pipeline_config->setVisionLibCaptureInc(5);
                    
                    totalStartTime = std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count();
                    grabStartTime = totalStartTime;
                }
                else
                {
                    // Set-up for intermittent capture
                    turretVision.pipeline_config->setVisionLibDebugStr("TargetDetect_");
                    turretVision.pipeline_config->setVisionLibDebugMode(false);
                    turretVision.pipeline_config->setVisionLibCaptureMode(true);
                    turretVision.pipeline_config->setVisionLibCaptureInc(20);
                }

                // printf("I am here  :  Inside capture Loop\n");

                cam0->grab();
                cam0->read(*rawTurretCam);
                targetImage->image = rawTurretCam->data;
                targetImage->pixel_format = PIXEL_FORMAT::BGR;

                if (debugInfoState) {
                    grabEndTime = std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count();
                    processStartTime = grabEndTime;
                    std::cout << "Grab and Read Time: " << grabEndTime - grabStartTime << "\n";
                }

                // Get the current angle of the Turret and set it 
                NT_instance.initialAngleEntry.SetDouble(NT_instance.currentAngleEntry.GetDouble(-1));

                run_target_process( targetImage, &turretVision );

                if (debugInfoState) {
                    processEndTime = std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count();
                    networkTablesStartTime = processEndTime;
                    std::cout << "Process Time: " << processEndTime - processStartTime << "\n";
                }

                NT_instance.deltaAngleEntry.SetDouble(turretVision.target_LOC->getTurretAngle());
                NT_instance.distanceEntry.SetDouble(turretVision.target_LOC->getDistance());
                NT_instance.timeReceivedEntry.SetDouble(time++);

                if (debugInfoState) {
                    networkTablesEndTime = std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count();
                    streamStartTime = networkTablesEndTime;
                    std::cout << "NetworkTables Time: " << networkTablesEndTime - networkTablesStartTime << "\n";
                }

                // Get the current timer 
                frameRateTimer_Current = std::chrono::high_resolution_clock::now();
                frameRateTimer_Duration = std::chrono::duration_cast<std::chrono::milliseconds>(frameRateTimer_Current - frameRateTimer_Start);

                if (frameRateTimer_Duration >= NT_instance.frameRateTimer_waitTime)
                {
                    if (turretVision.target_LOC->checkValidity())
                    {
                        int result = annotateDriverStationTargetImage(&turretVision);
                        // TurretVision.printTargetLocation();
                        turretCamOutput->data = turretVision.driver_station->image;
                    }
                    else
                    {
                        // Use the placeholder image
                        turretCamOutput->data = turretVision.scaled_image->image;
                    }

                    // Push the video feed
                    turretFeed.PutFrame(*turretCamOutput);

                    // Reset the start timer 
                    frameRateTimer_Start = std::chrono::high_resolution_clock::now();
                }

                if (debugInfoState) {
                    streamEndTime = std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count();
                    std::cout << "Streaming Time: " << streamEndTime - streamStartTime << "\n";
                    totalEndTime = streamEndTime;
                    std::cout << "Total Time Taken: " << totalEndTime - totalStartTime << "\n";
                }                
                break;

            case VISION_STATE::BALL_DETECTION:
            
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
                    turretVision.pipeline_config->setVisionLibDebugStr("BallDetect_");
                    turretVision.pipeline_config->setVisionLibDebugMode(false);
                    turretVision.pipeline_config->setVisionLibCaptureMode(true);
                    turretVision.pipeline_config->setVisionLibCaptureInc(15);
                }

                if (cam1->isOpened())
                {
                    // Grab the Camera feed to display on the driver station
                    cam1->grab();
                    cam1->read(*rawGroundCam);
                    groundImage->image = rawGroundCam->data;
                    groundImage->pixel_format = PIXEL_FORMAT::BGR;

                    run_ground_process(groundImage, &groundVision);

                    if (groundVision.ball_locations.balls_found > 0)
                    {
                        // Set the number of balls seen in the field of view
                        NT_instance.ballCountEntry.SetDouble(groundVision.ball_locations.balls_found);
                        double count = 0;

                        size_t array_size = groundVision.ball_locations.balls_found * 2;
                        std::vector <double> doublearray(array_size);
                        for (int i = 0; i < array_size; i += 2)
                        {
                            Location ball_location = groundVision.ball_locations.location[(int)round(i / 2)];
                            doublearray[count++] = ball_location.x;
                            doublearray[count++] = ball_location.y;
                        }

                        NT_instance.ballLocationsEntry.SetDoubleArray(doublearray);

                        // Get the current timer 
                        frameRateTimer_Current = std::chrono::high_resolution_clock::now();
                        frameRateTimer_Duration = std::chrono::duration_cast<std::chrono::milliseconds>(frameRateTimer_Current - frameRateTimer_Start);

                        if (frameRateTimer_Duration >= NT_instance.frameRateTimer_waitTime)
                        {
                            // Display the image on the drivers station
                            int result = annotateDriverStationBallCamImage(&groundVision);
                            groundCamOutput->data = groundVision.driver_station->image;

                            if (debugInfoState)
                            {
                                // Writing out the cameras image
                                write_image_store(groundVision.driver_station, PNM_FORMAT::BINARY, "./Runtime_Capture//Ground_Cam_Annotated_Stream.ppm");
                            }

                            // Push the video feed
                            groundFeed.PutFrame(*groundCamOutput);

                        }

                        // Reset the start timer 
                        frameRateTimer_Start = std::chrono::high_resolution_clock::now();
                    }
                    else
                    {
                        NT_instance.ballCountEntry.SetDouble(0);
                        std::cout << "Ground Camera did not find any balls\n";
                    }

                }
                break;
        }
    }
}

