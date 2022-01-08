#pragma once

#ifndef _VISION_PIPELINE_
#define _VISION_PIPELINE_

#include "ImageStore.hpp"
#include "Vision_Model.hpp"
#include "TargetLocation.hpp"
#include "PipelineConfig.hpp"

#ifdef _MSC_VER
	#include <vcruntime_string.h>
#endif


// Temporary declarations because they are not being loaded from the headers correctly
typedef unsigned char uint8 ;
struct BoundingBox_VM ;
struct Location;

struct BallLocations {
	Location* location;
	int		  location_length = 0;
	int		  balls_found = 0;
};

enum class TARGET_DETECTION_TYPE;	// Forward Declaration
enum class SENSOR_ORIENTATION;		// Forward Declaration

const int MAX_BOUNDING_BOXES = 10;

class Vision_Pipeline
{

public :

    Image_Store* scaled_image = NULL;
	Image_Store* thresholded_out = NULL ;
	Image_Store* distance_out = NULL;
	Image_Store* segmented_imgout = NULL;
	Image_Store* annotated_image = NULL;
	Image_Store* driver_station = NULL;

	ExecutorService* exec_service = NULL;
	CLUT_Table* CLUT_table = NULL;

	BoundingBox_VM* target_bounding_box = NULL;
	TargetLocation* target_LOC = NULL;

	BallLocations ball_locations ;
	BoundingBox_VM* ball_bounding_boxes = NULL;

	PipelineConfig* pipeline_config = NULL;

	// Local Attributes that must be updated with a Push
	double timing_scale = 0;
	double timing_process = 0;
	double timing_segment = 0;
	double timing_detect = 0;
	double timing_morphology = 0;

	// Constructor that initalizes the pipeline
	Vision_Pipeline( double scalefactor, int CLUT_Nodes, SENSOR_ORIENTATION orient, TARGET_DETECTION_TYPE target_mode );

	// This is the destructor
	~Vision_Pipeline();

	void setTestPath( char* test_path );
	void printTargetLocation();
	void printBallLocations();
	void enableExecutorService();

private:

	char test_path[FILENAME_MAX];

};

#endif
