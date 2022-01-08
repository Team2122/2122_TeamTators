
//
// Vision Model
//
// author:	Team Tators, 2021
//

// Include the relevant libraries

#include <math.h>
#include <stdlib.h>
#include <iostream>
#include "VisionLib.hpp"
#include <sys/stat.h>
#include "opencv2/opencv.hpp"
#include <thread>
#include "CLUT_Table.h"
#include <string>
#include "conn_components.hpp"
#include "boundary_detection.h"
#include "annotate.h"
#include "LUT_1D.h"

#ifdef _MSC_VER
	#include <vcruntime_string.h>
#endif

#include <stdio.h>
#include <vector>

using namespace std;

// pickAuto Function
bool unset = true;
Vector_Loc* blueA;
Vector_Loc* blueB;
Vector_Loc* redA;
Vector_Loc* redB;

bool doesPathExist(const std::string& s)
{
	struct stat buffer;
	return (stat(s.c_str(), &buffer) == 0);
}

void process_RGB_pixels_loop(   int pixel_offset, int num_pixels, CLUT_Table* CLUT_table, Image_Store* scaled_in,
								Image_Store* debug_rgb_image, Image_Store* debug_xyz_image, Image_Store* debug_lab_image,
								Image_Store* distance_out, Image_Store* thresholded_out, bool debug_mode );


const char* get_gs_auto_key(GSAUTO a) {
	switch (a) {
	case   GSAUTO::REDA: return "RA";
	case   GSAUTO::REDB: return "RB";
	case   GSAUTO::BLUEA: return "BA";
	case   GSAUTO::BLUEB: return "BB";
	case   GSAUTO::UNKNOWN: return "UK";
	}
	return "UK";
}

void setVector_Loc() {
	if (unset == true) {
		unset = false;

		blueA = new Vector_Loc;
		blueB = new Vector_Loc;
		redA = new Vector_Loc;
		redB = new Vector_Loc;
		
		// Format: L - R
		blueA->l->x = 167;
		blueA->l->y = 92;
		blueA->r->x = 236;
		blueA->r->y = 105;
		blueA->x = blueA->l->x - blueA->r->x;
		blueA->y = blueA->l->y - blueA->r->y;

		blueB->l->x = 121;
		blueB->l->y = 88;
		blueB->r->x = 221;
		blueB->r->y = 98;
		blueB->x = blueB->l->x - blueB->r->x;
		blueB->y = blueB->l->y - blueB->r->y;

		redA->l->x = 69;
		redA->l->y = 130;
		redA->r->x = 168;
		redA->r->y = 204;
		redA->x = redA->l->x - redA->r->x;
		redA->y = redA->l->y - redA->r->y;

		redB->l->x = 67;
		redB->l->y = 133;
		redB->r->x = 237;
		redB->r->y = 105;
		redB->x = redB->l->x - redB->r->x;
		redB->y = redB->l->y - redB->r->y;

	}
}

// Now Define the code

int run_idle_process(Image_Store* input_image, Vision_Pipeline* vision_pipeline)
{
	// Start the counter
	clock_t tStart = clock();
	double elapsed_time = 0;
	
	if (vision_pipeline->target_LOC == NULL)
	{
		// Make sure that the scalefactor has been set correctly
		vision_pipeline->target_LOC = new TargetLocation;
		vision_pipeline->target_LOC->setScalefactor(vision_pipeline->pipeline_config->getScaleFactor());
	}
	else
	{
		// Make sure that the scalefactor has been set correctly
		vision_pipeline->target_LOC->setScalefactor(vision_pipeline->pipeline_config->getScaleFactor());
	}

	// Resize the image and convert BGR to RGB if needed
	image_resize(input_image, vision_pipeline);

	elapsed_time = (double)(clock() - tStart) / CLOCKS_PER_SEC;
	vision_pipeline->timing_scale = 1000 * elapsed_time;
 
	return EXIT_SUCCESS;

}


