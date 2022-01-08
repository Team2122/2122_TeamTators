#include "CLUT_Table.h"
#include <math.h>
#include <stdio.h>


// constants
const double sRGB_matrix[3][3] = {
	0.4124, 0.3576, 0.1805,
	0.2126, 0.7152, 0.0722,
	0.0193, 0.1192, 0.9505
};

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

CLUT_Table::CLUT_Table( int LUT_nodes )
{
	this->set_nodes(LUT_nodes);
}


CLUT_Table::CLUT_Table(int LUT_nodes, int min_RGB, int norm_range, double L_ref, double A_ref, double B_ref, double threshold )
{
	this->CLUT_working_space = CLUT_WORKING_SPACE::CIELAB;
	this->min_RGB = min_RGB;
	this->norm_range = norm_range;

	this->xng = pow(X_ref, 0.3333);
	this->yng = pow(Y_ref, 0.3333);
	this->zng = pow(Z_ref, 0.3333);

	this->L_ref = L_ref;
	this->A_ref = A_ref;
	this->B_ref = B_ref;

	this->maxDiff = sqrt(L_ref* L_ref + A_ref*A_ref + B_ref*B_ref);
	this->threshold = threshold;

	this->set_nodes(LUT_nodes);
	this->initalize();
	this->processLabLUT();

	//fclose(fileout);
}


CLUT_Table::CLUT_Table(int LUT_nodes, int min_RGB, int norm_range, uint8 ref_R, uint8 ref_G, uint8 ref_B, double threshold )
{
	this->CLUT_working_space = CLUT_WORKING_SPACE::RGB;
	this->min_RGB = min_RGB;
	this->norm_range = norm_range;
	this->ref_R = ref_R;
	this->ref_G = ref_G;
	this->ref_B = ref_B;
	this->threshold = threshold;

	this->set_nodes(LUT_nodes);
	this->initalize();
	this->processRGBLUT();
}


void CLUT_Table::set_nodes(int num_nodes)
{
	LUT_nodes    = num_nodes;
	LUT_nodes_sq = LUT_nodes * LUT_nodes;
	node_offset  = (int) round( 255.0 / ( LUT_nodes * 2.0 ) );
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

	int counter = 0, RGB_counter = 0;

	for (int r_node = 0; r_node < (int) LUT_nodes; r_node++)
	{
		for (int g_node = 0; g_node < (int) LUT_nodes; g_node++)
		{
			for (int b_node = 0; b_node < (int) LUT_nodes; b_node++)
			{
				this->Dist_LUT_data[counter] = (uint8)0;		// This is the output array
				this->Thresh_LUT_data[counter] = (uint8)0;		// This is the output array
				this->RGB_data[RGB_counter]   = (uint8) r_node ;
				this->RGB_data[RGB_counter+1] = (uint8) g_node ;
				this->RGB_data[RGB_counter+2] = (uint8) b_node ;

				// Increment the counters
				counter++;
				RGB_counter += 3;

			}
		}
	}	
	
}

void CLUT_Table::update_MinMaxRange(int min_RGB, int norm_range)
{
	this->min_RGB = min_RGB;
	this->norm_range = norm_range;
}


void CLUT_Table::prepare_CLUT_Lab( uint8* RGB, uint8* Thresh_LUT_data, uint8* Dist_LUT_data )
{

	// RGB Data is assumed to have been pre-normalized
	float RGB_in[3];

	RGB_in[0] = (float)RGB[0] / (LUT_nodes - 1);
	RGB_in[1] = (float)RGB[1] / (LUT_nodes - 1);
	RGB_in[2] = (float)RGB[2] / (LUT_nodes - 1);

	double rp = pow(RGB_in[0], 2.2);
	double gp = pow(RGB_in[1], 2.2);
	double bp = pow(RGB_in[2], 2.2);

	double X = sRGB_matrix[0][0] * rp + sRGB_matrix[0][1] * gp + sRGB_matrix[0][2] * bp;
	double Y = sRGB_matrix[1][0] * rp + sRGB_matrix[1][1] * gp + sRGB_matrix[1][2] * bp;
	double Z = sRGB_matrix[2][0] * rp + sRGB_matrix[2][1] * gp + sRGB_matrix[2][2] * bp;

	double tmpy = pow(Y, 0.3333) / yng;
	double L = 116 * tmpy - 16;
	double A = 500 * (pow(X, 0.3333) / xng - tmpy);
	double B = 200 * (tmpy - pow(Z, 0.3333) / zng);

	// Make sure the value does not go negative
	L = vision_range(L, 0.0, 100.0);

	double dL = L - L_ref;
	double dA = A - A_ref;
	double dB = B - B_ref;

	double distance_px_val = sqrt(dL * dL + dA * dA + dB * dB) / maxDiff;
	distance_px_val = vision_range(1.0 - distance_px_val, 0.0, 1.0);
	Dist_LUT_data[0] = (uint8)(255 * distance_px_val);

	// Compute a thresholded value
	double threshold_px_val = vision_range((255 * distance_px_val - 100) / 50.0, 0, 1);
	threshold_px_val = (int)(threshold_px_val + (1 - threshold));
	Thresh_LUT_data[0] = (uint8)(255 * threshold_px_val);

}


