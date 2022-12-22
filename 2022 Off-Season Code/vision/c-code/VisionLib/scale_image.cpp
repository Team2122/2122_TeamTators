
#include "scale_image.hpp"
#include "Vision_Pipeline.hpp"
#include <string>
#include <sys/stat.h>
#include <thread>
#include "pnm.hpp"

void image_resize_rotationHelper(Image_Store* scaled_image, Image_Store* cropped_image, int heightOffset, int stripHeight) ; 
void eddieImageScale(Image_Store* image_data_in, Image_Store* scaled_image_data, double effSF, int* channel_offset);
void markImageScale(Image_Store* image_data_in, Image_Store* scaled_image_data, int* crop_offsets, int strip_offset,
					int number_of_strips, double effSF, int* channel_offset, int* min_max_RGB);


ScaleImage::~ScaleImage(void)
{
	// Default Destructor - cleans up
}

ScaleImage::ScaleImage(void)
{
	// Default Constructor as a helper class
}


// Overload Constructor
ScaleImage::ScaleImage( Vision_Pipeline* vision_pipeline_data )
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

	//Getting the current crop values from the pipeline config
	 crop_percent[0] =  vision_pipeline->pipeline_config->getCameraCropLeft();
	 crop_percent[1] = vision_pipeline->pipeline_config->getCameraCropRight();
	 crop_percent[2] = vision_pipeline->pipeline_config->getCameraCropTop();
	 crop_percent[3] = vision_pipeline->pipeline_config->getCameraCropBottom();
}

void ScaleImage::scale( Image_Store* input_image, SCALING_METHOD method )
{

	switch (method)
	{
	case SCALING_METHOD::SUBSAMPLE:
		image_resize(input_image);
		break;

	case SCALING_METHOD::BLOCK_AVERAGE:
		break;
	case SCALING_METHOD::LINEAR_INTERP:
		break;
	default:
		break;
	}

}


bool ScaleImage::doesPathExist(const std::string& s)
{
	struct stat buffer;
	return (stat(s.c_str(), &buffer) == 0);
}



int ScaleImage::image_resize(Image_Store* image_data)
{
	int ret_val = 0;
	bool debugState = vision_pipeline->pipeline_config->getVisionLibDebugMode();
	Image_Store* scaledImage = vision_pipeline->scaled_image;

	// First we scale the image, irrespective of orientation
	ret_val = image_resize(image_data, scaledImage, vision_pipeline->pipeline_config->getScaleFactor(), debugState);

	// Now Rotate if necessary
	if ((vision_pipeline->pipeline_config->getSensorOrientation() == SENSOR_ORIENTATION::PORTRAIT) ||
		(vision_pipeline->pipeline_config->getSensorOrientation() == SENSOR_ORIENTATION::PORTRAIT_UD)) {

		image_rotate( scaledImage, debugState );
	}

	return ret_val;
}


void image_resize_rotationHelper( Image_Store* scaled_image, Image_Store* rotated_image, int heightOffset, int stripHeight )
{
	// Initialize the offset
	int scanline_offset = 0;
	int flip_offset = 0;
	int new_offset = 0;
	int pixel_offset = 0;

	for (int loop_col = heightOffset; loop_col < heightOffset + stripHeight; loop_col++)
	{
		// Increment the destination pixel offest
		pixel_offset = loop_col * rotated_image->width * rotated_image->planes;
		scanline_offset = 0 ;

		for (int loop_row = 0; loop_row < scaled_image->height; loop_row++)
		{
			// Calculate the source pixel offset
			new_offset = (loop_col + scanline_offset) * rotated_image->planes;

			// Calculate the flipped pixel_offset
			flip_offset = pixel_offset + ((scaled_image->height - 1 - loop_row) * rotated_image->planes);

			// Copy over the image pixels
			rotated_image->image[flip_offset] = scaled_image->image[new_offset++];
			rotated_image->image[flip_offset + 1] = scaled_image->image[new_offset++];
			rotated_image->image[flip_offset + 2] = scaled_image->image[new_offset++];

			scanline_offset += scaled_image->width;

		}

	}

	//char output_file[255];
	//sprintf(output_file, ".//Runtime_Capture//%05d_Rotated_Scaled_%s.ppm", VisionLib_debug_frame_counter, VisionLib_debug_output_file_str);
	//write_image_store(scaled_image, PNM_FORMAT::BINARY, output_file);

}