int run_target_process( Image_Store* input_image, Vision_Pipeline* vision_pipeline )
{

	// Start the counter
	clock_t tStart = clock(); 
	clock_t tBegin = tStart;
	double elapsed_time = 0;

	// Make sure that the scalefactor has been set correctly
	vision_pipeline->target_LOC->setScalefactor(vision_pipeline->pipeline_config->getScaleFactor());

	// Resize the image and convert BGR to RGB if needed
	image_resize( input_image, vision_pipeline );

	elapsed_time = (double)(clock() - tStart) / CLOCKS_PER_SEC;
	vision_pipeline->timing_scale = 1000 * elapsed_time;

	// Perform the color conversion
	if (vision_pipeline->pipeline_config->getVisionLibDebugMode()) {
		printf("\tImage Resize ran in %0.2f ms\n", vision_pipeline->timing_scale);	
	}

	tStart = clock();

	if (1 == process_pixels(vision_pipeline)) {
		vision_pipeline->target_LOC->top_right_x = -1;
		return 0;
	}

	elapsed_time = (double)(clock() - tStart) / CLOCKS_PER_SEC;
	vision_pipeline->timing_process = 1000 * elapsed_time;

	// Run Morphology Algorithm
	if (vision_pipeline->pipeline_config->getVisionLibDebugMode()) {
		printf("\tProcess Pixels ran in %0.2f ms\n", vision_pipeline->timing_process );
	}

	tStart = clock();

	if (true) 
	{
		// Define the structuring element
		int structureElement[9] = { 0, 1, 0, 1, 1, 1, 0, 1, 0 };

		// Build the Morphology pipeline
		Morphology morphology_process(vision_pipeline);
		morphology_process.setSE(structureElement);

		switch (vision_pipeline->pipeline_config->getMorphologyMethod()) {
			case MORPHOLOGY_METHOD::CLOSING:
				morphology_process.closing();
				break;
			case MORPHOLOGY_METHOD::OPENING:
				morphology_process.opening();
				break;
			case MORPHOLOGY_METHOD::ERODE:
				morphology_process.erode();
				break;
			case MORPHOLOGY_METHOD::DIALATE:
				morphology_process.dialate();
				break;
		}

		elapsed_time = (double)(clock() - tStart) / CLOCKS_PER_SEC;
		vision_pipeline->timing_morphology = 1000 * elapsed_time;

		// Run segmentation algorithm
		if (vision_pipeline->pipeline_config->getVisionLibDebugMode()) {
			printf("\tMorphology ran in %0.2f ms\n", vision_pipeline->timing_morphology);
		}
	}

	tStart = clock();
	ConnComponents ccomp( vision_pipeline ) ;
	int result = ccomp.buildSegmentionMap();

	if ( result == EXIT_FAILURE ) {
		vision_pipeline->target_LOC->top_right_x = -1;
		return EXIT_FAILURE;
	}

	elapsed_time = (double)(clock() - tStart) / CLOCKS_PER_SEC;
	vision_pipeline->timing_segment = 1000 * elapsed_time;

	// Detect the boundary segments
	if (vision_pipeline->pipeline_config->getVisionLibDebugMode()) {
		printf("\tConn Components ran in %0.2f ms\n", vision_pipeline->timing_segment);
	}

	tStart = clock();

	// BoundaryDetection boundaryDetect(vision_pipeline, BOUNDARY_METHOD::LEGACY);
	BoundaryDetection boundaryDetect( vision_pipeline );
	boundaryDetect.computeBoundary();

	elapsed_time = (double)(clock() - tStart) / CLOCKS_PER_SEC;
	vision_pipeline->timing_detect = 1000 * elapsed_time;

	// Print out debug information
	if (vision_pipeline->pipeline_config->getVisionLibDebugMode()) {
		printf("\tBoundary Detection ran in %0.2f ms\n\n", vision_pipeline->timing_detect);
		vision_pipeline->printTargetLocation();
	}

	// Initalize the capture mode to the current debug state
	bool capture_debug_image = vision_pipeline->pipeline_config->getVisionLibDebugMode() || vision_pipeline->pipeline_config->getVisionLibCaptureMode();

	// Check to see if the capture criteria are met ( if in capture mode )
	if (vision_pipeline->pipeline_config->getVisionLibCaptureMode() ) {
		if ((vision_pipeline->pipeline_config->getVisionLibFrameCounter() % vision_pipeline->pipeline_config->getVisionLibCaptureInc()) != 0) {
			capture_debug_image = false;
		}
	}

	// Assuming the image is to be captured, make sure the directory exists
	if ( doesPathExist("Runtime_Capture") && capture_debug_image )
	{
		// Paint the Annotation
		int result = annotateTargetImage( vision_pipeline );

		char output_image_path[255];
		sprintf(output_image_path, ".//Runtime_Capture//%05d_run_target_%s.pnm", vision_pipeline->pipeline_config->getVisionLibFrameCounter(), vision_pipeline->pipeline_config->getVisionLibDebugStr());
		write_image_store(vision_pipeline->annotated_image, PNM_FORMAT::BINARY, output_image_path);
	}

	// Increment the counter
	vision_pipeline->pipeline_config->incrementVisionLibFrameCounter();

	return EXIT_SUCCESS;
}

