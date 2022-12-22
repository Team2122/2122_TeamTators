#include "CLUT_Table.h"
#include <math.h>
#include <stdio.h>
#include <cassert>

#define PI 3.14159265

// constants
const double sRGB_matrix[3][3] = {
	0.4124, 0.3576, 0.1805,
	0.2126, 0.7152, 0.0722,
	0.0193, 0.1192, 0.9505
};

const double sRGB_gamma = 2.2;

// Define local function

double vision_range(double inval, double LB, double UB)
{
	inval = (inval < LB ? LB : inval);
	return inval > UB ? UB : inval;
}


// Member functions definitions including constructor and destructor;


CLUT_Table::~CLUT_Table(void)
{
	if (Thresh_LUT_data != NULL)
	{
		delete[] Thresh_LUT_data;
		Thresh_LUT_data = NULL;
	}

	if (Dist_LUT_data != NULL)
	{
		delete[] Dist_LUT_data;
		Dist_LUT_data = NULL;
	}

	if (RGB_data != NULL)
	{
		delete[] RGB_data;
		RGB_data = NULL;
	}

}

// Overloaded Constructor

CLUT_Table::CLUT_Table( int LUT_nodes, PipelineConfig* pipelineConfig)
{
	this->set_nodes(LUT_nodes);
	this->pipelineConfig = pipelineConfig;
}


CLUT_Table::CLUT_Table(int LUT_nodes, PipelineConfig* pipelineConfig, int min_RGB, int norm_range )
{
	// Pass in the pipeline configuration class information 
	this->pipelineConfig = pipelineConfig;

	this->CLUT_working_space = CLUT_WORKING_SPACE::CIELAB;
	
	this->min_RGB = min_RGB;
	this->norm_range = norm_range;

	this->xng = pow(X_ref, 0.3333);
	this->yng = pow(Y_ref, 0.3333);
	this->zng = pow(Z_ref, 0.3333);

	// Determine whether or not Dynamic Scaling should be enabled / disabled
	this->dynamic_scaling = pipelineConfig->getDynamicRGBScaling();

	double lowRGB, highRGB, lowL, highL, lowC, highC, lowH, highH;
	pipelineConfig->getRGB_ClippingLUT(&lowRGB, &highRGB);
	pipelineConfig->getL_ClippingLUT(&lowL, &highL);
	pipelineConfig->getChroma_ClippingLUT(&lowC, &highC);
	pipelineConfig->getHue_ClippingLUT(&lowH, &highH);

	// Initialize the clipping LUTs
	build_Scaling_LUT(RGB_clipping_LUT, lowRGB, highRGB, 255, LUT_1D_LEVELS);
	build_Clipping_LUT(Lightness_LUT, lowL, highL, 100, LUT_1D_LEVELS);
	build_Clipping_LUT(chroma_LUT, lowC, highC, 128, LUT_1D_LEVELS);
	build_Clipping_LUT(hue_LUT, lowH, highH, 360, LUT_HUE_LEVELS);

	this->set_nodes(LUT_nodes);
	this->initalize();
	this->processLabLUT();

	//fclose(fileout);
}


CLUT_Table::CLUT_Table(int LUT_nodes, PipelineConfig* pipelineConfig, int min_RGB, int norm_range, uint8 ref_R, uint8 ref_G, uint8 ref_B )
{
	this->CLUT_working_space = CLUT_WORKING_SPACE::RGB;
	this->min_RGB = min_RGB;
	this->norm_range = norm_range;

	this->ref_R = ref_R;
	this->ref_G = ref_G;
	this->ref_B = ref_B;

	// Initialize the clipping LUTs
	build_Scaling_LUT(RGB_clipping_LUT, 0, 255, 255, LUT_1D_LEVELS);

	this->set_nodes(LUT_nodes);
	this->initalize();
	this->processRGBLUT();
}

void CLUT_Table::set_nodes(int num_nodes)
{
	LUT_nodes = num_nodes;
	LUT_nodes_sq = LUT_nodes * LUT_nodes;
	node_offset = (int)round(255.0 / (LUT_nodes * 2.0));
}

