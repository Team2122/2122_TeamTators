#pragma once

#ifndef _SCALE_IMAGE_
#define _SCALE_IMAGE_

#include "ImageStore.hpp"
#include "Vision_Pipeline.hpp"
#include <iostream>

enum class SCALING_METHOD {
	SUBSAMPLE,
	BLOCK_AVERAGE,
	LINEAR_INTERP
};

class ScaleImage
{
	public:
		int image_hist[256] = { 0 };

		~ScaleImage();  // This is the default destructor
		ScaleImage();   // This is the defualt constructor as a helper class
		ScaleImage( Vision_Pipeline* vision_pipeline );
		
		void scale( Image_Store* input_image, SCALING_METHOD method );

		int returnFirst(int* LUT_data, int points);
		int returnLast(int* LUT_data, int points);
		void display_LUT(int* array, const char* desc, int length);

	private:	

		Vision_Pipeline* vision_pipeline = NULL ;
		bool capture_debug_image = false;

		double crop_percent[4] = { 0 };

		int image_resize( Image_Store* image_data );
		int image_resize(Image_Store* image_data, Image_Store* scaled_image, double scale_factor, bool debug);

		int image_rotate( Image_Store* scaled_image, bool debug);

		bool doesPathExist( const std::string& s);	
};

#endif