int run_ground_process(Image_Store* input_image, Vision_Pipeline* vision_pipeline)
{

	// Start the counter
	clock_t tStart = clock();
	clock_t tBegin = tStart;
	double elapsed_time = 0;
	int result;

	result = image_resize(input_image, vision_pipeline);

	elapsed_time = (double)(clock() - tStart) / CLOCKS_PER_SEC;
	vision_pipeline->timing_scale = 1000 * elapsed_time;

	// Perform the color conversion
	if (vision_pipeline->pipeline_config->getVisionLibDebugMode()) {
		printf("\tImage Resize ran in %0.2f ms\n", vision_pipeline->timing_scale);
	}

	tStart = clock();
	result = process_pixels(vision_pipeline);

	elapsed_time = (double)(clock() - tStart) / CLOCKS_PER_SEC;
	vision_pipeline->timing_process = 1000 * elapsed_time;

	// Run segmentation algorithm
	if (vision_pipeline->pipeline_config->getVisionLibDebugMode()) {
		printf("\tProcess Pixels ran in %0.2f ms\n", vision_pipeline->timing_process);
	}

	tStart = clock();

	ConnComponents ccomp(vision_pipeline, N_TYPE::N8, SEGMENTATION_METHOD::MARK, TARGET_DETECTION_TYPE::BALL);
	result = ccomp.buildSegmentionMap();

	elapsed_time = (double)(clock() - tStart) / CLOCKS_PER_SEC;
	vision_pipeline->timing_segment = 1000 * elapsed_time;

	// Detect the boundary segments
	if (vision_pipeline->pipeline_config->getVisionLibDebugMode()) {
		printf("\tConn Components ran in %0.2f ms\n", vision_pipeline->timing_segment);
	}

	if (result == EXIT_FAILURE) {
		return EXIT_FAILURE;
	}

	// Print out debug information
	if (vision_pipeline->pipeline_config->getVisionLibDebugMode()) {
		printf("Ball Detection ran in %0.2f ms\n\n", 1000 * elapsed_time);
		vision_pipeline->printBallLocations();
	}

	// Check to see if the capture criteria are met ( if in capture mode )
	// Initalize the capture mode to the current debug state
	bool capture_debug_image = vision_pipeline->pipeline_config->getVisionLibDebugMode() || vision_pipeline->pipeline_config->getVisionLibCaptureMode();

	if (vision_pipeline->pipeline_config->getVisionLibCaptureMode()) {
		if ((vision_pipeline->pipeline_config->getVisionLibFrameCounter() % vision_pipeline->pipeline_config->getVisionLibCaptureInc()) != 0) {
			capture_debug_image = false;
		}
	}

	// Assuming the image is to be captured, make sure the directory exists
	if (doesPathExist("Runtime_Capture") && capture_debug_image)
	{
		char output_image_path[255];
		sprintf(output_image_path, ".//Runtime_Capture//%05d_segmented_%s.pnm", vision_pipeline->pipeline_config->getVisionLibFrameCounter(), vision_pipeline->pipeline_config->getVisionLibDebugStr());
		write_image_store(vision_pipeline->segmented_imgout, PNM_FORMAT::BINARY, output_image_path);
	}

	for (int i = 0; i < vision_pipeline->ball_locations.location_length; i++)
	{
		if (is_null_bb(&vision_pipeline->ball_bounding_boxes[i])) {
			break;
		}

		get_center(&vision_pipeline->ball_bounding_boxes[i], &(vision_pipeline->ball_locations.location[i]));
		vision_pipeline->ball_locations.balls_found = i + 1;
	}

	printf("Balls Found : %d\n", vision_pipeline->ball_locations.balls_found);

	// Assuming the image is to be captured, make sure the directory exists
	if (doesPathExist("Runtime_Capture") && capture_debug_image)
	{
		// Paint the Annotation
		annotateBallCamImage(vision_pipeline);

		std::cout << "\tAnnotating Image, Balls Found: " << vision_pipeline->ball_locations.balls_found << "\n";

		char output_image_path[255];
		sprintf(output_image_path, ".//Runtime_Capture//%05d_ball_loc_%s.pnm", vision_pipeline->pipeline_config->getVisionLibFrameCounter(), vision_pipeline->pipeline_config->getVisionLibDebugStr());
		write_image_store(vision_pipeline->annotated_image, PNM_FORMAT::BINARY, output_image_path);
	}

	// Increment the counter
	vision_pipeline->pipeline_config->incrementVisionLibFrameCounter();

	return EXIT_SUCCESS;
}

bool is_null_bb(BoundingBox_VM* box) {

	if ((box->left == 0) & (box->right == 0) & (box->top == 0) & (box->bottom == 0))
		return true;
	else
		return false;

}

void get_center(BoundingBox_VM* box, Location* center) {

	//printf("Left, Right, Top, Bottom : %d\t%d\t%d\t%d\n",
	//	box->left, box->right, box->top, box->bottom);

	center->x = (box->left + box->right) / 2;
	center->y = (box->top + box->bottom) / 2;

	// printf("Center X : %d\nCenter Y : %d\n", center->x, center->y);

}


