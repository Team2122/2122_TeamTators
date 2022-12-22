#pragma once

#ifndef _VISION_IMAGE_
#define _VISION_IMAGE_

//#include "VisionLib.hpp"
#include <vector>
#include <functional>
#include "ImageStore.hpp"
#include "ExecutorService.hpp"
#include "opencv2/opencv.hpp"


class VisionImage { // FIGURE OUT HOW TO PREVENT INSTANTIATION -> add "virtual void noInstantiation() = 0;"

public:

	// Necessary Structs
	struct Color {
		Color(double r, double g, double b) {
			this->r = r;
			this->g = g;
			this->b = b;
		}
		double r = 0;
		double g = 0;
		double b = 0;
	};

	struct ImagePoint {
		ImagePoint(int x, int y, int i) {
			this->x = x;
			this->y = y;
			this->i = i;
		}
		int x;
		int y;
		int i;
	};

	struct BoundingBox { // BoundingBoxes are always inclusive.
		int left = 0;
		int right = 0;
		double top = 0;
		int bottom = 0;

		BoundingBox* clone() {
			BoundingBox* box = new BoundingBox();

			box->left = left;
			box->right = right;
			box->top = top;
			box->bottom = bottom;

			return box;
		}
	};

	struct Segment {
		int segmentID = 0;

		int totalPixels = 0;
		double totalDistance = 0;

		double averageDistance = 0;

		BoundingBox* box;
		std::vector<ImagePoint>* imagePointList;
		std::vector<ImagePoint>* scaledPointList;

		bool merged = false;

		double segScore = 0;

	};

	struct PConfig {

		BoundingBox* box;

		Image_Store* image;
		Image_Store* scaled;

		std::vector<Segment*>* segments;
		uint8_t* previousValues;
		uint8_t topLeft = 0; // Only Used in N8 Algorithms

		Color* color;
		double threshold;

		double scaleFactor;
	};

	struct FutureMerge {
		FutureMerge(int ID1, int ID2) {
			this->ID1 = ID1;
			this->ID2 = ID2;
		}
		int ID1;
		int ID2;
	};

	// Constructors and Destructors
	VisionImage();
	VisionImage(Image_Store* image, double scaleFactor);
	VisionImage(cv::Mat* mat, double scaleFactor);
	~VisionImage();

	void create(double scaleFactor);


	// Images
	Image_Store* image;
	cv::Mat* mat;

	Image_Store* scaled_image;
	cv::Mat* scaled_mat;

	
	// GENERAL
		// General Variables
	double scaleFactor;
	double sideScaleFactor;
	double effSF;
	Color* targetColor;
	double threshold;
	ExecutorService* executorService;
	BoundingBox* interestBox;
	int maximumSegments;
	std::vector<Segment*>* segments;
	double pixelSegScoreFactor;
	PConfig* pConfig;


	// General Image Processing Functions
	void scaleImage();
	void scaleImage(BoundingBox* interestBox, Image_Store* image, Image_Store* scaled, double scaleFactor);

	void thresholdImage();
	void thresholdImage(BoundingBox* interestBox, Image_Store* image, Image_Store* thresholded, double threshold, double scaleFactor);

	int n4Segmentation();
	int n8Segmentation();


	// General Helper Functions
	void prepareScaledImage();
	void prepareScaledImage(Image_Store* image, Image_Store* scaled);

	bool checkBoundingBoxOverlap(BoundingBox* b1, BoundingBox* b2);
	BoundingBox* combineBoundingBoxes(BoundingBox* b1, BoundingBox* b2);

	void configureImages(int width, int height, double scaleFactor);

	void prepareSegmentation(std::vector<Segment*>* segments);
	void prepareSegmentation(std::vector<Segment*>* segments, uint8_t* previousValues, int length);
	void setSegmentIDs(std::vector<Segment*>* segments, int newID, int oldID);
	void setSegmentIDs(std::vector<FutureMerge*>* futureMerges, int newID, int oldID);
	void initializeSegment(Segment* segment, int segmentID, int x, int y);
	void setFinalSegmentValues(std::vector<Segment*>* segments); // This one and the next one really annoy me.
	void mergeSegments(std::vector<Segment*>* segments); 
	void trimSegments(std::vector<Segment*>* segments);

	double defaultScoreSegment(Segment* segment);
	void scoreSegments();
	void scoreSegments(std::vector<Segment*>* segments, std::function<double(Segment*)>* scoringMethod);
	std::vector<Segment*>* getBestSegments(int num);
	std::vector<Segment*>* getBestSegments(std::vector<Segment*>* segments, int num);
	Segment* getBestSegment(std::vector<Segment*>* segments);
	void colorSegment(Image_Store* image, std::vector<ImagePoint>* points, Color* color);

	void mergeIntersectingBoundingBoxes();
	void mergeIntersectingBoundingBoxes(std::vector<Segment*>* segments);

	Image_Store* getImage_Store(cv::Mat* mat);
	cv::Mat* getMat(Image_Store* image);

	void setInterestBox(BoundingBox* interestBox);

	std::vector<PConfig*>* threadConfigs;
	void initializePConfigs(int num, std::vector<PConfig*>* threadConfigs, PConfig* model);
	void setBoundingBoxes(BoundingBox* interestBox, std::vector<PConfig*>* semgents);

	// PROCESS ALGOTITHMS
	// "Process" encompases the following steps: scaling, distance, thresholding, segmentation
	int n4Process();
	int n4ProcessC(PConfig* pConfig);
	int n4Process(BoundingBox* interestBox, Image_Store* image, Image_Store* scaled, std::vector<Segment*>* segments, uint8_t* previousValues, Color* targetColor, double threshold, double scaleFactor);

	int n4MultithreadProcess();
	int n4MultithreadProcess(BoundingBox* interestBox, std::vector<PConfig*>* threadConfigs, PConfig* model, std::vector<Segment*>* segments);

	int n4ProcessC_SubPro(PConfig* pConfig);
	int n4Process_SubPro(BoundingBox* interestBox, Image_Store* image, Image_Store* scaled, std::vector<Segment*>* segments, uint8_t* previousValues, Color* targetColor, double threshold, double scaleFactor);

	int n8Process();
	int n8ProcessC(PConfig* pConfig);
	int n8Process(BoundingBox* interestBox, Image_Store* image, Image_Store* scaled, std::vector<Segment*>* segments, uint8_t* previousValues, Color* targetColor, double threshold, double scaleFactor);

	int n8MultithreadProcess();
	int n8MultithreadProcess(BoundingBox* interestBox, std::vector<PConfig*>* threadConfigs, PConfig* model, std::vector<Segment*>* segments);
	
	int n8ProcessC_SubPro(PConfig* pConfig);
	int n8Process_SubPro(BoundingBox* interestBox, Image_Store* image, Image_Store* scaled, std::vector<Segment*>* segments, uint8_t* previousValues, Color* targetColor, double threshold, double scaleFactor);

};

#endif