int ScaleImage::image_resize(Image_Store* image_data, Image_Store* scaled_image, double scale_factor, bool debug_mode)
{
	bool execServiceEnabled = vision_pipeline->pipeline_config->getExecutorService();
	bool multiThreadedEnabled = vision_pipeline->pipeline_config->getMultiThreaded();
	bool debugState = vision_pipeline->pipeline_config->getVisionLibDebugMode();

	ExecutorService* exec_service = vision_pipeline->exec_service;

	// Need to assign the color planes, because some images come in as BGR
	// so we will flip the channels to RGB
	int channel_offset[3];
	int r, g, b;

	if (image_data->pixel_format == PIXEL_FORMAT::RGB)
	{
		channel_offset[0] = 0;
		channel_offset[1] = 1;
		channel_offset[2] = 2;
	}
	else if (image_data->pixel_format == PIXEL_FORMAT::BGR)
	{
		channel_offset[0] = 2;
		channel_offset[1] = 1;
		channel_offset[2] = 0;
	}
	else
	{
		printf("Pixel Format must be RGB or BGR!\n");
		return -1;
	}

	// Experiment with a crop region
	int crop_region[4] = { 0 };

	// Left, Right, Top, Bottom
	crop_region[0] = (int) ( image_data->width * crop_percent[0] );
	crop_region[1] = (int) ( image_data->width - (image_data->width * crop_percent[1]) );
	crop_region[2] = (int) ( image_data->height * crop_percent[2] );
	crop_region[3] = (int) ( image_data->height - (image_data->height * crop_percent[3]) );

	// printf("Crop Region : L %g  R %g T %g B %g\n", crop_percent[0], crop_percent[1], crop_percent[2], crop_percent[3]);

	// Area Effective Scale Factor
	double effSF = sqrt(1 / scale_factor);

	int cropped_width = (int) ( ( crop_region[1] - crop_region[0] ) / effSF ) ;
	int cropped_height = (int) ( ( crop_region[3] - crop_region[2] ) / effSF ) ;

	// Setting scaled_image Attributes
	scaled_image->set_attributes( cropped_width, cropped_height, image_data->planes, image_data->max_val, 
								  PIXEL_FORMAT::RGB, INTERLEAVE_FORMAT::PIXEL_INTERLEAVED);
	scaled_image->allocate_memory();

	// Run through the image and extract the min/max and convert BGR to RGB if necessary
	if (scale_factor == 1)
	{			

		if (scaled_image->max_val == 0)
			scaled_image->max_val = 255;

		// Initalize the values (Crazy to force update)
		scaled_image->max_RGB = 0;
		scaled_image->min_RGB = 255;

		if (scaled_image->interleave != INTERLEAVE_FORMAT::PIXEL_INTERLEAVED) {
			printf("no support for non-pixel interleaved formats!\n");
			return -1;
		}

		uint8* image_row_offset = image_data->image + (image_data->width * image_data->planes * crop_region[2] );
		uint8* scaled_row_offset = scaled_image->image;

		for (int loop_row = crop_region[2]; loop_row < crop_region[3]; loop_row++)
		{
			// Now Copy the cropped scanline data
			memcpy( scaled_row_offset, image_row_offset + ( crop_region[0] * image_data->planes ), sizeof(uint8) * image_data->planes * cropped_width );

			// Loop through the columns, swapping if needed
			for (int loop_col = 0; loop_col < (scaled_image->planes*cropped_width); loop_col+=3)
			{
				// Extract the pixel values 
				// either RGB or BGR
				r = *(scaled_row_offset + loop_col + channel_offset[0]);
				g = *(scaled_row_offset + loop_col + channel_offset[1]);
				b = *(scaled_row_offset + loop_col + channel_offset[2]);

				// Increment the histogram counters
				this->image_hist[r] ++;
				this->image_hist[g] ++;
				this->image_hist[b] ++;

				// Dont need to copy over Green, not necessary
				*(scaled_row_offset + loop_col + 0) = r;
				*(scaled_row_offset + loop_col + 2) = b;
			}

			// Calculate the scanline offset
			image_row_offset += (image_data->width * image_data->planes);
			scaled_row_offset += (scaled_image->width * scaled_image->planes);
		}
		
		// Now extract the hist min/max
		scaled_image->max_RGB = returnLast(this->image_hist, 256);
		scaled_image->min_RGB = returnFirst(this->image_hist, 256);

		if (false) {
			display_LUT(this->image_hist, "Histogram", 256);
		}

		image_data->min_RGB = scaled_image->min_RGB;
		image_data->max_RGB = scaled_image->max_RGB;
	}
	else 
	{
		// printf(" I am here : 314\n");

		if (scaled_image->max_val == 0)
			scaled_image->max_val = 255;

		// Initalize the values (Crazy to force update)
		scaled_image->max_RGB = 0;
		scaled_image->min_RGB = 255;

		// printf(" I am here : 332\n");

		// multiThreadedEnabled = false;

		if (multiThreadedEnabled)
		{
			// Split the image up into multiple strips and run them
			int num_strips = vision_pipeline->pipeline_config->getNumberOfThreads();
			int strip_height = scaled_image->height / num_strips;

			// printf(" NumStrips : %d\n", num_strips );

			// Calculate the strip start and length
			int* strip_offset = new int[num_strips];
			int* strip_length = new int[num_strips];
			int* min_max_arr  = new int[num_strips*2];
			
			// printf(" I am here : 345\n");

			// Initialize the values
			for (int i = 0; i < num_strips; i++) {
				min_max_arr[i * 2] = 255;
				min_max_arr[i * 2 + 1] = 0;
				strip_offset[i] = i * strip_height;
				strip_length[i] = strip_height;
			}

			if ((num_strips * strip_height) < scaled_image->height) {
				strip_length[num_strips - 1] = (strip_height + (scaled_image->height - (num_strips * strip_height)));
			}

			// printf(" I am here : 361\n");

			// Allocate array to store the pointer locations to the 
			std::thread** threads = new std::thread * [num_strips];

			for (int i = 0; i < num_strips; i++) {
				threads[i] = new std::thread( markImageScale, image_data, scaled_image, crop_region, strip_offset[i], strip_length[i], effSF, channel_offset, &(min_max_arr[i*2]) );
			}

			for (int i = 0; i < num_strips; i++) {
				// Joint the streams
				threads[i]->join();
			}

			for (int i = 0; i < num_strips; i++) {

				//if (debugState) {
				//	printf("Strip : %d , Min RGB : %d \t", i, min_max_arr[i * 2]);
				//	printf("Strip : %d , Max RGB : %d\n", i, min_max_arr[i * 2 + 1]);
				//}

				// Go through and calculate the global min max
				scaled_image->min_RGB = min(scaled_image->min_RGB, min_max_arr[i*2]);
				scaled_image->max_RGB = max(scaled_image->max_RGB, min_max_arr[i*2+1]);
			}

			// printf(" I am here : 385\n");

			// Cleanup
			delete[] threads;
			delete[] strip_offset;
			delete[] strip_length;
			delete[] min_max_arr;
		}
		else
		{
			// Initalize the Values
			int min_max_RGB[2] = { 255, 0 };

			// Scale the image using Eddie's Approach
			// eddieImageScale(image_data, scaled_image, effSF, channel_offset);
			
			// printf(" I am here : 398\n");

			// Scale the image using Mark's Approach
			markImageScale(image_data, scaled_image, crop_region, 0, scaled_image->height, effSF, channel_offset, min_max_RGB);

			// Copy over the min max values
			scaled_image->min_RGB = min_max_RGB[0];
			scaled_image->max_RGB = min_max_RGB[1];
		}

		image_data->min_RGB = scaled_image->min_RGB;
		image_data->max_RGB = scaled_image->max_RGB;

	}

	if (0) {
		// Temporary debug
		char output_file[255];

		sprintf(output_file, ".//Runtime_Capture//Original.ppm");
		write_image_store(image_data, PNM_FORMAT::BINARY, output_file);

		sprintf(output_file, ".//Runtime_Capture//Cropped_Scaled.ppm");
		write_image_store(scaled_image, PNM_FORMAT::BINARY, output_file);
	}

	return 0;
}

