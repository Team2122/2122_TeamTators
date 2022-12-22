#include "boundary_detection.h"
#include "VisionLib.hpp"

BoundaryDetection::~BoundaryDetection(void)
{
	// Default Destructor - cleans up
	
}

// Default Constructor
BoundaryDetection::BoundaryDetection(Vision_Pipeline* vision_pipeline_data )
{
	this->vision_pipeline = vision_pipeline_data;
	this->setMethod(vision_pipeline->pipeline_config->getBoundaryDetectionMethod() );
}

// Overload Constructor
BoundaryDetection::BoundaryDetection(Vision_Pipeline* vision_pipeline_data, BOUNDARY_METHOD method)
{
	this->vision_pipeline = vision_pipeline_data;
	this->setMethod(method);
}

// Set the detection mothod you want to use
void BoundaryDetection::setMethod(BOUNDARY_METHOD method)
{
	boundary_method = method;
}


// Choose between which of the boundary detection algorithms to run
int BoundaryDetection::computeBoundary()
{
	int result = 0;

	switch (boundary_method)
	{
	case BOUNDARY_METHOD::LEGACY:
		result = this->detect_boundary();
		break;

	case BOUNDARY_METHOD::PARABOUND:
		result = this->para_bound_detect();
		break;

	case BOUNDARY_METHOD::DIVIDE_CONQUOR:
		result = this->divide_and_conquor();
		break;
	}

	// Old Code
	// para_bound_detect(segmented_imgout, bounding_box->left, bounding_box->top, bounding_box->right, bounding_box->bottom, results);
	// detect_boundary(segmented_imgout, 0, 0, segmented_imgout->width, segmented_imgout->height, results);

	return result;
}

