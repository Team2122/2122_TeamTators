#pragma once

#ifndef _NETWORKTABLES_
#define _NETWORKTABLES_

#include "networktables/NetworkTableInstance.h"
#include "networktables/NetworkTableEntry.h"

// Set up parameters that will be used

#define INITIAL_FRAME_RATE 15
#define INITIAL_LED_BRIGHTNESS 0.25
#define INITIAL_VISION_EXPOSURE 12

class PipelineConfig;
class NetworkTables;

enum class VISION_STATE {
	APRILTAG,
	SHUFFLEBOARD,
	TARGET_DETECTION,
	BALL_DETECTION,
	THRESHOLDING,
	INITIALIZE
};


class NetworkTables
{
public:

	NetworkTables();  // Default Constructor
	~NetworkTables(); // Default Destructor - cleans up

	// Add state machine attributes
	VISION_STATE vision_trackingState_current = VISION_STATE::SHUFFLEBOARD;
	VISION_STATE vision_trackingState_new = VISION_STATE::SHUFFLEBOARD;

	// Setting up varibles for frame rate control
	double vision_frameRateLimit;
	std::chrono::milliseconds frameRateTimer_waitTime = (std::chrono::milliseconds) 0;

	// High Level State Machine
	const wpi::Twine visionStateMachineTableKey = wpi::Twine("Vision State Machine");
		const wpi::Twine visionStateMachineKey	= wpi::Twine("Current State");
		const wpi::Twine visionStatusKey		= wpi::Twine("Vision Status");
		const wpi::Twine ballDetectStatusKey	= wpi::Twine("Ball Detection Status");

		// Network Table entry for Vision StateMachine
		nt::NetworkTableEntry visionStateMachineEntry;
		nt::NetworkTableEntry visionStatusEntry;
		nt::NetworkTableEntry ballDetectStatusEntry;

	// Setup DriverStation Viewing    
	const wpi::Twine visionNTEntry = wpi::Twine("VisionOutput");		// Turret Cam
	const wpi::Twine groundNTEntry = wpi::Twine("GroundOutput");		// Ground Cam
	const wpi::Twine pickerNTEntry = wpi::Twine("PickerOutput");		// Picker Cam

	// Vison NetworkTable communication
	const wpi::Twine visionTableKey = wpi::Twine("visionTable");
		const wpi::Twine deltaAngleKey = wpi::Twine("deltaAngle");
		const wpi::Twine initialAngleKey = wpi::Twine("initialAngle");
		const wpi::Twine distanceKey = wpi::Twine("distance");
		const wpi::Twine timeReceivedKey = wpi::Twine("timeRecieved");
		const wpi::Twine currentAngleKey = wpi::Twine("currentAngle");
		const wpi::Twine currentFrameStartKey = wpi::Twine("currentFrameStart");
		const wpi::Twine currentFrameEndKey = wpi::Twine("currentFrameEnd");
		const wpi::Twine currentFrameRateKey = wpi::Twine("Current Frame Rate");

		// Network Tables for Vision Camera
		nt::NetworkTableEntry deltaAngleEntry;
		nt::NetworkTableEntry initialAngleEntry;
		nt::NetworkTableEntry distanceEntry;
		nt::NetworkTableEntry timeReceivedEntry;
		nt::NetworkTableEntry currentAngleEntry;
		nt::NetworkTableEntry currentFrameStartEntry;
		nt::NetworkTableEntry currentFrameEndEntry;
		nt::NetworkTableEntry currentFrameRateEntry;

	// Objects Keys
	const wpi::Twine objectTableKey = wpi::Twine("Object Detection");
		const wpi::Twine numberOfObjectsKey = wpi::Twine("Number Of Objects");
		const wpi::Twine objectCenterKey = wpi::Twine("Center");
		const wpi::Twine objectHeightKey = wpi::Twine("Height");
		const wpi::Twine objectWidthKey = wpi::Twine("Width");
		const wpi::Twine objectColorKey = wpi::Twine("Color");

		// Object Entries
		nt::NetworkTableEntry numberOfObjectsEntry;
		nt::NetworkTableEntry objectCenterEntry;
		nt::NetworkTableEntry objectHeightEntry;
		nt::NetworkTableEntry objectWidthEntry;
		nt::NetworkTableEntry objectColorEntry;

	// Listener Functions
	const wpi::Twine listenerTableKey = wpi::Twine("Listeners");
		const wpi::Twine visionExposureKey   = wpi::Twine("visionExposure");
		const wpi::Twine frameRateLimitKey   = wpi::Twine("frameRateLimit");
		const wpi::Twine visionBrightnessKey = wpi::Twine("visionBrightness");

	nt::NetworkTableEntry frameRateLimitEntry;
	nt::NetworkTableEntry visionExposureEntry;
	nt::NetworkTableEntry visionBrightnessEntry;

	void printNetworkTableInfo();
	void setupListenerServices( NetworkTables* NT_instance, PipelineConfig* configPipeline );

private:

	void setupNetworkTableObjects();
	void refreshNetworkTableObjects();
	void initalizeNetworkTableInfo();
	void delay(int milliseconds);

};

#endif // _NETWORKTABLES_