void CLUT_Table::update_MinMaxRange(int min_RGB, int norm_range)
{
	this->min_RGB = min_RGB;
	this->norm_range = norm_range;
}


void CLUT_Table::prepare_CLUT_Lab(uint8* RGB8_in, uint8* Thresh_LUT_data, uint8* Dist_LUT_data)
{
	// The LUT RGB[] is in the range [0,LUT_Nodes-1], so needs to be scaled first

	double RGB[3] = { 0 };
	double XYZ[3] = { 0 };
	double Lab[3] = { 0 };

	RGB[0] = RGB8_in[0] / (double) (LUT_nodes - 1);
	RGB[1] = RGB8_in[1] / (double) (LUT_nodes - 1);
	RGB[2] = RGB8_in[2] / (double) (LUT_nodes - 1);
	
	this->maxDiff = sqrt((this->pipelineConfig->getTargetL() * this->pipelineConfig->getTargetL()) +
						 (this->pipelineConfig->getTargetA() * this->pipelineConfig->getTargetA()) +
						 (this->pipelineConfig->getTargetB() * this->pipelineConfig->getTargetB()));

	convert_RGB_pixel_to_CIELab( RGB, XYZ, Lab, Dist_LUT_data, Thresh_LUT_data);

}


void CLUT_Table::convert_RGB_pixel_to_CIELab(double* RGB, double* XYZ, double* Lab, uint8* distance, uint8* thresholded)
{
	double LCh[3] = { 0 };

	// Values are assumed to already be in the 0-1 range
	double rp = pow(RGB[0], sRGB_gamma);
	double gp = pow(RGB[1], sRGB_gamma);
	double bp = pow(RGB[2], sRGB_gamma);

	XYZ[0] = sRGB_matrix[0][0] * rp + sRGB_matrix[0][1] * gp + sRGB_matrix[0][2] * bp;
	XYZ[1] = sRGB_matrix[1][0] * rp + sRGB_matrix[1][1] * gp + sRGB_matrix[1][2] * bp;
	XYZ[2] = sRGB_matrix[2][0] * rp + sRGB_matrix[2][1] * gp + sRGB_matrix[2][2] * bp;

	double tmpy = pow(XYZ[1], 0.3333) / yng;
	Lab[0] = 116 * tmpy - 16;
	Lab[1] = 500 * (pow(XYZ[0], 0.3333) / xng - tmpy);
	Lab[2] = 200 * (tmpy - pow(XYZ[2], 0.3333) / zng);

	// Make sure the value does not go negative
	Lab[0] = vision_range(Lab[0], 0.0, 100.0);

	// Calculate LCH Values
	convert_Lab_to_LCh( Lab, LCh );

	// This is where we want to apply the CLIPPING LUTs
	bool l_clip = Lightness_LUT[(uint8)(Lab[0] * 2.55)];
	bool c_clip = chroma_LUT[(uint8)LCh[1]*2];
	bool h_clip = hue_LUT[(uint8)LCh[2]];

	if (!(l_clip && c_clip && h_clip)) {
		Lab[0] = 0; Lab[1] = 0; Lab[2] = 0;
	}

	// Skip if the Distance pointer is NULL
	if (distance != NULL) 
	{
		double dL, dA, dB, distance_px_val = 1;

		if (this->pipelineConfig->getNumTargetColors() == 1)
		{
			dL = Lab[0] - this->pipelineConfig->getTargetL();
			dA = Lab[1] - this->pipelineConfig->getTargetA();
			dB = Lab[2] - this->pipelineConfig->getTargetB();
			distance_px_val = sqrt(dL * dL + dA * dA + dB * dB) / maxDiff;
		}
		else
		{
			for ( int i = 0; i < this->pipelineConfig->getNumTargetColors(); i++ )
			{
				dL = Lab[0] - this->pipelineConfig->getTargetL( i );
				dA = Lab[1] - this->pipelineConfig->getTargetA( i );
				dB = Lab[2] - this->pipelineConfig->getTargetB( i );

				distance_px_val = min( distance_px_val, sqrt(dL * dL + dA * dA + dB * dB) / maxDiff);
			}
		}

		//Compute Distance Estimate 
		distance_px_val = vision_range(1.0 - distance_px_val, 0.0, 1.0);

		distance[0] = (uint8)(255 * distance_px_val);

		// Compute the thresholded value
		double threshold_px_val = 1 + ( distance_px_val - this->pipelineConfig->getThreshold() ) ;
		threshold_px_val = (int) threshold_px_val;
		thresholded[0] = (uint8)(255 * threshold_px_val);
	}

}