int image_resize (Image_Store* image_data, Vision_Pipeline * vision_pipeline )
 {
	// Set Up temporary crop image
	Image_Store cropped_image;
	int ret_val = 0;

	int cropped_image_width, cropped_image_height = 0;
	double aspect_ratio = image_data->height / (double) image_data->width;

	if (vision_pipeline->pipeline_config->getSensorOrientation() == SENSOR_ORIENTATION::PORTRAIT)
	{

		// Resample the image, then rotate it
		ret_val = image_resize( image_data, &cropped_image, vision_pipeline->pipeline_config->getScaleFactor(), vision_pipeline->pipeline_config->getVisionLibDebugMode() );

		// Run through the image and extract the sub region
		cropped_image_width = cropped_image.height;
		cropped_image_height = (int) (cropped_image.width ) ;

		vision_pipeline->scaled_image->set_attributes(cropped_image_width, cropped_image_height, cropped_image.planes, cropped_image.max_val, cropped_image.pixel_format, cropped_image.interleave);
		vision_pipeline->scaled_image->allocate_memory();
		vision_pipeline->scaled_image->min_RGB = cropped_image.min_RGB;
		vision_pipeline->scaled_image->max_RGB = cropped_image.max_RGB;

		int sensor_offset = cropped_image.width - cropped_image_height;

		// Extract the image data
		int pixel_offset = 0, new_offset = 0;
		int scanline_offset = 0 ;
		int flip_offset;

		for (int loop_col = sensor_offset ; loop_col < sensor_offset+cropped_image_height; loop_col++)
		{
			scanline_offset = 0;

			// Increment the destination pixel offest
			pixel_offset = loop_col * cropped_image_width * cropped_image.planes ;

			for (int loop_row = 0; loop_row < cropped_image.height; loop_row++)
			{
				// Calculate the source pixel offset
				new_offset   = ( loop_col + scanline_offset) * cropped_image.planes ;

				// Calculate the flipped pixel_offset
				flip_offset = pixel_offset + ( (cropped_image.height -1 - loop_row) * cropped_image.planes );

				// Copy over the image pixels
				vision_pipeline->scaled_image->image[flip_offset]    = cropped_image.image[new_offset++];
				vision_pipeline->scaled_image->image[flip_offset +1] = cropped_image.image[new_offset++];
				vision_pipeline->scaled_image->image[flip_offset +2] = cropped_image.image[new_offset++];
				
				scanline_offset += cropped_image.width;

			}

		}

		//char output_file[255];
		//sprintf(output_file, ".//Runtime_Capture//%05d_Rotated_Scaled_%s.ppm", VisionLib_debug_frame_counter, VisionLib_debug_output_file_str);
		//write_image_store(scaled_image, PNM_FORMAT::BINARY, output_file);

	}
	else  //  This is the case whereby the sensor is LANDSCAPE, no rotation/crop is needed.
	{

		// Resample the image
		ret_val = image_resize(image_data, vision_pipeline->scaled_image, vision_pipeline->pipeline_config->getScaleFactor(), vision_pipeline->pipeline_config->getVisionLibDebugMode() );

	}

	return ret_val;
}


int image_resize( Image_Store* image_data, Image_Store* scaled_image, double scale_factor, bool debug_mode )
{

	// Need to assign the color planes, because some images come in as BGR
	// so we will flip the channels to RGB
	int r_channel_offset, g_channel_offset, b_channel_offset;
	int r, g, b, mx, mn;

	if (image_data->pixel_format == PIXEL_FORMAT::RGB)
	{
		r_channel_offset = 0;
		g_channel_offset = 1;
		b_channel_offset = 2;
	}
	else if (image_data->pixel_format == PIXEL_FORMAT::BGR)
	{
		b_channel_offset = 0;
		g_channel_offset = 1;
		r_channel_offset = 2;
	}
	else
	{
		printf("Pixel Format must be RGB or BGR!\n");
		return -1;
	}

	if (scale_factor == 1)
	{

		scaled_image->set_attributes(image_data->width, image_data->height, image_data->planes, image_data->max_val, PIXEL_FORMAT::RGB, INTERLEAVE_FORMAT::PIXEL_INTERLEAVED);
		scaled_image->allocate_memory();

		if (scaled_image->max_val == 0)
			scaled_image->max_val = 255 ;

		int pixels = scaled_image->width * scaled_image->height * scaled_image->planes;
		memcpy(scaled_image->image, image_data->image, sizeof(uint8) * pixels);

		if (scaled_image->interleave != INTERLEAVE_FORMAT::PIXEL_INTERLEAVED) {
			printf("no support for non-pixel interleaved formats!\n");
			return -1;
		}

		for (int i = 0; i < pixels; i += 3)
		{
			// Extract the pixel values 
			// either RGB or BGR
			r = image_data->image[i + r_channel_offset];
			g = image_data->image[i + g_channel_offset];
			b = image_data->image[i + b_channel_offset];

			// Find the minimum and maximum values 			
			mx = max(max(r, g), b);
			mn = min(min(r, g), b);

			// Update the global max
			if (scaled_image->max_RGB < mx)
				scaled_image->max_RGB = mx;

			// Update the global min
			if (scaled_image->min_RGB > mn)
				scaled_image->min_RGB = mn;

			scaled_image->image[i + 0] = r;
			scaled_image->image[i + 1] = g;
			scaled_image->image[i + 2] = b;
		}

		image_data->min_RGB = scaled_image->min_RGB;
		image_data->max_RGB = scaled_image->max_RGB;
	}
	else {

		// Area Effective Scale Factor
		double effSF = sqrt(1 / scale_factor);

		// Setting scaled_image Attributes
		scaled_image->set_attributes((int)(image_data->width / effSF), (int)(image_data->height / effSF), image_data->planes, image_data->max_val, PIXEL_FORMAT::RGB, INTERLEAVE_FORMAT::PIXEL_INTERLEAVED);
		scaled_image->allocate_memory();

		if (scaled_image->max_val == 0)
			scaled_image->max_val = 255;

		// Calculating Totals
		int total_image_pixels = image_data->width * image_data->height;
		int total_image_values = total_image_pixels * image_data->planes;
		int total_scaled_pixels = scaled_image->width * scaled_image->height;
		int total_scaled_values = total_scaled_pixels * scaled_image->planes;

		// Navigation Variables
		double old_image_i_pixel = 0; // Dependent
		double old_image_i_value = 0; // Dependent
		double old_image_x_pixel = 0;
		double old_image_x_value = 0;
		double old_image_y_level = 0;

		double new_image_i_pixel = 0; // Dependent
		double new_image_i_value = 0; // Dependent
		double new_image_x_pixel = 0;
		double new_image_x_value = 0;
		double new_image_y_level = 0;

		while (true) {

			// Get Position
			old_image_i_pixel = ((((double)((int)old_image_y_level)) * image_data->width) + old_image_x_pixel);
			old_image_i_value = ((double)((int)old_image_i_pixel)) * image_data->planes;

			new_image_i_pixel = ((((double)((int)new_image_y_level)) * scaled_image->width) + new_image_x_pixel);
			new_image_i_value = ((double)((int)new_image_i_pixel)) * scaled_image->planes;

			// Detect Over Memory
			if (old_image_i_pixel > total_image_pixels || 
				new_image_i_pixel > total_scaled_pixels || 
				new_image_i_value + 2 > total_scaled_values || 
				old_image_i_value + 2 > total_image_values) {
				break;
			}

			// Get the current values 
			r = image_data->image[(int)old_image_i_value + r_channel_offset];
			g = image_data->image[(int)old_image_i_value + g_channel_offset];
			b = image_data->image[(int)old_image_i_value + b_channel_offset];

			// Copy Stuff and do the BGR to RGB conversion at the same time if needed
			scaled_image->image[(int)new_image_i_value + 0] = r;
			scaled_image->image[(int)new_image_i_value + 1] = g;
			scaled_image->image[(int)new_image_i_value + 2] = b;

			// Find the minimum and maximum values 			
			mx = max(max(r, g), b);
			mn = min(min(r, g), b);

			// Update the global min/max
			if (scaled_image->max_RGB < mx)
				scaled_image->max_RGB = mx;
			if (scaled_image->min_RGB > mn)
				scaled_image->min_RGB = mn;

			// Increment
			if (old_image_x_pixel >= image_data->width || new_image_x_pixel >= scaled_image->width) {

				old_image_y_level += effSF;
				new_image_y_level++;

				new_image_x_pixel = 0;
				new_image_x_value = 0;

				old_image_x_pixel = 0;
				old_image_x_value = 0;
			}
			else {
				old_image_x_pixel += effSF;
				old_image_x_value += (effSF * image_data->planes);
				new_image_x_pixel++;
			}
		}
	}
	return 0;
}

