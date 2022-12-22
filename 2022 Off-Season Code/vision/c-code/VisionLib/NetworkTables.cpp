#include "NetworkTables.h"
#include "LED_Control.h"
#include <iostream>
#include "PipelineConfig.hpp"


NetworkTables::NetworkTables()
{

    vision_frameRateLimit = INITIAL_FRAME_RATE;

    void setupNetworkTableObjects();
}

NetworkTables::~NetworkTables(void)
{
	// Default Destructor - cleans up
}

void NetworkTables::setupNetworkTableObjects() {

    // State Machine Status and Tracking
    visionStateMachineEntry = nt::NetworkTableInstance::GetDefault().GetTable(visionStateMachineTableKey)->GetEntry(visionStateMachineKey);
    visionStatusEntry     = nt::NetworkTableInstance::GetDefault().GetTable(visionStateMachineTableKey)->GetEntry(visionStatusKey);
    ballDetectStatusEntry = nt::NetworkTableInstance::GetDefault().GetTable(visionStateMachineTableKey)->GetEntry(ballDetectStatusKey);

    // Vision Specific
    deltaAngleEntry = nt::NetworkTableInstance::GetDefault().GetTable(visionTableKey)->GetEntry(deltaAngleKey);
	initialAngleEntry = nt::NetworkTableInstance::GetDefault().GetTable(visionTableKey)->GetEntry(initialAngleKey);
	distanceEntry = nt::NetworkTableInstance::GetDefault().GetTable(visionTableKey)->GetEntry(distanceKey);
    timeReceivedEntry = nt::NetworkTableInstance::GetDefault().GetTable(visionTableKey)->GetEntry(timeReceivedKey);
    currentAngleEntry = nt::NetworkTableInstance::GetDefault().GetTable(visionTableKey)->GetEntry(currentAngleKey);
    currentFrameStartEntry = nt::NetworkTableInstance::GetDefault().GetTable(visionTableKey)->GetEntry(currentFrameStartKey);
    currentFrameEndEntry = nt::NetworkTableInstance::GetDefault().GetTable(visionTableKey)->GetEntry(currentFrameEndKey);
    currentFrameRateEntry = nt::NetworkTableInstance::GetDefault().GetTable(visionTableKey)->GetEntry(currentFrameRateKey);

    // Object Detection Specific
	numberOfObjectsEntry = nt::NetworkTableInstance::GetDefault().GetTable(objectTableKey)->GetEntry(numberOfObjectsKey);
	objectCenterEntry = nt::NetworkTableInstance::GetDefault().GetTable(objectTableKey)->GetEntry(objectCenterKey);
	objectHeightEntry = nt::NetworkTableInstance::GetDefault().GetTable(objectTableKey)->GetEntry(objectHeightKey);
	objectWidthEntry = nt::NetworkTableInstance::GetDefault().GetTable(objectTableKey)->GetEntry(objectWidthKey);
	objectColorEntry = nt::NetworkTableInstance::GetDefault().GetTable(objectTableKey)->GetEntry(objectColorKey);

    // Controls for Listeners
    frameRateLimitEntry = nt::NetworkTableInstance::GetDefault().GetTable(listenerTableKey)->GetEntry(frameRateLimitKey);
    visionExposureEntry = nt::NetworkTableInstance::GetDefault().GetTable(listenerTableKey)->GetEntry(visionExposureKey);
    visionBrightnessEntry = nt::NetworkTableInstance::GetDefault().GetTable(listenerTableKey)->GetEntry(visionBrightnessKey);

}

void NetworkTables::refreshNetworkTableObjects() {

    // Check to see if these objects have been created yet.   
    // If the code was not connected to the NT server when 
    // the class was constructed then they will not exist
    
    if ( visionStatusEntry.Exists() == false ) {
        //printf(" I am here - inside refreshNetworkTableObjects\n ");
        setupNetworkTableObjects();
    }

}


void NetworkTables::printNetworkTableInfo() {

    std::cout << "\nNetwork Table Entry Values\n";
    std::cout << "--------------------------\n";
    std::cout << "Vision State Machine : " << visionStateMachineEntry.GetString("") << "\n";

    std::cout << "Vision Status Entry  : " << visionStatusEntry.GetBoolean(false) << "\n";
    std::cout << "Ball Status Entry    : " << ballDetectStatusEntry.GetBoolean(false) << "\n";

    std::cout << "Initial Angle Entry  : " << initialAngleEntry.GetDouble(-1) << "\n";
    std::cout << "Current Angle Entry  : " << currentAngleEntry.GetDouble(-1) << "\n";
    std::cout << "Delta Angle Entry    : " << deltaAngleEntry.GetDouble(-1) << "\n";
    std::cout << "Distance Entry       : " << distanceEntry.GetDouble(-1) << "\n";
    std::cout << "Time Received Entry  : " << timeReceivedEntry.GetDouble(-1) << "\n";
    std::cout << "Frame Start Entry    : " << currentFrameStartEntry.GetDouble(-1) << "\n";
    std::cout << "Frame End Entry      : " << currentFrameEndEntry.GetDouble(-1) << "\n";
    std::cout << "Frame Rate Entry     : " << currentFrameRateEntry.GetDouble(-1) << "\n";

    std::cout << "Object Count Entry   : " << numberOfObjectsEntry.GetDouble(-1) << "\n";
    // std::cout << "Object Center        : " << objectCenterEntry.GetDoubleArray({ 1 }) << "\n";
    // std::cout << "Object Height        : " << objectHeightEntry.GetDoubleArray({ 1 }) << "\n";
    // std::cout << "Object Width         : " << objectWidthEntry.GetDoubleArray({ 1 }) << "\n";
    // std::cout << "Object Color         : " << objectColorEntry.GetStringArray({ "" }) << "\n";

    std::cout << "Vision Exposure      : " << visionExposureEntry.GetDouble(INITIAL_VISION_EXPOSURE) << "\n";
    std::cout << "Frame Rate Entry     : " << frameRateLimitEntry.GetDouble(INITIAL_FRAME_RATE) << "\n";
    std::cout << "Vision Bright Entry  : " << visionBrightnessEntry.GetDouble(INITIAL_LED_BRIGHTNESS) << "\n";

}

