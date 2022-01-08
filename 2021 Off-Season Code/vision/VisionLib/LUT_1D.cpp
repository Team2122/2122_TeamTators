#include "LUT_1D.h"
#include <cstring>
#include <stdio.h>
#include <assert.h>     /* assert */

LUT_uint_t LUT_max( LUT_uint_t a, LUT_uint_t b) { return a > b ? a : b; }

// Constructor
LUT_1D::LUT_1D( int length)
{
	// Setting the legth parameter first!
	points = length;

	// Allocate the memory needed 
	x_data = new LUT_uint_t[points +2];
	y_data = new LUT_uint_t[points +2];
	
	// Place Markers at the end of the data
	x_data[points] = 0;
	x_data[points + 1] = enc_max_value;
	y_data[points] = 0;
	y_data[points + 1] = enc_max_value;

	// Initialize the LUT values to x-linear, y-zero
	resetZero();
}

// Destructor
LUT_1D::~LUT_1D()
{

	if ((x_data[points] != 0) && (x_data[points + 1] != enc_max_value)) {
		assert( "x-lut memory has been over-written\n" );
	}

	// Clean up the memory
	if (x_data != NULL) {
		delete[] x_data;
		x_data = NULL;
	}

	if ((y_data[points] != 0) && (y_data[points + 1] != enc_max_value)) {
		assert("y-lut memory has been over-written\n");
	}

	if (y_data != NULL) {
		delete[] y_data;
		y_data = NULL;
	}

}

// Reset the LUT to a linear state 
void LUT_1D::resetLinear()
{
	resetLinear(x_data);
	resetLinear(y_data);
}

// Reset the LUT to a linear state 
void LUT_1D::resetZero()
{
	resetLinear(x_data);
	resetZero(y_data);
}

// Reset the LUT to a linear state 
void LUT_1D::resetLinear( LUT_uint_t* ptr )
{
	for ( LUT_uint_t i = 0; i < points; i++) {
		ptr[i] = i;
	}
}

// Reset the LUT to a linear state 
void LUT_1D::resetZero( LUT_uint_t* ptr )
{
	memset( ptr, 0, points * sizeof(LUT_uint_t) );
}

// Print out the full LUT
void LUT_1D::printLUT(const char* desc)
{
	printLUT( desc, 0 );
}

// Print out the LUT to desired length
void LUT_1D::printLUT( const char* desc, int length)
{
	if (length == 0) {
		length = this->points;
	}

	display_LUT(x_data, y_data, desc, length);
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
void LUT_1D::display_LUT( LUT_uint_t* x_array, LUT_uint_t* y_array, const char* desc, int length)
{
	if (x_array == 0)
	{
		for (int i = 0; i < length; i++) {
			printf("%s LUT Entry : %d , %d\n", desc, i, y_array[i]);
		}
	}
	else {
		for (int i = 0; i < length; i++) {
			printf("%s LUT Entry : %d , %d\n", desc, x_array[i], y_array[i]);
		}
	}
}

int LUT_1D::returnFirst()
{
	LUT_uint_t location = 0;

	// Run through the LUT entries and find the first that meets the criteria ( > 0 )
	for ( LUT_uint_t i = 0; i < points; i++) {
		location = i;
		if (y_data[location] > 0) {
			break;
		}
	}
	return location;
}


int LUT_1D::returnLast()
{
	LUT_uint_t location = 0;

	// Run through the LUT entries and find the first that meets the criteria ( > 0 )
	for ( LUT_uint_t i = 0; i < points; i++) {
		location = points - i - 1;
		if (y_data[location] > 0) {
			break;
		}
	}
	return location;
}

int LUT_1D::returnLUTMax()
{
	LUT_uint_t max = 0;

	for ( LUT_uint_t i = 0; i < points; i++) {
		max = LUT_max(max, y_data[i]);
	}
	return max;
}

void LUT_1D::normalizeLUT()
{
	LUT_uint_t max = returnLUTMax();

	if (max == 0)
		return;

	LUT_uint_t scalar = ( enc_max_value / max );

	for (int i = 0; i < points; i++) {
		y_data[i] = y_data[i] * scalar;
	}
}


