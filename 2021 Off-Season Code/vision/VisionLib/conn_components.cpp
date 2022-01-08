
#include "conn_components.hpp"
#include "VisionLib.hpp"
#include <math.h>

int vision_max_i(int a, int b) { return a > b ? a : b; }
int vision_min_i(int a, int b) { return a < b ? a : b; }

double vision_max_d(double a, double b) { return a < b ? b : a; }
double vision_min_d(double a, double b) { return a < b ? a : b; }

#define COUNTER_EXCEEDS_BOUNDS_ERROR "Counter exceeded the maximum of 255! Segments will be messed up!\n"
const int MAX_ENTRIES_JACOB = 2048;
const int MAX_ENTRIES_MARK = 256;

ConnComponents::~ConnComponents(void)
{
	// Default Destructor - cleans up

	if (segment_stats != NULL) {
		delete[] segment_stats;
		segment_stats = NULL;
	}
}

ConnComponents::ConnComponents(Vision_Pipeline* vision_pipeline)
{
	this->vision_pipeline = vision_pipeline;
	this->segmentation_mode = vision_pipeline->pipeline_config->getTargetType();
	this->method = vision_pipeline->pipeline_config->getSegmentationMethod();
	this->setConnectivity(vision_pipeline->pipeline_config->getConnectComponentsConnectivity());
}

// Overload Constructor
ConnComponents::ConnComponents(Vision_Pipeline* vision_pipeline_data, N_TYPE connectivity, SEGMENTATION_METHOD seg_method, TARGET_DETECTION_TYPE seg_mode)
{
	this->vision_pipeline = vision_pipeline_data;
	this->segmentation_mode = seg_mode;
	this->method = seg_method;
	this->setConnectivity(connectivity);
}

// Method to allow setting of the connected components type - N8 or N4
void ConnComponents::setConnectivity(N_TYPE set_connectivity)
{
	connectivity = set_connectivity;
}

// Generic entry method to switch between different algorithms
int ConnComponents::buildSegmentionMap( )
{
	int result = 0;

	if (method == SEGMENTATION_METHOD::JACOB) {
		result = ccomp_Jacob( segmentation_mode );
	}
	else {

		if (connectivity == N_TYPE::UNDEFINED) {
			assert("Must define connected components connectivity\n");
		}

		result = ccomp_Mark( segmentation_mode );
	}

	return result;
}

