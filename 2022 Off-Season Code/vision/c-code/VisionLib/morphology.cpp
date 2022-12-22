
#include "morphology.hpp"
#include "Vision_Pipeline.hpp"
#include "pnm.hpp"
#include <math.h>
#include <string>
#include <sys/stat.h>

Morphology::~Morphology(void)
{
	// Default Destructor - cleans up
	if (temporary_image != NULL) {
		delete temporary_image;
		temporary_image = NULL;
	}

}

// Overload Constructor
Morphology::Morphology( Vision_Pipeline* vision_pipeline_data )
{
	vision_pipeline = vision_pipeline_data;

	// Initalize the capture mode to the current debug state
	capture_debug_image = vision_pipeline->pipeline_config->getVisionLibDebugMode()|| vision_pipeline->pipeline_config->getVisionLibCaptureMode();

	// Check to see if the frame count meets the criteria if capture mode
	if (vision_pipeline->pipeline_config->getVisionLibCaptureMode()) {
		if ((vision_pipeline->pipeline_config->getVisionLibFrameCounter() % vision_pipeline->pipeline_config->getVisionLibCaptureInc()) != 0) {
			capture_debug_image = false;
		}
	}
}

void Morphology::dialate_erode( MORPHOLOGY_METHOD method )
{
	// Marks Segmentation Code
	Image_Store* thresholded_in = vision_pipeline->thresholded_out;
	BoundingBox* bounding_box = vision_pipeline->target_bounding_box;

	if (thresholded_in->pixel_format != PIXEL_FORMAT::GRAY) {
		printf("Thresholded image pixel format is not grayscale!\n");
		exit(-1);
	}

	// Set up pointers to image data
	uint8* thresholded_image_base_ptr = NULL;
	uint8* morphological_image_base_ptr = NULL;

	if (temporary_image == NULL) {
		// Allocate the memory needed for the dilated/eroded image
		temporary_image = new(Image_Store);
		temporary_image->set_attributes(vision_pipeline->thresholded_out->width, vision_pipeline->thresholded_out->height,
			vision_pipeline->thresholded_out->planes, vision_pipeline->thresholded_out->max_val,
			vision_pipeline->thresholded_out->pixel_format, vision_pipeline->thresholded_out->interleave);
		temporary_image->min_RGB = 0;
		temporary_image->max_RGB = 255;
		temporary_image->allocate_memory();
	}
	else
	{
		// Reset the output memory buffer
		temporary_image->clear_memory();
	}

	// Define the image region to work on
	int image_region[4] = { 0 } ;
	image_region[0] = 1;
	image_region[1] = 1;
	image_region[2] = thresholded_in->width-1;
	image_region[3] = thresholded_in->height-1;

	int pixel_count = 0;

	// Start calculating some of the offest attributes to make things faster
	int stride = thresholded_in->width;

	// Offsets for keeping track of values
	int TM_offset = -stride;
	int TL_offset = TM_offset - 1;
	int TR_offset = TM_offset + 1;
	int BM_offset = stride;
	int BL_offset = BM_offset - 1;
	int BR_offset = BM_offset + 1;
	int ML_offset = - 1;
	int MR_offset = + 1;

	int max_pixel_value = 0;	
	int pixel_sum = 0;

	for ( int i=0; i<9; i++) {
		max_pixel_value += structuring_element[i]*255;
	}

	// Build the segmentation label map
	for (int loop_row = image_region[1]; loop_row < image_region[3]; loop_row++ )
	{
		// Intentionally skip the outer 1 pixel perimeter
		thresholded_image_base_ptr = thresholded_in->image + (loop_row * stride) + 1;
		morphological_image_base_ptr = temporary_image->image + (loop_row * stride) + 1;

		for (int loop_col = image_region[0]; loop_col < image_region[2]; loop_col++ )
		{
			// Calculate the sum of the pixels 
			pixel_sum = ( structuring_element[0] * *(thresholded_image_base_ptr + TL_offset) ) +
						( structuring_element[1] * *(thresholded_image_base_ptr + TM_offset) ) +
						( structuring_element[2] * *(thresholded_image_base_ptr + TR_offset) ) +
						( structuring_element[3] * *(thresholded_image_base_ptr + ML_offset) ) +
						( structuring_element[4] * *(thresholded_image_base_ptr ) ) +
						( structuring_element[5] * *(thresholded_image_base_ptr + MR_offset) ) +
						( structuring_element[6] * *(thresholded_image_base_ptr + BL_offset) ) +
						( structuring_element[7] * *(thresholded_image_base_ptr + BM_offset) ) +
						( structuring_element[8] * *(thresholded_image_base_ptr + BR_offset) ) ;

			/*
			if (structuring_element[0])
				pixel_sum += *(thresholded_image_base_ptr + TL_offset);

			if (structuring_element[1])
				pixel_sum += *(thresholded_image_base_ptr + TM_offset);

			if (structuring_element[2])
				pixel_sum += *(thresholded_image_base_ptr + TR_offset);

			if (structuring_element[3])
				pixel_sum += *(thresholded_image_base_ptr + ML_offset);

			if (structuring_element[4])
				pixel_sum += *(thresholded_image_base_ptr);

			if (structuring_element[5])
				pixel_sum += *(thresholded_image_base_ptr + MR_offset);

			if ( structuring_element[6])
				pixel_sum += *(thresholded_image_base_ptr + BL_offset);

			if ( structuring_element[7])
				pixel_sum += *(thresholded_image_base_ptr + BM_offset);

			if ( structuring_element[8])
				pixel_sum += *(thresholded_image_base_ptr + BR_offset);
			*/

			// Start processing through the pixels
			if (method == MORPHOLOGY_METHOD::DIALATE) {
				// Dilation Case
				if ( pixel_sum > 0 ){
					*(morphological_image_base_ptr) = 255 ;
				}
			}
			else {
				// Erosion case
				if (pixel_sum  == max_pixel_value ) {
					*(morphological_image_base_ptr) = 255;
				}
			}

			// Increment the image pointers
			thresholded_image_base_ptr++;
			morphological_image_base_ptr++;
		}
	}

	// Now fix the pointers to switch the image buffers
	Image_Store * temp_pointer = vision_pipeline->thresholded_out;
	vision_pipeline->thresholded_out = temporary_image;
	temporary_image = temp_pointer;

}

