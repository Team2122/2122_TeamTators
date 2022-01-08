#pragma once

#ifndef _VISION_MODEL_
#define _VISION_MODEL_

	#include "ExecutorService.hpp"
	#include "CLUT_Table.h"
	#include "Vision_Pipeline.hpp"
	#include "TargetLocation.hpp"
	#include <vector>

	class Vision_Pipeline;
	struct BallLocations;

	enum class CAMERA_IN_USE {
		TURRETCAM,
		BALLCAM,
		UNDEFINED = 0
	};

	enum class TARGET_DETECTION_TYPE {
		TARGET,
		BALL
	};

	enum class SENSOR_ORIENTATION {
		UNDEFINED,
		PORTRAIT,
		LANDSCAPE,
		PORTRAIT_UD,
		LANDSCAPE_UD
	};

	struct Location {
		int x = 0, y = 0;
	};

	struct Vector_Loc {
		int x = 0, y = 0; // Format: L - R
		Location* l;
		Location* r;
	};

	enum class GSAUTO {
		// don't touch!
		BLUEA = 0,
		BLUEB = 1,
		REDA = 2,
		REDB = 3,
		UNKNOWN = 4
	};

	struct BoundingBox_VM {
		int left = 0;
		int right = 0;
		int top = 0;
		int bottom = 0;
	};

	const char* get_gs_auto_key(GSAUTO);

	void get_center(BoundingBox_VM* box, Location* center);

	bool is_null_bb(BoundingBox_VM* box);

	int image_resize(Image_Store* image_data, Image_Store* scaled_image, double scale_factor, bool debug );
	int image_resize(Image_Store* image_data, Vision_Pipeline* vision_pipeline);

	//int detect_boundary(Image_Store* image_data, int x1, int y1, int x2, int y2, TargetLocation* location);
	//int para_bound_detect(Image_Store* image_data, int x1, int y1, int x2, int y2, TargetLocation* location);

	int detect_boundary(Vision_Pipeline* vision_pipeline);
	int para_bound_detect(Vision_Pipeline* vision_pipeline);
	int process_pixels(Vision_Pipeline* vision_pipeline);

	int extract_target_location( Vision_Pipeline* vision_pipeline, int counter, int* lookup );

	int extract_ball_location( Vision_Pipeline* vision_pipeline, int counter, int* lookup, int* numSegments);
	
	void process_pixels_multi_thread(Vision_Pipeline* vision_pipeline, Image_Store* debug_rgb_image, Image_Store* debug_xyz_image,
							 		 Image_Store* debug_lab_image);

	int run_idle_process(Image_Store* input_image, Vision_Pipeline* vision_pipeline);
	int run_target_process(Image_Store* input_image, Vision_Pipeline* vision_pipeline);
    int run_ground_process(Image_Store* input_image, Vision_Pipeline* vision_pipeline );

	GSAUTO pickAuto(BallLocations* ball_location);
	GSAUTO newPickAuto(BallLocations* ball_location, double scale_factor);
	GSAUTO newPickAuto2(BallLocations* ball_location, double scale_factor);

	void setAuto(GSAUTO gsauto);

	double getDiff(Vector_Loc* v1, Vector_Loc* v2);

	void setVector_Loc();

#endif