int ConnComponents::ccomp_Jacob( TARGET_DETECTION_TYPE segmentation_mode )
{
	// Jacob's segmentation code 

	Image_Store* distance_in = vision_pipeline->distance_out;
	Image_Store* thresholded_in = vision_pipeline->thresholded_out;
	Image_Store* segmented_image_out = vision_pipeline->segmented_imgout;

	if (vision_pipeline->pipeline_config->getVisionLibDebugMode())
	{
		std::cout << "In conn_components\n";
	}

	if (distance_in->pixel_format != PIXEL_FORMAT::GRAY) {
		printf("`scaled_in` input image pixel format is not grayscale!\n");
		exit(-1);
	}

	if (thresholded_in->pixel_format != PIXEL_FORMAT::GRAY) {
		printf("`thresholded_in` input image pixel format is not grayscale!\n");
	}

	// Set up the ImageStore output parameters

	segmented_image_out->width = thresholded_in->width;
	segmented_image_out->height = thresholded_in->height;

	segmented_image_out->planes = 1;
	segmented_image_out->max_val = 255;
	segmented_image_out->max_RGB = 1;
	segmented_image_out->min_RGB = 0;
	segmented_image_out->pixel_format = PIXEL_FORMAT::GRAY;

	segmented_image_out->interleave = thresholded_in->interleave;
	segmented_image_out->allocate_memory();

	boundary_region[0] = 1;
	boundary_region[1] = 1;
	boundary_region[2] = segmented_image_out->width-1;
	boundary_region[3] = segmented_image_out->height-1;

	// Initalize the memory to 0
	memset((void*)segmented_image_out->image, 0, segmented_image_out->width * segmented_image_out->height * segmented_image_out->planes);

	LUT_1D lookup( MAX_ENTRIES_JACOB );
	lookup.y_data[1] = 1;
	int counter = 1;

	// for incrementing index by x or y position
	const int i_y1 = thresholded_in->width * thresholded_in->planes;
	const int i_x1 = thresholded_in->planes;
	const int i_inc = thresholded_in->planes;

	const int o_y1 = segmented_image_out->width * segmented_image_out->planes;
	const int o_x1 = segmented_image_out->planes;
	const int o_inc = segmented_image_out->planes;

	const int width = segmented_image_out->width;
	const int height = segmented_image_out->height;

	const int segmented_image_out_pixels = width * height * segmented_image_out->planes;
	const int pixels = width * height;

	const uint8 background = 0;
	const uint8 foreground = 1;

	// N4 CC
	{
		// assume INTERLEAVE_FORMAT::PIXEL_INTERLEAVED
		int in_index = 0,
			segmented_image_out_index = 0;

		uint8	iup = 0,   // thresholded up value
			ileft = 0, // thresholded left value
			oup = 0,   // integer value of segment
			oleft = 0; // integer value of segment

		bool upValid = false, leftValid = false;
		int higher, lower;

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {

				in_index = i_y1 * y + i_x1 * x;
				segmented_image_out_index = o_y1 * y + o_x1 * x;
				uint8 in_value = thresholded_in->image[in_index];

				if (in_value == background) {
					segmented_image_out->image[segmented_image_out_index] = background;
					continue;
				}

				segmented_image_out->image[segmented_image_out_index] = 1; // maybe should not be here

				if (y > 0) {
					upValid = true;
					iup = thresholded_in->image[in_index - i_y1];
					oup = segmented_image_out->image[segmented_image_out_index - o_y1];
				}
				else {
					upValid = false;
				}
				if (x > 0) {
					leftValid = true;
					ileft = thresholded_in->image[in_index - i_x1];
					oleft = segmented_image_out->image[segmented_image_out_index - o_x1];
				}
				else {
					leftValid = false;
				}

				// binary if-trees :)
				if (upValid) {
					if (leftValid) { // vast majority of cases (happens <(width-1)*(height-1)> times)
						// here comes another binary tree
						// re-use of terms ok because we already decided
						upValid = iup != background;
						leftValid = ileft != background;
						if (upValid) {
							if (leftValid) { // happens the most
								// merge
								if (oup != oleft) {
									// merge conflict
									higher = vision_max_i((int)oup, (int)oleft);
									lower = vision_min_i((int)oup, (int)oleft);
									int t = trace_CC_Jacob( &lookup, higher);
									int lt = trace_CC_Jacob( &lookup, lower);
									// must prevent circles!
									if (lt != higher && lt != t) {
										lookup.y_data[t] = lower;
									}
									lookup.y_data[higher] = lower;
									segmented_image_out->image[segmented_image_out_index] = (uint8)lower;
								}
								else {
									segmented_image_out->image[segmented_image_out_index] = oup;
								}
							}
							else { // probably happens the second most?
								segmented_image_out->image[segmented_image_out_index] = oup;
							}
						}
						else {
							if (leftValid) { // third most
								segmented_image_out->image[segmented_image_out_index] = oleft;
							}
							else { // 4th most
								segmented_image_out->image[segmented_image_out_index] = counter;
								counter++;
								if (counter > lookup.points) {
									printf(COUNTER_EXCEEDS_BOUNDS_ERROR);
									return EXIT_FAILURE;
								}
								lookup.y_data[counter] = counter;
							}
						}
					}
					else { // left column (happens <height-1> times)
						if (iup == foreground) {
							segmented_image_out->image[segmented_image_out_index] = oup;
						}
						else {
							segmented_image_out->image[segmented_image_out_index] = counter;
							counter++;
							if (counter > lookup.points) {
								printf(COUNTER_EXCEEDS_BOUNDS_ERROR);
								return EXIT_FAILURE;
							}
							lookup.y_data[counter] = counter;
						}
					}
				}
				else {
					if (leftValid) { // top row (happens <width-1> times)
						if (ileft == foreground) {
							segmented_image_out->image[segmented_image_out_index] = oleft;
						}
						else {
							segmented_image_out->image[segmented_image_out_index] = counter;
							counter++;
							if (counter > lookup.points) {
								printf(COUNTER_EXCEEDS_BOUNDS_ERROR);
								return EXIT_FAILURE;
							}
							lookup.y_data[counter] = counter;
						}
					}
					else { // top-left corner (happens exactly once)
						segmented_image_out->image[segmented_image_out_index] = counter;
						counter++;
						if (counter > lookup.points) {
							printf(COUNTER_EXCEEDS_BOUNDS_ERROR);
							return EXIT_FAILURE;
						}
						lookup.y_data[counter] = counter;
					}
				}

				//in_index += i_inc;
				//out_index += o_inc;
			}
		}
	}

	// build lookup table to completion
	int maxIndex = 1;
	for (int i = 1; i < counter; i++) {
		lookup.y_data[i] = trace_CC_Jacob( &lookup, i);
		maxIndex = vision_max_i(maxIndex, lookup.y_data[i]);
	}
	maxIndex++;

	if (0) {
		// Print out the LUT for verification
		lookup.printLUT("Jacob's LUT", counter);
	}

	// Apply the remapping LUT 
	int maxIndex2 = apply_image_LUT( &lookup );

	if (maxIndex2 == 1)
	{
		printf("No Segments Found\n");
		return EXIT_FAILURE;
	}
	else {
		printf( "Number of Components : %d\n", maxIndex2-1 );
	}

	// Now extract the label stats
	extract_label_stats(maxIndex);

	if (segmentation_mode == TARGET_DETECTION_TYPE::TARGET)
	{
		// Call into the code to extract the target location
		extract_target_location( maxIndex );
	}
	else
	{
		// Call into the code to extract the target location
		int numSegments; // use this at your own risk
		extract_ball_location( maxIndex, &numSegments ); //make bounding_box an array for this method

		// Display the number of segments&
		printf("Number of Balls Found : %d\n", numSegments);
	}

	return EXIT_SUCCESS;
}


int ConnComponents::trace_CC_Jacob( LUT_1D* table, int index) {

	if ( index >= table->points ) {
		printf("access out of bounds in cc table! index: %d\n", index);
		return 0;
	}

	int result = table->y_data[index];

	for (int i = 0; result != index && i < table->points; ++i) {
		index = result;
		result = table->y_data[index];
	}
	return index;
}
 

