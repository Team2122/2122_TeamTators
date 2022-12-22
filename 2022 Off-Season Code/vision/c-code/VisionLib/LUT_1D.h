#pragma once

#include <stdint.h>

#ifndef _1D_LUT_
#define _1D_LUT_

typedef uint16_t LUT_uint_t;

class LUT_1D
{
	public :

		LUT_uint_t*  x_data;
		LUT_uint_t*  y_data;
		LUT_uint_t   enc_max_value = ~0;
		int   points = 0;

		LUT_1D( int length );
		~LUT_1D();

		// Define Generic function handlers
		void resetLinear();
		void resetZero();
		int  returnFirst();
		int  returnLast();
		void normalizeLUT();

		void printLUT(const char* desc, int length);
		void printLUT(const char* desc);

	private :

		void display_LUT(LUT_uint_t* x_array, LUT_uint_t* y_array, const char* desc, int length);

		void resetLinear(LUT_uint_t* ptr );
		void resetZero(LUT_uint_t* ptr );

		int  returnLUTMax();

};

#endif