#pragma once

#ifndef _SEGMENT_FILTERING_
#define _SEGMENT_FILTERING_

#include "ImageStore.hpp"
#include "Vision_Pipeline.hpp"
#include <iostream>

struct segment_data
{
	uint16_t segment_ID = 0;
	double average_brightness = 0;
	double number_elements = 0;
	double average_Lab[3] = { 0 };
	double average_LCh[3] = { 0 };
	double average_RGB[3] = { 0 };
	double fullness = 0;
	double chroma = 0;
	double aspect_ratio = 0;
	double width = 0;
	double height = 0;
	double area_pct = 0;
	OBJECTCOLOR color = OBJECTCOLOR::UNDEFINED;
	uint16_t bounding_box[4] = { 0 };
};

class SegmentFiltering
{
	public:

		~SegmentFiltering( );  // This is the default destructor
		SegmentFiltering( Vision_Pipeline* vision_pipeline );
		SegmentFiltering( Vision_Pipeline* vision_pipeline, TARGET_DETECTION_TYPE mode );

		int extract_label_stats(int maxIndex);

		int extract_Hexagon_location( int maxIndex );
		int extract_MultiObject_location( int maxIndex, int* numSegments );

		int filterSegmentData( int unique_segments, uint* boundary_region );
		int selectObjects();


	private:

		Vision_Pipeline* vision_pipeline = NULL;
		TARGET_DETECTION_TYPE segmentation_mode;
		segment_data* segment_stats = NULL;
		uint boundary_region[4] = { 0 };

		int numberOfObjects = 0;

		void displaySegmentStats( int numSegments );

		int vision_max_i(int a, int b);
		int vision_min_i(int a, int b);

		double vision_max_d(double a, double b);
		double vision_min_d(double a, double b);

		bool doesPathExist(const std::string& s);

		void get_center(BoundingBox* box, Location* center);

};

#endif