void NetworkTables::initalizeNetworkTableInfo()
{
    // Force a refresh
    refreshNetworkTableObjects();

    // State Machine Status and Tracking
    visionStateMachineEntry.SetString("Initializing");
    ballDetectStatusEntry.SetBoolean(false);
    visionStatusEntry.SetBoolean(false);

    // Vision Specific
    deltaAngleEntry.SetDouble(-1);
    initialAngleEntry.SetDouble(-1);
    distanceEntry.SetDouble(-1);
    timeReceivedEntry.SetDouble(-1);
    currentAngleEntry.SetDouble(-1);
    currentFrameStartEntry.SetDouble(-1);
    currentFrameEndEntry.SetDouble(-1);
    currentFrameRateEntry.SetDouble(-1);

    // Object Detection Specific
    numberOfObjectsEntry.SetDouble(1);

    std::vector <double> doublearray(2);
    doublearray[0] = -1.0;
    doublearray[1] = -1.0;
    objectCenterEntry.SetDoubleArray(doublearray);

    std::vector <double> sizearray(1);
    sizearray[0] = -1.0;
    objectHeightEntry.SetDoubleArray(sizearray);
    objectWidthEntry.SetDoubleArray(sizearray);

    //std::vector <double> colorarray(1);
    //colorarray[0] = 0;
    //objectColorEntry.SetDoubleArray(colorarray);

    std::vector <std::string> colorarray(1);
    colorarray[0] = "";
    objectColorEntry.SetStringArray(colorarray);

    // Controls for Listeners
    frameRateLimitEntry.SetDouble(INITIAL_FRAME_RATE);
    visionExposureEntry.SetDouble(INITIAL_VISION_EXPOSURE);
    visionBrightnessEntry.SetDouble(INITIAL_LED_BRIGHTNESS);

    // std::cout << "Vision Bright Entry Before : " << visionBrightnessEntry.GetDouble(-1) << "\n";

    // Read the values from the LED Hardware, set the network values
    LED_get_brightness();

    // Wait 250 ms for network tables to update
    delay(250);

    //Bug Fix in Python Code -  turn off the LED whigh will be on
    LED_Turn_OFF();

    // std::cout << "Vision Bright Entry After : " << vision_brightnessState << "\n";

}

