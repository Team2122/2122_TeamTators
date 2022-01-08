#include <math.h>
#include <stdlib.h>
#include <iostream>
#include <string.h>
#include "VisionLib.hpp"
#include <sys/stat.h>
#include "opencv2/opencv.hpp"
#include <thread>
#include "string.h"
#include <string>
#include "EddiesCode.hpp"

#ifdef _MSC_VER
#include <vcruntime_string.h>
#endif

#include <stdio.h>
#include <vector>

using namespace std;
using namespace cv;

#ifndef __linux__

int processRGBGreenTest(std::filesystem::path* path, int numberID) {

	std::string str = path->string();
	char* cstr = new char[str.length() + 1];
	strcpy(cstr, str.c_str());

	pnm* image = new pnm();
	image->PNM_format = PNM_FORMAT::BINARY;
	image->file_path = cstr;
	image->image_data->interleave = INTERLEAVE_FORMAT::PIXEL_INTERLEAVED;
	image->image_data->pixel_format = PIXEL_FORMAT::RGB;
	image->read();

	//Image_Store* scaled = new Image_Store();

	//image_resize(image->image_data, scaled, .2);

	//delete image->image_data;
	//image->image_data = scaled;

	double threshold = 150;
	// Targets in percent
	// target_R = 2.4;
	// target_G = 94.9;
	// target_B = 42.7;
	double target_R = 6.12;
	double target_G = 241.995;
	double target_B = 108.885;

	for (int i = 0; i < (image->image_data->height * image->image_data->width * image->image_data->planes); i += 3) {

		if (pow(pow((image->image_data->image[i] - target_R), 2) + pow((image->image_data->image[i + 1] - target_G), 2) + pow((image->image_data->image[i + 2] - target_B), 2), .5) < threshold) {
			image->image_data->image[i] = 255;
			image->image_data->image[i + 1] = 255;
			image->image_data->image[i + 2] = 255;
		}
		else {
			image->image_data->image[i] = uint8_t (image->image_data->image[i] * .5);
			image->image_data->image[i + 1] = uint8_t(image->image_data->image[i + 1] * .5);
			image->image_data->image[i + 2] = uint8_t(image->image_data->image[i + 2] * .5);
		}

	}

	std::string destination = "..//..//..//..//..//Tests//Raw RGB Green Detection//" + std::to_string(numberID) +".ppm";
	char* destCharArray = new char[str.length() + 1];
	strcpy(destCharArray, destination.c_str());

	image->file_path = destCharArray;
	image->write();

	delete image;
	delete[] destCharArray;

	return 0;
}
#endif

int OptimizedStartingProcess(Image_Store* image, double scale_factor, TargetColor color, std::vector<int>* pointList) {

	double effSF = sqrt(1 / scale_factor);

	double threshold = 150;

	int total_image_pixels = image->width * image->height;
	int total_image_values = total_image_pixels * image->planes;

	double x_image = 0, y_image = 0; // Image Position in Pixels
	double x_scaled = 0, y_scaled = 0; // Scaled Position in Pixels
	double x_limit = image->width / effSF, y_limit = image->height / effSF; // New Scaled Dimensions
	x_limit = (int)x_limit;
	y_limit = (int)y_limit;

	int total_scaled_pixels = ((int)x_limit) * ((int)y_limit);
	int total_scaled_values = total_scaled_pixels * image->planes;

	double i_image = 0; // In Pixels
	double i_scaled = 0; // In Pixels
	while (true) {

		x_image += effSF;
		x_scaled++;

		if (x_image > image->width || x_scaled > x_limit) { // we go over the edge of the x-side
			x_image = 0;
			x_scaled = 0;

			y_image += effSF;
			y_scaled++;
		}

		i_image = ((int)x_image) + (((int)y_image) * image->width);
		i_scaled = ((int)x_scaled) + (((int)y_scaled) * x_limit);

		if (i_image >= total_image_pixels || i_scaled >= total_scaled_pixels) {
			break;
		}

		if (pow(pow((image->image[((int)(i_image)) * image->planes] - color.r), 2) + pow((image->image[((int)(i_image)) * image->planes + 1] - color.g), 2) + pow((image->image[((int)(i_image)) * image->planes + 2] - color.b), 2), .5) < threshold) {

			pointList->push_back(i_scaled);

		}
	}

	return 0;
}

int OptimizedStartingProcessWrite(Image_Store* image, Image_Store* scaled, double scale_factor, TargetColor color, vector<int>* pointList) {

	double effSF = sqrt(1 / scale_factor);

	double threshold = 150;

	int total_image_pixels = image->width * image->height;
	int total_image_values = total_image_pixels * image->planes;

	double x_image = 0, y_image = 0; // Image Position in Pixels
	double x_scaled = 0, y_scaled = 0; // Scaled Position in Pixels
	double x_limit = image->width / effSF, y_limit = image->height / effSF; // New Scaled Dimensions
	x_limit = (int)x_limit;
	y_limit = (int)y_limit;

	int total_scaled_pixels = ((int)x_limit) * ((int)y_limit);
	int total_scaled_values = total_scaled_pixels * image->planes;

	//scaled = new Image_Store();
	scaled->width = (int) x_limit;
	scaled->height = (int) y_limit;
	scaled->planes = image->planes;
	scaled->allocate_memory();

	double i_image = 0; // In Pixels
	double i_scaled = 0; // In Pixels
	while (true) {

		x_image += effSF;
		x_scaled++;

		if (x_image > image->width || x_scaled > x_limit) { // we go over the edge of the x-side
			x_image = 0;
			x_scaled = 0;

			y_image += effSF;
			y_scaled++;
		}

		i_image = ((int) x_image) + (((int ) y_image) * image->width);
		i_scaled = ((int) x_scaled) + (((int) y_scaled) * x_limit);

		if (i_image >= total_image_pixels || i_scaled >= total_scaled_pixels) {
			break;
		}

		if (pow(pow((image->image[((int) (i_image)) * image->planes] - color.r), 2) + pow((image->image[((int) (i_image)) * image->planes + 1] - color.g), 2) + pow((image->image[((int) (i_image)) * image->planes + 2] - color.b), 2), .5) < threshold) {

			pointList->push_back(i_scaled);

			scaled->image[((int) (i_scaled)) * image->planes] = 255;
			scaled->image[((int) (i_scaled)) * image->planes + 1] = 255;
			scaled->image[((int) (i_scaled)) * image->planes + 2] = 255;

		}
		else {

			scaled->image[((int) (i_scaled)) * image->planes] = (int) ( image->image[(int) ((i_image)) * image->planes] * .5) ;
			scaled->image[((int) (i_scaled)) * image->planes + 1] = (int) (image->image[((int) (i_image)) * image->planes + 1] * .5);
			scaled->image[((int) (i_scaled)) * image->planes + 2] = (int) (image->image[((int) (i_image)) * image->planes + 2] * .5);

		}
	}

	return 0;
}
#ifndef __linux__
int TestProcess_OSPW(std::filesystem::path* path, int numberID) {

	std::string str = path->string();
	char* cstr = new char[str.length() + 1];
	strcpy(cstr, str.c_str());

	pnm* image = new pnm();
	image->PNM_format = PNM_FORMAT::ASCII;
	image->file_path = cstr;
	image->image_data->interleave = INTERLEAVE_FORMAT::PIXEL_INTERLEAVED;
	image->image_data->pixel_format = PIXEL_FORMAT::RGB;
	image->read();

	Image_Store* scaled = new Image_Store();
	scaled->set_attributes(image->image_data->width, image->image_data->height, image->image_data->planes, image->image_data->max_val, image->image_data->pixel_format, image->image_data->interleave);
	std::vector<int>* pointList = new std::vector<int>();


	OptimizedStartingProcessWrite(image->image_data, scaled, .2, TargetColor{}, pointList);


	delete image->image_data;
	image->image_data = scaled;
	std::string destination = "..//..//..//..//..//Tests//Raw RGB Green Detection//" + std::to_string(numberID) + ".ppm";
	char* destCharArray = new char[str.length() + 1];
	strcpy(destCharArray, destination.c_str());

	image->file_path = destCharArray;
	image->write();

	delete image;
	delete[] destCharArray;

	return 0;
}