int process_pixels( Vision_Pipeline * vision_pipeline )
{

	if (vision_pipeline->pipeline_config->getVisionLibDebugMode())
	{
		std::cout << "In process_pixels\n";
	}

	// Boolean used to tell code to capture intermediate images
	bool capture_debug_image = false;

	Image_Store* debug_rgb_image = NULL;
	Image_Store* debug_xyz_image = NULL;
	Image_Store* debug_lab_image = NULL;

	if ( vision_pipeline->scaled_image->pixel_format != PIXEL_FORMAT::RGB) {
		printf("input image should be RGB!\n");
		exit(-1);
	}

	vision_pipeline->thresholded_out->set_attributes(vision_pipeline->scaled_image->width, vision_pipeline->scaled_image->height, 1, 255, PIXEL_FORMAT::GRAY, vision_pipeline->scaled_image->interleave);
	vision_pipeline->thresholded_out->min_RGB = 0;
	vision_pipeline->thresholded_out->max_RGB = 255;
	vision_pipeline->thresholded_out->allocate_memory();

	vision_pipeline->distance_out->set_attributes(vision_pipeline->scaled_image->width, vision_pipeline->scaled_image->height, 1, 255, PIXEL_FORMAT::GRAY, vision_pipeline->scaled_image->interleave);
	vision_pipeline->distance_out->min_RGB = 0;
	vision_pipeline->distance_out->max_RGB = 255;
	vision_pipeline->distance_out->allocate_memory();

	if (vision_pipeline->pipeline_config->getVisionLibDebugMode() || vision_pipeline->pipeline_config->getVisionLibCaptureMode() )
	{
		capture_debug_image = true;

		if (vision_pipeline->pipeline_config->getVisionLibCaptureMode())
		{
			if ((vision_pipeline->pipeline_config->getVisionLibFrameCounter() % vision_pipeline->pipeline_config->getVisionLibCaptureInc()) != 0)
			{
				capture_debug_image = false;
			}
		}

		//// Configure the parameters
		//debug_rgb_image = new Image_Store();
		//debug_rgb_image->set_attributes(vision_pipeline->scaled_image->width, vision_pipeline->scaled_image->height, 3, 255, PIXEL_FORMAT::RGB, vision_pipeline->scaled_image->interleave);
		//debug_rgb_image->allocate_memory();

		//debug_xyz_image = new Image_Store();
		//debug_xyz_image->set_attributes(vision_pipeline->scaled_image->width, vision_pipeline->scaled_image->height, 3, 255, PIXEL_FORMAT::RGB, vision_pipeline->scaled_image->interleave);
		//debug_xyz_image->allocate_memory();

		debug_lab_image = new Image_Store();
		debug_lab_image->set_attributes(vision_pipeline->scaled_image->width, vision_pipeline->scaled_image->height, 3, 255, PIXEL_FORMAT::RGB, vision_pipeline->scaled_image->interleave);
		debug_lab_image->allocate_memory();
	}

	process_pixels_multi_thread( vision_pipeline, debug_rgb_image, debug_xyz_image, debug_lab_image );

	if (capture_debug_image & doesPathExist( "Runtime_Capture" ))
	{
		char output_file[255];

		sprintf(output_file, ".//Runtime_Capture//%05d_rgb_input_%s.ppm", vision_pipeline->pipeline_config->getVisionLibFrameCounter(), vision_pipeline->pipeline_config->getVisionLibDebugStr());
		write_image_store(vision_pipeline->scaled_image, PNM_FORMAT::BINARY, output_file );
		//sprintf(output_file, ".//Runtime_Capture//%05d_rgb_%s.ppm", vision_pipeline->pipeline_config->getVisionLibFrameCounter(), vision_pipeline->pipeline_config->getVisionLibDebugStr());
		//write_image_store(debug_rgb_image, PNM_FORMAT::BINARY, output_file );
		//sprintf(output_file, ".//Runtime_Capture//%05d_xyz_%s.ppm", vision_pipeline->pipeline_config->getVisionLibFrameCounter(), vision_pipeline->pipeline_config->getVisionLibDebugStr());
		//write_image_store(debug_xyz_image, PNM_FORMAT::BINARY, output_file );
		sprintf(output_file, ".//Runtime_Capture//%05d_lab_%s.ppm", vision_pipeline->pipeline_config->getVisionLibFrameCounter(), vision_pipeline->pipeline_config->getVisionLibDebugStr());
		write_image_store(debug_lab_image, PNM_FORMAT::BINARY, output_file );
		sprintf(output_file, ".//Runtime_Capture//%05d_dst_%s.pgm", vision_pipeline->pipeline_config->getVisionLibFrameCounter(), vision_pipeline->pipeline_config->getVisionLibDebugStr());
		write_image_store(vision_pipeline->distance_out, PNM_FORMAT::BINARY, output_file );
		sprintf(output_file, ".//Runtime_Capture//%05d_thresh_%s.pbm", vision_pipeline->pipeline_config->getVisionLibFrameCounter(), vision_pipeline->pipeline_config->getVisionLibDebugStr());
		write_image_store(vision_pipeline->thresholded_out, PNM_FORMAT::BINARY, output_file );

		if (debug_lab_image != NULL)
			delete debug_lab_image;

		if (debug_rgb_image != NULL)
			delete debug_rgb_image;

		if (debug_xyz_image != NULL)
			delete debug_xyz_image;
	}

	return 0;
}