int ConnComponents::extract_target_location( int maxIndex ) 
{
	Image_Store* segmented_image = vision_pipeline->segmented_imgout;
	Image_Store* distance_in = vision_pipeline->distance_out;
	BoundingBox_VM* bounding_box = vision_pipeline->target_bounding_box;

	// finish calculating averages
	double tolerance = 0.05;
	int best = -1; // best segment index
	int numGreen = 0; // number of segments that are "green enough"

	int allGreen[MAX_ENTRIES_MARK] = {}; // indexed in order by occurrance, integer segment index
	int allGreen_i = 0;
	{		
		// Find the brightest green segment
		double maxGreen = 0;
		{
			for (int i = 1; i < maxIndex; i++) {
				if (this->segment_stats[i].number_elements > 0) {
					this->segment_stats[i].average_brightness = this->segment_stats[i].average_brightness / this->segment_stats[i].number_elements;
					maxGreen = vision_max_d(maxGreen, this->segment_stats[i].average_brightness);
				}
			}
		}

		// Find the segments that exceed the minimum green required to be a "valid segment"
		double minGreen = maxGreen - maxGreen * tolerance;
		{
			double green;
			for (int i = 1; i < maxIndex; i++) {
				green = this->segment_stats[i].average_brightness;
				if (green > minGreen) {
					allGreen[allGreen_i++] = i;
					numGreen++;
					if (best < 0 || this->segment_stats[i].number_elements > this->segment_stats[best].number_elements) {
						best = i;
					}
				}
			}
		}
	}

	if (best == -1) {
		printf("\tNo segment chosen\n");
		return -1;
	}
	// filter out the most green segment
	{
		int index;
		int value;
		bounding_box->left = segmented_image->width;
		bounding_box->top = segmented_image->height;
		for (int x = 0; x < segmented_image->width; x++) {
			for (int y = 0; y < segmented_image->height; y++) {
				index = y * segmented_image->width + x;
				value = segmented_image->image[index];
				if (value == best) {
					segmented_image->image[index] = 255;
					bounding_box->left = vision_min_i(bounding_box->left, x);
					bounding_box->right = vision_max_i(bounding_box->right, x);
					bounding_box->top = vision_min_i(bounding_box->top, y);
					bounding_box->bottom = vision_max_i(bounding_box->bottom, y);
				}
				else
					segmented_image->image[index] = 0;
			}
		}
	}

	return EXIT_SUCCESS;
}


int ConnComponents::extract_ball_location( int maxIndex, int* numSegments)
{
	// Get the pointers to each image
	Image_Store* segmented_image = vision_pipeline->segmented_imgout;
	Image_Store* distance_in = vision_pipeline->distance_out;

	// finish calculating averages
	const double tolerance = 0.1; // 10% seems to work well
	int numYellow = 0; // number of segments that are "yellow enough"
	int allYellow[MAX_ENTRIES_MARK] = {}; // indexed in order by occurrance, integer segment index

	int allYellow_i = 0;
	{
		// Find the brightest yellow segment
		double maxYellow = 0;
		{
			for (int i = 1; i < maxIndex; i++) {
				if (this->segment_stats[i].number_elements > 0) {
					segment_stats[i].average_brightness = segment_stats[i].average_brightness / segment_stats[i].number_elements;
					maxYellow = vision_max_d(maxYellow, segment_stats[i].average_brightness);
				}
			}
		}

		// Find the segments that exceed the minimum yellow required to be a "valid ball segment"
		const double minYellow = maxYellow - maxYellow * tolerance;
		{
			double yellow;
			for (int i = 1; i < maxIndex; i++) {
				yellow = segment_stats[i].average_brightness;
				if (yellow > minYellow) {
					allYellow[allYellow_i++] = i;
					numYellow++;
				}
			}
		}
	}

	// printf("NumYellow : %d\n", numYellow );

	BoundingBox_VM* bounding_boxes = NULL;


	// Setting a limit to the number of ball locations
	if (numYellow >= MAX_BOUNDING_BOXES)
		numYellow = MAX_BOUNDING_BOXES - 1;
	{
		int index;
		int value;

		for (int i = 0; i < numYellow; i++)
		{
			bounding_boxes = &(vision_pipeline->ball_bounding_boxes[i]);

			bounding_boxes->top = distance_in->height;
			bounding_boxes->left = distance_in->width;
			bounding_boxes->right = 0;
			bounding_boxes->bottom = 0;
		}

		for (int x = 0; x < segmented_image->width; x++)
		{
			for (int y = 0; y < segmented_image->height; y++)
			{
				index = y * segmented_image->width + x;
				value = segmented_image->image[index];
				segmented_image->image[index] = 0;

				for (int i = 0; i < numYellow; i++)
				{
					bounding_boxes = &(vision_pipeline->ball_bounding_boxes[i]);

					if (value == allYellow[i])
					{
						segmented_image->image[index] = 255;

						if (bounding_boxes == 0)
							segmented_image->image[index] = 0;
						else
						{
							bounding_boxes->left = vision_min_i(bounding_boxes->left, x);
							bounding_boxes->right = vision_max_i(bounding_boxes->right, x);
							bounding_boxes->top = vision_min_i(bounding_boxes->top, y);
							bounding_boxes->bottom = vision_max_i(bounding_boxes->bottom, y);

							//printf( "Left, Right, Top, Bottom : %d\t%d\t%d\t%d\n",
							//	bounding_boxes[i]->left, bounding_boxes[i]->right,
							//	bounding_boxes[i]->top, bounding_boxes[i]->bottom );

						}
					}
					else {
						segmented_image->image[index] = 0;
					}
				}

			}
		}

	}
	*numSegments = numYellow;

	// Zero out the empties
	for (int i = numYellow; i < MAX_BOUNDING_BOXES; i++) {

		bounding_boxes = &(vision_pipeline->ball_bounding_boxes[i]);

		bounding_boxes->top = 0;
		bounding_boxes->left = 0;
		bounding_boxes->right = 0;
		bounding_boxes->bottom = 0;
	}

	return (numYellow > 0) - 1;
}


