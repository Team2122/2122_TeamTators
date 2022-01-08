#pragma once

#ifndef _NETWORKTABLES_
#define _NETWORKTABLES_

#include "networktables/NetworkTableInstance.h"
#include "networktables/NetworkTableEntry.h"

// Set up parameters that will be used

#define INITIAL_FRAME_RATE 10
#define INITIAL_LED_BRIGHTNESS 0.25
#define INITIAL_VISION_EXPOSURE 12

class NetworkTables;

enum class VISION_STATE {
	SHUFFLEBOARD,
	TARGET_DETECTION,
	BALL_DETECTION
};


class NetworkTables
{
public:

	NetworkTables();  // Default Constructor
	~NetworkTables(); // Default Destructor - cleans up

	// Add state machine attributes
	VISION_STATE vision_trackingState_current = VISION_STATE::SHUFFLEBOARD;
	VISION_STATE vision_trackingState_new = VISION_STATE::SHUFFLEBOARD;

	// Initialize inital values for Network Table Entries
	double vision_brightnessState ;
	double vision_frameRateLimit ;
	double vision_exposure ;

	// Setting up global varible for frame rate control
	std::chrono::milliseconds frameRateTimer_waitTime = (std::chrono::milliseconds) 0;


	// Setup DriverStation Viewing    
	const wpi::Twine visionNTEntry = wpi::Twine("VisionOutput");
	const wpi::Twine groundNTEntry = wpi::Twine("GroundOutput");

	// NetworkTable communication
	const wpi::Twine visionTableKey = wpi::Twine("visionTable");
	const wpi::Twine deltaAngleKey = wpi::Twine("deltaAngle");
	const wpi::Twine initialAngleKey = wpi::Twine("initialAngle");
	const wpi::Twine distanceKey = wpi::Twine("distance");
	const wpi::Twine timeReceivedKey = wpi::Twine("timeReceived");
	const wpi::Twine currentAngleKey = wpi::Twine("currentAngle");
	const wpi::Twine visionStatusKey = wpi::Twine("visionStatus");
	const wpi::Twine visionExposureKey = wpi::Twine("visionExposure");
	const wpi::Twine frameRateLimitKey = wpi::Twine("frameRateLimit");
	const wpi::Twine visionBrightnessKey = wpi::Twine("visionBrightness");

	const wpi::Twine ballCamTableKey = wpi::Twine("Ground Ball Detection");
	const wpi::Twine ballCamStateKey = wpi::Twine("BallCamStatus");
	const wpi::Twine ballCountKey = wpi::Twine("BallCount");
	const wpi::Twine ballLocationKey = wpi::Twine("BallLocations");

	const wpi::Twine visionStateMachineTableKey = wpi::Twine("Vision State Machine");
	const wpi::Twine visionStateMachineKey = wpi::Twine("State");

	// Network Tables for Vision Camera
	nt::NetworkTableEntry visionStatusEntry;
	nt::NetworkTableEntry deltaAngleEntry;
	nt::NetworkTableEntry initialAngleEntry;
	nt::NetworkTableEntry distanceEntry;
	nt::NetworkTableEntry timeReceivedEntry;
	nt::NetworkTableEntry currentAngleEntry;
	nt::NetworkTableEntry frameRateLimitEntry;
	nt::NetworkTableEntry visionExposureEntry;
	nt::NetworkTableEntry visionBrightnessEntry;

	// Network Tables for Ball Camera
	nt::NetworkTableEntry ballCamStateEntry;
	nt::NetworkTableEntry ballCountEntry;
	nt::NetworkTableEntry ballLocationsEntry;

	// Network Table entry for Vision StateMachine
	nt::NetworkTableEntry visionStateEntry;

	void printNetworkTableInfo();
	void setupListenerServices(NetworkTables* NT_instance);

private:

	void setupNetworkTableObjects();
	void refreshNetworkTableObjects();
	void initalizeNetworkTableInfo();
	void delay(int milliseconds);

};

#endif // _NETWORKTABLES_
