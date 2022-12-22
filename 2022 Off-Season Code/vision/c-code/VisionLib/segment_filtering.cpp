
#include "segment_filtering.hpp"
#include <math.h>
#include <string>
#include <sys/stat.h>
#include "annotate.h"
#include "boundary_detection.h"

const int MAX_ENTRIES_MARK = 256;


SegmentFiltering::~SegmentFiltering(void)
{
	// Cleans Up
	if (segment_stats != NULL) {
		delete[] segment_stats;
		segment_stats = NULL;
	}
}


// Constructor
SegmentFiltering::SegmentFiltering( Vision_Pipeline* vision_pipeline_data )
{
	vision_pipeline = vision_pipeline_data;
	segmentation_mode = vision_pipeline->pipeline_config->getTargetType();
}


// Overload Constructor
SegmentFiltering::SegmentFiltering(Vision_Pipeline* vision_pipeline_data, TARGET_DETECTION_TYPE mode )
{
	vision_pipeline = vision_pipeline_data;
	segmentation_mode = mode ;
}

int SegmentFiltering::vision_max_i(int a, int b) { return a > b ? a : b; }
int SegmentFiltering::vision_min_i(int a, int b) { return a < b ? a : b; }

double SegmentFiltering::vision_max_d(double a, double b) { return a < b ? b : a; }
double SegmentFiltering::vision_min_d(double a, double b) { return a < b ? a : b; }