int ConnComponents::ccomp_Mark( TARGET_DETECTION_TYPE segmentation_mode)
{
	// Marks Segmentation Code

	Image_Store* distance_in = vision_pipeline->distance_out;
	Image_Store* thresholded_in = vision_pipeline->thresholded_out;
	Image_Store* segmented_image_out = vision_pipeline->segmented_imgout;
	BoundingBox_VM* bounding_box = vision_pipeline->target_bounding_box;

	if (vision_pipeline->pipeline_config->getVisionLibDebugMode())
	{
		std::cout << "In conn_components\n";
	}

	// Double check that the images are set-up correctly
	if (distance_in->pixel_format != PIXEL_FORMAT::GRAY) {
		printf("distance image pixel format is not grayscale!\n");
		exit(-1);
	}

	if (thresholded_in->pixel_format != PIXEL_FORMAT::GRAY) {
		printf("Thresholded image pixel format is not grayscale!\n");
		exit(-1);
	}

	// Set up the ImageStore for the segmentation map
	segmented_image_out->set_attributes(thresholded_in->width, thresholded_in->height, 1, 255, PIXEL_FORMAT::GRAY, INTERLEAVE_FORMAT::PIXEL_INTERLEAVED);
	segmented_image_out->allocate_memory();

	// Reset the min and max values
	segmented_image_out->max_RGB = 1;
	segmented_image_out->min_RGB = 0;

	// Start calculating some of the offest attributes to make things faster
	int stride = segmented_image_out->width;
	int TL_offset = -stride - 1;
	int TM_offset = -stride;
	int TR_offset = -stride + 1;
	int ML_offset = -1;

	// Set up pointers to image data
	uint8* thresholded_image_base_ptr = NULL;
	uint8* segmentation_map_base_ptr = NULL;

	// Use to keep track of components
	int connected_pixel_count = 0;
	int ccomp_label = 1;

	// Labels for keeping track of values
	int TL_label = 0, TL_state = 0;
	int TM_label = 0, TM_state = 0;
	int TR_label = 0, TR_state = 0;
	int ML_label = 0, ML_state = 0;

	uint rmapArrayN4[2] = { 0 };
	uint rmapArrayN8[6] = { 0 };

	// Starting with inverted values here so that the Update function pulls them
	// in for each itteration
	boundary_region[0] = (uint) segmented_image_out->height;
	boundary_region[1] = (uint) segmented_image_out->width;
	boundary_region[2] = 0;
	boundary_region[3] = 0;

	LUT_1D remapArrayLUT( MAX_ENTRIES_MARK );
	remapArrayLUT.resetLinear();

	// Debugging Code
	if (0) {
		remapArrayLUT.printLUT("Init");
	}

	int pixel_count = 0;

	// Build the segmentation label map
	for (int loop_row = 1; loop_row < segmented_image_out->height-1; loop_row++ )
	{
		// Intentionally skip the outer 1 pixel perimeter
		thresholded_image_base_ptr = thresholded_in->image + (loop_row * stride) + 1;
		segmentation_map_base_ptr = segmented_image_out->image + (loop_row * stride) + 1;

		for (int loop_col = 1; loop_col < segmented_image_out->width-1; loop_col++ )
		{
			// Start processing through the pixels
			if ( *thresholded_image_base_ptr > 0 )
			{
 				if (vision_pipeline->pipeline_config->getVisionLibDebugMode()) {
					pixel_count = (int) ( segmentation_map_base_ptr - vision_pipeline->segmented_imgout->image );
				}

				// The baseline cases for N4 and N8
				TM_state = *(thresholded_image_base_ptr + TM_offset);
				ML_state = *(thresholded_image_base_ptr + ML_offset);
				TM_label = *(segmentation_map_base_ptr + TM_offset);
				ML_label = *(segmentation_map_base_ptr + ML_offset);

				if (connectivity == N_TYPE::N4)
				{
					// N4 Connected Components
					if ((loop_row == 1) || (loop_col == 1)) {
						connected_pixel_count = 0;
					} else if ((loop_row == 1) && (loop_col > 1)) {
						connected_pixel_count = ML_state ;
					} else if ((loop_row > 1) && (loop_col == 1)) {
						connected_pixel_count = TM_state;
					} else {
						connected_pixel_count = TM_state + ML_state;
					}

					if (connected_pixel_count == 0) 
					{
						// This is a new isolated pixel, so tag it.
						*(segmentation_map_base_ptr) = ccomp_label;
						if (ccomp_label < remapArrayLUT.points-1) {
							ccomp_label += 1;
						}
						else 
						{
							// Run the image through the trace
							ccomp_label = trace_CC_Mark( &remapArrayLUT, ccomp_label);

							// Reset the Output LUT
							remapArrayLUT.resetLinear();
							remapArrayLUT.printLUT("Reset");
						}
					}
					else if (connected_pixel_count == 255 )
					{
						// First need to determine which pixel was tagged
						if (TM_state) {
							// Top Middle case
							*(segmentation_map_base_ptr) = TM_label;
						}
						else {
							// Middle Left case
							*(segmentation_map_base_ptr) = ML_label;
						}
					}
					else
					{
						// Determine which label to use
						*(segmentation_map_base_ptr) = remap_pixels_N4(TM_label, ML_label, rmapArrayN4 );

						// Determine if we need to update the image remap array
						if ((rmapArrayN4[0] + rmapArrayN4[1]) > 0)
						{
							// Update the label counter
							ccomp_label = remap_pixels_array( rmapArrayN4, &remapArrayLUT, ccomp_label, segmentation_map_base_ptr, connectivity) ;

							if (ccomp_label >= remapArrayLUT.points - 1 )
							{
								// Run the image through the trace
								ccomp_label = trace_CC_Mark( &remapArrayLUT, ccomp_label);

								// Reset the Output LUT
								remapArrayLUT.resetLinear();
								remapArrayLUT.printLUT("Reset");
							}
						}

						if (0) { // Debug code
							remapArrayLUT.printLUT( "Remap", 30 );
						}
					}
				}
				else
				{
					// Adding in the new cases for N8
					TL_state = *(thresholded_image_base_ptr + TL_offset);
					TR_state = *(thresholded_image_base_ptr + TR_offset);
					TL_label = *(segmentation_map_base_ptr + TL_offset);
					TR_label = *(segmentation_map_base_ptr + TR_offset);

					// N8 Connected Components
					if ((loop_row == 1) || (loop_col == 1)) {
						connected_pixel_count = 0;
					}
					else if ((loop_row == 1) && (loop_col > 1)) {
						connected_pixel_count = ML_state;
					}
					else if ((loop_row > 1) && (loop_col == 1)) {
						connected_pixel_count = TM_state + TR_state;
					}
					else {
						connected_pixel_count = TL_state + TM_state + TR_state + ML_state;
					}

					if (connected_pixel_count == 0)
					{
						// This is a new isolated pixel, so tag it.
						*(segmentation_map_base_ptr) = ccomp_label;
						if (ccomp_label < remapArrayLUT.points-1) {
							ccomp_label += 1;
						}
						else 
						{
							// Run the image through the trace
							ccomp_label = trace_CC_Mark( &remapArrayLUT, ccomp_label);

							// Reset the Output LUT
							remapArrayLUT.resetLinear();
						}
					}
					else if (connected_pixel_count == 255)
					{
						if ( TM_state ) {
							// Top Middle case
							*(segmentation_map_base_ptr) = TM_label;
						}
						else if ( ML_state ) {
							// Middle Left case
							*(segmentation_map_base_ptr) = ML_label;
						}
						else if ( TR_state ) {
							// Top Right case
							*(segmentation_map_base_ptr) = TR_label;
						}
						else {
							// Top Left case
							*(segmentation_map_base_ptr) = TL_label;
						}
					}
					else
					{
						// Need to decide what the pixel value should be
						// Remap the pixels that need to be merged
						*(segmentation_map_base_ptr) = remap_pixels_N8(TM_label, ML_label, TL_label, TR_label, rmapArrayN8 );

						if ((rmapArrayN8[0] + rmapArrayN8[1] + 
							 rmapArrayN8[2] + rmapArrayN8[3] +
							 rmapArrayN8[4] + rmapArrayN8[5]) > 0)
						{
							// Update the label counter
							ccomp_label = remap_pixels_array( rmapArrayN8, &remapArrayLUT, ccomp_label, segmentation_map_base_ptr, connectivity );

							if (ccomp_label >= remapArrayLUT.points-1 )
							{
								// Run the image through the trace
								ccomp_label = trace_CC_Mark(&remapArrayLUT, ccomp_label);

								// Reset the Output LUT
								remapArrayLUT.resetLinear();
							}
						}

					}
				}

				// Update the boundary region
				updateboundaryRegion( loop_row, loop_col );
			}
			else {
				// Do nothing case, the segmentation map was pre-initalized to 0's
			}

			// int pixel_count = segmentation_map_base_ptr - vision_pipeline->segmented_imgout->image;
			// printf("  Pixels Count : %d\n", pixel_count );

			// Increment the image pointers
			thresholded_image_base_ptr++;
			segmentation_map_base_ptr++;
		}
	}


	if (vision_pipeline->pipeline_config->getVisionLibDebugMode())
	{
		//// Write out the image for Debug
		//char output_file[255];
		//sprintf(output_file, ".//Runtime_Capture//%05d_b4_trace_image.pgm", vision_pipeline->VisionLib_debug_frame_counter);
		//write_image_store(segmented_image_out, PNM_FORMAT::BINARY, output_file);
	}

	// Run the Trace algorithm to resolve conflicts in the label map
	int unique_segments = trace_CC_Mark( &remapArrayLUT, ccomp_label );
		
	if (unique_segments == 1)
	{
		printf("No Segments Found\n");
		return EXIT_FAILURE;
	}
	else {
		printf("Number of Components : %d\n", unique_segments-1);
	}

	// Now compute the label stats
	extract_label_stats( unique_segments );

	// Clean Up unwanted array  
	// ( It will be cleansed up when the function scope closes )
	// delete[] remapArrayLUT;

	if (segmentation_mode == TARGET_DETECTION_TYPE::TARGET)
	{
		// Call into the code to extract the target location
		extract_target_location( unique_segments );
	}
	else
	{
		// Call into the code to extract the target location
		int numSegments; // use this at your own risk	
		extract_ball_location( unique_segments, &numSegments ); //make bounding_box an array for this method

		// Display the number of segments&
		printf("Number of Balls Found : %d\n", numSegments);
	}

	return EXIT_SUCCESS;
}

