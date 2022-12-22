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
struct BoundingBox ;
struct Location;

enum class TARGET_DETECTION_TYPE;	// Forward Declaration
enum class SENSOR_ORIENTATION;		// Forward Declaration
enum class OBJECTCOLOR;				// Forward Declaration

struct ObjectLocations {
	Location*		center_location = NULL;
	OBJECTCOLOR*		object_color = NULL;
	BoundingBox*	bounding_box = NULL;
	int				location_length = 0;
	int				objects_found = 0;
};

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

	BoundingBox* target_bounding_box = NULL;
	TargetLocation* target_LOC = NULL;

	ObjectLocations object_location;

	PipelineConfig* pipeline_config = NULL;

	// Local Attributes that must be updated with a Push
	double timing_scale = 0;
	double timing_process = 0;
	double timing_segment = 0;
	double timing_detect = 0;
	double timing_morphology = 0;
	double lastKnownScaleFactor = 0;
	double timing_total_processing = 0 ;

	// Constructor that initalizes the pipeline
	Vision_Pipeline( double scalefactor, int CLUT_Nodes, SENSOR_ORIENTATION orient, TARGET_DETECTION_TYPE target_mode );

	// This is the destructor
	~Vision_Pipeline();

	void setTestPath( char* test_path );
	void printTargetLocation();
	void printObjectLocations();

	void enableExecutorService();
	void disableExecutorService();
	void resetVisionPipelineScaleFactor();

private:

	char test_path[FILENAME_MAX];

};

#endif
