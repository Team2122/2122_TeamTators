#include "TargetLocation.hpp"
#include "Vision_Model.hpp"
#include <math.h>
#include <string>
#include <iostream>
#include <string>
#include <tgmath.h>

#ifndef __linux__
	# define M_PI           3.14159265358979323846 ;
#endif // !__linux__

using std::string;
using std::endl;

double TargetLocation::getDistance()
{
	return getCenterDistance();
}

double TargetLocation::getCenterDistance()
{
	double corrected_distance = (1.0 / sqrt(scalefactor)) * getCenterY();
	//this was the origanl distance cal for Utah
	//double result = ((0.00000094 * corrected_distance * corrected_distance * corrected_distance) - (0.00021123 * corrected_distance * corrected_distance) + (0.0379369 * corrected_distance) + 7.83203065);

	// New Calibration curve for Houston 4/16/2022
	// This is in Inches, converting to feet 
	//This was for trasportataor
	//double result = ((2.5075e-3 * corrected_distance * corrected_distance) - (8.1986e-2 * corrected_distance) + 1.0555e2) / 12.0;

	//New equation 
	double result = ((1.8856e-4 * corrected_distance * corrected_distance) - (1.415e-2 * corrected_distance) + 8.4486);

	// printf(" Scalefactor : %g \n", scalefactor);
	// printf("Y Value : %g\n", corrected_distance);
	// printf("Distance : %g\n", result);

	return result;
}

double TargetLocation::getDistanceTopWidth()
{
	// Distance Estimation using the width of the top of the target
	double TopWidth = (1 / sqrt(scalefactor)) * sqrt( pow(top_right_x - top_left_x,2) + pow( top_right_y - top_left_y, 2 ) ) ;

	// Apr 18, 2021  -  Updated using Mar 21 data
	double distance_to_target   =  28619.82593 * pow( TopWidth, -1.06925 ) ;
	
	// Convert the distance vector to the x-axis distance
	double distance_to_target_x = sqrt(distance_to_target * distance_to_target - pow(target_height - turret_height, 2)) / 12.0;

	return distance_to_target_x;
}

double TargetLocation::getDistanceCenterHeight()
{

	double TMX = (1 / sqrt(scalefactor)) * (top_right_x + top_left_x) / 2.0;
	double TMY = (1 / sqrt(scalefactor)) * (top_right_y + top_left_y) / 2.0;

	double BMX = (1 / sqrt(scalefactor)) * (bottom_right_x + bottom_left_x) / 2.0;
	double BMY = (1 / sqrt(scalefactor)) * (bottom_right_y + bottom_left_y) / 2.0;

	double center_height = sqrt(pow((TMX - BMX), 2) + pow((TMY - BMY), 2));

	// Updated using 26 Aug 21 data
	double center_height_focal_length = 0.0498 * center_height * center_height - 10.26 * center_height + 599.18;

	// Convert the distance vector to the x-axis distance
	double distance_to_target_x = sqrt( center_height_focal_length * center_height_focal_length - pow(target_height - turret_height, 2)) / 12.0;

	return distance_to_target_x;

}

double TargetLocation::getDistanceFocalLength()
{
	// This is not currently used 

	double TMX = (1 / sqrt(scalefactor)) * (top_right_x + top_left_x) / 2.0;
	double TMY = (1 / sqrt(scalefactor)) * (top_right_y + top_left_y) / 2.0;

	double BMX = (1 / sqrt(scalefactor)) * (bottom_right_x + bottom_left_x) / 2.0;
	double BMY = (1 / sqrt(scalefactor)) * (bottom_right_y + bottom_left_y) / 2.0;

	double center_height = sqrt(pow((TMX - BMX), 2) + pow((TMY - BMY), 2));

	// Updated using 26 Aug 21 data
	double center_height_focal_length = 0.223811 * center_height * center_height - 14.598797 * center_height + 328.584533;
	double distance_to_target = 4280.793305 * pow(center_height_focal_length, -0.591973);

	// Convert the distance vector to the x-axis distance
	double distance_to_target_x = sqrt(distance_to_target * distance_to_target - pow(target_height - turret_height, 2)) / 12.0;

	return distance_to_target_x;

}

