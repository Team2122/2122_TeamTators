#pragma once

#ifndef _CLUT_TABLE_
#define _CLUT_TABLE_

#include "ImageStore.hpp"

#ifdef _MSC_VER
#include <vcruntime_string.h>
#endif

typedef unsigned char uint8;

class CLUT_Table
{

public :

	CLUT_Table( int nodes );
	CLUT_Table( int LUT_nodes, int min_RGB, int norm_range, uint8 ref_R, uint8 ref_G, uint8 ref_B, double threshold );		// RGB LUT
	CLUT_Table( int LUT_nodes, int min_RGB, int norm_range, double L_ref, double A_ref, double B_ref, double threshold );	// CIELAB LUT
	~CLUT_Table();  // This is the destructor

	void update_MinMaxRange( int min_RGB, int norm_range );

	void setReferenceLAB( double L_ref, double A_ref, double B_ref, double threshold);
	void setReferenceRGB( uint8 ref_R, uint8 ref_G, uint8 ref_B, double threshold );

	void rebuildCLUT();

	void color_process_rgb_pixel(Image_Store* scaled_in, int rgb_pixel_location, int gray_pixel_location,
								 Image_Store* debug_rgb_image, Image_Store* debug_xyz_image, Image_Store* debug_lab_image,
							 	 Image_Store* distance_out, Image_Store* thresholded_out);

	void color_process_rgb_pixel_LUT(Image_Store* scaled_in, int rgb_pixel_location, int gray_pixel_location,
									 Image_Store* distance_out, Image_Store* thresholded_out);

	enum class CLUT_WORKING_SPACE {
		RGB,
		CIELAB
	};

	CLUT_WORKING_SPACE CLUT_working_space;


private :

	int    LUT_nodes = 0;
	int	   LUT_nodes_sq = 0;
	int	   node_offset = 0;

	int    min_RGB;
	int    norm_range;
	
	// Default Value for sRGB White point
	double X_ref = 0.9505;
	double Y_ref = 1.0;
	double Z_ref = 1.089;

	double xng ;
	double yng ;
	double zng ;

	double L_ref; 
	double A_ref;
	double B_ref;

	double maxDiff;
	double threshold;

	uint8 ref_R;
	uint8 ref_G;
	uint8 ref_B;

	uint8* RGB_data = NULL;
	uint8* Thresh_LUT_data = NULL;
	uint8* Dist_LUT_data = NULL;

	void set_nodes(int num_nodes);

	void prepare_CLUT_Lab( uint8* RGB, uint8* Thresh_LUT_data, uint8* Dist_LUT_data );
	void prepare_CLUT_RGB( uint8* RGB, uint8* Thresh_LUT_data, uint8* Dist_LUT_data );

	void initalize();

	void processLabLUT();
	void processRGBLUT();

};

#endif