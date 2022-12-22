#pragma once

#ifndef _VISION_MODEL_
#define _VISION_MODEL_

	#include "ExecutorService.hpp"
	#include "CLUT_Table.h"
	#include "Vision_Pipeline.hpp"
	#include "TargetLocation.hpp"
	#include <vector>

	class Vision_Pipeline;
	struct ObjectLocations;

	enum class CAMERA_IN_USE {
		UNDEFINED = 0,
		TURRETCAM,
		BALLCAM,
	};

	enum class TARGET_DETECTION_TYPE {
		UNDEFINED = 0,
		HEXAGON,
		BALL,
		HUB,
	};

	enum class SENSOR_ORIENTATION {
		UNDEFINED = 0,
		PORTRAIT,
		LANDSCAPE,
		PORTRAIT_UD,
		LANDSCAPE_UD,
	};

	enum class GSAUTO {
		// don't touch!
		BLUEA = 0,
		BLUEB = 1,
		REDA = 2,
		REDB = 3,
		UNKNOWN = 4
	};
	// TODO maybe instead of an enum use Strings?
	enum class OBJECTCOLOR {
		UNDEFINED = 0,
		RED,
		BLUE,
		YELLOW, 
		GREEN
	};

	struct BoundingBox {
		int left = 0;
		int right = 0;
		int top = 0;
		int bottom = 0;
	};

	struct Location {
		int x = 0, y = 0;
	};

	struct Vector_Loc {
		int x = 0, y = 0; // Format: L - R
		Location* l;
		Location* r;
	};

	void getObjectColorString(OBJECTCOLOR OBJECTCOLOR, char* OBJECTCOLOR_string);

	int process_pixels(Vision_Pipeline* vision_pipeline);

	void process_pixels_multi_thread( Vision_Pipeline* vision_pipeline, Image_Store* debug_rgb_image, Image_Store* debug_xyz_image,
							 		  Image_Store* debug_lab_image);

	int run_idle_process(Image_Store* input_image, Vision_Pipeline* vision_pipeline);

	// BB-T8R Code for Target and Ball Detection
	int run_target_process(Image_Store* input_image, Vision_Pipeline* vision_pipeline);
    int run_ground_process(Image_Store* input_image, Vision_Pipeline* vision_pipeline );

	// More Generic code for TransporTator
	int run_object_detection_process(Image_Store* input_image, Vision_Pipeline* vision_pipeline);

	double getDiff(Vector_Loc* v1, Vector_Loc* v2);

	const char* get_gs_auto_key(GSAUTO);
	GSAUTO newPickAuto2(ObjectLocations* object_location, double scale_factor);
	
	//GSAUTO pickAuto(ObjectLocations* object_location);
	//GSAUTO newPickAuto(ObjectLocations* object_location, double scale_factor);
	//void setAuto(GSAUTO gsauto);
	//void setVector_Loc();

#endif