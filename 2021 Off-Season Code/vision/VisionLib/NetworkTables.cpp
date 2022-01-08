#include "NetworkTables.h"
#include "LED_Control.h"
#include <iostream>


NetworkTables::NetworkTables()
{

    vision_brightnessState = INITIAL_LED_BRIGHTNESS;
    vision_frameRateLimit = INITIAL_FRAME_RATE;
    vision_exposure = INITIAL_VISION_EXPOSURE;

    void setupNetworkTableObjects();
}

NetworkTables::~NetworkTables(void)
{
	// Default Destructor - cleans up
}

void NetworkTables::setupNetworkTableObjects() {
    
    visionStatusEntry = nt::NetworkTableInstance::GetDefault().GetTable(visionTableKey)->GetEntry(visionStatusKey);
	deltaAngleEntry = nt::NetworkTableInstance::GetDefault().GetTable(visionTableKey)->GetEntry(deltaAngleKey);
	initialAngleEntry = nt::NetworkTableInstance::GetDefault().GetTable(visionTableKey)->GetEntry(initialAngleKey);
	distanceEntry = nt::NetworkTableInstance::GetDefault().GetTable(visionTableKey)->GetEntry(distanceKey);
	timeReceivedEntry = nt::NetworkTableInstance::GetDefault().GetTable(visionTableKey)->GetEntry(timeReceivedKey);
	currentAngleEntry = nt::NetworkTableInstance::GetDefault().GetTable(visionTableKey)->GetEntry(currentAngleKey);
	frameRateLimitEntry = nt::NetworkTableInstance::GetDefault().GetTable(visionTableKey)->GetEntry(frameRateLimitKey);
	visionExposureEntry = nt::NetworkTableInstance::GetDefault().GetTable(visionTableKey)->GetEntry(visionExposureKey);
	visionBrightnessEntry = nt::NetworkTableInstance::GetDefault().GetTable(visionTableKey)->GetEntry(visionBrightnessKey);

	ballCamStateEntry = nt::NetworkTableInstance::GetDefault().GetTable(ballCamTableKey)->GetEntry(ballCamStateKey);
	ballCountEntry = nt::NetworkTableInstance::GetDefault().GetTable(ballCamTableKey)->GetEntry(ballCountKey);
	ballLocationsEntry = nt::NetworkTableInstance::GetDefault().GetTable(ballCamTableKey)->GetEntry(ballLocationKey);

	visionStateEntry = nt::NetworkTableInstance::GetDefault().GetTable(visionStateMachineTableKey)->GetEntry(visionStateMachineKey);

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
    std::cout << "State Machine       : " << visionStateEntry.GetString("") << "\n";
    std::cout << "Vision State        : " << visionStatusEntry.GetDouble(-1) << "\n";
    std::cout << "Initial Angle Entry : " << initialAngleEntry.GetDouble(-1) << "\n";
    std::cout << "Current Angle Entry : " << currentAngleEntry.GetDouble(-1) << "\n";
    std::cout << "Delta Angle Entry   : " << deltaAngleEntry.GetDouble(-1) << "\n";
    std::cout << "Distance Entry      : " << distanceEntry.GetDouble(-1) << "\n";
    std::cout << "Time Received Entry : " << timeReceivedEntry.GetDouble(-1) << "\n";
    std::cout << "Vision Status Entry : " << visionStatusEntry.GetBoolean(false) << "\n";
    std::cout << "Vision Exposure     : " << visionExposureEntry.GetDouble(INITIAL_VISION_EXPOSURE) << "\n";
    std::cout << "Frame Rate Entry    : " << frameRateLimitEntry.GetDouble(INITIAL_FRAME_RATE) << "\n";
    std::cout << "Vision Bright Entry : " << visionBrightnessEntry.GetDouble(INITIAL_LED_BRIGHTNESS) << "\n";
    std::cout << "BallCam Status Entry: " << ballCamStateEntry.GetBoolean(false) << "\n";
    std::cout << "Ball Count Entry    : " << ballCountEntry.GetDouble(-1) << "\n\n";

}

void NetworkTables::initalizeNetworkTableInfo()
{
    // Force a refresh
    refreshNetworkTableObjects();

    deltaAngleEntry.SetDouble(-1);
    initialAngleEntry.SetDouble(-1);
    distanceEntry.SetDouble(-1);
    timeReceivedEntry.SetDouble(-1);
    currentAngleEntry.SetDouble(-1);
    visionStatusEntry.SetBoolean(false);
    frameRateLimitEntry.SetDouble(INITIAL_FRAME_RATE);
    visionExposureEntry.SetDouble(INITIAL_VISION_EXPOSURE);
    visionBrightnessEntry.SetDouble(INITIAL_LED_BRIGHTNESS);

    ballCamStateEntry.SetBoolean(false);
    ballCountEntry.SetDouble(1);

    visionStateEntry.SetString("Initializing");

    size_t array_size = 1;
    std::vector <double> doublearray(array_size * 2);

    for (int i = 0; i < array_size; i++) {
        doublearray[i] = -1.0;
    }

    ballLocationsEntry.SetDoubleArray(doublearray);

    // std::cout << "Vision Bright Entry Before : " << visionBrightnessEntry.GetDouble(-1) << "\n";

    // Read the values from the LED Hardware, set the network values
    LED_get_brightness();

    // Wait 250 ms for network tables to update
    delay(250);

    //Bug Fix in Python Code -  turn off the LED whigh will be on
    LED_Turn_OFF();

    // std::cout << "Vision Bright Entry After : " << vision_brightnessState << "\n";

}

