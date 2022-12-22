#pragma once

// #include "apriltag.h"
#include "common/getopt.h"
#include "common/image_u8.h"
#include "common/pjpeg.h"
#include "common/zarray.h"
#include <opencv2\opencv.hpp>

typedef struct apriltag_family apriltag_family_t;
typedef struct apriltag_detector apriltag_detector_t;

enum class tagID {
	UNDEFINED = 0,
	TAG_36H11,
    TAG_25H9,
	TAG_16H5
};

class AprilTagPipeline
{
	public:

		AprilTagPipeline( tagID tagId );					// Default Constructor
		~AprilTagPipeline();								// Default Destructor
	
		int processFrame(cv::Mat* currentFrame, cv::Mat* gray) ;

	private:

		// Create the Family and detector
		apriltag_family_t* tf = NULL;
		apriltag_detector_t* td = NULL;
		zarray_t* detections = NULL;
		tagID mytagID = tagID::UNDEFINED ;
		
};