// This is the default detection algorithm
int BoundaryDetection::detect_boundary()
{
	Image_Store* image_data = vision_pipeline->segmented_imgout;
	int x1 = vision_pipeline->target_bounding_box->left;
	int y1 = vision_pipeline->target_bounding_box->top;
	int x2 = vision_pipeline->target_bounding_box->right;
	int y2 = vision_pipeline->target_bounding_box->bottom;

	TargetLocation* location = vision_pipeline->target_LOC;

	// printf("Width  : %u\n", image_data->width );
	// printf("Height : %u\n", image_data->height );

	int* X_histo = new int[image_data->width];
	memset(X_histo, 0, image_data->width * sizeof(int));

	int add_constant = image_data->width - ((x2 - x1) + 0); // Possibly + 1;

	int greyThresh = 250;

	for (int i = x1 + (y1 * image_data->width); i < x2 + (y2 * image_data->width); i++) {

		// Correct Position
		if (i % image_data->width > x2) {
			i += add_constant;
		}

		// Set Histogram Info
		if (image_data->image[i] > greyThresh) {
			//Y_histo[(int)(i / image_data->width)]++;
			X_histo[i % image_data->width]++;
		}
	}

	// Find Points
	int totalValues = image_data->width * image_data->height;
	int width = image_data->width;

	// Top X's
	bool rXFirst = false, lXFirst = false;
	int lx = x1, rx = x2;

	// Find Top X's
	while (!(rXFirst && lXFirst)) {

		// Top Right X
		if (!rXFirst && X_histo[rx] > 1) {
			rXFirst = true;
			location->top_right_x = rx;
		}
		else {
			rx--;
			if (rx < 0) {
				rXFirst = true;
				location->top_right_x = -1;
				rx = 0;
			}
		}

		// Top Left X
		if (!lXFirst && X_histo[lx] > 1) {
			lXFirst = true;
			location->top_left_x = lx;
		}
		else {
			lx++;
			if (lx > width) {
				location->top_left_x = -1;
				lXFirst = true;
				lx = 0;
			}
		}
	}

	// Bottom X's
	bool brxFirst = false, blxFirst = false;
	int middle_x = (int)(0.5 * (location->top_right_x + location->top_left_x));
	int brx = middle_x,
		blx = middle_x;
	double threshold = (X_histo[middle_x] * 1.5); // number of pixels

	// Find Bottom X's
	while (!(brxFirst && blxFirst)) {
		// Bottom Right X
		if (!brxFirst && X_histo[brx] > threshold) {
			brxFirst = true;
			location->bottom_right_x = brx;
		}
		else {
			brx++;
			if (brx > width) {
				location->bottom_right_x = -1;
				brxFirst = true;
				brx = 0;
			}
		}

		// Bottom Left X
		if (!blxFirst && X_histo[blx] > threshold) {
			blxFirst = true;
			location->bottom_left_x = blx;
		}
		else {
			blx--;
			if (blx < 0) {
				blxFirst = true;
				location->bottom_left_x = -1;
				blx = 0;
			}
		}
	}

	// Top Y's
	bool tlyFirst = false, tryFirst = false;
	int lty = location->top_left_x, rty = location->top_right_x;

	// Bottom Y's
	bool blyFirst = false, bryFirst = false;
	int lby = y2 * width - (width - location->bottom_left_x), rby = y2 * width - (width - location->bottom_right_x);

	// Find Y's
	while (!(tlyFirst && tryFirst && blyFirst && bryFirst)) {

		// Top Left Y
		if (!tlyFirst && image_data->image[lty] > 250) {
			tlyFirst = true;
			location->top_left_y = (int)(lty / width);
			// printf("Location : Top Left Y %u, lty : %u, Width : %u\n", location->top_left_y, lty, width );

		}
		else {
			lty += width;
			if (lty > y2 * width) {
				location->top_left_y = -1;
				tlyFirst = true;
				lty = 0;
			}
		}

		// Bottom Left Y
		if (!blyFirst && image_data->image[lby] > 250) {
			blyFirst = true;
			location->bottom_left_y = (int)(lby / width);
			// printf("Location : Bottom Left Y %u, lby : %u, Width : %u\n", location->bottom_left_y, lby, width);
		}
		else {
			lby -= width;
			//if (lby < y1 * width) { // todo
			if (lby < 0) {
				location->bottom_left_y = -1;
				blyFirst = true;
				lby = 0;
			}
		}

		// Bottom Right Y
		if (!bryFirst && image_data->image[rby] > 250) {
			bryFirst = true;
			location->bottom_right_y = (int)(rby / width);
			// printf("Location : Top Right Y %u, rby : %u, Width : %u\n", location->bottom_right_y, rby, width);
		}
		else {
			rby -= width;
			//if (rby < y1 * width) { // todo
			if (rby < 0) {
				location->bottom_right_y = -1;
				bryFirst = true;
				rby = 0;
			}
		}

		// Top Right Y
		if (!tryFirst && image_data->image[rty] > 250) {
			tryFirst = true;
			location->top_right_y = (int)(rty / width);
			// printf("Location : Top Right Y %u, rty : %u, Width : %u\n", location->bottom_left_y, rty, width);
		}
		else {
			rty += width;
			if (rty > y2 * width) { // todo
				location->top_right_y = -1;
				tryFirst = true;
				rty = 0;
			}
		}
	}

	delete[] X_histo;

	if (location->checkValidity())
	{
		return 0;
	}
	else
	{
		printf("Location has invalid data.\n");
		return -1;
	}
}