void NetworkTables::setupListenerServices( NetworkTables * NT_instance )
{
    //Check to see if this is the first time that this has been run
    initalizeNetworkTableInfo();

    // Code used for debugging
    //printf("Inside setupListnerServices\n");
    //printf("NT Instance ptr : %x\n", NT_instance );

    // Now set-up the network table listeners
    std::function<void(const nt::EntryNotification& event)> visionListener = [NT_instance](const nt::EntryNotification& event)
    {
        // Code used for debugging
        //std::cout << "Inside VisionListener\n";
        // printf("NT Instance ptr : %x\n", NT_instance ) ;

        // This listener checks for updates in vision target detection state
        bool NT_visionState = NT_instance->visionStatusEntry.GetBoolean(false);
        // std::cout << "Vision State : " << NT_visionState << "\n";

        // Force - Turn off the ball cam state
        NT_instance->ballCamStateEntry.SetBoolean(false);
        bool ballCam_trackingState = NT_instance->ballCamStateEntry.GetBoolean(false);
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

        // Code used for debugging
        //printf("NT Instance ptr : %x\n", NT_instance);
        //printf("Current Tracking State %d\n", vision_trackingState_current);
        //printf("New Tracking State     %d\n", vision_trackingState_new);
        
    };

    std::function<void(const nt::EntryNotification& event)> ballCamListener = [NT_instance](const nt::EntryNotification& event)
    {
        // Code used for debugging
        //std::cout << "Inside BallCamListener\n";
        // printf("NT Instance ptr : %x\n", NT_instance);

        // This listener checks for requests to use the ballcam to enable the finding of ball locations
        bool ballCam_trackingState = NT_instance->ballCamStateEntry.GetBoolean(false);
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

        // Code used for debugging
        //printf("NT Instance ptr : %x\n", NT_instance);
        //printf("Current Tracking State %d\n", vision_trackingState_current);
        //printf("New Tracking State     %d\n", vision_trackingState_new);

    };

    std::function<void(const nt::EntryNotification& event)> visionBrightnessListener = [NT_instance](const nt::EntryNotification& event)
    {
        // Code used for debugging
        //std::cout << "Inside BrightnessListener\n";
        //printf("NT Instance ptr : %x\n", NT_instance);

        // This listener checks for updates in the LED brightness
        NT_instance->vision_brightnessState = NT_instance->visionBrightnessEntry.GetDouble(INITIAL_LED_BRIGHTNESS);
        std::cout << "Vision Brightness Listener : " << NT_instance->vision_brightnessState << "\n";

        // Set the LED brightness
        LED_set_brightness(NT_instance->vision_brightnessState);
    };

    std::function<void(const nt::EntryNotification& event)> visionFrameRateListener = [NT_instance](const nt::EntryNotification& event)
    {
        // Code used for debugging
        //std::cout << "Inside Frame Rate Listener\n";
        //printf("NT Instance ptr : %x\n", NT_instance);

        // This listener checks for updates in the framerate control
        NT_instance->vision_frameRateLimit = NT_instance->frameRateLimitEntry.GetDouble(INITIAL_FRAME_RATE);
        NT_instance->frameRateTimer_waitTime = std::chrono::milliseconds((long)(1000.0 / NT_instance->vision_frameRateLimit));
        std::cout << "Vision FrameRate Listener : " << NT_instance->vision_frameRateLimit << "\n";
    };

    std::function<void(const nt::EntryNotification& event)> visionExposureListener = [NT_instance](const nt::EntryNotification& event)
    {
        // Code used for debugging
        //std::cout << "Inside Exposure Listener\n";
        //printf("NT Instance ptr : %x\n", NT_instance);

        // This listener checks for updates in the framerate control
        NT_instance->vision_exposure = NT_instance->visionExposureEntry.GetDouble(INITIAL_VISION_EXPOSURE);
        std::cout << "Vision Exposure Listener : " << NT_instance->vision_exposure << ", " << pow(2.0, NT_instance->vision_exposure) << "\n";
    };

    // Set Up Listeners for specific network table entries
    unsigned int listenerState = NT_NOTIFY_UPDATE | NT_NOTIFY_IMMEDIATE;

    NT_instance->visionStatusEntry.AddListener(visionListener, listenerState);
    NT_instance->ballCamStateEntry.AddListener(ballCamListener, listenerState);
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