int ConnComponents::remap_pixels_N4(int TM_pixval, int ML_pixval, uint* rmapArray)
{
	// Assign the first value
	int max_val = vision_max_i(TM_pixval, ML_pixval);

	// Reset the rmapArray because we dont know what it was coming in
	rmapArray[0] = 0;
	rmapArray[1] = 0;

	// Test the other values, assign if possible
	if ((TM_pixval != 0) && (TM_pixval != max_val))
	{
		rmapArray[0] = TM_pixval;
		rmapArray[1] = max_val;
	}

	// Test the other value, assign if possible
	if ((ML_pixval != 0) && (ML_pixval != max_val))
	{
		rmapArray[0] = ML_pixval;
		rmapArray[1] = max_val;
	}

	return max_val;
}

int ConnComponents::remap_pixels_N8(int TL_pixval, int TM_pixval, int TR_pixval, int ML_pixval, uint* rmapArray)
{
	// Assign the first value
	int max_val = vision_max_i(TL_pixval, TM_pixval);
	max_val = vision_max_i(max_val, TR_pixval);
	max_val = vision_max_i(max_val, ML_pixval);
	int remap_idx = 0;

	// Reset the rmapArray because we dont know what it was coming in
	rmapArray[0] = 0;	rmapArray[1] = 0;
	rmapArray[2] = 0;	rmapArray[3] = 0;
	rmapArray[4] = 0;	rmapArray[5] = 0;

	// Test the other values, assign if possible
	if ((TM_pixval != 0) && (TM_pixval != max_val))
	{
		rmapArray[0] = TM_pixval;
		rmapArray[1] = max_val;
		remap_idx += 2;
	}

	// Test the other value, assign if possible
	if ((ML_pixval != 0) && (ML_pixval != max_val))
	{
		rmapArray[remap_idx] = ML_pixval;
		rmapArray[remap_idx+1] = max_val;
		remap_idx += 2;
	}

	// Add in the N8 cases
	if ((TL_pixval != 0) && (TL_pixval != max_val))
	{
		rmapArray[remap_idx] = TL_pixval;
		rmapArray[remap_idx+1] = max_val;
		remap_idx += 2;
	}

	if ((TR_pixval != 0) && (TR_pixval != max_val))
	{
		rmapArray[remap_idx] = TR_pixval;
		rmapArray[remap_idx+1] = max_val;
	}

	return max_val;
}