void CLUT_Table::convert_Lab_to_LCh( double* Lab, double* LCh )
{

	// Copy over the L* value
	LCh[0] = Lab[0];

	// Calculate the Chroma
	LCh[1] = pow(Lab[1] * Lab[1] + Lab[2] * Lab[2], 0.5);

	// Make sure that Chroma is in range ( -127.5 to 127.5 )
	LCh[1] = vision_range( LCh[1], 0.0, 127.5 );

	// Calculate the Hue
	LCh[2] = atan2(Lab[2], Lab[1]) * 180 / PI;

	// Correct Hue for periodicity
	if (LCh[2] < 0)
		LCh[2] += 360;

}


void CLUT_Table::prepare_CLUT_RGB( uint8* RGB, uint8* Thresh_LUT_data, uint8* Dist_LUT_data)
{
	// The LUT RGB[] is in the range [0,LUT_Nodes-1], so needs to be scaled first

	uint8 rp = 255 * ( RGB[0] / (LUT_nodes - 1) ) ;
	uint8 gp = 255 * ( RGB[1] / (LUT_nodes - 1) ) ;
	uint8 bp = 255 * ( RGB[2] / (LUT_nodes - 1) ) ;

	// RGB Data is assumed to have been pre-normalized
	double distance_R = (float) rp - (float) ref_R;
	double distance_G = (float) gp - (float) ref_G;
	double distance_B = (float) bp - (float) ref_B;

	double max_Diff = sqrt( (ref_R * ref_R) + (ref_G * ref_G) + (ref_B * ref_B) );
	double RGB_distance = sqrt( (distance_R * distance_R) + (distance_G * distance_G) + (distance_B * distance_B) ) / max_Diff ;

	double distance_px_val = vision_range(1.0 - RGB_distance, 0.0, 1.0);
	Dist_LUT_data[0] = (uint8)(255 * distance_px_val);

	double threshold_px_val = vision_range((255 * distance_px_val - 100) / 50.0, 0, 1);
	threshold_px_val = floor( (threshold_px_val + (1 - this->pipelineConfig->getThreshold())));

	Thresh_LUT_data[0] = (uint8)(255 * threshold_px_val);

}

void CLUT_Table::color_process_rgb_pixel( uint8_t* RGB_in, double* Lab, double* LCh)
{

	// Set up temporary arrays to hold the results
	Image_Store* RGB_img = new Image_Store(1, 1, 3, 255, PIXEL_FORMAT::RGB, INTERLEAVE_FORMAT::PIXEL_INTERLEAVED);
	RGB_img->allocate_memory();

	Image_Store* lab_img = new Image_Store(1, 1, 3, 255, PIXEL_FORMAT::RGB, INTERLEAVE_FORMAT::PIXEL_INTERLEAVED);
	lab_img->allocate_memory();

	// Image Dimensions
	RGB_img->image[0] = RGB_in[0];
	RGB_img->image[1] = RGB_in[1];
	RGB_img->image[2] = RGB_in[2];

	// Run the image through the pipeline
	color_process_rgb_pixel(RGB_img, 0, 0, NULL, NULL, lab_img, NULL, NULL );

	Lab[0] = lab_img->image[0] / 2.55;
	Lab[1] = lab_img->image[1] - 128;
	Lab[2] = lab_img->image[2] - 128;

	// Grab the LCh values
	convert_Lab_to_LCh(Lab, LCh);

	// Clean up
	delete RGB_img;
	delete lab_img;

}


