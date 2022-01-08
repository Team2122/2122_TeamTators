#include "TargetLocation.hpp"
#include <math.h>
#include <string>
#include <iostream>
#include <string>
#include <tgmath.h>

#ifndef __linux__
	# define M_PI           3.14159265358979323846 ;
#endif // !__linux__

#define botAngleEntryLength 4

double botAngleEntry[botAngleEntryLength] = { 0 } ;
long int botAngleCounter = 0; 

using std::string;
using std::endl;



double TargetLocation::getDistance()
{

	double dist1 = getDistanceTopWidth();
	double dist2 = getDistanceCenterHeight();

	double avgDist = (dist1 + dist2) / 2.0;

	//if ( dist2 > ( dist1 + avgDist) )
	//	return dist1;
	//else
	//	return dist2;

	return avgDist;

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

	double upperSkew = 0;
	double lowerSkew = 0;
	double sum = 0;

	upperSkew = 180 * atan((top_left_y - top_right_y) / double(top_left_x - top_right_x)) / M_PI ;
	upperSkew = 3.0191 * upperSkew + 2.6551;
	
	botAngleEntry[ botAngleCounter % botAngleEntryLength ] = upperSkew;
	++botAngleCounter;

	for(int i = 0; i < botAngleEntryLength; ++i)
	{
		sum += botAngleEntry[i];
	}

	sum/=botAngleEntryLength;
	return sum;
}

double TargetLocation::getTurretAngle() 
{
	//
	// This is the routine that is called to align the turret with
	// the center of the target
	//

	//double image_width = 640;		// Number of pixels in image
	//double physical_width = 19.5;   // Distance of FOV in ft
	//double physical_dist = 15;		// Distance to wall in ft

	//// Compute the FOV of the camera
	//double FOV_length = physical_width / ( 2 * physical_dist) ;
	//double FOV_angle  = 2.0 * 180.0 * atan( FOV_length ) / M_PI;

	double image_width = 480;		// Number of pixels in image
	double FOV_angle = 48.8;
	double firingLocationPoint = 0;

	/*
	// Compute the location where we want to target
	if ( getBotAngle() >= 3.0 ) 
	{
		// Distance Shot from Left, we need to correct for the distance
		firingLocationPoint = (1.0/4.0) * ( 3*top_left_x + top_right_x ) ;
		// printf("I am here : positive angle\n");
	}
	else
	*/

	if ( getDistance() > 10.0 )
	{
 		if ( getBotAngle() <= 10)
		{
			// Distance Shot from Right, we need to correct for the distance
			firingLocationPoint = top_left_x + ((3.0 / 4.0) * (top_right_x - top_left_x)) ;
			//printf("Shooting from right\n");
		}
		else
		{
			// Distance Shot from Left, we need to correct for the distance
		    firingLocationPoint = top_left_x + ((3.0 / 8.0) * (top_right_x - top_left_x)) ;
			//printf(Shooting from Left\n");
		}
	}
	else
	{
		// Close up to the target, dont correct for the distance
		firingLocationPoint = (double) (1.0/2.0) * ( top_right_x + top_left_x );
		// printf("I Shooting straight shot\n");
	}

	// Now compute the pixel offset from middle
	// scalefactor corrects for the image subsampling ( relative to 640 pixel width )
	double TMX = (1 / sqrt(scalefactor)) * firingLocationPoint ;

	// Now compute the angular offset 
	double turretAngletoRotate = (TMX - image_width/2) * ( FOV_angle / image_width ) ;

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
	bottom_left_x = 0;
	bottom_right_x = 0;
	bottom_left_y = 0;
	bottom_right_y = 0;
	top_left_x = 0;
	top_right_x = 0;
	top_left_y = 0;
	top_right_y = 0;
}

void TargetLocation::setScalefactor(double scale)
{
	// setting the scalefactor
	this->scalefactor = scale;
}

// This is not used
//double TargetLocation::getDistanceTransform() {
//	double targetBottom = (bottom_left_y + bottom_right_y) / 2;
//	double targetTop = (top_left_y + top_right_y) / 2;
//	double thetaBottom = topAngle - (targetBottom * degreesPerPixelY);
//	double thetaTop = topAngle - (targetTop * degreesPerPixelY); 
//	double slopeTop = tan(thetaTop); // This calculates the slope of the top line
//	double slopeBottom = tan(thetaBottom); // This calculates the slope of the bottom line
//	return (targetHeight / (slopeTop - slopeBottom)); // Horizontal distance to Target from camera
//}

// These are not used
//
//double TargetLocation::sinRule(double const theta){
//    double a = theta;
//    double b = 90-theta;
//    double c = 90;
//    double sideA = sin(a);
//    double sideB = sin(b);
//    return sideA/sideB;
//}
//
//double TargetLocation::getDeltaAngle()
//{
//	// Code to calculate the angle of deviation
//	return ((width * .5) - (top_right_y - top_left_y)) * degreesPerPixelX;
//}
//
//double TargetLocation::GetCX(int imageWidth)
//{
//	return  GetTMX() - (imageWidth * .5);
//}
//
//double TargetLocation::GetTMX()
//{
//	return (top_right_x + top_left_x) / 2;
//}