int TestProcess_OPW(std::filesystem::path* path, std::string* string, int numberID) {

	std::string str = path->string();
	char* cstr = new char[str.length() + 1];
	strcpy(cstr, str.c_str());

	*string += ("Input Path: \"" + str + "\"\n\n");

	pnm* image = new pnm();
	image->PNM_format = PNM_FORMAT::ASCII;
	image->file_path = cstr;
	image->image_data->interleave = INTERLEAVE_FORMAT::PIXEL_INTERLEAVED;
	image->image_data->pixel_format = PIXEL_FORMAT::RGB;
	image->read();

	Image_Store* scaled = new Image_Store();
	scaled->set_attributes(image->image_data->width, image->image_data->height, image->image_data->planes, image->image_data->max_val, image->image_data->pixel_format, image->image_data->interleave);
	std::vector<Point>* pointList = new std::vector<Point>();
	BoundingBox_VM* box = new BoundingBox_VM();
	TargetLocation* location = new TargetLocation();

	auto start_time = std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count();
	NewOptimizedProcessWriteEnhancedSelection(image->image_data, scaled, 1, 80, TargetColor{}, box, pointList, location, string);
	auto end_time = std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count();

	auto time = end_time - start_time;
	*string += ("Time Taken: " + std::to_string(time) + " milliseconds \n\n");

	std::cout << "Time Taken: " << time << " milliseconds\n";

	delete image->image_data;
	image->image_data = scaled;
	std::string destination = "..//..//..//..//..//Tests//Raw RGB Green Detection//" + std::to_string(numberID) + ".ppm";
	char* destCharArray = new char[str.length() + 1];
	strcpy(destCharArray, destination.c_str());

	*string += ("Output Path: \"" + destination + "\"\n");

	image->file_path = destCharArray;
	image->write();

	delete image;
	delete[] destCharArray;
	delete location;
	delete box;

	return 0;
}
#endif
int OptimizedProcessWrite(Image_Store* image, Image_Store* scaled, double scale_factor, TargetColor color, BoundingBox_VM box, std::vector<int>* pointList) {

	double effSF = sqrt(1 / scale_factor);
	const uint8_t max_entries = 50;
	uint8_t* lookup = new uint8_t[max_entries];
	int* maxes = new int[max_entries];
	int* final_maxes = new int[max_entries];
	uint8_t left = 255, up = 255;
	uint8_t IDCounter = 1;

	memset(lookup, 0, sizeof(uint8_t));
	memset(maxes, 0, sizeof(int));
	memset(final_maxes, 0, sizeof(int));

	double threshold = 150;

	int total_image_pixels = image->width * image->height;
	int total_image_values = total_image_pixels * image->planes;

	double x_image = 0, y_image = 0; // Image Position in Pixels
	double x_scaled = 0, y_scaled = 0; // Scaled Position in Pixels
	double x_limit = image->width / effSF, y_limit = image->height / effSF; // New Scaled Dimensions
	x_limit = (int)x_limit;
	y_limit = (int)y_limit;

	int total_scaled_pixels = ((int)x_limit) * ((int)y_limit);
	int total_scaled_values = total_scaled_pixels * image->planes;

	//scaled = new Image_Store();
	scaled->width = (int) x_limit;
	scaled->height = (int) y_limit;
	scaled->planes = image->planes;
	scaled->allocate_memory();

	if (pow(pow((image->image[0] - color.r), 2) + pow((image->image[1] - color.g), 2) + pow((image->image[2] - color.b), 2), .5) < threshold) {
		// Create New Entry
		image->image[0] = IDCounter;
		lookup[IDCounter] = IDCounter;
		IDCounter++;

		scaled->image[0] = 0;
		scaled->image[0 + 1] = 255;
		scaled->image[0 + 2] = 0;

	}
	else {

		scaled->image[0] = (uint8_t) (image->image[0] * .5 );
		scaled->image[0 + 1] = (uint8_t) (image->image[0 + 1] * .5 );
		scaled->image[0 + 2] = (uint8_t) (image->image[0 + 2] * .5 );

		image->image[0] = 0;
	}

	for (int ix = 1; ix < x_limit; ix++) { // Horizontal

		if (pow(pow((image->image[((int)(((int)(ix)) * effSF)) * image->planes] - color.r), 2) + pow((image->image[((int)(((int)(ix)) * effSF)) * image->planes + 1] - color.g), 2) + pow((image->image[((int)(((int)(ix)) * effSF)) * image->planes + 2] - color.b), 2), .5) < threshold) {

			pointList->push_back(ix);

			scaled->image[((int)(ix)) * image->planes] = 0;
			scaled->image[((int)(ix)) * image->planes + 1] = 255;
			scaled->image[((int)(ix)) * image->planes + 2] = 0;

			left = image->image[((int)(((int)(ix - 1)) * effSF)) * image->planes];

			if (left != 0) {
				image->image[((int)(((int)(ix)) * effSF)) * image->planes] = left;
				maxes[left]++;
			}
			else {
				// Create New Entry
				image->image[((int)(((int)(ix)) * effSF)) * image->planes] = IDCounter;
				if (IDCounter == max_entries) {
					return 1;
				}
				lookup[IDCounter] = IDCounter;
				maxes[IDCounter]++;
				IDCounter++;

			}

		}
		else {

			scaled->image[((int)(ix)) * image->planes] = (uint8_t)(image->image[((int)(((int)(ix)) * effSF)) * image->planes] * .5);
			scaled->image[((int)(ix)) * image->planes + 1] = (uint8_t)(image->image[((int)(((int)(ix)) * effSF)) * image->planes + 1] * .5);
			scaled->image[((int)(ix)) * image->planes + 2] = (uint8_t)(image->image[((int)(((int)(ix)) * effSF)) * image->planes + 2] * .5);

			image->image[((int)(((int)(ix)) * effSF)) * image->planes] = 0;
		}

	}
	
	for (int iy = 1; iy < (y_limit * x_limit); iy += (int) y_limit) { // Vertical

		if (pow(pow((image->image[((int)(((int)(iy)) * effSF)) * image->planes] - color.r), 2) + pow((image->image[((int)(((int)(iy)) * effSF)) * image->planes + 1] - color.g), 2) + pow((image->image[((int)(((int)(iy)) * effSF)) * image->planes + 2] - color.b), 2), .5) < threshold) {

			pointList->push_back(iy);

			scaled->image[((int)(iy)) * image->planes] = 0;
			scaled->image[((int)(iy)) * image->planes + 1] = 255;
			scaled->image[((int)(iy)) * image->planes + 2] = 0;

			up = image->image[((int)(((int)(iy - 1)) * effSF)) * image->planes];

			if (up != 0) {
				image->image[((int)(((int)(iy)) * effSF)) * image->planes] = up;
				maxes[up]++;
			}
			else {
				// Create New Entry
				image->image[((int)(((int)(iy)) * effSF)) * image->planes] = IDCounter;
				if (IDCounter == max_entries) {
					return 1;
				}
				lookup[IDCounter] = IDCounter;
				maxes[IDCounter]++;
				IDCounter++;

			}

		}
		else {

			scaled->image[((int)(iy)) * image->planes] = (uint8_t)(image->image[((int)(((int)(iy)) * effSF)) * image->planes] * .5);
			scaled->image[((int)(iy)) * image->planes + 1] = (uint8_t)(image->image[((int)(((int)(iy)) * effSF)) * image->planes + 1] * .5);
			scaled->image[((int)(iy)) * image->planes + 2] = (uint8_t)(image->image[((int)(((int)(iy)) * effSF)) * image->planes + 2] * .5);

			image->image[((int)(((int)(iy)) * effSF)) * image->planes] = 0;
		}

	}

	double i_image_pixels = 0; // In Pixels
	double i_scaled_pixels = 0; // In Pixels

	double i_image_value = 0; // In Values
	double i_scaled_valie = 0; // In Values

	y_image = effSF;
	y_scaled = 1;

	while (true) {

		x_image += effSF;
		x_scaled++;

		if (x_image > image->width || x_scaled > x_limit) { // we go over the edge of the x-side
			x_image = effSF;
			x_scaled = 1;

			y_image += effSF;
			y_scaled++;
		}

		i_image_pixels = ((int)x_image) + (((int)y_image) * image->width);
		i_scaled_pixels = ((int)x_scaled) + (((int)y_scaled) * x_limit);

		if (i_image_pixels >= total_image_pixels || i_scaled_pixels >= total_scaled_pixels) {
			break;
		}

		if (pow(pow((image->image[((int)(i_image_pixels)) * image->planes] - color.r), 2) + pow((image->image[((int)(i_image_pixels)) * image->planes + 1] - color.g), 2) + pow((image->image[((int)(i_image_pixels)) * image->planes + 2] - color.b), 2), .5) < threshold) {

			pointList->push_back(i_scaled_pixels);

			scaled->image[((int)(i_scaled_pixels)) * image->planes] = 0;
			scaled->image[((int)(i_scaled_pixels)) * image->planes + 1] = 255;
			scaled->image[((int)(i_scaled_pixels)) * image->planes + 2] = 0;

			up = image->image[(((int)(i_image_pixels)) - image->width) * image->planes];
			left = image->image[(((int)(i_image_pixels)) - ((int)(effSF))) * image->planes];
			
			if (left != 0 && up != 0) {
				// Merge left and up
				image->image[((int)(i_image_pixels)) * image->planes] = std::min(left, up);
				maxes[min(left, up)]++;

				for (int i = 0; i < max_entries; i++) {

					if (lookup[i] == std::max(left, up)) {
						lookup[i] = std::min(left, up);
					}

				}

			}
			else if (left != 0) {
				image->image[((int)(i_image_pixels)) * image->planes] = left;
				maxes[left]++;
			}
			else if (up != 0) {
				image->image[((int)(i_image_pixels)) * image->planes] = up;
				maxes[up]++;
			}
			else {
				// Create New Entry
				image->image[((int)(i_image_pixels)) * image->planes] = IDCounter;
				if (IDCounter == max_entries) {
					return 1;
				}
				lookup[IDCounter] = IDCounter;
				maxes[IDCounter]++;
				IDCounter++;

			}

		}
		else {

			scaled->image[((int)(i_scaled_pixels)) * image->planes] = (uint8_t)(image->image[(int)((i_image_pixels)) * image->planes] * .5);
			scaled->image[((int)(i_scaled_pixels)) * image->planes + 1] = (uint8_t)(image->image[((int)(i_image_pixels)) * image->planes + 1] * .5);
			scaled->image[((int)(i_scaled_pixels)) * image->planes + 2] = (uint8_t)(image->image[((int)(i_image_pixels)) * image->planes + 2] * .5);

			image->image[((int)(i_image_pixels)) * image->planes] = 0;
		}
	}

	return 0;

	for (int i = 1; i < max_entries; i++) { // Setting Final Maxes

		final_maxes[lookup[i]] += maxes[i];

	}

	int maxLocation = 1;
	for (int i = 1; i < max_entries; i++) { // Finding Maximum

		if (final_maxes[i] > final_maxes[maxLocation]) {
			maxLocation = i;
		}

	}

	std::vector<int>* realPointList = new std::vector<int>();
	double xPos = 0, yPos = 0;
	for (int i = 0; i < pointList->size(); i++) {

		if (lookup[image->image[pointList->at(i) * image->planes]] == maxLocation) {

			realPointList->push_back(pointList->at(i));

			xPos = (pointList->at(i) % image->width);
			yPos = (pointList->at(i) / image->width);

			scaled->image[(int) (((int)(xPos / effSF)) + (((int)(yPos / effSF)) * x_limit))] = 255;
			scaled->image[(int) (((int)(xPos / effSF)) + (((int)(yPos / effSF)) * x_limit)) + 1] = 255;
			scaled->image[(int) (((int) (xPos / effSF)) + (((int) (yPos / effSF)) * x_limit)) + 2] = 255;

			if ((int) xPos > box.right) { 
				box.right = (pointList->at(i) % image->width);
			}
			else if ((int) xPos < box.left) {
				box.left = (pointList->at(i) / image->width);
			}

			if ((int) yPos > box.bottom) {
				box.bottom = (pointList->at(i) / image->width);
			}
			else if ((int) yPos < box.top) {
				box.top = (pointList->at(i) / image->width);
			}

		}

	}

	delete pointList;
	pointList = realPointList;

	return 0;
}