void CLUT_Table::color_process_rgb_pixel(Image_Store* scaled_in, int rgb_pixel_location, int gray_pixel_location,
										 Image_Store* debug_rgb_image, Image_Store* debug_xyz_image, Image_Store* debug_lab_image,
										 Image_Store* distance_out, Image_Store* thresholded_out)
{
	// Initialize Input data 
	double RGB[3] = { 0 };
	double XYZ[3] = { 0 };
	double Lab[3] = { 0 };
	uint8* distance = NULL ;
	uint8* thresholded = NULL ;

	// This is where we want to apply the RGB PRE LUT 1D
	RGB[0] = (double) scaled_in->image[rgb_pixel_location] ;
	RGB[1] = (double) scaled_in->image[rgb_pixel_location + 1] ;
	RGB[2] = (double) scaled_in->image[rgb_pixel_location + 2] ;

	if (distance_out != NULL) {
		distance = &distance_out->image[gray_pixel_location];
		thresholded = &thresholded_out->image[gray_pixel_location];
	}

	// Check to see if Dynamic Scaling has been enabled
	if (dynamic_scaling)
	{
		RGB[0] = (RGB[0] - min_RGB) / norm_range ;
		RGB[1] = (RGB[1] - min_RGB) / norm_range ;
		RGB[2] = (RGB[2] - min_RGB) / norm_range ;

		RGB[0] = vision_range(RGB[0], 0.0, 1.0) ;
		RGB[1] = vision_range(RGB[1], 0.0, 1.0) ;
		RGB[2] = vision_range(RGB[2], 0.0, 1.0) ;
	}
	else
	{
		// This is where we want to apply the RGB PRE LUT 1D
		RGB[0] = RGB_clipping_LUT[ (uint8) RGB[0] ] / 255.0 ;
		RGB[1] = RGB_clipping_LUT[ (uint8) RGB[1] ] / 255.0 ;
		RGB[2] = RGB_clipping_LUT[ (uint8) RGB[2] ] / 255.0 ;
	}

	if (debug_rgb_image != NULL)
	{
		debug_rgb_image->image[rgb_pixel_location + 0] = uint8(RGB[0] * 255);
		debug_rgb_image->image[rgb_pixel_location + 1] = uint8(RGB[1] * 255);
		debug_rgb_image->image[rgb_pixel_location + 2] = uint8(RGB[2] * 255);
	}

	convert_RGB_pixel_to_CIELab( RGB, XYZ, Lab, distance, thresholded);

	if (debug_xyz_image != NULL)
	{
		// Using 233 because D65 XYZ image
		debug_xyz_image->image[rgb_pixel_location + 0] = uint8(XYZ[0] * 233);
		debug_xyz_image->image[rgb_pixel_location + 1] = uint8(XYZ[1] * 233);
		debug_xyz_image->image[rgb_pixel_location + 2] = uint8(XYZ[2] * 233);
	}

	if (debug_lab_image != NULL)
	{
		double enc_L = ((Lab[0] * 2.55) <= 255) ? uint8(Lab[0] * 2.55) : 255;
		double enc_a = ((128 + Lab[1]) <= 255) ? uint8(128 + Lab[1]) : 255;
		double enc_b = ((128 + Lab[2]) <= 255) ? uint8(128 + Lab[2]) : 255;

		debug_lab_image->image[rgb_pixel_location + 0] = (int)enc_L;
		debug_lab_image->image[rgb_pixel_location + 1] = (int)((enc_a < 0) ? 0 : enc_a);
		debug_lab_image->image[rgb_pixel_location + 2] = (int)((enc_b < 0) ? 0 : enc_b);
	}

}


