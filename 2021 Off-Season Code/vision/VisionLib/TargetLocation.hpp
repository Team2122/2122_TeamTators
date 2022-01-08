#pragma once

#ifndef _TARGETLOCATION_
#define _TARGETLOCATION_

#include <string>

using std::string;

class TargetLocation {

public:

	int top_right_x = 0;
	int top_right_y = 0;

	int bottom_right_x = 0;
	int bottom_right_y = 0;

	int top_left_x = 0;
	int top_left_y = 0;

	int bottom_left_x = 0;
	int bottom_left_y = 0;

	double getBotAngle();
	double getDistance();
	double getTurretAngle();

	void stringLocation();
	bool checkValidity();

	void resetData();
	void setScalefactor(double scalefactor);

	// These are not used
	//double GetCX(int imageWidth);
	//double GetTMX();
	//double getDistanceTransform();
	//double getDeltaAngle();

private:

	double getDistanceTopWidth();
	double getDistanceCenterHeight();
	double getDistanceFocalLength();

	double scalefactor = 1.0;

	const double actualWidth = 1;
	const double actualHeight = 1;

	double target_height = (8 * 12) + 2.25; // Height in in
	double turret_height = 40;				// Height in in

	const double cameraAngle = 0;
	const double topAngle = cameraAngle + (.5 * fovY);
	const double fovX = 1, fovY = 1;
	const double width = 1, height = 1;
	const double targetHeight = 1; // In inches, the distance from top to bottom of the target
	const double degreesPerPixelX = fovX / width;
	const double degreesPerPixelY = fovY / height;

	double GetDistanceTransform();
	double sinRule(double const theta);
};
#endif 