int ScaleImage::returnFirst( int* LUT_data, int points )
{
	int location = 0;

	// Run through the LUT entries and find the first that meets the criteria ( > 0 )
	for (int i = 0; i < points; i++) {
		location = i;
		if (LUT_data[location] > 0) {
			break;
		}
	}
	return location;
}


int ScaleImage::returnLast(int* LUT_data, int points)
{
	int location = 0;

	// Run through the LUT entries and find the first that meets the criteria ( > 0 )
	for (int i = 0; i < points; i++) {
		location = points - i - 1;
		if (LUT_data[location] > 0) {
			break;
		}
	}
	return location;
}


int ScaleImage::image_rotate( Image_Store* scaledImage, bool debugState)
{

	bool execServiceEnabled = vision_pipeline->pipeline_config->getExecutorService();
	bool multiThreadedEnabled = vision_pipeline->pipeline_config->getMultiThreaded();
	ExecutorService* exec_service = vision_pipeline->exec_service;

	// Set Up temporary rotated image on heap
	int rotatedImageWidth = scaledImage->height;
	int rotatedImageHeight = scaledImage->width;

	// printf("I am here : 90\n");

	Image_Store* rotatedImage = new Image_Store(rotatedImageWidth, rotatedImageHeight, scaledImage->planes,
												scaledImage->max_val, scaledImage->pixel_format,
												scaledImage->interleave);

	rotatedImage->min_RGB = scaledImage->min_RGB;
	rotatedImage->max_RGB = scaledImage->max_RGB;
	rotatedImage->allocate_memory();

	if (multiThreadedEnabled)
	{
		// Split the image up into multiple strips and run them
		int num_strips = vision_pipeline->pipeline_config->getNumberOfThreads();
		int strip_height = rotatedImageHeight / num_strips;

		// Calculate the strip start and length
		int* strip_offset = new int[num_strips];
		int* strip_length = new int[num_strips];

		for (int i = 0; i < num_strips; i++) {
			strip_offset[i] = i * strip_height;
			strip_length[i] = strip_height;
		}

		if ((num_strips * strip_height) < rotatedImageHeight) {
			strip_length[num_strips - 1] = (strip_height + (rotatedImageHeight - (num_strips * strip_height)));
		}

		if (execServiceEnabled) // Using executor service
		{
			// Allocate array to store the pointer locations to the threads

			if (debugState)
				std::cout << "Using ExecutorService\n";

			for (int i = 0; i < num_strips; i++)
			{
				// Create a lambda instance 
				std::function<int()>* lambda;

				if (debugState) {
					// printf("Processing Pixels Thread %d\n", i);
				}

				int this_strip_offset = strip_offset[i];
				int this_strip_length = strip_length[i];

				lambda = new std::function<int()>();
				*lambda = [scaledImage, rotatedImage, this_strip_offset, this_strip_length]()
				{
					image_resize_rotationHelper(scaledImage, rotatedImage, this_strip_offset, this_strip_length);
					return EXIT_SUCCESS;
				};

				// Register the task
				exec_service->addLambdaTask(lambda);
			}

			exec_service->registerThisThread(true);
		}
		else // Not using the Executor Service
		{
			// Allocate array to store the pointer locations to the threads
			std::thread** threads = new std::thread * [num_strips];

			for (int i = 0; i < num_strips; i++)
			{
				threads[i] = new std::thread(image_resize_rotationHelper, scaledImage, rotatedImage, strip_offset[i], strip_length[i]);
			}

			for (int i = 0; i < num_strips; i++) {
				// Joint the streams
				threads[i]->join();
			}

			// Cleanup
			delete[] threads;
		}

		// Cleanup
		delete[] strip_offset;
		delete[] strip_length;
	}
	else
	{
		// Run the image through on a single thread
		image_resize_rotationHelper(scaledImage, rotatedImage, 0, rotatedImageHeight);
	}

	// Now need to re-shuffle the pointers so we dont loose the memory we have just allocated
	// when this goes out of scope 

	// printf("I am here : 181\n");

	Image_Store* originalScaledImageBuffer = scaledImage;
	vision_pipeline->scaled_image = rotatedImage;
	rotatedImage = originalScaledImageBuffer;

	// printf("I am here : 187\n");

	// Now clean up
	delete rotatedImage;

	if (0) {
		// Temporary debug
		char output_file[255];
		sprintf(output_file, ".//Runtime_Capture//Cropped_Scaled_Rotated.ppm");
		write_image_store(vision_pipeline->scaled_image, PNM_FORMAT::BINARY, output_file);
	}

	return EXIT_SUCCESS;
}




