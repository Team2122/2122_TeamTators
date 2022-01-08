#pragma once

#ifndef _TARGET_IMAGE_
#define _TARGET_IMAGE_

#include "VisionImage.hpp"

class TargetImage : public VisionImage {

public:

	struct TargetLoc {
		ImagePoint* topLeft;
		ImagePoint* topRight;
		ImagePoint* bottomCenter;
	};

	TargetImage();
	TargetImage(Image_Store* image, double scaleFactor);
	TargetImage(cv::Mat* mat, double scaleFactor);
	~TargetImage();

	void create();

	double getDeltaAngle();
	double getDeltaAngle(TargetLoc* location);
	double getDistanceTransform();
	double getDistanceTransform(TargetLoc* location);
	bool checkValidity();
	bool checkValidity(TargetLoc* location);
	

	TargetLoc* location;

	int boundDetect( TargetImage::BoundingBox* boundingBox,
					 std::vector<VisionImage::ImagePoint>* pointList,
					 TargetImage::TargetLoc* finalLoc);

	double fovX;
	double fovY;
	double degreesPerPixelX;
	double degreesPerPixelY;

	double cameraAngle = 0;
	double topAngle = cameraAngle + (.5 * fovY);
	const double targetHeight = 1; // In inches, the distance from top to bottom of the target
	
};

#endif