int NewOptimizedProcessWrite(Image_Store* image, Image_Store* scaled, double scale_factor, double threshold, TargetColor color, BoundingBox_VM box, std::vector<int>* pointList) {

		// Miscellaneous
	double effSF = sqrt(1 / scale_factor);

		// Attributes
	double image_width = image->width, image_height = image->height; // In Pixels
	double scaled_width = (int)((image_width - 2) / effSF), scaled_height = (int)((image_height - 2) / effSF); // In Pixels

	double total_image_pixels = (image_width) * (image_height); // In Pixels
	double total_scaled_pixels = scaled_width * scaled_height; // In Pixels

	double planes = image->planes;

	double total_image_values = image_width * image_height * planes; // In Pixels
	double total_scaled_values = scaled_width * scaled_height * planes; // In Pixels

		// Setting Scaled Attributes
	scaled->set_attributes( (int) scaled_width, (int) scaled_height, (int) planes, image->max_val, image->pixel_format, image->interleave);
	scaled->allocate_memory();

		// Threshold
	double sqrdThreshold = threshold * threshold;
	double rDiff = 0, gDiff = 0, bDiff = 0;

		// Segmentation
	const uint8_t max_entries = 50;
	uint8_t IDCounter = 1;
	uint8_t* lookup = new uint8_t[max_entries];
	int* maxes = new int[max_entries];
	int* final_maxes = new int[max_entries];
	uint8_t* previousValue = new uint8_t[(int) scaled_width];
	int up = 0, left = 0;

	std::vector<int>* image_pointList = new std::vector<int>();
	std::vector<int>* scaled_pointList = new std::vector<int>();

	memset(lookup, 0, sizeof(uint8_t) * max_entries);
	memset(maxes, 0, sizeof(int) * max_entries);
	memset(final_maxes, 0, sizeof(int) * max_entries);
	memset(previousValue, 0, sizeof(uint8_t) * (int) scaled_width);

		// Navigation
	double image_x = 0, image_y = 1, image_i = 0; // In Pixels
	double scaled_x = -1, scaled_y = 0, scaled_i = -1; // In Pixels;
	double image_value = 0, scaled_value = -3; // In Values

		// Boundaries
	int widthBound = (int) image_width - 1;
	int endBound = (int) ( total_image_pixels - image_width );

	// Ignoring the outside box
	while (true) {

		// Incrementaion
		image_x += effSF;
		scaled_x++;
		scaled_i++;
		scaled_value += 3;

		if (image_x >= widthBound || scaled_x >= scaled_width) {
			image_x = effSF;
			scaled_x = 0;

			image_y += effSF;
			scaled_y++;
		}

		image_i = (int)image_x + (((int)image_y) * image_width);
		image_value = image_i * planes;

		// End of Data
		if (image_i >= endBound || scaled_i >= total_scaled_pixels) { // try >= if memory leak
			break;
		}


		rDiff = image->image[(int) image_value] - color.r;
		gDiff = image->image[(int) image_value + 1] - color.g;
		bDiff = image->image[(int) image_value + 2] - color.b;

		if ((((rDiff) * (rDiff)) + ((gDiff) * (gDiff)) + ((bDiff) * (bDiff))) < sqrdThreshold) {

			image_pointList->push_back(image_i);
			scaled_pointList->push_back(scaled_i);

			up = lookup[previousValue[(int) scaled_x]];
			left = lookup[previousValue[(int) scaled_x - 1]];

			if (up != 0 && left != 0) {
				if (up == left) { // Easy

					previousValue[(int) scaled_x] = up;
					maxes[up]++;
					image->image[(int) image_value] = up;

				}
				else { // Merge up and left

					for (int i = 0; i < max_entries; i++) {

						if (lookup[i] == left) {
							lookup[i] = up;
						}

					}

					previousValue[(int) scaled_x] = up;
					maxes[up]++;
					image->image[(int)image_value] = up;

				}
			}
			else if (up != 0) {

				previousValue[(int) scaled_x] = up;
				maxes[up]++;
				image->image[(int)image_value] = up;

			}
			else if (left != 0) {

				previousValue[(int)scaled_x] = left;
				maxes[left]++;
				image->image[(int)image_value] = left;

			}
			else { // Create New Entry

				previousValue[(int) scaled_x] = IDCounter;
				lookup[IDCounter] = IDCounter;
				maxes[IDCounter]++;
				image->image[(int)image_value] = IDCounter;
				IDCounter++;
				if (IDCounter == max_entries) {
					// PUT DELETION CODE HERE!

					delete[] lookup;
					delete[] maxes;
					delete[] final_maxes;
					delete[] previousValue;

					std::cout << "Too Many Entries\n";
					return 1;
				}

			}

			scaled->image[(int) scaled_value] = 0;
			scaled->image[(int) scaled_value + 1] = 255;
			scaled->image[(int) scaled_value + 2] = 0;

		}
		else {

			previousValue[(int) scaled_x] = 0;

			scaled->image[(int) scaled_value] = (uint8_t)(image->image[(int) image_value] * .5);
			scaled->image[(int) scaled_value + 1] = (uint8_t)(image->image[(int) image_value + 1] * .5);
			scaled->image[(int) scaled_value + 2] = (uint8_t)(image->image[(int) image_value + 2] * .5);

		}

	}

	for (int i = 1; i <= IDCounter; i++) { // Setting Final Maxes

		final_maxes[lookup[i]] += maxes[i];

	}

	int maxLocation = 1;
	for (int i = 1; i <= IDCounter; i++) { // Finding Maximum

		if (final_maxes[i] > final_maxes[maxLocation]) {
			maxLocation = i;
		}

	}

	std::vector<int>* realPointList = new std::vector<int>();
	for (int i = 0; i < scaled_pointList->size(); i++) {

		if (lookup[image->image[image_pointList->at(i) * (int)planes]] == maxLocation) {

			realPointList->push_back(scaled_pointList->at(i));

			// Image Stuff
			image_x = ((image_pointList->at(i) % (int)image_width));
			image_y = (int)(image_pointList->at(i) / image_width);

			image_i = image_x + (image_y * image_width);
			image_value = image_i * planes;

			// Scaled Stuff
			scaled_x = ((scaled_pointList->at(i) % (int)scaled_width));
			scaled_y = (int)(scaled_pointList->at(i) / scaled_width);

			scaled_i = scaled_x + (scaled_y * scaled_width);
			scaled_value = scaled_i * planes;

			scaled->image[(int)scaled_value] = 255;
			scaled->image[(int)scaled_value + 1] = 255;
			scaled->image[(int)scaled_value + 2] = 255;

			// Postion stuff, choose image or scaled
			if ((int)image_x > box.right) {
				box.right = (int) image_x;
			}
			else if ((int)image_x < box.left) {
				box.left = (int) image_x;
			}

			if ((int)image_y > box.bottom) {
				box.bottom = (int) image_y;
			}
			else if ((int)image_y < box.top) {
				box.top = (int) image_y;
			}

		}

	}

	// START NOT NECESSARY: COLORING OTHER SECTIONS

	int secondLocation = 0;
	for (int i = 1; i <= IDCounter; i++) { // Finding Second Largest

		if (i == maxLocation) {
			continue;
		}

		if (final_maxes[i] > final_maxes[secondLocation]) {
			secondLocation = i;
		}

	}

	int thirdLocation = 0;
	for (int i = 1; i <= IDCounter; i++) { // Finding Third Largest

		if (i == maxLocation || i == secondLocation) {
			continue;
		}

		if (final_maxes[i] > final_maxes[thirdLocation]) {
			thirdLocation = i;
		}

	}

	int fourthLocation = 0;
	for (int i = 1; i <= IDCounter; i++) { // Finding Fourth Largest

		if (i == maxLocation || i == secondLocation || i == thirdLocation) {
			continue;
		}

		if (final_maxes[i] > final_maxes[fourthLocation]) {
			fourthLocation = i;
		}

	}


	for (int i = 0; i < scaled_pointList->size(); i++) {

		if (secondLocation != 0 && lookup[image->image[image_pointList->at(i) * (int)planes]] == secondLocation) { // Second is Red

			// Scaled Stuff
			scaled_x = ((scaled_pointList->at(i) % (int)scaled_width));
			scaled_y = (int)(scaled_pointList->at(i) / scaled_width);

			scaled_i = scaled_x + (scaled_y * scaled_width);
			scaled_value = scaled_i * planes;

			scaled->image[(int)scaled_value] = 255;
			scaled->image[(int)scaled_value + 1] = 0;
			scaled->image[(int)scaled_value + 2] = 0;

		} else if (thirdLocation != 0 && lookup[image->image[image_pointList->at(i) * (int)planes]] == thirdLocation) { // Third is Blue

			// Scaled Stuff
			scaled_x = ((scaled_pointList->at(i) % (int)scaled_width));
			scaled_y = (int)(scaled_pointList->at(i) / scaled_width);

			scaled_i = scaled_x + (scaled_y * scaled_width);
			scaled_value = scaled_i * planes;

			scaled->image[(int)scaled_value] = 0;
			scaled->image[(int)scaled_value + 1] = 0;
			scaled->image[(int)scaled_value + 2] = 255;

		} else if (fourthLocation != 0 && lookup[image->image[image_pointList->at(i) * (int)planes]] == fourthLocation) { // Fourth is Yellow

			// Scaled Stuff
			scaled_x = ((scaled_pointList->at(i) % (int)scaled_width));
			scaled_y = (int)(scaled_pointList->at(i) / scaled_width);

			scaled_i = scaled_x + (scaled_y * scaled_width);
			scaled_value = scaled_i * planes;

			scaled->image[(int)scaled_value] = 255;
			scaled->image[(int)scaled_value + 1] = 0;
			scaled->image[(int)scaled_value + 2] = 255;

		}

	}

	// END NOT NECESSARY: COLORING OTHER SEGMENTS

	pointList = realPointList;

	delete scaled_pointList;
	delete image_pointList;
	delete[] lookup;
	delete[] maxes;
	delete[] final_maxes;
	delete[] previousValue;

	return 0;
}