void CLUT_Table::prepare_CLUT_RGB( uint8* RGB, uint8* Thresh_LUT_data, uint8* Dist_LUT_data)
{

	// RGB Data is assumed to have been pre-normalized
	double distance_R = (float)RGB[0] * (LUT_nodes - 1) - (float)ref_R;
	double distance_G = (float)RGB[1] * (LUT_nodes - 1) - (float)ref_G;
	double distance_B = (float)RGB[2] * (LUT_nodes - 1) - (float)ref_B;

	double max_Diff = sqrt( (ref_R * ref_R) + (ref_G * ref_G) + (ref_B * ref_B) );
	double RGB_distance = sqrt( (distance_R * distance_R) + (distance_G * distance_G) + (distance_B * distance_B) ) / max_Diff ;

	double distance_px_val = vision_range(1.0 - RGB_distance, 0.0, 1.0);
	Dist_LUT_data[0] = (uint8)(255 * distance_px_val);

	double threshold_px_val = vision_range((255 * distance_px_val - 100) / 50.0, 0, 1);
	threshold_px_val = (int)(threshold_px_val + (1 - threshold));
	Thresh_LUT_data[0] = (uint8)(255 * threshold_px_val);

}


void CLUT_Table::color_process_rgb_pixel(Image_Store* scaled_in, int rgb_pixel_location, int gray_pixel_location,
										 Image_Store* debug_rgb_image, Image_Store* debug_xyz_image, Image_Store* debug_lab_image,
										 Image_Store* distance_out, Image_Store* thresholded_out)
{

	double rp = pow(((double)scaled_in->image[rgb_pixel_location] - min_RGB) / norm_range, 2.2);
	double gp = pow(((double)scaled_in->image[rgb_pixel_location + 1] - min_RGB) / norm_range, 2.2);
	double bp = pow(((double)scaled_in->image[rgb_pixel_location + 2] - min_RGB) / norm_range, 2.2);

	if (debug_rgb_image != NULL)
	{
		debug_rgb_image->image[rgb_pixel_location + 0] = uint8(rp * 255);
		debug_rgb_image->image[rgb_pixel_location + 1] = uint8(gp * 255);
		debug_rgb_image->image[rgb_pixel_location + 2] = uint8(bp * 255);
	}

	double X = sRGB_matrix[0][0] * rp + sRGB_matrix[0][1] * gp + sRGB_matrix[0][2] * bp;
	double Y = sRGB_matrix[1][0] * rp + sRGB_matrix[1][1] * gp + sRGB_matrix[1][2] * bp;
	double Z = sRGB_matrix[2][0] * rp + sRGB_matrix[2][1] * gp + sRGB_matrix[2][2] * bp;

	if (debug_xyz_image != NULL)
	{
		// Using 233 because D65 XYZ image
		debug_xyz_image->image[rgb_pixel_location + 0] = uint8(X * 233);
		debug_xyz_image->image[rgb_pixel_location + 1] = uint8(Y * 233);
		debug_xyz_image->image[rgb_pixel_location + 2] = uint8(Z * 233);
	}

	double tmpy = pow(Y, 0.3333) / yng;
	double L = 116 * tmpy - 16;
	double A = 500 * (pow(X, 0.3333) / xng - tmpy);
	double B = 200 * (tmpy - pow(Z, 0.3333) / zng);

	// Make sure the value does not go negative
	L = vision_range(L, 0.0, 100.0);

	double dL = L - L_ref;
	double dA = A - A_ref;
	double dB = B - B_ref;

	if (debug_lab_image != NULL)
	{
		double enc_L = ((L * 2.55) <= 255) ? uint8(L * 2.55) : 255;
		double enc_a = ((128 + A) <= 255) ? uint8(128 + A) : 255;
		double enc_b = ((128 + B) <= 255) ? uint8(128 + B) : 255;

		debug_lab_image->image[rgb_pixel_location + 0] = (int)enc_L;
		debug_lab_image->image[rgb_pixel_location + 1] = (int)((enc_a < 0) ? 0 : enc_a);
		debug_lab_image->image[rgb_pixel_location + 2] = (int)((enc_b < 0) ? 0 : enc_b);
	}

	double distance_px_val = sqrt(dL * dL + dA * dA + dB * dB) / maxDiff;
	distance_px_val = vision_range(1.0 - distance_px_val, 0.0, 1.0);

	distance_out->image[gray_pixel_location] = (uint8)(255 * distance_px_val);

	// Compute a thresholded value
	double threshold_px_val = vision_range((255 * distance_px_val - 100) / 50.0, 0, 1);
	threshold_px_val = (int)(threshold_px_val + (1 - threshold));

	//double threshold_px_val = (int)(distance_px_val + (1 - threshold));
	thresholded_out->image[gray_pixel_location] = (uint8)(255 * threshold_px_val);

}