// Function to write a 1D LUT out to stdout
//
// Input Arguments 
//     x_array  -  LUT_uint_t*  -  Input X data ( or NULL )
//     y_array  -  LUT_uint_t*  -  Output Y data
//     desc     -  char*   -  Description to prepend to each line 
//     length   -  int     -  Length of the array to display
// 
// Output Arguments 
//     None
void ScaleImage::display_LUT( int* array, const char* desc, int length)
{
	for (int i = 0; i < length; i++) {
			printf("%s LUT Entry : %d , %d\n", desc, i, array[i]);
	}
}

void eddieImageScale( Image_Store* image_data_in, Image_Store* scaled_image_data, 
								  double effSF, int* channel_offset )
{
	// Initalize basic variables
	int mx = 0, mn = 0;
	int r, g, b;

	// Calculating Totals
	int total_image_pixels = image_data_in->width * image_data_in->height;
	int total_image_values = total_image_pixels * image_data_in->planes;
	int total_scaled_pixels = scaled_image_data->width * scaled_image_data->height;
	int total_scaled_values = total_scaled_pixels * scaled_image_data->planes;

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
		old_image_i_pixel = ((((double)((int)old_image_y_level)) * image_data_in->width) + old_image_x_pixel);
		old_image_i_value = ((double)((int)old_image_i_pixel)) * image_data_in->planes;

		new_image_i_pixel = ((((double)((int)new_image_y_level)) * scaled_image_data->width) + new_image_x_pixel);
		new_image_i_value = ((double)((int)new_image_i_pixel)) * scaled_image_data->planes;

		// Detect Over Memory
		if (old_image_i_pixel > total_image_pixels ||
			new_image_i_pixel > total_scaled_pixels ||
			new_image_i_value + 2 > total_scaled_values ||
			old_image_i_value + 2 > total_image_values) {
			break;
		}

		// Get the current values 
		r = image_data_in->image[(int)old_image_i_value + channel_offset[0]];
		g = image_data_in->image[(int)old_image_i_value + channel_offset[1]];
		b = image_data_in->image[(int)old_image_i_value + channel_offset[2]];

		// Copy Stuff and do the BGR to RGB conversion at the same time if needed
		scaled_image_data->image[(int)new_image_i_value + 0] = r;
		scaled_image_data->image[(int)new_image_i_value + 1] = g;
		scaled_image_data->image[(int)new_image_i_value + 2] = b;

		// Find the minimum and maximum values 			
		mx = max(max(r, g), b);
		mn = min(min(r, g), b);

		// Update the global min/max
		if (scaled_image_data->max_RGB < mx)
			scaled_image_data->max_RGB = mx;
		
		if (scaled_image_data->min_RGB > mn)
			scaled_image_data->min_RGB = mn;

		// Increment

		if (old_image_x_pixel >= image_data_in->width || new_image_x_pixel >= scaled_image_data->width) {

			old_image_y_level += effSF;
			new_image_y_level++;

			new_image_x_pixel = 0;
			new_image_x_value = 0;

			old_image_x_pixel = 0;
			old_image_x_value = 0;
		}
		else {
			old_image_x_pixel += effSF;
			old_image_x_value += (effSF * image_data_in->planes);
			new_image_x_pixel++;
		}
	}
}