int ConnComponents::remap_pixels_array( uint* remapUpdateArray, LUT_1D* remapArrayLUT, int ccomp_label_in, 
										uint8* current_pixel, N_TYPE connectivity )
{
	// Need to convert to LUT
	int ccomp_label_out = ccomp_label_in;

	// printf("Remap Pixels Entry : %d %d\n", remapUpdateArray[0], remapUpdateArray[1] );

	if (connectivity == N_TYPE::N4)
	{
		// Check the mapping 
		ccomp_label_out = remap_pixels_subfn( remapArrayLUT, ccomp_label_in, remapUpdateArray[0], 
											  remapUpdateArray[1], current_pixel);
	}
	else {
		// N8 Connected Components
		for (int loop_remap = 0; loop_remap < 6; loop_remap+=2)
		{
			// Make sure the entry is not empty
			if ((remapUpdateArray[loop_remap] + remapUpdateArray[loop_remap + 1]) == 0) {
				continue;
			}

			// Call in and remap the pixels
			ccomp_label_out = remap_pixels_subfn( remapArrayLUT, ccomp_label_out, remapUpdateArray[loop_remap],
												  remapUpdateArray[loop_remap+1], current_pixel );			
		}
	}

	// Hand back the updated label
	return ccomp_label_out;
}