void CLUT_Table::color_process_rgb_pixel_LUT( Image_Store* scaled_in, int rgb_pixel_location, int gray_pixel_location,
										      Image_Store* distance_out, Image_Store* thresholded_out)
{
	// Initialize the values
	int R_val_norm, G_val_norm, B_val_norm;

	if (dynamic_scaling) 
	{
		double scalar = 255.0 / (double)norm_range;

		// output is in the range 0-255, node offset is the offset required for each node
		R_val_norm = (int) (scalar * (scaled_in->image[rgb_pixel_location] - min_RGB) + node_offset );
		G_val_norm = (int) (scalar * (scaled_in->image[rgb_pixel_location + 1] - min_RGB) + node_offset );
		B_val_norm = (int) (scalar * (scaled_in->image[rgb_pixel_location + 2] - min_RGB) + node_offset );
	}
	else
	{
		R_val_norm = (int) RGB_clipping_LUT[scaled_in->image[rgb_pixel_location]] + node_offset ;
		G_val_norm = (int) RGB_clipping_LUT[scaled_in->image[rgb_pixel_location + 1]] + node_offset ;
		B_val_norm = (int) RGB_clipping_LUT[scaled_in->image[rgb_pixel_location + 2]] + node_offset ;
	}

	// Calculate the closest LUT index
	int LUT_idx = (( R_val_norm >> 4 ) * LUT_nodes_sq ) +
				  (( G_val_norm >> 4 ) * LUT_nodes) +
				  ( B_val_norm >> 4 ) ; 

	// if (gray_pixel_location == (216 + (528 * 480))) {
		// printf("Break Here\n");
		// printf( "LUT_ix = %d, Value = %d\n", LUT_idx, LUT_data[LUT_idx] ) ;
	// }

	distance_out->image[gray_pixel_location] = Dist_LUT_data[LUT_idx];
	thresholded_out->image[gray_pixel_location] = Thresh_LUT_data[LUT_idx];

}


void CLUT_Table::rebuild_CLUT()
{
	this->initalize();
	this->updatePipelineParameters();

	if (this->CLUT_working_space == CLUT_WORKING_SPACE::CIELAB) {
		this->processLabLUT();
	}
	else {
		this->processRGBLUT();
	}
}


void CLUT_Table::initalize()
{
	// Check to see if cleanup is needed
	if (RGB_data != NULL) {
		delete[] RGB_data;
	}

	this->RGB_data = new uint8_t[3 * LUT_nodes * LUT_nodes * LUT_nodes];

	if (Dist_LUT_data != NULL) {
		delete[] Dist_LUT_data;
	}

	this->Dist_LUT_data = new uint8_t[1 * LUT_nodes * LUT_nodes * LUT_nodes];

	if (Thresh_LUT_data != NULL) {
		delete[] Thresh_LUT_data;
	}

	this->Thresh_LUT_data = new uint8_t[1 * LUT_nodes * LUT_nodes * LUT_nodes];


	// Start incrementing throught the 3D-LUT , building the nodes

	int counter = 0, RGB_counter = 0;

	for (int r_node = 0; r_node < (int)LUT_nodes; r_node++)
	{
		for (int g_node = 0; g_node < (int)LUT_nodes; g_node++)
		{
			for (int b_node = 0; b_node < (int)LUT_nodes; b_node++)
			{
				this->Dist_LUT_data[counter] = (uint8)0;		// This is the output array
				this->Thresh_LUT_data[counter] = (uint8)0;		// This is the output array
				this->RGB_data[RGB_counter] = (uint8)r_node;
				this->RGB_data[RGB_counter + 1] = (uint8)g_node;
				this->RGB_data[RGB_counter + 2] = (uint8)b_node;

				// Increment the counters
				counter++;
				RGB_counter += 3;
			}
		}
	}

}


void CLUT_Table::updatePipelineParameters()
{
	// Determine whether or not Dynamic Scaling should be enabled / disabled
	this->dynamic_scaling = pipelineConfig->getDynamicRGBScaling();

	// initialize local variables to use
	double lowRGB, highRGB, lowL, highL, lowC, highC, lowH, highH;

	// Pull the parameters
	pipelineConfig->getRGB_ClippingLUT(&lowRGB, &highRGB);
	pipelineConfig->getL_ClippingLUT(&lowL, &highL);
	pipelineConfig->getChroma_ClippingLUT(&lowC, &highC);
	pipelineConfig->getHue_ClippingLUT(&lowH, &highH);

	// Rebuild the clipping LUTs
	build_Scaling_LUT(RGB_clipping_LUT, lowRGB, highRGB, 255, LUT_1D_LEVELS);
	build_Clipping_LUT(Lightness_LUT, lowL, highL, 100, LUT_1D_LEVELS);
	build_Clipping_LUT(chroma_LUT, lowC, highC, 128, LUT_1D_LEVELS);
	build_Clipping_LUT(hue_LUT, lowH, highH, 360, LUT_HUE_LEVELS);
}