int SegmentFiltering::extract_label_stats(int maxIndex)
{
	// Set up pointers
	Image_Store* scaledRGB_Image = vision_pipeline->scaled_image;
	Image_Store* thresholded_Image = vision_pipeline->thresholded_out;
	Image_Store* segmented_image = vision_pipeline->segmented_imgout;
	Image_Store* distance_in = vision_pipeline->distance_out;

	// Create an array of structs to hold the information
	// This gets cleaned up by the destructor

	if (segment_stats == NULL) {
		segment_stats = new segment_data[maxIndex];
		
		// Initalize the data to be "out of bounds"
		for (int i = 0; i < maxIndex; i++) {
			segment_stats[i].bounding_box[0] = scaledRGB_Image->width;
			segment_stats[i].bounding_box[1] = scaledRGB_Image->height;
			segment_stats[i].bounding_box[2] = 0;
			segment_stats[i].bounding_box[3] = 0;
		}
	}

	// Pre-calculate the row offset
	uint row_offset = boundary_region[0] * segmented_image->width;
	uint pixel_offset = 0, RGB_pixel_offset = 0, loop_row, loop_col;
	uint8 pixval;

	// Now go through the image a second time and merge the labels
	for (loop_row = boundary_region[0]; loop_row < boundary_region[2]; loop_row++)
	{
		// Intentionally skip the outer 1 pixel perimeter
		pixel_offset	 = row_offset + boundary_region[1];
		RGB_pixel_offset = pixel_offset * 3;

		// Remap the pixels in the region of interest
		for (loop_col = boundary_region[1]; loop_col < boundary_region[3]; loop_col++)
		{
			pixval = segmented_image->image[pixel_offset];

			if (pixval > 0) 
			{
				this->segment_stats[pixval].number_elements++;
				this->segment_stats[pixval].average_brightness += (double)distance_in->image[pixel_offset];
				this->segment_stats[pixval].segment_ID = (int)pixval;

				// Add the source RGB pixel values together
				this->segment_stats[pixval].average_RGB[0] += (double)scaledRGB_Image->image[RGB_pixel_offset++];
				this->segment_stats[pixval].average_RGB[1] += (double)scaledRGB_Image->image[RGB_pixel_offset++];
				this->segment_stats[pixval].average_RGB[2] += (double)scaledRGB_Image->image[RGB_pixel_offset++];

				// Determine the boundary for the segment
				// Update Left
				if (this->segment_stats[pixval].bounding_box[0] > loop_col+1) {
					this->segment_stats[pixval].bounding_box[0] = loop_col+1;
				}
				// Update Top
				if (this->segment_stats[pixval].bounding_box[1] > loop_row) {
					this->segment_stats[pixval].bounding_box[1] = loop_row;
				}
				// Update Right
				if (this->segment_stats[pixval].bounding_box[2] < loop_col) {
					this->segment_stats[pixval].bounding_box[2] = loop_col;
				}
				// Update Bottom
				if (this->segment_stats[pixval].bounding_box[3] < loop_row+1) {
					this->segment_stats[pixval].bounding_box[3] = loop_row+1;
				}

			} else {
				RGB_pixel_offset += 3;
			}

			// Increment the pixel pointer
			pixel_offset++;
		}

		// Increment the row offset
		row_offset += segmented_image->width;
	}

	// Run through and fix the normalization
	for (int i = 0; i < maxIndex; i++) 
	{
		// Compute the average brightness
		this->segment_stats[i].average_brightness = (1/2.55) * this->segment_stats[i].average_brightness / this->segment_stats[i].number_elements;

		//Compute the average RGB
		this->segment_stats[i].average_RGB[0] = this->segment_stats[i].average_RGB[0] / this->segment_stats[i].number_elements;
		this->segment_stats[i].average_RGB[1] = this->segment_stats[i].average_RGB[1] / this->segment_stats[i].number_elements;
		this->segment_stats[i].average_RGB[2] = this->segment_stats[i].average_RGB[2] / this->segment_stats[i].number_elements;
			
		// Now compute the average Lab and LCh
		uint8 RGB_in[3] = { (uint8) this->segment_stats[i].average_RGB[0] , (uint8) this->segment_stats[i].average_RGB[1] , (uint8) this->segment_stats[i].average_RGB[2] };
		vision_pipeline->CLUT_table->color_process_rgb_pixel( RGB_in, this->segment_stats[i].average_Lab, this->segment_stats[i].average_LCh );

		// Need to make sure that we dont end-up with zero width and height segements
		this->segment_stats[i].width = segment_stats[i].bounding_box[2] - segment_stats[i].bounding_box[0] + 1 ;
		this->segment_stats[i].height = segment_stats[i].bounding_box[3] - segment_stats[i].bounding_box[1] + 1 ;
		
		//Computes aspect ratio
		this->segment_stats[i].aspect_ratio = segment_stats[i].width / segment_stats[i].height;

		//Computes fullness 
		this->segment_stats[i].fullness = 100 * segment_stats[i].number_elements / (segment_stats[i].width * segment_stats[i].height);

		//Computes Chroma
		this->segment_stats[i].chroma = segment_stats[i].average_LCh[1];

		//Computes the relative size
		this->segment_stats[i].area_pct = 100 * (segment_stats[i].width * segment_stats[i].height) / (scaledRGB_Image->width * scaledRGB_Image->height);

		double hue = segment_stats[i].average_LCh[2];

		if ((hue > 0) && (hue <= 75)) {
			this->segment_stats[i].color = OBJECTCOLOR::RED;
		}
		else if ((hue > 75) && (hue <= 115)) {
			this->segment_stats[i].color = OBJECTCOLOR::YELLOW;
		}
		else if ((hue > 115) && (hue <= 200)) {
			this->segment_stats[i].color = OBJECTCOLOR::GREEN;
		}
		else if ((hue > 200) && (hue <= 360)) {
			this->segment_stats[i].color = OBJECTCOLOR::BLUE;
		}
		else {
			this->segment_stats[i].color = OBJECTCOLOR::UNDEFINED;
		}

	}


	if (vision_pipeline->pipeline_config->getVisionLibDebugMode())
	{

		printf("Displaying Segment Stats\n");
		printf("~~~~~~~~~~~~~~~~~~~~~~~~\n\n");

		// Display the Segment Statistics 
		for (int loop_seg = 0; loop_seg < maxIndex; loop_seg++)
		{
			displaySegmentStats(loop_seg);
		}
	}

	return maxIndex;
}


int SegmentFiltering::extract_Hexagon_location(int maxIndex)
{
	Image_Store* segmented_image = vision_pipeline->segmented_imgout;
	Image_Store* distance_in = vision_pipeline->distance_out;
	BoundingBox* bounding_box = vision_pipeline->target_bounding_box;

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
		numberOfObjects = 0;
		return EXIT_FAILURE;
	}

	// Extract the bounding box for the segment of interest
	bounding_box->left = segment_stats[best].bounding_box[0];
	bounding_box->top = segment_stats[best].bounding_box[1];
	bounding_box->right = segment_stats[best].bounding_box[2];
	bounding_box->bottom = segment_stats[best].bounding_box[3];

	// Hard code that we have found only 1 object
	numberOfObjects = 1;

	return EXIT_SUCCESS;
}