GSAUTO pickAuto(BallLocations* ball_location) {

	Vector_Loc* v = new Vector_Loc();

	// Find The leftmost ball if there are 3
	int minPos = 2;
	if (ball_location->balls_found > 2) {

		int min = ball_location->location[2].x; 

		for (int i = 0; i < ball_location->balls_found - 1; i++) {
			if (ball_location->location[i].x < min) {
				min = ball_location->location[i].x;
				minPos = i;
			}
		}
	}

	// set the left are right Vector_Loc->Location
	switch(minPos) {
		case 0:
			if (ball_location->location[1].x < ball_location->location[2].x) {
				v->l = &ball_location->location[1];
				v->r = &ball_location->location[2];
			} else {
				v->l = &ball_location->location[2];
				v->r = &ball_location->location[1];
			}
			break;
		case 1:
			if (ball_location->location[0].x < ball_location->location[2].x) {
				v->l = &ball_location->location[0];
				v->r = &ball_location->location[2];
			} else {
				v->l = &ball_location->location[2];
				v->r = &ball_location->location[0];
			}
			break;
		case 2:
			if (ball_location->location[0].x < ball_location->location[1].x) {
				v->l = &ball_location->location[0];
				v->r = &ball_location->location[1];
			} else {
				v->l = &ball_location->location[1];
				v->r = &ball_location->location[0];
			}
			break;
	}

	// set Vector_Loc
	v->x = v->l->x - v->r->x;
	v->y = v->l->y - v->r->y;

	// Finding the differences
	double posDiff = 0, vecDiff = 0;
	double BADiff = 0, BBDiff = 0, RADiff = 0, RBDiff = 0;

	BADiff = getDiff(v, blueA);
	BBDiff = getDiff(v, blueB);
	RADiff = getDiff(v, redA);
	RBDiff = getDiff(v, redB);

	if (BADiff <= BBDiff && BADiff <= RADiff && BADiff <= RBDiff) {
		return GSAUTO::BLUEA;
	} else if (BBDiff <= BADiff && BBDiff <= RADiff && BBDiff <= RBDiff) {
		return GSAUTO::BLUEB;
	} else if (RADiff <= BBDiff && RADiff <= BADiff && RADiff <= RBDiff) {
		return GSAUTO::REDA;
	} else if (RBDiff <= BBDiff && RBDiff <= RADiff && RBDiff <= RBDiff) {
		return GSAUTO::REDB;
	}

	delete v;

	return GSAUTO::UNKNOWN;
}