void Morphology::dialate() 
{
	// run the dialation
	dialate_erode( MORPHOLOGY_METHOD::DIALATE );

	if (capture_debug_image & doesPathExist("Runtime_Capture"))
	{
		char output_image_path[255];
		sprintf(output_image_path, ".//Runtime_Capture//%05d_dilated_target_%s.pnm", vision_pipeline->pipeline_config->getVisionLibFrameCounter(), vision_pipeline->pipeline_config->getVisionLibDebugStr());
		write_image_store(vision_pipeline->thresholded_out, PNM_FORMAT::BINARY, output_image_path);
	}
}

void Morphology::erode() 
{
	// run the erosion
	dialate_erode(MORPHOLOGY_METHOD::ERODE);

	if (capture_debug_image & doesPathExist("Runtime_Capture"))
	{
		char output_image_path[255];
		sprintf(output_image_path, ".//Runtime_Capture//%05d_eroded_target_%s.pnm", vision_pipeline->pipeline_config->getVisionLibFrameCounter(), vision_pipeline->pipeline_config->getVisionLibDebugStr());
		write_image_store(vision_pipeline->thresholded_out, PNM_FORMAT::BINARY, output_image_path);
	}

}

void Morphology::opening() 
{
	// First run dialate then erode
	dialate_erode(MORPHOLOGY_METHOD::ERODE);
	dialate_erode(MORPHOLOGY_METHOD::DIALATE);

	if (capture_debug_image & doesPathExist("Runtime_Capture"))
	{
		char output_image_path[255];
		sprintf(output_image_path, ".//Runtime_Capture//%05d_opened_target_%s.pnm", vision_pipeline->pipeline_config->getVisionLibFrameCounter(), vision_pipeline->pipeline_config->getVisionLibDebugStr());
		write_image_store(vision_pipeline->thresholded_out, PNM_FORMAT::BINARY, output_image_path);
	}
}

void Morphology::closing() 
{
	// First run erode then dialate
	dialate_erode(MORPHOLOGY_METHOD::DIALATE);
	dialate_erode(MORPHOLOGY_METHOD::ERODE);

	if (capture_debug_image & doesPathExist("Runtime_Capture"))
	{
		char output_image_path[255];
		sprintf(output_image_path, ".//Runtime_Capture//%05d_closed_target_%s.pnm", vision_pipeline->pipeline_config->getVisionLibFrameCounter(), vision_pipeline->pipeline_config->getVisionLibDebugStr());
		write_image_store(vision_pipeline->thresholded_out, PNM_FORMAT::BINARY, output_image_path);
	}
}

void Morphology::setSE( int* SE )
{
	// Loop through and copy the structuring element values
	for ( int i=0; i < 9; i++) {
		structuring_element[i] = SE[i];
	}
}


bool Morphology::doesPathExist(const std::string& s)
{
	struct stat buffer;
	return (stat(s.c_str(), &buffer) == 0);
}