int ConnComponents::remap_pixels_subfn( LUT_1D* remapArrayLUT, int ccomp_label_in, uint value1, uint value2, 
										uint8* current_pixel)
{
	int pixel_count = 0;

	// Calculate the current pixel
	if (vision_pipeline->pipeline_config->getVisionLibDebugMode()) {
		pixel_count = (int)(current_pixel - vision_pipeline->segmented_imgout->image);
	}

	// Initialize the return value
	int ccomp_label_out = ccomp_label_in;

	// Starting with the simple case
	if ((remapArrayLUT->x_data[value1] == remapArrayLUT->y_data[value1]) ||
		(remapArrayLUT->y_data[value1] == value2 ))
	{
		//Make the assignment mapping
		remapArrayLUT->y_data[value1] = value2;
	}
	else
	{
		// Conflict Case
		// The Current Label has already been remapped, so need to
		// use a new label otherwise we will break the flow

		remapArrayLUT->y_data[value2] = value1;
		remapArrayLUT->y_data[ccomp_label_in] = value2;

		// Need to assign the current label
		*current_pixel = ccomp_label_in;

		// Increment the counter
		if (ccomp_label_in < remapArrayLUT->points-1) {
			ccomp_label_out = ccomp_label_in + 1;
		}
	}

	if (vision_pipeline->pipeline_config->getVisionLibDebugMode()) {
		// printf("  Remap Pixels : %d %d %d %d %d\n", pixel_count, ccomp_label_in, ccomp_label_out, value1, value2);
	}

	if (ccomp_label_out < remapArrayLUT->points ) {
		return ccomp_label_out;
	}
	else {
		return -1;
	}

}


void ConnComponents::updateboundaryRegion( int loop_row, int loop_col)
{
	boundary_region[0] = vision_min_i(boundary_region[0], loop_row);
	boundary_region[1] = vision_min_i(boundary_region[1], loop_col);
	boundary_region[2] = vision_max_i(boundary_region[2], loop_row+1);
	boundary_region[3] = vision_max_i(boundary_region[3], loop_col+1);
}


// This routine takes a segment label mapping LUT and runs through it
// tracing out duplicates, to flatten the current label stack
//
// Input Arguments :
//     remapArrayList   -  Pointer to memory location for array
//     remapArrayLength -  Length of first dimension of LUT array
//     label_in         -  Current Connected Components label
//
//
// Output Argument :
//     Updated label for connected component, after re-mapping
//
int ConnComponents::trace_CC_Mark( LUT_1D* remapArrayList, int label_in )
{
	// Create some counters
	uint start_index = 0, updated_start_index = 0;
	uint end_index = 0, updated_end_index = 0;
	uint remap_counter_idx = 0;
	uint unique_segments = 0;
	uint LUT_length = 1;

	// Allocate space for the new array list
	LUT_1D remap_counter_list( remapArrayList->points ) ;

	if (0) { // Debug Code
		remap_counter_list.printLUT( "Trace Init" ) ;
		remapArrayList->printLUT("Remap Input");
	}

	for (int loop_i = 1; loop_i < remapArrayList->points; loop_i++)
	{
		// Extract the start and end values of the current entry
		start_index = remapArrayList->x_data[loop_i];
		end_index = remapArrayList->y_data[loop_i];

		// Check to see if different
		if (start_index != end_index)
		{
			// Set-up the break condition
			bool break_condition = 0;

			// Get the start and end points
			updated_start_index = remapArrayList->x_data[ end_index ];
			updated_end_index = remapArrayList->y_data[ end_index ];

			// If we have reached a dead end, then stop
			if (updated_start_index == updated_end_index) {
				// printf("Updated End Index Values : %d, %d\n", updated_start_index, updated_end_index);
				continue;
			}

			// Reset the remap counter for this next loop
			remap_counter_list.resetZero();

			// Start tracking the pointsthat need to be remapped later
			remap_counter_list.y_data[0] = start_index;
			remap_counter_idx = 1;

			while (updated_start_index != updated_end_index)
			{
				// Loop through to make sure that we have not already seen this entry
				for (uint loop_counter = 0; loop_counter < remap_counter_idx; loop_counter++)
				{
					// If we have seen it, break out
					if ( remap_counter_list.y_data[loop_counter] == updated_start_index )
					{
						break_condition = true;
						LUT_length = vision_max_i(LUT_length, updated_end_index+1);
						break;
					}
				}

				// Need to break out of the while loop
				if (break_condition) {
					break;
				}

				// Store the remapped value
				remap_counter_list.y_data[remap_counter_idx++] = updated_start_index ;

				// Update the start and end values
				updated_start_index = remapArrayList->x_data[updated_end_index];
				updated_end_index = remapArrayList->y_data[updated_end_index];
				
				LUT_length = vision_max_i(LUT_length, updated_end_index+1);

				// printf("Updated Index Values : %d, %d\n", updated_start_index, updated_end_index );
			}

			// Assign the new end point
			for (uint loop_counter = 0; loop_counter < remap_counter_idx; loop_counter++) {
				// printf( "Updating End Index Values : %d, %d\n", remapArrayList[remap_counter_list[loop_counter] + remapArrayLength], updated_end_index  );
				remapArrayList->y_data[remap_counter_list.y_data[loop_counter]] = updated_end_index;
			}

		}
		else
		{
			LUT_length = vision_max_i(LUT_length, end_index);
			// printf("Updated End Index Values : %d, %d\n", start_index, end_index );
			// Break out of the loop
			continue;
		}

	}

	if (0) { // Debug code
		remapArrayList->printLUT( "Trace Before Remap" );
	}

	// Create a Label Mapping
	LUT_1D remap_labels(remapArrayList->points);
	uint segment_ID = 0;

	// loop through and count the number of unique segments
	for (int loop_segment = 0; loop_segment < remapArrayList->points; loop_segment++)
	{
		// Get the current ID mapping
		segment_ID = remapArrayList->y_data[loop_segment];

		if ( ( segment_ID > 0 ) && (remap_labels.y_data[segment_ID]==0) ) {
			remap_labels.y_data[segment_ID] = ++unique_segments;
		}
	}

	// loop through and remap the labels
	for (int loop_segment = 0; loop_segment < remapArrayList->points; loop_segment++) {
		remapArrayList->y_data[loop_segment] = remap_labels.y_data[remapArrayList->y_data[loop_segment]];
	}

	if (0) { // Debug code
		remapArrayList->printLUT( "Trace Updated Remap" );
	}

	// Map the image through the corrected LUT 
	int max_ccomp_label = apply_image_LUT( remapArrayList );
 
	// Do some cleanup, they will be cleaned up when the function goes out of scope
	// delete[] remap_labels;
	// delete[] remap_counter_list;

	// Return the updated label count
	return max_ccomp_label ;

}