int SegmentFiltering::extract_MultiObject_location(int maxIndex, int* numSegments)
{

	// Get the pointers to each image
	Image_Store*   segmented_image = vision_pipeline->segmented_imgout;
	Image_Store*   distance_in = vision_pipeline->distance_out;
	ObjectLocations* object_locations = &(vision_pipeline->object_location);
	bool  debug_Mode = vision_pipeline->pipeline_config->getVisionLibDebugMode();

	// finish calculating averages
	const double tolerance = 0.3;
	int numObjectsFound = 0;						// number of segments that are "yellow enough"
	int objectsFoundList[MAX_ENTRIES_MARK] = {0};	// indexed in order by occurrance, integer segment index
	int objectsFoundList_i = 0;

	//// Find the brightest segment
	//double brightnessNormFactor = 0;
	//{
	//	for (int i = 1; i < maxIndex; i++) {
	//		if (this->segment_stats[i].number_elements > 0) {
	//			brightnessNormFactor = vision_max_d(brightnessNormFactor, segment_stats[i].average_brightness);
	//		}
	//	}
	//}

	double brightness_control[2] = { 0,1 };
	double aspect_ratio_control[2] = { 0,1 };
	double fullness_control[2] = { 0,1 };
	double chroma_control[2] = { 0,1 };
	double percent_area_control[2] = { 0,1 };
	OBJECTCOLOR segment_color = OBJECTCOLOR::UNDEFINED;

	vision_pipeline->pipeline_config->getBrightnessRange(&brightness_control[0], &brightness_control[1]);
	vision_pipeline->pipeline_config->getAspectRatioRange(&aspect_ratio_control[0], &aspect_ratio_control[1]);
	vision_pipeline->pipeline_config->getFullnessRange(&fullness_control[0], &fullness_control[1]);
	vision_pipeline->pipeline_config->getChromaRange(&chroma_control[0], &chroma_control[1]);
	vision_pipeline->pipeline_config->getPercentAreaRange( &percent_area_control[0], &percent_area_control[1]);
	vision_pipeline->pipeline_config->getColorFilter(&segment_color);

	// // Make sure that the brightness is normalized [ 0 - 100 ]
	// brightnessNormFactor = 100 * (1 / brightnessNormFactor);

	// Find the segments that exceed the minimum required to be a "valid segment"
	// const double minBrighness = maxBrightness - maxBrightness * tolerance;
	for (int i = 1; i < maxIndex; i++) 
	{	
		bool candidate = true;
				
		// Checking the brightness of the object
		// if (segment_stats[i].average_brightness < minBrighness || segment_stats[i].average_brightness > maxBrightness) {
		// if (segment_stats[i].average_brightness * brightnessNormFactor < brightness_control[0] || segment_stats[i].average_brightness * brightnessNormFactor > brightness_control[1]) {
		if (segment_stats[i].average_brightness < brightness_control[0] || segment_stats[i].average_brightness > brightness_control[1]) {
			candidate = false;
			if (debug_Mode) {
				printf("\tBrightness : False\n");
			}
		};			


		// Checking the percent area of the full image 
		// if (segment_stats[i].area_pct < 0.0005 || segment_stats[i].area_pct > 0.01) {
		if (segment_stats[i].area_pct < percent_area_control[0] || segment_stats[i].area_pct > percent_area_control[1] ) {
			candidate = false;
			if (debug_Mode) {
				printf("\tPercent Area : False\n");
			}
		};

		// Checking the fullness criteria
		// if (segment_stats[i].fullness < 0.5 || segment_stats[i].fullness > 1.0) {
		if ( segment_stats[i].fullness < fullness_control[0] || segment_stats[i].fullness > fullness_control[1] ) {
			candidate = false;
			if (debug_Mode) {
				printf("\tFullness : False\n");
			}
		};

		// Checking the Chroma criteria
		// if (segment_stats[i].chroma < 0.5 || segment_stats[i].chroma > 1.0) {
		if (segment_stats[i].chroma < chroma_control[0] || segment_stats[i].chroma > chroma_control[1]) {
			candidate = false;
			if (debug_Mode) {
				printf("\tChroma : False\n");
			}
		};

		// Checking the aspect ratio
		// if (segment_stats[i].aspect_ratio < 0.75 || segment_stats[i].aspect_ratio > 2.5) {
		if (segment_stats[i].aspect_ratio < aspect_ratio_control[0] || segment_stats[i].aspect_ratio > aspect_ratio_control[1] ) {
			candidate = false;
			if (debug_Mode) {
				printf("\tAspect Ratio : False\n");
			}
		};

		// Check the Color
		if (segment_color != OBJECTCOLOR::UNDEFINED)
		{
			if (segment_stats[i].color != segment_color ) {
				candidate = false;
				if (debug_Mode) {
					printf("\tObject Color : False\n");
				}
			};
		}

		// printf("Candidate Segment : %d\n", candidate);

		// If none of the above were true, we have a candidate ball
		if ( candidate ==  true )
		{
			objectsFoundList[objectsFoundList_i++] = i;
			numObjectsFound++;
		}
	}

	if ( numObjectsFound == 0 )
	{
		// Only display if we cant find objects
		for (int i = 1; i < maxIndex; i++)
		{
			// Could not find an object, so diplay the stats
			displaySegmentStats(i);
		}
	}

	// Reset the Number of Object Count 
	numberOfObjects = 0;

	// Setting a limit to the number of ball locations
	if (numObjectsFound >= MAX_BOUNDING_BOXES)
		numObjectsFound = MAX_BOUNDING_BOXES - 1;
	{
		for (int i = 0; i < numObjectsFound; i++)
		{
			BoundingBox * thisLocation = &object_locations->bounding_box[i];

			// Copy over the location information
			thisLocation->left = segment_stats[ objectsFoundList[i] ].bounding_box[0];
			thisLocation->top = segment_stats[ objectsFoundList[i] ].bounding_box[1] ;
			thisLocation->right = segment_stats[ objectsFoundList[i] ].bounding_box[2];
			thisLocation->bottom = segment_stats[ objectsFoundList[i] ].bounding_box[3] ;

			// Copy over the color information
			object_locations->object_color[i] = segment_stats[objectsFoundList[i]].color;

			get_center(thisLocation, &object_locations->center_location[i]);
			
			// Increment the Counter
			numberOfObjects++;
		}
	}

	// Update the class with the number of balls
	vision_pipeline->object_location.objects_found = numberOfObjects;

	*numSegments = numberOfObjects;

	return EXIT_SUCCESS;
}


