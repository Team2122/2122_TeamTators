#pragma once

#ifndef _CONN_COMPONENTS_
#define _CONN_COMPONENTS_

#include "ImageStore.hpp"
#include "Vision_Pipeline.hpp"
#include <iostream>
#include "LUT_1D.h"

typedef unsigned int uint;

using std::string;

enum class N_TYPE {
	UNDEFINED,
	N4,
	N8
};

enum class SEGMENTATION_METHOD {
	MARK,
	JACOB
};

struct segment_data
{
	uint16_t segment_ID = 0;
	double average_brightness = 0;
	double number_elements = 0;
	double average_Lab[3] = { 0 };
	uint16_t bounding_box[4] = { 0 };

};

class ConnComponents
{
	public:

		~ConnComponents();  // This is the default destructor
		ConnComponents( Vision_Pipeline* vision_pipeline );
		ConnComponents( Vision_Pipeline* vision_pipeline, N_TYPE connectivity, SEGMENTATION_METHOD method, TARGET_DETECTION_TYPE mode);
		
		void setConnectivity(N_TYPE connectivity);
		int buildSegmentionMap( );


	private:
		
		Vision_Pipeline* vision_pipeline;
		TARGET_DETECTION_TYPE segmentation_mode;
		SEGMENTATION_METHOD method;
		N_TYPE connectivity;
		segment_data* segment_stats = NULL;
		uint boundary_region[4] = { 0 };

		// Connected Components Algorithms
		int ccomp_Jacob( TARGET_DETECTION_TYPE segmentation_mode);
		int ccomp_Mark( TARGET_DETECTION_TYPE segmentation_mode);

		// Map image though the current LUT
		int apply_image_LUT( LUT_1D* lookup );

		// Extract the stats for each of the connected components 
		int extract_label_stats( int maxIndex );

		// Extract location of targets
		int extract_target_location( int counter);
		int extract_ball_location( int counter, int* numSegments );

		int trace_CC_Jacob( LUT_1D* table, int index );
		int trace_CC_Mark( LUT_1D* remapArrayList, int label_in );

		// Helper Functions
		int remap_pixels_N4(int TM_pixval, int ML_pixval, uint* rmapArray );
		int remap_pixels_N8(int TL_pixval, int TM_pixval, int TR_pixval, int ML_pixval, uint* rmapArray );
		int remap_pixels_array( uint* rmapArrayN4, LUT_1D* remapArrayList, int ccomp_label, uint8* current_pixel, N_TYPE connectivity);
		int remap_pixels_subfn( LUT_1D* remapArrayList, int ccomp_label, uint value1, uint value2, uint8* current_pixel);

		void updateboundaryRegion( int loop_row, int loop_col);
		void display_LUT( uint* x_array, uint* y_array, const char* desc, int length );

};

#endif