// Push an image through the LUT and compute segment Stats
int ConnComponents::extract_label_stats( int maxIndex )
{
	// Set up pointers
	Image_Store* thresholded_Image = vision_pipeline->thresholded_out;
	Image_Store* segmented_image = vision_pipeline->segmented_imgout;
	Image_Store* distance_in = vision_pipeline->distance_out;

	// Create an array of structs to hold the information
	// This gets cleaned up by the destructor

	if (segment_stats == NULL) {
		segment_stats = new segment_data[maxIndex];
		memset(segment_stats, 0, maxIndex * sizeof(segment_stats));
	}

	// Pre-calculate the row offset
	uint row_offset = boundary_region[0] * segmented_image->width;
	uint pixel_offset = 0, loop_row, loop_col;
	uint8 pixval;

	// Now go through the image a second time and merge the labels
	for (loop_row = boundary_region[0]; loop_row < boundary_region[2]; loop_row++)
	{
		// Intentionally skip the outer 1 pixel perimeter
		pixel_offset = row_offset + boundary_region[1];

		// Remap the pixels in the region of interest
		for (loop_col = boundary_region[1]; loop_col < boundary_region[3]; loop_col++)
		{
			pixval = segmented_image->image[pixel_offset];

			if (pixval > 0) {
				this->segment_stats[pixval].number_elements++;
				this->segment_stats[pixval].average_brightness += (double)distance_in->image[pixel_offset];
				this->segment_stats[pixval].segment_ID = (int)pixval;
			}

			// Increment the pixel pointer
			pixel_offset++;
		}

		// Increment the row offset
		row_offset += segmented_image->width;
	}

	if (vision_pipeline->pipeline_config->getVisionLibDebugMode())
	{
		if (0) {
			printf("Displaying Segment Stats\n");
			printf("~~~~~~~~~~~~~~~~~~~~~~~~\n\n");

			// Display the Segment Statistics 
			for (int loop_seg = 0; loop_seg < maxIndex; loop_seg++)
			{
				if ( this->segment_stats[loop_seg].number_elements > 0)
				{
					printf("Segment Number     : %d\n", this->segment_stats[loop_seg].segment_ID );
					printf("Segment Size       : %f\n", this->segment_stats[loop_seg].number_elements);
					printf("Segment Brightness : %f\n\n", this->segment_stats[loop_seg].average_brightness / this->segment_stats[loop_seg].number_elements );
				}
			}
		}
	}

	return maxIndex;
}

int ConnComponents::apply_image_LUT( LUT_1D* lookup )
{
	// Set up pointers
	Image_Store* thresholded_Image = vision_pipeline->thresholded_out;
	Image_Store* segmented_image = vision_pipeline->segmented_imgout;
	Image_Store* distance_in = vision_pipeline->distance_out;

	// Get image base pointer
	uint8* base_label_ptr = segmented_image->image;

	// Calculate start and end pointers
    int start_pixel = (boundary_region[0] * segmented_image->width) + boundary_region[1];
	int end_pixel = (boundary_region[2] * segmented_image->width) + boundary_region[3];
	int loop_pix = 0, max_count = 0;
	uint8 pixval = 0;

	// Loop through the pixels and apply the LUT
	for ( loop_pix = start_pixel; loop_pix < end_pixel; loop_pix++) {
		
		if (base_label_ptr[loop_pix])
		{
			// Only perform the look-up if we have a valid pixel
			pixval = (uint8) lookup->y_data[base_label_ptr[loop_pix]];
			base_label_ptr[loop_pix] = pixval ;
			max_count = vision_max_i(base_label_ptr[loop_pix], max_count);
		}
	}
	max_count++;

	if (vision_pipeline->pipeline_config->getVisionLibDebugMode()) {
		printf("Maximum Image Label : %d\n", max_count);
	}

	return max_count;
}