// This is Eddie's parabound algorithm 
int BoundaryDetection::para_bound_detect()
{
	Image_Store* image_data = vision_pipeline->segmented_imgout;
	int x1 = vision_pipeline->target_bounding_box->left;
	int y1 = vision_pipeline->target_bounding_box->top;
	int x2 = vision_pipeline->target_bounding_box->right;
	int y2 = vision_pipeline->target_bounding_box->bottom;

	TargetLocation* location = vision_pipeline->target_LOC;

	int* X_histo = new int[image_data->width];
	memset(X_histo, 0, image_data->width * sizeof(int));

	int add_constant = image_data->width - ((x2 - x1) + 0); // Possibly + 1;

	int greyThresh = 250;

	for (int i = x1 + (y1 * image_data->width); i < x2 + (y2 * image_data->width); i++) {

		// Correct Position
		if (i % image_data->width > x2) {
			i += add_constant;
		}

		// Set Histogram Info
		if (image_data->image[i] > greyThresh) {
			//Y_histo[(int)(i / image_data->width)]++;
			X_histo[i % image_data->width]++;
		}
	}

	// Find Points
	int totalValues = image_data->width * image_data->height;
	int width = image_data->width;

	// Top X's
	bool rXFirst = false, lXFirst = false;
	int lx = x1, rx = x2;

	// Find Top X's
	while (!(rXFirst && lXFirst)) {

		// Top Right X
		if (!rXFirst && X_histo[rx] > 1) {
			rXFirst = true;
			location->top_right_x = rx;
		}
		else {
			rx--;
			if (rx < 0) {
				rXFirst = true;
				location->top_right_x = -1;
				rx = 0;
			}
		}

		// Top Left X
		if (!lXFirst && X_histo[lx] > 1) {
			lXFirst = true;
			location->top_left_x = lx;
		}
		else {
			lx++;
			if (lx > width) {
				location->top_left_x = -1;
				lXFirst = true;
				lx = 0;
			}
		}
	}

	// Top Y's
	bool tlyFirst = false, tryFirst = false;
	int lty = location->top_left_x, rty = location->top_right_x;

	// Find Y's
	while (!(tlyFirst && tryFirst)) {

		// Top Left Y
		if (!tlyFirst && image_data->image[lty] > 250) {
			tlyFirst = true;
			location->top_left_y = (lty / width);
			// printf("Location : Top Left Y %u, lty : %u, Width : %u\n", location->top_left_y, lty, width );

		}
		else {
			lty += width;
			if (lty > y2 * width) {
				location->top_left_y = -1;
				tlyFirst = true;
				lty = 0;
			}
		}

		// Top Right Y
		if (!tryFirst && image_data->image[rty] > 250) {
			tryFirst = true;
			location->top_right_y = (rty / width);
			// printf("Location : Top Right Y %u, rty : %u, Width : %u\n", location->bottom_left_y, rty, width);
		}
		else {
			rty += width;
			if (rty > y2 * width) {
				location->top_right_y = -1;
				tryFirst = true;
				rty = 0;
			}
		}
	}

	// Get Midpoint
	double mx = (location->top_right_x + location->top_left_x) / 2, my;
	bool myTopFound = false, myBottomFound = false;
	int myTop = y1 * image_data->width + (int)mx, myBottom = (y2 - 1) * image_data->width + (int)mx;

	while (!(myTopFound && myBottomFound)) {

		// Middle Top Y
		if (!myTopFound && image_data->image[myTop] > 250) {
			myTopFound = true;
		}
		else if (!myTopFound) {
			myTop += image_data->width;
			if (myTop > y2 * image_data->width) {
				std::cout << "Middle Bar Not Found";
				location->bottom_left_x = -1;
				location->bottom_left_y = -1;
				location->bottom_right_x = -1;
				location->bottom_right_x = -1;
				return -1;
			}
		}

		// Middle Bottom Y
		if (!myBottomFound && image_data->image[myBottom] > 250) {
			myBottomFound = true;
		}
		else if (!myBottomFound) {
			myBottom -= image_data->width;
			if (myBottom < y1 * image_data->width) {
				std::cout << "Middle Bar Not Found";
				location->bottom_left_x = -1;
				location->bottom_left_y = -1;
				location->bottom_right_x = -1;
				location->bottom_right_x = -1;
				return -1;
			}
		}

	}

	my = (((myTop / image_data->width) + (myBottom / image_data->width)) / 2 + 1);

	// Get Slope
	double m = (double)(location->top_right_y - location->top_left_y) / (double)(location->top_right_x - location->top_left_x);
	double b = my - (m * mx);

	bool leftFound = false, rightFound = false;
	int lbx = (int)mx, rbx = (int)mx;
	double lby = my, rby = my;

	while (!(leftFound && rightFound)) { // ISSUE

		// Bottom Right
		rby = (m * rbx) + b;
		if (!rightFound && image_data->image[(int)(round(round(rby) * image_data->width)) + rbx] < 200) { // B ISSUE
			rightFound = true;
			location->bottom_right_x = rbx - 1;
			location->bottom_right_y = (int)((m * (rbx - 1)) + b);
		}
		else {
			rbx++;
			if (rbx > x2) {
				location->bottom_right_x = -1;
				location->bottom_right_y = -1;
				rightFound = true;
			}
		}

		// Bottom Left
		lby = (m * lbx) + b;
		if (!leftFound && image_data->image[(int)(round(round(lby) * image_data->width)) + lbx] < 250) {
			leftFound = true;
			location->bottom_left_x = lbx - 1;
			location->bottom_left_y = (int)((m * (lbx - 1)) + b);
		}
		else {
			lbx--;
			if (lbx < x1) {
				location->bottom_left_x = -1;
				location->bottom_left_y = -1;
				leftFound = true;
			}
		}
	}

	delete[] X_histo;

	if (location->checkValidity())
	{
		return 0;
	}
	else
	{
		printf("Location has invalid data.\n");
		return -1;
	}
}