void CLUT_Table::processLabLUT()
{
	int counter = 0;
	int RGB_counter = 0;

	// FILE* fileout = fopen(".//test_CLUT.txt", "wb");

	for (int r_node = 0; r_node < (int)LUT_nodes; r_node++)
	{
		for (int g_node = 0; g_node < (int)LUT_nodes; g_node++)
		{
			for (int b_node = 0; b_node < (int)LUT_nodes; b_node++)
			{
				//if (counter == 341) {
				//	printf(" Break here \n");
				//}
							
				prepare_CLUT_Lab(&(this->RGB_data[RGB_counter]), &Thresh_LUT_data[counter], &Dist_LUT_data[counter]);

				// fprintf(fileout, "%d %d %d %d %d\n", this->RGB_data[RGB_counter], this->RGB_data[RGB_counter + 1], this->RGB_data[RGB_counter + 2], Dist_LUT_data[counter], Thresh_LUT_data[counter]);

				counter++;
				RGB_counter += 3;
			}
		}
	}

}


void CLUT_Table::processRGBLUT()
{
	int counter = 0;
	int RGB_counter = 0;

	//FILE* fileout = fopen( ".//test_CLUT_RGB.txt", "wb");

	for (int r_node = 0; r_node < (int)LUT_nodes; r_node++)
	{
		for (int g_node = 0; g_node < (int)LUT_nodes; g_node++)
		{
			for (int b_node = 0; b_node < (int)LUT_nodes; b_node++)
			{
				this->prepare_CLUT_RGB(&(this->RGB_data[RGB_counter]), &Thresh_LUT_data[counter], &Dist_LUT_data[counter]);

				//fprintf(fileout, "%d %d %d %d\n", this->RGB_data[RGB_counter], this->RGB_data[RGB_counter + 1], this->RGB_data[RGB_counter + 2], LUT_data[counter-1]);
				counter++;
				RGB_counter += 3;
			}
		}
	}
	
	//fclose(fileout);
}


// Scaling LUT
void CLUT_Table::build_Scaling_LUT(double* clipping_LUT, double ClipLow, double ClipHigh, double maxValue, int LUT_levels)
{
	int starting_value = (int) ( ClipLow );
	int ending_value = (int) ( ClipHigh );
	double gamma = 1.0;

	for (int i = 0; i < LUT_levels; i++) {

		if (i < starting_value) {
			clipping_LUT[i] = 0;
		}
		else if (i >= ending_value) {
			clipping_LUT[i] = maxValue;
		}
		else {
			clipping_LUT[i] = maxValue * pow( (i - starting_value) / (double) (ending_value - starting_value), gamma ) ;
		}
	}

}

// Clipping LUT
void CLUT_Table::build_Clipping_LUT(bool* clipping_LUT, double ClipLow, double ClipHigh, double maxValue, int LUT_levels)
{
	// Scale the LUT to use the full range
	int starting_value = (int) ( LUT_levels * ClipLow / maxValue ) ;
	int ending_value = (int) ( LUT_levels * ClipHigh / maxValue ) ;
	
	if (ClipHigh > LUT_levels)
		assert("Cannot modify more levels that allowed in LUT\n");

	for (int i = 0; i < LUT_levels; i++) {

		if (i < starting_value) {
			clipping_LUT[i] = 0;
		}
		else if (i >= ending_value) {
			clipping_LUT[i] = 0;
		}
		else {
			clipping_LUT[i] = 1;
		}
	}

}