//
// Note :
// Crop Offsets are relative to the SOURCE input image, not the scaled image
// Scanline_offset and number_of_scanlines are relative to the SCALED image,
// not the source image!!!
//
void markImageScale(Image_Store* image_data_in, Image_Store* scaled_image_data, int* crop_offsets, 
					int scanline_offset, int number_of_scanlines, double effSF, int* channel_offset, int* min_max_RGB )
{
	// Create Helper Class
	ScaleImage scaleHelper;

	// Create an array to hold Pixel Counts
	int pixel_offset_base = 0;
	int pixel_offset_out = 0;
	int pixel_offset_in = 0;
	int mx = 0, mn = 0;
	int r, g, b;

	// Need to handle both the crop X and Y case
	int start_offset_y = crop_offsets[2] * image_data_in->width * image_data_in->planes;
	int start_offset_x = crop_offsets[0] * image_data_in->planes;

	int scanline_offset_in = image_data_in->width * image_data_in->planes;
	int scanline_offset_out = scaled_image_data->width * image_data_in->planes;

	// printf(" I am here : 518\n");
	// printf(" Number of Strips : %d\n", number_of_strips );

	for (int loop_row = scanline_offset; loop_row < (scanline_offset + number_of_scanlines); loop_row++)
	{
		pixel_offset_base = start_offset_x + start_offset_y + ( (int) ( loop_row * effSF ) ) * scanline_offset_in;
		pixel_offset_out = loop_row * scanline_offset_out ;

		for ( int loop_col = 0; loop_col < scaled_image_data->width; loop_col ++ )
		{
			// Compute the input location
			pixel_offset_in = pixel_offset_base + ( image_data_in->planes * (int) ( loop_col * effSF ) ) ;

			// Get the current values 
			r = image_data_in->image[ pixel_offset_in + channel_offset[0] ];
			g = image_data_in->image[ pixel_offset_in + channel_offset[1] ];
			b = image_data_in->image[ pixel_offset_in + channel_offset[2] ];

			// Copy Stuff and do the BGR to RGB conversion at the same time if needed
			scaled_image_data->image[ pixel_offset_out ] = r;
			scaled_image_data->image[ pixel_offset_out + 1] = g;
			scaled_image_data->image[ pixel_offset_out + 2] = b;

			// Increment the histogram counters
			scaleHelper.image_hist[r] ++;
			scaleHelper.image_hist[g] ++;
			scaleHelper.image_hist[b] ++;

			// Shift the output location
			pixel_offset_out += scaled_image_data->planes;

		}
	}

	// Now extract the hist min/max
	min_max_RGB[1] = scaleHelper.returnLast(scaleHelper.image_hist, 256);
	min_max_RGB[0] = scaleHelper.returnFirst(scaleHelper.image_hist, 256);

	if (false) {
		scaleHelper.display_LUT(scaleHelper.image_hist, "Histogram", 256);
	}

}