// UNTIL I FIX BOUNDARY DETECTION SCALE FACTOR MUST BE 1
int NewOptimizedProcessWriteEnhancedSelection(Image_Store* image, Image_Store* scaled, double scale_factor, double threshold, TargetColor color, BoundingBox_VM* box, std::vector<Point>* pointList, TargetLocation* location, std::string* string) {

	//std::cout << "0\n";

		// Miscellaneous
	double effSF = sqrt(1 / scale_factor);
	*string += ("Scale Factor: " + std::to_string(scale_factor) + "\n");
	*string += ("Threshold: " + std::to_string(threshold) + "\n\n");

	//std::cout << "1\n";

		// Attributes
	double image_width = image->width, image_height = image->height; // In Pixels
	double scaled_width = (int)((image_width - 2) / effSF), scaled_height = (int)((image_height - 2) / effSF); // In Pixels

	double total_image_pixels = (image_width) * (image_height); // In Pixels
	double total_scaled_pixels = scaled_width * scaled_height; // In Pixels

	double planes = image->planes;

	double total_image_values = image_width * image_height * planes; // In Pixels
	double total_scaled_values = scaled_width * scaled_height * planes; // In Pixels

	//std::cout << "2\n";

		// Setting Scaled Attributes
	scaled->set_attributes((int) scaled_width, (int) scaled_height, (int) planes, image->max_val, image->pixel_format, image->interleave);
	scaled->allocate_memory();

	//std::cout << "3\n";

		// Threshold
	double sqrdThreshold = threshold * threshold;
	double rDiff = 0, gDiff = 0, bDiff = 0, totalDiff = 0;

		// Segmentation
	const uint8_t max_entries = 250;
	uint8_t IDCounter = 1;
	Segment_VM* segments = new Segment_VM[max_entries];
	int* previousValue = new int[(int) scaled_width + 1];
	int up = 0, left = 0;
	int usingID = 0;
	double pixelSizeFactor = 1;

	//std::cout << "4\n";

	memset(segments, 0, sizeof(Segment_VM) * max_entries);
	segments[0] = Segment_VM();
	segments[0].segScore = 1000000;
	memset(previousValue, 0, sizeof(int) * (int) (scaled_width + 1));

	//std::cout << "5\n";

	std::vector<int>* image_iList = new std::vector<int>();
	std::vector<int>* scaled_iList = new std::vector<int>();
	std::vector<Point>* image_pointList = new std::vector<Point>();

	//std::cout << "6\n";

		// Navigation
	double image_x = 0, image_y = 1, image_i = 0; // In Pixels
	double scaled_x = -1, scaled_y = 0, scaled_i = -1; // In Pixels;
	double image_value = 0, scaled_value = -3; // In Values

		// Boundaries
	int widthBound = (int) ( image_width - 1 );
	int endBound = (int) ( total_image_pixels - image_width );

	//std::cout << "Starting Loop\n";

	// Ignoring the outside box
	while (true) {

		// Incrementaion
		image_x += effSF;
		scaled_x++;
		scaled_i++;
		scaled_value += 3;

		if (image_x >= widthBound || scaled_x >= scaled_width) {
			image_x = effSF;
			scaled_x = 0;

			image_y += effSF;
			scaled_y++;
		}

		image_i = (int)image_x + (((int)image_y) * image_width);
		image_value = image_i * planes;

		// End of Data
		if (image_i >= endBound || scaled_i >= total_scaled_pixels) {
			std::cout << "End of Data, Exiting Loop\n";
			break;
		}

		//std::cout << "7\n";

		rDiff = image->image[(int)image_value] - color.r;
		gDiff = image->image[(int)image_value + 1] - color.g;
		bDiff = image->image[(int)image_value + 2] - color.b;
		totalDiff = ((rDiff) * (rDiff)) + ((gDiff) * (gDiff)) + ((bDiff) * (bDiff));

		//std::cout << "8\n";

		if (totalDiff < sqrdThreshold) {

			//std::cout << "Passed Thresholding\n";

			image_iList->push_back(image_i);
			scaled_iList->push_back(scaled_i);
			image_pointList->push_back(Point(image_x, image_y));

			up = segments[previousValue[(int)scaled_x + 1]].ID;
			left = segments[previousValue[(int)scaled_x]].ID;

			if (up != 0 && left != 0) {

				if (up != left) { // Merge up and left

					//std::cout << "Merging Segments\n";

					for (int i = 0; i < IDCounter; i++) {

						if (segments[i].ID == left) {
							segments[i].ID = up;
						}

					}

				}
				usingID = up;

			}
			else if (up != 0) {

				//std::cout << "Part of Another Segment\n";
				usingID = up;

			}
			else if (left != 0) {

				//std::cout << "Part of Another Segment\n";
				usingID = left;

			}
			else { // Create New Entry

				//std::cout << "Creating new Segment\n";

				segments[IDCounter] = Segment_VM();
				segments[IDCounter].ID = IDCounter;
				usingID = IDCounter;
				segments[IDCounter].box.top = (int) image_y;
				segments[IDCounter].box.left = (int) image_x;
				segments[IDCounter].box.bottom = (int) image_y;
				segments[IDCounter].box.right = (int) image_x;
				IDCounter++;
				if (IDCounter == max_entries) {
					// PUT DELETION CODE HERE!

					delete pointList;
					delete image_iList;
					delete scaled_iList;
					delete[] previousValue;
					delete[] segments;

					std::cout << "Too Many Entries\n";
					return 1;
				}

			}

			previousValue[(int)scaled_x + 1] = usingID;
			segments[usingID].total_pixels++;
			segments[usingID].total_distance += totalDiff;
			image->image[(int)image_value] = usingID;

			if (image_x > segments[usingID].box.right) { // Setting X Bound
				segments[usingID].box.right = (int) image_x;
			}
			else if (image_x < segments[usingID].box.left) {
				segments[usingID].box.left = (int) image_x;
			}

			if (image_y > segments[usingID].box.bottom) { // Setting Y Bound
				segments[usingID].box.bottom = (int) image_y;
			}
			else if (image_y < segments[usingID].box.top) {
				segments[usingID].box.top = (int) image_y;
			}

			scaled->image[(int)scaled_value] = 0;
			scaled->image[(int)scaled_value + 1] = 255;
			scaled->image[(int)scaled_value + 2] = 0;

			//image->image[(int)image_value] = 255;
			//image->image[(int)image_value + 1] = 255;
			//image->image[(int)image_value + 2] = 255;

		}
		else {

			//std::cout << "9\n";

			previousValue[(int)scaled_x + 1] = 0;

			//std::cout << "10\n";

			scaled->image[(int)scaled_value] = (uint8_t)(image->image[(int)image_value] * .5);
			scaled->image[(int)scaled_value + 1] = (uint8_t)(image->image[(int)image_value + 1] * .5);
			scaled->image[(int)scaled_value + 2] = (uint8_t)(image->image[(int)image_value + 2] * .5);

			//std::cout << "11\n";

		}

	}

	if (IDCounter <= 1) {

		std::cout << "Not Enough Segments, Exiting\n";

		// delete pointList;
		pointList->clear();
		delete image_iList;
		delete scaled_iList;
		delete[] previousValue;
		delete[] segments;

		return 1;
	}

	std::cout << "Setting Merged Values\n";

	for (int i = 1; i < IDCounter; i++) { // Setting Merged Values

		if (i == segments[i].ID || !segments[i].stillUsing) { // I think maybe this should be an &&
			continue;
		}

		segments[segments[i].ID].total_pixels += segments[i].total_pixels;
		segments[segments[i].ID].total_distance += segments[i].total_distance;
		segments[segments[i].ID].box = combineBoundingBoxes(segments[segments[i].ID].box, segments[i].box);

		segments[i].stillUsing = false;

	}

	std::cout << "Combinning bounding boxes\n";

	for (int leftBox = 0; leftBox < IDCounter - 1; leftBox++) { // Merging Intersecting BoundingBoxes

		if (segments[leftBox].ID != leftBox) {
			continue;
		}

		for (int rightBox = leftBox + 1; rightBox < IDCounter; rightBox++) {

			if (segments[leftBox].ID == segments[rightBox].ID || segments[rightBox].ID != rightBox) {
				continue;
			}

			if (checkBoundingBoxOverlap(segments[leftBox].box, segments[rightBox].box)) { // Check Overlap
				for (int i = 0; i < IDCounter; i++) { // Merge ID's

					if (segments[i].ID == rightBox) {
						segments[i].ID = leftBox;
					}

				}
			}

		}
	}

	std::cout << "Setting Merged Values";

	for (int i = 1; i < IDCounter; i++) { // Setting Merged Values

		if (i == segments[i].ID || !segments[i].stillUsing) {
			continue;
		}

		segments[segments[i].ID].total_pixels += segments[i].total_pixels;
		segments[segments[i].ID].total_distance += segments[i].total_distance;
		segments[segments[i].ID].box = combineBoundingBoxes(segments[segments[i].ID].box, segments[i].box);

		segments[i].stillUsing = false;

	}

	std::cout << "Setting Scores\n";

	for (int i = 1; i < IDCounter; i++) { // Setting Scores

		if (segments[i].ID != i) {
			continue;
		}

		segments[i].average_distance = segments[i].total_distance / segments[i].total_pixels;
		segments[i].segScore = segments[i].average_distance - (pixelSizeFactor * segments[i].total_pixels);

	}

	int minLocation = 0;
	for (int i = 1; i <= IDCounter; i++) { // Finding Minimum

		if (segments[i].ID != i) {
			continue;
		}

		if (segments[i].segScore < segments[minLocation].segScore) {
			minLocation = i;
		}

	}

	*string += ("Segment Chosen: " + std::to_string(minLocation) + "\n");
	*string += ("\tScore: " + std::to_string(segments[minLocation].segScore) + "\n");
	*string += ("\tAverage Distance: " + std::to_string(segments[minLocation].average_distance) + "\n");
	*string += ("\tTotal Distance: " + std::to_string(segments[minLocation].total_distance) + "\n");
	*string += ("\tTotalPixels: " + std::to_string(segments[minLocation].total_pixels) + "\n");
	*string += ("\t\tPixel Score: " + std::to_string(segments[minLocation].total_pixels * pixelSizeFactor) + "\n\n");

	std::vector<Point>* realPointList = new std::vector<Point>();
	for (int i = 0; i < image_iList->size(); i++) { // Setting realPointList and Coloring scaled

		if (segments[image->image[image_iList->at(i) * (int)planes]].ID == minLocation) {

			realPointList->push_back(image_pointList->at(i));

			// Scaled Stuff
			scaled_x = ((scaled_iList->at(i) % (int)scaled_width));
			scaled_y = (int)(scaled_iList->at(i) / scaled_width);

			scaled_i = scaled_x + (scaled_y * scaled_width);
			scaled_value = scaled_i * planes;

			scaled->image[(int)scaled_value] = 255;
			scaled->image[(int)scaled_value + 1] = 255;
			scaled->image[(int)scaled_value + 2] = 255;

		}

	}

	box->left = segments[minLocation].box.left;
	box->right = segments[minLocation].box.right;
	box->top = segments[minLocation].box.top;
	box->bottom = segments[minLocation].box.bottom;

	*string += ("BoundingBox: \n");
	*string += ("\tLeft: " + std::to_string(box->left) + "\n");
	*string += ("\tRight: " + std::to_string(box->right) + "\n");
	*string += ("\tTop: " + std::to_string(box->top) + "\n");
	*string += ("\tBottom: " + std::to_string(box->bottom) + "\n\n");

	// Boundary Detection
	// DECIDE: USE IMAGE OR SCALED FOR THIS!

	std::cout << "Starting Boundary Detection\n";

	int centerX = (int) ((box->left + box->right) / 2.0);
	Point point;
	location->top_left_x = centerX;
	location->top_right_x = centerX;
	location->bottom_left_x = centerX;
	location->bottom_right_x = centerX;
	location->bottom_left_x = 0;
	for (int i = 0; i < realPointList->size(); i++) {

		point = realPointList->at(i);

		if (point.x > centerX) {
			if (point.x > location->top_right_x) {
				location->top_right_x = point.x;
				location->top_right_y = point.y;
			}
		}
		else if (point.x < centerX) {
			if (point.x < location->top_left_x) {
				location->top_left_x = point.x;
				location->top_left_y = point.y;
			}
		}
		else {
			if (point.y > location->bottom_left_x) {
				location->bottom_left_y = point.y;
				location->bottom_right_y = point.y;
			}
		}

	}

	*string += "Top Left Point: \n\tX: " + std::to_string(location->top_left_x) + "\n\tY: " + std::to_string(location->top_left_y) + "\n";
	*string += "Top Right Point: \n\tX: " + std::to_string(location->top_right_x) + "\n\tY: " + std::to_string(location->top_right_y) + "\n";
	*string += "Bottom Middle Point: \n\tX: " + std::to_string(centerX) + "\n\tY: " + std::to_string(location->bottom_right_y) + "\n\n";

	// START NOT NECESSARY: COLORING OTHER SECTIONS

	int secondLocation = 0;
	for (int i = 1; i < IDCounter; i++) { // Finding Second Smallest

		if (i == minLocation || segments[i].ID != i) {
			continue;
		}

		if (segments[i].segScore < segments[secondLocation].segScore) {
			secondLocation = i;
		}

	}

	int thirdLocation = 0;
	for (int i = 1; i < IDCounter; i++) { // Finding Third Smallest

		if (i == minLocation || i == secondLocation || segments[i].ID != i) {
			continue;
		}

		if (segments[i].segScore < segments[thirdLocation].segScore) {
			thirdLocation = i;
		}

	}

	int fourthLocation = 0;
	for (int i = 1; i < IDCounter; i++) { // Finding Fourth Smallest

		if (i == minLocation || i == secondLocation || i == thirdLocation || segments[i].ID != i) {
			continue;
		}

		if (segments[i].segScore < segments[fourthLocation].segScore) {
			fourthLocation = i;
		}

	}


	for (int i = 0; i < scaled_iList->size(); i++) {

		if (secondLocation != minLocation && segments[image->image[image_iList->at(i) * (int)planes]].ID == secondLocation) { // Second is Red

			// Scaled Stuff
			scaled_x = ((scaled_iList->at(i) % (int)scaled_width));
			scaled_y = (int)(scaled_iList->at(i) / scaled_width);

			scaled_i = scaled_x + (scaled_y * scaled_width);
			scaled_value = scaled_i * planes;

			scaled->image[(int)scaled_value] = 255;
			scaled->image[(int)scaled_value + 1] = 0;
			scaled->image[(int)scaled_value + 2] = 0;

		}
		else if (thirdLocation != minLocation && segments[image->image[image_iList->at(i) * (int)planes]].ID == thirdLocation) { // Third is Blue

		 // Scaled Stuff
			scaled_x = ((scaled_iList->at(i) % (int)scaled_width));
			scaled_y = (int)(scaled_iList->at(i) / scaled_width);

			scaled_i = scaled_x + (scaled_y * scaled_width);
			scaled_value = scaled_i * planes;

			scaled->image[(int)scaled_value] = 0;
			scaled->image[(int)scaled_value + 1] = 0;
			scaled->image[(int)scaled_value + 2] = 255;

		}
		else if (fourthLocation != minLocation && segments[image->image[image_iList->at(i) * (int)planes]].ID == fourthLocation) { // Fourth is Yellow

		 // Scaled Stuff
			scaled_x = ((scaled_iList->at(i) % (int)scaled_width));
			scaled_y = (int)(scaled_iList->at(i) / scaled_width);

			scaled_i = scaled_x + (scaled_y * scaled_width);
			scaled_value = scaled_i * planes;

			scaled->image[(int)scaled_value] = 255;
			scaled->image[(int)scaled_value + 1] = 0;
			scaled->image[(int)scaled_value + 2] = 255;

		}

	}

	// END NOT NECESSARY: COLORING OTHER SEGMENTS

	//delete pointList; // Figure out how to delete certain parts of the list, keep others from being deleted
	pointList = realPointList;
	
	delete image_iList;
	delete scaled_iList;
	delete[] previousValue;
	delete[] segments;

	return 0;
}