double TargetLocation::getBotAngle() 
{
	// This code estimates the angle of the robot relative to the target
	// Using the two upper co-ordinates

	//double upperSkew = 0;
	//double lowerSkew = 0;
	//double sum = 0;

	//upperSkew = 180 * atan((top_left_y - top_right_y) / double(top_left_x - top_right_x)) / M_PI ;
	//upperSkew = 3.0191 * upperSkew + 2.6551;
	//
	//botAngleEntry[ botAngleCounter % botAngleEntryLength ] = upperSkew;
	//++botAngleCounter;

	//for(int i = 0; i < botAngleEntryLength; ++i)
	//{
	//	sum += botAngleEntry[i];
	//}

	//sum/=botAngleEntryLength;
	// return sum;

	return 0;
}

double TargetLocation::getCenterX() {
	return( (this->top_left_x+this->bottom_right_x) / 2.0) ;
}

double TargetLocation::getCenterY() {
	return( (this->top_left_y + this->top_right_y) / 2.0);
}

void TargetLocation::getTargetCenter( Location* centerLoc ) {

	centerLoc->x = this->getCenterX() ;
	centerLoc->y = this->getCenterY();
}

void TargetLocation::printTargetCenter() {
	printf("Center [x, y] : [ %g, %g ]\n", this->getCenterX(), this->getCenterY() );
}


double TargetLocation::getTurretAngle()
{
	//
	// This is the routine that is called to align the turret with
	// the center of the target
	//

	double image_width = 640;								// Number of pixels in image
	double FOV_angle = 75.76079874010732;					// Field of View
	double centerLocationPoint = 0;							// Location of target center
	double targetDistance = getCenterDistance();			// distance correction for optical corr

	double near_distance = 8.3;								// Close-Up distance to hub
	double far_distance = 18.5;								// Far-out distance to hub

	double optical_near_corr = 14;							// Optical correction needed close up
	double optical_dist_corr = 20;							// Optical correction needed far away

	// Transportator
	// double optical_correction = -16; // % Pixel correction : 320 - 336;

	// Sassitator
	double optical_delta = (targetDistance - near_distance) / (far_distance - near_distance);

	if (optical_delta < 0) {
		optical_delta = 0;
	} else if (optical_delta > 1) {
		optical_delta = 1;
	}

	double optical_correction = optical_near_corr + optical_delta*(optical_dist_corr-optical_near_corr); 

	// scalefactor corrects for the image subsampling ( relative to 640 pixel width )
	centerLocationPoint = optical_correction + (getCenterX() / sqrt(scalefactor)) ;

	// Now compute the angular offset 
	double turretAngletoRotate = (centerLocationPoint - image_width/2) * ( FOV_angle / image_width ) ;

	if (turretAngletoRotate > 90) {
		turretAngletoRotate = turretAngletoRotate - 90;
	}

	return turretAngletoRotate;
}

void TargetLocation::stringLocation() {

	string TLX = std::to_string(top_left_x);
	string TLY = std::to_string(top_left_y);

	string TRX = std::to_string(top_right_x);
	string TRY = std::to_string(top_right_y);

	string BLX = std::to_string(bottom_left_x);
	string BLY = std::to_string(bottom_left_y);

	string BRX = std::to_string(bottom_right_x);
	string BRY = std::to_string(bottom_right_y);

	std::cout << (TLX + ", " + TLY + ", " + TRX + ", " + TRY + ", " + BLX + ", " + BLY + ", " + BRX + ", " + BRY);
}

bool TargetLocation::checkValidity() 
{
	//printf(" BLx : %u, BLy : %u\n", bottom_left_x, bottom_left_y);
	//printf(" BRx : %u, BRy : %u\n", bottom_right_x, bottom_right_y);
	//printf(" TLx : %u, TLy : %u\n", top_left_x, top_left_y);
	//printf(" TRx : %u, TRy : %u\n", top_right_x, top_right_y);

	if (bottom_left_x == -1 ||
		bottom_right_x == -1 ||
		bottom_left_y == -1 ||
		bottom_right_y == -1 ||
		top_left_x == -1 ||
		top_right_x == -1 ||
		top_left_y == -1 ||
		top_right_y == -1
		) 
	{
		// printf("Location has invalid data.\n");
		return false;
	}

	return true;
}

void TargetLocation::resetData() {
	bottom_left_x = -1;
	bottom_right_x = -1;
	bottom_left_y = -1;
	bottom_right_y = -1;
	top_left_x = -1;
	top_right_x = -1;
	top_left_y = -1;
	top_right_y = -1;
}

void TargetLocation::setScalefactor(double scale)
{
	// setting the scalefactor
	this->scalefactor = scale;
}

