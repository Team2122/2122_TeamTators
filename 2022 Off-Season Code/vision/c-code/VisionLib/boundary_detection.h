#pragma once

#ifndef _BOUNDARY_DETECTION_
#define _BOUNDARY_DETECTION_

#include "ImageStore.hpp"
#include "Vision_Pipeline.hpp"
#include <iostream>

typedef unsigned int uint;

using std::string;

enum class BOUNDARY_METHOD {
	LEGACY,
	PARABOUND,
	DIVIDE_CONQUOR
};

class BoundaryDetection
{
	public:

		BoundaryDetection( Vision_Pipeline* vision_pipeline );
		BoundaryDetection( Vision_Pipeline* vision_pipeline, BOUNDARY_METHOD method );
		~BoundaryDetection();  // This is the default destructor

		void setMethod(BOUNDARY_METHOD method);
		int computeBoundary();

	private:

		Vision_Pipeline* vision_pipeline;
		BOUNDARY_METHOD boundary_method;

		int detect_boundary();
		int para_bound_detect();
		int divide_and_conquor();

		// Supporting routines
		int returnFirst(uint* LUT, uint length);
		int returnFirst(float* LUT, uint length);
		int returnLast(uint* LUT, uint length);
		int returnLast(float* LUT, uint length);
		int returnLUTMax(float* LUT, uint length);
		void normalizeLUT(float* LUT, uint length);

		int max_i(int a, int b);
		int min_i(int a, int b);
};

#endif // _BOUNDARY_DETECTION_