BoundingBox_VM combineBoundingBoxes(BoundingBox_VM b1, BoundingBox_VM b2) {

	if (b1.right < b2.right) {
		b1.right = b2.right;
	}

	if (b1.left > b2.left) {
		b1.left = b2.left;
	}

	if (b1.bottom < b2.bottom) {
		b1.bottom = b2.bottom;
	}

	if (b1.top > b2.top) {
		b1.top = b2.top;
	}
	
	return b1;
}

bool checkBoundingBoxOverlap(BoundingBox_VM b1, BoundingBox_VM b2) {

	if ((b1.top >= b2.top && b1.top <= b2.bottom) || (b1.bottom >= b2.top && b1.bottom <= b2.bottom)) {
		if ((b1.left >= b2.left && b1.left <= b2.right) || (b1.right >= b2.left && b1.right <= b2.right)) {
			return true;
		}
	}

	if ((b2.top >= b1.top && b2.top <= b1.bottom) || (b2.bottom >= b1.top && b2.bottom <= b1.bottom)) {
		if ((b2.left >= b1.left && b2.left <= b1.right) || (b2.right >= b1.left && b2.right <= b1.right)) {
			return true;
		}
	}

	return false;
}

#ifndef __linux__

int TestProcess_TGT(Tester* tester, std::filesystem::path* path, std::string* string, int numberID) {

	std::string str = path->string();
	char* cstr = new char[str.length() + 1];
	strcpy(cstr, str.c_str());

	*string += ("Input Path: \"" + str + "\"\n\n");

	pnm* image = new pnm();
	image->PNM_format = PNM_FORMAT::ASCII;
	image->file_path = cstr;
	image->image_data->interleave = INTERLEAVE_FORMAT::PIXEL_INTERLEAVED;
	image->image_data->pixel_format = PIXEL_FORMAT::RGB;
	image->read();

	Image_Store* scaled = new Image_Store();
	scaled->set_attributes(image->image_data->width, image->image_data->height, image->image_data->planes, image->image_data->max_val, image->image_data->pixel_format, image->image_data->interleave);
	std::vector<Point>* pointList = new std::vector<Point>();
	BoundingBox_VM* box = new BoundingBox_VM();
	TargetLocation* location = new TargetLocation();

	VisionImage* vImage = new VisionImage(image->image_data, 1);

	auto start_time = std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count();
	NewOptimizedProcessWriteEnhancedSelection(image->image_data, scaled, 1, 80, TargetColor{}, box, pointList, location, string);
	auto end_time = std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count();

	auto time = end_time - start_time;
	*string += ("Time Taken: " + std::to_string(time) + " milliseconds \n\n");

	std::cout << "Time Taken: " << time << " milliseconds\n";

	std::vector<std::string*>* lines = tester->getPairedFileLines(path);
	Point topLeft;
	Point topRight;
	Point bottomLeft;
	Point bottomRight;

	if (lines->size() < 5) {
		*string += ("Out of View\n\n");
	}
	else {

		topLeft = parseMarksFileLine(lines->at(1));
		bottomLeft = parseMarksFileLine(lines->at(2));
		topRight = parseMarksFileLine(lines->at(3));
		bottomRight = parseMarksFileLine(lines->at(4));
		Point bottomCenter = Point((bottomRight.x + bottomLeft.x) / 2, (bottomRight.y + bottomLeft.y) / 2);
		
		double thresh = 7;

		if (topLeft.x == 0 || topLeft.y == 0 || topRight.x == 0 || topRight.y == 0) {
			*string += ("Failed to Parse!!!!!\n\n");
		}

		*string += ("Ground Truth As Parsed: \n\tTopLeft: (" + std::to_string(topLeft.x) + ", " + std::to_string(topLeft.y) + ")" + "\n\tTopRight: (" + std::to_string(topRight.x) + ", " + std::to_string(topRight.y) + ")" + +"\n\tBottomMiddle: (" + std::to_string(bottomCenter.x) + ", " + std::to_string(bottomCenter.y) + ")\n\n");

		*string += ("Maximum Error: " + std::to_string(thresh) + "\n\n");

		double dist = sqrt(((topLeft.x - location->top_left_x) * (topLeft.x - location->top_left_x)) + ((topLeft.y - location->top_left_y) * (topLeft.y - location->top_left_y)));
		if (dist < thresh) {
			*string += ("Top Left Test Passed! \n\tError: " + std::to_string(dist) + "\n");
		}
		else {
			*string += ("Top Left Test Failed! \n\tError: " + std::to_string(dist) + "\n");
		}

		dist = sqrt(((topRight.x - location->top_right_x) * (topRight.x - location->top_right_x)) + ((topRight.y - location->top_right_y) * (topRight.y - location->top_right_y)));
		if (dist < thresh) {
			*string += ("Top Right Test Passed! \n\tError: " + std::to_string(dist) + "\n");
		}
		else {
			*string += ("Top Right Test Failed! \n\tError: " + std::to_string(dist) + "\n");
		}
		
		dist = sqrt(((bottomCenter.x - location->bottom_right_x) * (bottomCenter.x - location->bottom_right_x)) + ((bottomCenter.y - location->bottom_right_y) * (bottomCenter.y - location->bottom_right_y)));
		if (dist < thresh) {
			*string += ("Bottom Center Test Passed! \n\tError: " + std::to_string(dist) + "\n\n");
		}
		else {
			*string += ("Bottom Center Test Failed! \n\tError: " + std::to_string(dist) + "\n\n");
		}

	}

	delete image->image_data;
	image->image_data = scaled;
	std::string destination = "..//..//..//..//..//Tests//Raw RGB Green Detection//" + std::to_string(numberID) + ".ppm";
	char* destCharArray = new char[destination.length() + 1];
	strcpy(destCharArray, destination.c_str());

	*string += ("Output Path: \"" + destination + "\"\n\n");

	image->file_path = destCharArray;
	image->write();

	delete image;
	delete[] destCharArray;
	delete location;
	delete box;

	return 0;
}
#endif

