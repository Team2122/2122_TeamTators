#pragma once

#ifndef _EDDIES_CODE_
#define _EDDIES_CODE_


	#ifndef __linux__
		#include <filesystem>
		#include "Tester.hpp"
	#endif

	struct TargetColor {
		double r = 33.915;
		double g = 171.105;
		double b = 75.99;
	};

	struct Segment_VM {
		int ID;
		int total_pixels;
		double total_distance;
		double average_distance;
		bool stillUsing = true;
		BoundingBox box;

		double segScore = 0;
	};

	// Debug interface to enable/disable debugging
	void setVisionLibDebugMode( bool debug_status );
	void setVisionLibCaptureMode( bool capture_mode );
	void setVisionLibDebugStr(char* debug_str);
	void setVisionLibCaptureInc(int frame_inc);
	void setVisionLibTurretAngle(int angle);

	int getVisionLibTurretAngle();

	int OptimizedStartingProcess(Image_Store* image, double scale_factor, TargetColor color, std::vector<int>* pointList);
	//int OptimizedStartingProcessWrite(Image_Store* image, Image_Store* scaled, double scale_factor, TargetColor color, vector<int>* pointList);

	#ifndef __linux__
		int processRGBGreenTest(std::filesystem::path* path, int numberID);
		int TestProcess_OSPW(std::filesystem::path* path, int numberID);
		int TestProcess_OPW(std::filesystem::path* path, std::string*, int numberID);
		int TestProcess_TGT(Tester* tester, std::filesystem::path* path, std::string* string, int numberID);
	#endif

	int OptimizedProcessWrite(Image_Store* image, Image_Store* scaled, double scale_factor, TargetColor color, BoundingBox box, std::vector<int>* pointList);
	int NewOptimizedProcessWrite(Image_Store* image, Image_Store* scaled, double scale_factor, double threshold, TargetColor color, BoundingBox box, std::vector<int>* pointList);
	int NewOptimizedProcessWriteEnhancedSelection(Image_Store* image, Image_Store* scaled, double scale_factor, double threshold, TargetColor color, BoundingBox* box, std::vector<cv::Point>* pointList, TargetLocation* location, std::string* string);

	BoundingBox combineBoundingBoxes(BoundingBox b1, BoundingBox b2);
	bool checkBoundingBoxOverlap(BoundingBox b1, BoundingBox b2);
	cv::Point parseMarksFileLine(std::string* line);

	void loopTypeTest();

#endif