int SegmentFiltering::filterSegmentData( int unique_segments, uint* boundary )
{
	// Copy over the boundary information
	for (int i = 0; i < 4; i++) {
		boundary_region[i] = boundary[i];
	}

	if ( unique_segments == 1)
	{
		printf("No Segments Found\n");
		return EXIT_FAILURE;
	}
	else {
		if (vision_pipeline->pipeline_config->getVisionLibDebugMode()) {
			printf("Number of Components : %d\n", unique_segments - 1);
		}
	}

	// Now extract the label stats
	extract_label_stats( unique_segments );

	switch (segmentation_mode)
	{
		case TARGET_DETECTION_TYPE::HEXAGON :
		{
			// Call into the code to extract the target location
			extract_Hexagon_location(unique_segments);

			break;
		}

		case TARGET_DETECTION_TYPE::BALL :
		{
			// Call into the code to extract the target location
			int numSegments; // use this at your own risk
			extract_MultiObject_location(unique_segments, &numSegments); //make bounding_box an array for this method

			// Display the number of segments&
			printf("Number of Objects Found : %d\n", numSegments);

			break;
		}

		case TARGET_DETECTION_TYPE::HUB :
		{
			// Call into the code to extract the target location
			int numSegments; // use this at your own risk
			extract_MultiObject_location(unique_segments, &numSegments); //make bounding_box an array for this method

			Location TL, BR;
			TL.x = vision_pipeline->scaled_image->width; TL.y = vision_pipeline->scaled_image->height;
			BR.x = 0; BR.y = 0;

			// Find the bounding box center
			for (int loop_object = 0; loop_object < vision_pipeline->object_location.objects_found; loop_object++)
			{

				// printf("Top, Left     : %d %d\n", vision_pipeline->object_location.bounding_box[loop_object].left, vision_pipeline->object_location.bounding_box[loop_object].top );
				// printf("Bottom, Right : %d %d\n", vision_pipeline->object_location.bounding_box[loop_object].bottom, vision_pipeline->object_location.bounding_box[loop_object].right );

				// Top Left
				if ( vision_pipeline->object_location.bounding_box[loop_object].left < TL.x) {
					TL.x = vision_pipeline->object_location.bounding_box[loop_object].left;
				}
				if (vision_pipeline->object_location.bounding_box[loop_object].top < TL.y) {
					TL.y = vision_pipeline->object_location.bounding_box[loop_object].top;
				}

				// Bottom Right
				if (vision_pipeline->object_location.bounding_box[loop_object].right > BR.x) {
					BR.x = vision_pipeline->object_location.bounding_box[loop_object].right;
				}
				if (vision_pipeline->object_location.bounding_box[loop_object].bottom > BR.y) {
					BR.y = vision_pipeline->object_location.bounding_box[loop_object].bottom;
				}
			}

			// Update the target bounding box
			if (numSegments == 0) {
				vision_pipeline->target_LOC->resetData();
			} 
			else {
				vision_pipeline->target_LOC->top_left_x = TL.x;
				vision_pipeline->target_LOC->top_left_y = TL.y;
				vision_pipeline->target_LOC->top_right_x = BR.x;
				vision_pipeline->target_LOC->top_right_y = TL.y;
				vision_pipeline->target_LOC->bottom_left_x = TL.x;
				vision_pipeline->target_LOC->bottom_left_y = BR.y;
				vision_pipeline->target_LOC->bottom_right_x = BR.x;
				vision_pipeline->target_LOC->bottom_right_y = BR.y;
			}

			// Ensure that we are setting the scale factor element
			vision_pipeline->target_LOC->setScalefactor(vision_pipeline->pipeline_config->getScaleFactor());

			if (vision_pipeline->pipeline_config->getVisionLibDebugMode()) {
				// Display the number of segments
				printf("Number of Objects Found : %d\n", numSegments);
			}

			break;
		}
	}

	return EXIT_SUCCESS;

}