int BoundaryDetection::divide_and_conquor()
{
	Image_Store* image_data = vision_pipeline->thresholded_out;
	int x1 = vision_pipeline->target_bounding_box->left;
	int y1 = vision_pipeline->target_bounding_box->top;
	int x2 = vision_pipeline->target_bounding_box->right;
	int y2 = vision_pipeline->target_bounding_box->bottom;

	TargetLocation* location = vision_pipeline->target_LOC;

	if (vision_pipeline->pipeline_config->getVisionLibDebugMode()) {
		//// Write out the image for Debug
		//char output_file[255];
		//sprintf( output_file, ".//Runtime_Capture//%05d_boundary_detect.pbm", vision_pipeline->VisionLib_debug_frame_counter );
		//write_image_store(image_data, PNM_FORMAT::BINARY, output_file);
	}

	// Do Some Sanity checking, make sure that we have a valid segment
	if (x2 == 0) {
		x2 = image_data->width - 1;
	}
	if (y2 == 0) {
		y2 = image_data->height - 1;
	}

	// Set up some variables to start with 
	uint LBx = x1;
	uint UBx = x2;
	uint LBy = y1;
	uint UBy = y2;

	// Top (Left and Right) Upper Bound X
	uint TLUBx = LBx-1;
	uint TRUBx = UBx+1;
	uint x_mid_point = (UBx - (UBx - LBx) / 2);

	// Pre-Compute a parameter
	uint sub_region_height = y2 - y1;
	uint sub_region_width_L = x_mid_point - x1 ;
	uint sub_region_width_R = x2 - x_mid_point;

	// Allocate the memeory for the two half projections
	LUT_1D x_proj_L( sub_region_width_L );
	LUT_1D y_proj_L( sub_region_height );

	LUT_1D x_proj_R( sub_region_width_R );
	LUT_1D y_proj_R( sub_region_height );

	// counter to keep track of the pixel location
	int pixel_offset = 0;
	int	y_pix_loc = 0;
	int x_pix_loc = 0;

	// Now compute the x_proj_L, y_proj_L, x_proj_R 
	// and y_proj_R projections
	for (int loop_row = y1; loop_row < y2; loop_row++)
	{
		// Compute the start location
		pixel_offset = loop_row * image_data->width + x1;
		x_pix_loc = 0;

		// These are the x_proj_L and y_proj_L
		for (int loop_col = x1; loop_col < (int) x_mid_point; loop_col++)
		{
			if (image_data->image[pixel_offset])
			{
				y_proj_L.y_data[y_pix_loc] ++;
				x_proj_L.y_data[x_pix_loc] ++;
			}
			pixel_offset++;
			x_pix_loc++;
		}

		// Reset the start location
		x_pix_loc = 0;

		// These are the x_proj_R and y_proj_R
		for (int loop_col = x_mid_point; loop_col < x2; loop_col++)
		{
			if (image_data->image[pixel_offset])
			{
				y_proj_R.y_data[y_pix_loc] ++;
				x_proj_R.y_data[x_pix_loc] ++;
			}
			pixel_offset++;
			x_pix_loc++;
		}

		// Increment the Y loc counter
		y_pix_loc++;
	}

	if (vision_pipeline->pipeline_config->getVisionLibDebugMode())
	{
		//// Print out the LUTs for debuging
		// y_proj_L.printLUT("Y proj L");
		//printf("\n");

		//// Print out the LUTs for debuging
		// y_proj_R.printLUT("Y proj L");
		//printf("\n");

		//// Print out the LUTs for debuging
		// x_proj_L.printLUT("X proj L");
		//printf("\n");

		//// Print out the LUTs for debuging
		// x_proj_R.printLUT("X proj R");
	}
	
	// Verify and Update the X Upper boundaries
	TLUBx = x1 + x_proj_L.returnFirst();
	TRUBx = x_mid_point + x_proj_R.returnLast();

	// Extract the Y direction points
	uint TLUBy = LBy + y_proj_L.returnFirst() ;
	uint TRUBy = LBy + y_proj_R.returnFirst() ;

	uint LRLBx, LLLBx;
	float scalar = (float) 1.20;

	// Make sure that the LUT is not Zero Length
	if (x_proj_L.points > 0)
	{
		// Now look for the inflection points in the X direction
		x_proj_L.normalizeLUT();
		int inflection_xL = 0;

		if (vision_pipeline->pipeline_config->getVisionLibDebugMode())
		{
			//// Print out the LUTs for debuging
			//x_proj_L.printLUT("Normalized X proj L");
			//printf("\n");
		}

		int LUT_length = x2 - (int)x_mid_point - 1;
		for (int i = LUT_length; i >= 0; i--) {
			if (x_proj_L.y_data[i] > (scalar * x_proj_L.y_data[LUT_length])) {
				inflection_xL = i;
				break;
			}
		}

		LLLBx = x_mid_point - (LUT_length - inflection_xL);
	}
	else
	{
		LLLBx = x_mid_point ;
	}


	// Make sure that the LUT is not Zero Length
	if (x_proj_R.points > 0)
	{
		// Now look for the inflection points in the X direction
		x_proj_R.normalizeLUT();
		int inflection_xR = 0;

		for (int i = 0; i < (int)x_mid_point - x1; i++) {
			if (x_proj_R.y_data[i] > (scalar * x_proj_R.y_data[0])) {
				inflection_xR = i;
				break;
			}
		}

		if (vision_pipeline->pipeline_config->getVisionLibDebugMode())
		{
			//// Print out the LUTs for debuging
			//x_proj_R.printLUT("Normalized X proj R");
			//printf("\n");
		}

		LRLBx = x_mid_point + inflection_xR;
	}
	else
	{
		LRLBx = x_mid_point;
	}

	// Now find the point in the Y direction where the inflection occurs
	uint LLLBy = 0;
	pixel_offset = y2 * image_data->width + LLLBx;
	for (int i = y2; i > y1; i--) {		
		if (image_data->image[pixel_offset]) {
			LLLBy = (uint) i;
			break;
		}
		pixel_offset -= image_data->width;
	}

	// Sanity Check to make sure that the Lower Bound is reasonable
	if (LLLBy == 0){
		LLLBy = y2;
	}
 
	uint LRLBy = 0;
	pixel_offset = y2 * image_data->width + LRLBx;
	for (int i = y2; i > y1; i--) {
		if (image_data->image[pixel_offset]) {
			LRLBy = (uint) i;
			break;
		}
		pixel_offset -= image_data->width;
	}

	// Sanity Check to make sure that the Lower Bound is reasonable
	if (LRLBy == 0) {
		LRLBy = y2;
	}

	// These are the final target co-ordinates

	location->bottom_left_x = LLLBx;
	location->bottom_left_y = LLLBy;
	location->bottom_right_x = LRLBx;
	location->bottom_right_y = LRLBy;

	location->top_left_x = TLUBx;
	location->top_left_y = TLUBy;
	location->top_right_x = TRUBx;
	location->top_right_y = TRUBy;

	// Update the bounding box information
	vision_pipeline->target_bounding_box->left = min_i(TLUBx, LLLBx);
	vision_pipeline->target_bounding_box->right = max_i(TRUBx, LRLBx);
	vision_pipeline->target_bounding_box->top = min_i(TLUBy, TRUBy);
	vision_pipeline->target_bounding_box->bottom = max_i(LLLBy, LRLBy);

	return EXIT_SUCCESS;
}

int BoundaryDetection::max_i(int a, int b) { 
	return a > b ? a : b; 
} 

int BoundaryDetection::min_i(int a, int b) { 
	return a < b ? a : b; 
}