double getDiff(Vector_Loc* v1, Vector_Loc* v2) {

	double vecDiff = sqrt(pow(v1->x - v2->x, 2) + pow(v1->y - v2->y, 2));

	double posDiff = sqrt(pow(v1->l->x - v2->l->x, 2) + pow(v1->l->y - v2->l->y, 2)) + sqrt(pow(v1->r->x - v2->r->x, 2) + pow(v1->r->y - v2->r->y, 2));

	return vecDiff * 1.5 + posDiff;
}

GSAUTO newPickAuto(BallLocations* ball_location, double scale_factor) {
	// Too many balls, something broke
	if (ball_location->balls_found > 3) {
		std::cout << "\t\t\tSomething Broke: Too Many Balls Found\n";
		return GSAUTO::UNKNOWN;
	}

	// Not Enough balls, something broke
	if (ball_location->balls_found < 2) {
		std::cout << "\t\t\tSomething Broke: Not Enough Balls Found\n";
		return GSAUTO::UNKNOWN;
	}

	 // Return for 2-ball special case
	if (ball_location->balls_found == 2) {
		return GSAUTO::REDA;
	}

	// return for spread-out special case
	int yThresh = 250;
	if (ball_location->location[0].y > yThresh || ball_location->location[1].y > yThresh || ball_location->location[2].y > yThresh) {
		return GSAUTO::REDB;
	}

	int xDistThresh = 100;
	int minPos = 0;
	int min = 10000;

	// Find leftmost ball
	for (int i = 0; i < ball_location->balls_found; i++) {
		if (ball_location->location[i].x < min) {
			min = ball_location->location[i].x;
			minPos = i;
		}
	}

	// Find x distances between remainning balls
	int xDist = 0;
	if (minPos == 0) {
		xDist = abs(ball_location->location[1].x - ball_location->location[2].x);
	} else if (minPos == 1) {
		xDist = abs(ball_location->location[0].x - ball_location->location[2].x);
	} else if (minPos == 2) {
		xDist = abs(ball_location->location[0].x - ball_location->location[1].x);
	}

	if (xDist > xDistThresh) {
		return GSAUTO::BLUEA;
	} else {
		return GSAUTO::BLUEB;
	}

}

GSAUTO newPickAuto2(BallLocations* ball_location, double scale_factor) {
	// Too many balls, something broke
	if (ball_location->balls_found > 3) {
		std::cout << "\t\t\tSomething Broke: Too Many Balls Found\n";
		return GSAUTO::UNKNOWN;
	}

	// Not Enough balls, something broke
	if (ball_location->balls_found < 2) {
		std::cout << "\t\t\tSomething Broke: Not Enough Balls Found\n";
		return GSAUTO::UNKNOWN;
	}

	 // Return for 2-ball special case
	if (ball_location->balls_found == 2) {
		return GSAUTO::REDA;
	}

	int yThresh = 250;
	if (ball_location->location[0].y > yThresh || ball_location->location[1].y > yThresh || ball_location->location[2].y > yThresh) {
		// Spread Out: Must be Either RedA or RedB
		// Check X Spread
#ifdef DEBUG_MODE
		std::cout << "Y Threshold test passed, must be RedA or RedB\n";
#endif
		int xThresh = 480;
		if (ball_location->location[0].x > xThresh || ball_location->location[1].x > xThresh || ball_location->location[2].x > xThresh) {
			return GSAUTO::REDA;
		} else {
			return GSAUTO::REDB;
		}
	} else {

#ifdef DEBUG_MODE
		std::cout << "Y Threshold not test passed, must be BlueA or BlueB\n";
#endif

		// Not Spread Out: Must be Either BlueA or BlueB
		int xDistThresh = 100;
		int minPos = 2;
		int min = ball_location->location[2].x; 

		// Find leftmost ball
		for (int i = 0; i < ball_location->balls_found - 1; i++) {
			if (ball_location->location[i].x < min) {
				min = ball_location->location[i].x;
				minPos = i;
			}
		}

		// Find x distances between remainning balls
		int xDist = 0;
		if (minPos == 0) {
			xDist = abs(ball_location->location[1].x - ball_location->location[2].x);
		} else if (minPos == 1) {
			xDist = abs(ball_location->location[0].x - ball_location->location[2].x);
		} else if (minPos == 2) {
			xDist = abs(ball_location->location[0].x - ball_location->location[1].x);
		}

		if (xDist > xDistThresh) {
			return GSAUTO::BLUEA;
		} else {
			return GSAUTO::BLUEB;
		}
	}

}