int SegmentFiltering::selectObjects()
{
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
		// Write out the segmented image
		char output_image_path[255];
		sprintf(output_image_path, ".//Runtime_Capture//%05d_segmented_%s.pnm", vision_pipeline->pipeline_config->getVisionLibFrameCounter(), vision_pipeline->pipeline_config->getVisionLibDebugStr());
		write_image_store(vision_pipeline->segmented_imgout, PNM_FORMAT::BINARY, output_image_path);
	}

	switch (vision_pipeline->pipeline_config->getTargetType())
	{
		case TARGET_DETECTION_TYPE::HEXAGON:
		{
			// Hexagon Case
			BoundaryDetection boundaryDetect(vision_pipeline);
			boundaryDetect.computeBoundary();

			// Assuming the image is to be captured, make sure the directory exists
			if (doesPathExist("Runtime_Capture") && capture_debug_image)
			{
				// Paint the Annotation
				int result = annotateTargetImage(vision_pipeline);

				char output_image_path[255];
				sprintf(output_image_path, ".//Runtime_Capture//%05d_run_target_%s.pnm", vision_pipeline->pipeline_config->getVisionLibFrameCounter(), vision_pipeline->pipeline_config->getVisionLibDebugStr());
				write_image_store(vision_pipeline->annotated_image, PNM_FORMAT::BINARY, output_image_path);
			}
			
			break;
		}
		case TARGET_DETECTION_TYPE::BALL:
		{
			// Print out debug information
			if (vision_pipeline->pipeline_config->getVisionLibDebugMode()) {
				vision_pipeline->printObjectLocations();
			}

			// Print out the number of balls found
			printf("Objects Found : %d\n", numberOfObjects);

			// Assuming the image is to be captured, make sure the directory exists
			if (doesPathExist("Runtime_Capture") && capture_debug_image)
			{
				// Paint the Annotation
				annotateBallCamImage(vision_pipeline);

				std::cout << "\tAnnotating Image, Objects Found: " << vision_pipeline->object_location.objects_found << "\n";

				char output_image_path[255];
				sprintf(output_image_path, ".//Runtime_Capture//%05d_object_loc_%s.pnm", vision_pipeline->pipeline_config->getVisionLibFrameCounter(), vision_pipeline->pipeline_config->getVisionLibDebugStr());
				write_image_store(vision_pipeline->annotated_image, PNM_FORMAT::BINARY, output_image_path);
			}
			break;
		}
		case TARGET_DETECTION_TYPE::HUB:
		{
			// Ball Case

			// Print out debug information
			if (vision_pipeline->pipeline_config->getVisionLibDebugMode()) {
				// Print out the number of objects found
				printf("Objects Found : %d\n", numberOfObjects);
				vision_pipeline->printObjectLocations();
			}
			else if (numberOfObjects == 0) {
				printf("Objects Found : %d\n", numberOfObjects);
			}

			// Assuming the image is to be captured, make sure the directory exists
			if (doesPathExist("Runtime_Capture") && capture_debug_image)
			{
				// Paint the Annotation
				annotateHubCamImage(vision_pipeline);

				std::cout << "\tAnnotating Image, Objects Found: " << vision_pipeline->object_location.objects_found << "\n";

				char output_image_path[255];
				sprintf(output_image_path, ".//Runtime_Capture//%05d_object_loc_%s.pnm", vision_pipeline->pipeline_config->getVisionLibFrameCounter(), vision_pipeline->pipeline_config->getVisionLibDebugStr());
				write_image_store(vision_pipeline->annotated_image, PNM_FORMAT::BINARY, output_image_path);
			}

			break;
		}
		default:
		{
			// Error Target Type is not known
			std::cout << "Target Type is NOT KNOWN\n";
		}
	}
	return EXIT_SUCCESS;
}