cv::Point parseMarksFileLine(std::string* line) {

	cv::Point point = cv::Point();

	int colonIndex = (int) line->find_first_of(":");
	int commaIndex = (int) line->find_first_of(",");

	point.x = std::stoi(line->substr(colonIndex + 1, commaIndex));
	point.y = std::stoi(line->substr(commaIndex + 2, line->size()));

	return point;
}

void loopTypeTest() {

	std::string string1 = "..//..//..//..//..//Tests//Test Images//CaptureTests//Apr_06_test//9ft//00000_rgb_input_MarkTest.ppm";
	char* path1 = new char[string1.length() + 1];
	strcpy(path1, string1.c_str());
	std::string string2 = "..//..//..//..//..//Tests//Test Images//CaptureTests//Apr_06_test//19ft_angle//00000_rgb_input_19ft.ppm";
	char* path2 = new char[string2.length() + 1];
	strcpy(path2, string2.c_str());
	std::string string3 = "..//..//..//..//..//Tests//Test Images//CaptureTests//Apr_06_test//19ft_dist//00000_rgb_input_DistTest_19.ppm";
	char* path3 = new char[string3.length() + 1];
	strcpy(path3, string3.c_str());

	std::vector<Point> nestedList = std::vector<Point>();
	std::vector<Point> nonNestedList = std::vector<Point>();

	long long nestedTime = 0, nonNestedTime = 0;

	pnm* image = new pnm();
	image->file_path = path1;
	image->read();

	const int width = image->image_data->width;
	const int height = image->image_data->height;
	const int length = height * width;

	auto nested_start_time = std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count();
	for (int y = 0; y < height; y++) {
		for (int x = 0; x <  width; x++) {

			

		}
	}
	auto nested_end_time = std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count();
	nestedTime += nested_end_time - nested_start_time;

	auto nonNested_start_time = std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count();
	for (int i = 0; i < length; i++) {

		

	}
	auto nonNested_end_time = std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count();
	nonNestedTime += nonNested_end_time - nonNested_start_time;

}