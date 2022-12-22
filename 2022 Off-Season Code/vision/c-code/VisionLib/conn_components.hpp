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


class ConnComponents
{
	public:

		int unique_segments = 0;
		uint boundary_region[4] = { 0 };

		~ConnComponents();  // This is the default destructor
		ConnComponents( Vision_Pipeline* vision_pipeline );
		ConnComponents( Vision_Pipeline* vision_pipeline, N_TYPE connectivity, SEGMENTATION_METHOD method );
		
		void setConnectivity(N_TYPE connectivity);
		int buildSegmentionMap( );


	private:
		
		Vision_Pipeline* vision_pipeline;
		SEGMENTATION_METHOD method;
		N_TYPE connectivity;

		// Connected Components Algorithms
		int ccomp_Jacob();
		int ccomp_Mark();

		// Map image though the current LUT
		int apply_image_LUT( LUT_1D* lookup );

		int trace_CC_Jacob( LUT_1D* table, int index );
		int trace_CC_Mark( LUT_1D* remapArrayList, int label_in );

		// Helper Functions
		int remap_pixels_N4(int TM_pixval, int ML_pixval, uint* rmapArray );
		int remap_pixels_N8(int TL_pixval, int TM_pixval, int TR_pixval, int ML_pixval, uint* rmapArray );
		int remap_pixels_array( uint* rmapArrayN4, LUT_1D* remapArrayList, int ccomp_label, uint8* current_pixel, N_TYPE connectivity);
		int remap_pixels_subfn( LUT_1D* remapArrayList, int ccomp_label, uint value1, uint value2, uint8* current_pixel);

		void updateboundaryRegion( int loop_row, int loop_col);
		void display_LUT( uint* x_array, uint* y_array, const char* desc, int length );

		int vision_max_i(int a, int b);
		int vision_min_i(int a, int b);

		double vision_max_d(double a, double b);
		double vision_min_d(double a, double b);

};

#endif


