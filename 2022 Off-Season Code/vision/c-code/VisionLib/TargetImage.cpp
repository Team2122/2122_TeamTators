#include "TargetImage.hpp"

TargetImage::TargetImage() : VisionImage() {
	create();
}

TargetImage::TargetImage(Image_Store* image, double scaleFactor) : VisionImage(image, scaleFactor) {
	create();
}

TargetImage::TargetImage(cv::Mat* mat, double scaleFactor) : VisionImage(mat, scaleFactor) {
	create();
}

void TargetImage::create() {
	location = new TargetLoc();
}

TargetImage::~TargetImage() {

	delete location;

}

double TargetImage::getDeltaAngle() {
	return getDeltaAngle(location);
}

double TargetImage::getDeltaAngle(TargetLoc* location) {
	double center = location->bottomCenter->x;

	return (center - (scaled_image->width / 2)) * degreesPerPixelX;
}

double TargetImage::getDistanceTransform() {
	return getDistanceTransform(location);
}

double TargetImage::getDistanceTransform(TargetLoc* location){
	double targetBottom = (location->bottomCenter->y) / 2;
	double targetTop = (location->topLeft->y + location->topRight->y) / 2;
	double thetaBottom = topAngle - (targetBottom * degreesPerPixelY);
	double thetaTop = topAngle - (targetTop * degreesPerPixelY);
	double slopeTop = tan(thetaTop); // This calculates the slope of the top line
	double slopeBottom = tan(thetaBottom); // This calculates the slope of the bottom line

	return (targetHeight / (slopeTop - slopeBottom)); // Horizontal distance to Target from camera
}

bool TargetImage::checkValidity() {
	return checkValidity(location);
}

bool TargetImage::checkValidity(TargetLoc* location){ 
	if (location->bottomCenter->x == -1 ||
		location->bottomCenter->y == -1 ||
		location->topLeft->x == -1 ||
		location->topLeft->y == -1 ||
		location->topRight->x == -1 ||
		location->topRight->y == -1) {

		return false;
	}
	else {
		return true;
	}
}

int  TargetImage::boundDetect( TargetImage:: BoundingBox* boundingBox,
				  std::vector<VisionImage::ImagePoint>* pointList, 
			      TargetImage::TargetLoc* finalLoc )
{

	for (int i = 0; i < pointList->size(); i++) {
		int imagePoint = 0;
	}

	return EXIT_SUCCESS ;
}