void process_pixels_multi_thread( Vision_Pipeline* vision_pipeline, Image_Store* debug_rgb_image, 
								  Image_Store* debug_xyz_image, Image_Store* debug_lab_image)
{
	Image_Store* scaled_in = vision_pipeline->scaled_image ;
	Image_Store* distance_out = vision_pipeline->distance_out ;
	Image_Store* thresholded_out = vision_pipeline-> thresholded_out ;
	CLUT_Table* CLUT_table = vision_pipeline->CLUT_table;
	ExecutorService* exec_service = vision_pipeline->exec_service;

	bool multiThreaded = vision_pipeline->pipeline_config->getMultiThreaded();
	bool debug_flag = vision_pipeline->pipeline_config->getVisionLibDebugMode();


	// Update the CLUT object	
	CLUT_table->update_MinMaxRange(scaled_in->min_RGB, scaled_in->max_RGB-scaled_in->min_RGB);

	int rgb_pixel_location = 0;

	if (multiThreaded == false)
	{
		// Number of pixels to process
		int pixels = thresholded_out->width * thresholded_out->height * thresholded_out->planes;
		int offset = 0;

		// Run the image through on a single thread
		process_RGB_pixels_loop( offset, pixels, CLUT_table, scaled_in, debug_rgb_image, debug_xyz_image, 
								 debug_lab_image, distance_out, thresholded_out, debug_flag );
	}
	else
	{
		// Split the image up into multiple strips and run them
		int num_strips = vision_pipeline->pipeline_config->getNumberOfThreads();

		int strip_height = scaled_in->height / num_strips;

		// Calculate the strip start and length
		int* strip_offset = new int[num_strips];
		int* strip_length = new int[num_strips];

		for (int i = 0; i < num_strips; i++) {
			strip_offset[i] = i * strip_height * thresholded_out->width * thresholded_out->planes;
			strip_length[i] = strip_height * thresholded_out->width * thresholded_out->planes;
		}

		if ( ( num_strips * strip_height ) < thresholded_out->height ) {
			strip_length[num_strips - 1] = ( strip_height + (thresholded_out->height - (num_strips * strip_height))) *
											thresholded_out->width * thresholded_out->planes;
		}

		if (exec_service == NULL) // Not using executor service
		{
			// Allocate array to store the pointer locations to the threads
			std::thread** threads = new std::thread * [num_strips];

			for (int i = 0; i < num_strips; i++)
			{
				if (debug_flag)
					printf("Processing Pixels Thread %d\n", i);

				threads[i] = new std::thread(process_RGB_pixels_loop, strip_offset[i],
					strip_length[i], CLUT_table, scaled_in, debug_rgb_image, debug_xyz_image,
					debug_lab_image, distance_out, thresholded_out, debug_flag);
			}

			for (int i = 0; i < num_strips; i++) {
				// Joint the streams
				threads[i]->join();
			}

			// Cleanup
			delete[] threads;
		}
		else  // Use the executor service 
		{
			// Allocate array to store the pointer locations to the threads

			if (debug_flag)
				std::cout << "Using ExecutorService\n";

			for (int i = 0; i < num_strips; i++)
			{
				// Create a lambda instance 
				std::function<int()>* lambda;

				if (debug_flag)
					printf("Processing Pixels Thread %d\n", i);

				int this_strip_offset = strip_offset[i];
				int this_strip_length = strip_length[i];

				lambda = new std::function<int()>();
				*lambda = [this_strip_offset, this_strip_length, CLUT_table, &scaled_in, &debug_rgb_image, &debug_xyz_image,
					&debug_lab_image, &distance_out, &thresholded_out, debug_flag]()
				{
					process_RGB_pixels_loop(this_strip_offset, this_strip_length, CLUT_table, scaled_in, debug_rgb_image,
						debug_xyz_image, debug_lab_image, distance_out, thresholded_out, debug_flag);
					return EXIT_SUCCESS;
				};

				// Register the task
				exec_service->addLambdaTask(lambda);
			}

			exec_service->registerThisThread(true);
		}
	}
}

void process_RGB_pixels_loop(int pixel_offset, int num_pixels, CLUT_Table* CLUT_table, Image_Store* scaled_in,
							Image_Store* debug_rgb_image, Image_Store* debug_xyz_image, Image_Store* debug_lab_image,
							Image_Store* distance_out, Image_Store* thresholded_out, bool debug_flag )
{
	// Define the start and end locations
	int start_pixel = pixel_offset;
	int end_pixel = pixel_offset + num_pixels;

	int rgb_pixel_location = start_pixel * scaled_in->planes;
	// int test_pixel = 284 + (542 * scaled_in->width);

	for (int gray_pixel_location = start_pixel; gray_pixel_location < end_pixel; gray_pixel_location++)
	{
		// Process each pixel by equations
		if (debug_flag)
		{
			CLUT_table->color_process_rgb_pixel(scaled_in, rgb_pixel_location, gray_pixel_location,
												debug_rgb_image, debug_xyz_image, debug_lab_image, 
												distance_out, thresholded_out);
		}
		else
		{
			// Process each pixel using the LUT
			CLUT_table->color_process_rgb_pixel_LUT(scaled_in, rgb_pixel_location, gray_pixel_location, 
													distance_out, thresholded_out);
		}

		rgb_pixel_location += 3;

	}

}