bool SegmentFiltering::doesPathExist(const std::string& s)
{
	struct stat buffer;
	return (stat(s.c_str(), &buffer) == 0);
}

void SegmentFiltering::displaySegmentStats( int selectedSegment )
{
	if (this->segment_stats[selectedSegment].number_elements > 0)
	{
		char myColor[255] = { 0 };
		getObjectColorString( segment_stats[selectedSegment].color, myColor );

		printf("Segment Number     : %d\n", this->segment_stats[selectedSegment].segment_ID);
		printf("Segment Size       : %f\n", this->segment_stats[selectedSegment].number_elements);
		printf("Segment Brightness : %f\n", this->segment_stats[selectedSegment].average_brightness);
		printf("Segment RGB        : R %5.3g , G %5.3g , B %5.3g\n", this->segment_stats[selectedSegment].average_RGB[0], this->segment_stats[selectedSegment].average_RGB[1], this->segment_stats[selectedSegment].average_RGB[2]);
		printf("Segment Lab        : L %5.3g , a %5.3g , b %5.3g\n", this->segment_stats[selectedSegment].average_Lab[0], this->segment_stats[selectedSegment].average_Lab[1], this->segment_stats[selectedSegment].average_Lab[2]);
		printf("Segment LCh        : L %5.3g , C %5.3g , h %5.3g\n", this->segment_stats[selectedSegment].average_LCh[0], this->segment_stats[selectedSegment].average_LCh[1], this->segment_stats[selectedSegment].average_LCh[2]);
		printf("Segment Width      : %g \n", segment_stats[selectedSegment].width);
		printf("Segment Heigth     : %g \n", segment_stats[selectedSegment].height);
		printf("Bounding Box       : %u %u %u %u\n", this->segment_stats[selectedSegment].bounding_box[0], this->segment_stats[selectedSegment].bounding_box[1], this->segment_stats[selectedSegment].bounding_box[2], this->segment_stats[selectedSegment].bounding_box[3]);
		printf("Aspect Ratio       : %g \n", segment_stats[selectedSegment].aspect_ratio);
		printf("Fullness           : %g \n", segment_stats[selectedSegment].fullness);
		printf("Chroma             : %g \n", segment_stats[selectedSegment].chroma);
		printf("Percent Area       : %g \n", segment_stats[selectedSegment].area_pct);
		printf("Object Color       : %s \n", myColor );
		printf("\n");
	}
}


void SegmentFiltering::get_center(BoundingBox* box, Location* center) {

	//printf("Left, Right, Top, Bottom : %d\t%d\t%d\t%d\n",
	//	box->left, box->right, box->top, box->bottom);

	center->x = (box->left + box->right) / 2;
	center->y = (box->top + box->bottom) / 2;

	// printf("Center X : %d\nCenter Y : %d\n", center->x, center->y);

}