void NetworkTables::setupListenerServices( NetworkTables * NT_instance, PipelineConfig* configPipeline )
{
    //Check to see if this is the first time that this has been run
    initalizeNetworkTableInfo();

    // Code used for debugging
    //printf("Inside setupListnerServices\n");
    //printf("NT Instance ptr : %x\n", NT_instance);
    //printf("Config Pipe ptr : %x\n", configPipeline);

    // Now set-up the network table listeners
    std::function<void(const nt::EntryNotification& event)> visionListener = [ NT_instance, configPipeline ](const nt::EntryNotification& event)
    {
        // Code used for debugging
        //std::cout << "Inside VisionListener\n";
        //printf("NT Instance ptr : %x\n", NT_instance ) ;
        //printf("Config Pipe ptr : %x\n", configPipeline);

        // This listener checks for updates in vision target detection state
        bool NT_visionState = NT_instance->visionStatusEntry.GetBoolean(false);
        // std::cout << "Vision State : " << NT_visionState << "\n";

        // Force - Turn off the ball cam state
        NT_instance->ballDetectStatusEntry.SetBoolean(false);
        bool ballCam_trackingState = NT_instance->ballDetectStatusEntry.GetBoolean(false);
        // std::cout << "BallCam State : " << ballCam_trackingState << "\n";

        if (NT_visionState) {
            LED_Turn_ON();
            std::cout << "Turning ON Vision LED\n";
            //vision_trackingState_new = VISION_STATE::TARGET_DETECTION;
            NT_instance->vision_trackingState_new = VISION_STATE::TARGET_DETECTION;
            std::cout << "Tracking State : TARGET DETECTION\n";
        }
        else {
            LED_Turn_OFF();
            std::cout << "Turning OFF Vision LED\n";
            //vision_trackingState_new = VISION_STATE::SHUFFLEBOARD;
            NT_instance->vision_trackingState_new = VISION_STATE::SHUFFLEBOARD;
            std::cout << "Tracking State : SHUFFLEBOARD\n";
        }
        
    };

    std::function<void(const nt::EntryNotification& event)> ballCamListener = [NT_instance, configPipeline](const nt::EntryNotification& event)
    {
        // Code used for debugging
        // std::cout << "Inside BallCamListener\n";
        // printf("NT Instance ptr : %x\n", NT_instance ) ;
        // printf("Config Pipe ptr : %x\n", configPipeline);

        // This listener checks for requests to use the ballcam to enable the finding of ball locations
        bool ballCam_trackingState = NT_instance->ballDetectStatusEntry.GetBoolean(false);
        // std::cout << "Current BallCam State : " << ballCam_trackingState << "\n";

        // Force - Turn off the Vision state
        NT_instance->visionStatusEntry.SetBoolean(false);
        bool NT_visionState = NT_instance->visionStatusEntry.GetBoolean(false);
        // std::cout << "Current Vision State : " << NT_visionState << "\n";

        // Ensure that the Turret LED is turned off
        LED_Turn_OFF();

        if (ballCam_trackingState) {
            // Set the state machine
            //vision_trackingState_new = VISION_STATE::BALL_DETECTION;
            NT_instance->vision_trackingState_new = VISION_STATE::BALL_DETECTION;
            std::cout << "Tracking State : BALL DETECTION\n";
        }
        else {
            //vision_trackingState_new = VISION_STATE::SHUFFLEBOARD;
            NT_instance->vision_trackingState_new = VISION_STATE::SHUFFLEBOARD;
            std::cout << "Tracking State : SHUFFLEBOARD\n";
        }

    };

    std::function<void(const nt::EntryNotification& event)> visionBrightnessListener = [NT_instance, configPipeline](const nt::EntryNotification& event)
    {
        // Code used for debugging
        // std::cout << "Inside BrightnessListener\n";
        // printf("NT Instance ptr : %x\n", NT_instance ) ;
        // printf("Config Pipe ptr : %x\n", configPipeline);

        // This listener checks for updates in the LED brightness
        configPipeline->setVisionLEDBrightness( NT_instance->visionBrightnessEntry.GetDouble(INITIAL_LED_BRIGHTNESS) );
        std::cout << "Vision Brightness Listener : " << configPipeline->getVisionLEDBrightness() << "\n";

        // Set the LED brightness
        LED_set_brightness( configPipeline->getVisionLEDBrightness() );
    };

    std::function<void(const nt::EntryNotification& event)> visionFrameRateListener = [NT_instance, configPipeline](const nt::EntryNotification& event)
    {
        // Code used for debugging
        // std::cout << "Inside Frame Rate Listener\n";
        // printf("NT Instance ptr : %x\n", NT_instance);
        // printf("Config Pipe ptr : %x\n", configPipeline);

        // This listener checks for updates in the framerate control
        NT_instance->vision_frameRateLimit = NT_instance->frameRateLimitEntry.GetDouble(INITIAL_FRAME_RATE);
        NT_instance->frameRateTimer_waitTime = std::chrono::milliseconds((long)(1000.0 / NT_instance->vision_frameRateLimit));
        std::cout << "Vision FrameRate Listener : " << NT_instance->vision_frameRateLimit << "\n";
    };

    std::function<void(const nt::EntryNotification& event)> visionExposureListener = [NT_instance, configPipeline](const nt::EntryNotification& event)
    {
        // Code used for debugging
        // std::cout << "Inside Exposure Listener\n";
        // printf("NT Instance ptr : %x\n", NT_instance);
        // printf("Config Pipe ptr : %x\n", configPipeline);

        // This listener checks for updates in the framerate control
        configPipeline->setCameraManualExposure( NT_instance->visionExposureEntry.GetDouble(INITIAL_VISION_EXPOSURE) ) ;
        std::cout << "Vision Exposure Listener : " << configPipeline->getCameraManualExposure() << "\n";
    };

    // Set Up Listeners for specific network table entries
    unsigned int listenerState = NT_NOTIFY_UPDATE | NT_NOTIFY_IMMEDIATE;

    NT_instance->visionStatusEntry.AddListener(visionListener, listenerState);
    NT_instance->ballDetectStatusEntry.AddListener(ballCamListener, listenerState);
    NT_instance->visionBrightnessEntry.AddListener(visionBrightnessListener, listenerState);
    NT_instance->frameRateLimitEntry.AddListener(visionFrameRateListener, listenerState);
    NT_instance->visionExposureEntry.AddListener(visionExposureListener, listenerState);

}

void NetworkTables::delay(int milliseconds)
{
    long pause;
    clock_t now, then;

    pause = milliseconds * (CLOCKS_PER_SEC / 1000);
    now = then = clock();
    while ((now - then) < pause)
        now = clock();
}