void CLUT_Table::color_process_rgb_pixel_LUT(Image_Store* scaled_in, int rgb_pixel_location, int gray_pixel_location,
										      Image_Store* distance_out, Image_Store* thresholded_out)
{
	double scalar = 255.0 / (double) norm_range;

	int R_val_norm = (int) ( scalar * ( scaled_in->image[rgb_pixel_location] - min_RGB) + node_offset );
	int G_val_norm = (int) ( scalar * ( scaled_in->image[rgb_pixel_location + 1] - min_RGB) + node_offset );
	int B_val_norm = (int) ( scalar * ( scaled_in->image[rgb_pixel_location + 2] - min_RGB) + node_offset );

	// Calculate the cl osest LUT index
	int LUT_idx = (( R_val_norm >> 4 ) * LUT_nodes_sq ) +
				  (( G_val_norm >> 4 ) * LUT_nodes) +
				  ( B_val_norm >> 4 ) ;

	// printf( "LUT_ix = %d, Value = %d\n", LUT_idx, LUT_data[LUT_idx] ) ;

	distance_out->image[gray_pixel_location] = Dist_LUT_data[LUT_idx];
	thresholded_out->image[gray_pixel_location] = Thresh_LUT_data[LUT_idx];

}


void CLUT_Table::rebuildCLUT()
{
	this->initalize();

	if (this->CLUT_working_space == CLUT_WORKING_SPACE::CIELAB) 
	{
		this->processLabLUT();
	}
	else 
	{
		this->processRGBLUT();
	}
}


void CLUT_Table::processLabLUT()
{
	int counter = 0;
	int RGB_counter = 0;

	//FILE* fileout = fopen(".//test_CLUT.txt", "wb");

	for (int r_node = 0; r_node < (int)LUT_nodes; r_node++)
	{
		for (int g_node = 0; g_node < (int)LUT_nodes; g_node++)
		{
			for (int b_node = 0; b_node < (int)LUT_nodes; b_node++)
			{
				prepare_CLUT_Lab(&(this->RGB_data[RGB_counter]), &Thresh_LUT_data[counter], &Dist_LUT_data[counter]);

				//fprintf(fileout, "%d %d %d %d\n", this->RGB_data[RGB_counter], this->RGB_data[RGB_counter + 1], this->RGB_data[RGB_counter + 2], LUT_data[counter - 1]);

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


void CLUT_Table::setReferenceLAB(double L_ref, double A_ref, double B_ref, double threshold)
{
	this->L_ref = L_ref;
	this->A_ref = A_ref;
	this->B_ref = B_ref;
	this->threshold = threshold;
}

void CLUT_Table::setReferenceRGB(uint8 ref_R, uint8 ref_G, uint8 ref_B, double threshold)
{
	this->ref_R = ref_R;
	this->ref_G = ref_G;
	this->ref_B = ref_B;
	this->threshold = threshold;
}
