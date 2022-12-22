#pragma once

#ifndef _CLUT_TABLE_
#define _CLUT_TABLE_

#include "ImageStore.hpp"
#include "PipelineConfig.hpp"

#define LUT_1D_LEVELS 256
#define LUT_HUE_LEVELS 361

#ifdef _MSC_VER
#include <vcruntime_string.h>
#endif

typedef unsigned char uint8;

class CLUT_Table
{

public :

	CLUT_Table( int LUT_nodes, PipelineConfig* pipelineConfig );
	CLUT_Table( int LUT_nodes, PipelineConfig* pipelineConfig, int min_RGB, int norm_range );	// CIELAB LUT
	CLUT_Table( int LUT_nodes, PipelineConfig* pipelineConfig, int min_RGB, int norm_range, uint8 ref_R, uint8 ref_G, uint8 ref_B );		// RGB LUT

	~CLUT_Table();  // This is the destructor

	void update_MinMaxRange( int min_RGB, int norm_range );

	void rebuild_CLUT();

	void color_process_rgb_pixel(uint8_t* RGB_in, double* Lab, double* LCh );

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

	PipelineConfig* pipelineConfig = NULL;

	bool   dynamic_scaling = true;

	int    LUT_nodes = 0;
	int	   LUT_nodes_sq = 0;
	int	   node_offset = 0;

	int    min_RGB;
	int    norm_range;
	double maxDiff;

	// Default Value for sRGB White point
	double X_ref = 0.9505;
	double Y_ref = 1.0;
	double Z_ref = 1.089;

	double xng ;
	double yng ;
	double zng ;

	// Color Processing Parameters
	double RGB_clipping_LUT[LUT_1D_LEVELS];
	bool Lightness_LUT[LUT_1D_LEVELS];
	bool chroma_LUT[LUT_1D_LEVELS];
	bool hue_LUT[LUT_HUE_LEVELS];

	uint8 ref_R;
	uint8 ref_G;
	uint8 ref_B;

	uint8* RGB_data = NULL;
	uint8* Thresh_LUT_data = NULL;
	uint8* Dist_LUT_data = NULL;

	void set_nodes(int num_nodes);

	void initalize();

	void processLabLUT();
	void processRGBLUT();

	void prepare_CLUT_Lab(uint8* RGB, uint8* Thresh_LUT_data, uint8* Dist_LUT_data);
	void prepare_CLUT_RGB(uint8* RGB, uint8* Thresh_LUT_data, uint8* Dist_LUT_data);

	// Code to build Clipping LUTs
	void updatePipelineParameters();

	void build_Scaling_LUT(double* LUT, double low_value, double high_value, double maxValue, int num_elements);
	void build_Clipping_LUT(bool* LUT, double low_value, double high_value, double maxValue, int num_elements);

	void convert_Lab_to_LCh(double* Lab, double* LCh);
	void convert_RGB_pixel_to_CIELab(double* RGB, double* XYZ, double* Lab, uint8* distance, uint8* thresholded);

};

#endif