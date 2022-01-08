#pragma once

#ifndef _IMAGESTORE_
#define _IMAGESTORE_

#include <cstddef>
#include <cstdint>

typedef unsigned char uint8;

enum class INTERLEAVE_FORMAT {
	UNDEFINED,
	PIXEL_INTERLEAVED,
	SCAN_LINE_INTERLEAVED,
	PLANAR_INTERLEAVED
};

enum class PIXEL_FORMAT {
	UNDEFINED,
	BINARY,
	RGB,
	BGR,
	CMY,
	GRAY,
	GRAYA,
	RGBA,
	CMYA,
	CMYK
};


// Set up a class to store the image data 

class Image_Store {

public:

    uint8_t*  image = nullptr;
    int		width = 0;
    int		height = 0;
    int		planes = 0;
    int		max_val = 0; // maximum brightness of any channel possible (format)
	int		min_RGB = 0; // minimum brightness of any pixel in this image (image data)
	int		max_RGB = 0; // minimum brightness of any pixel in this image (image data)

    PIXEL_FORMAT		pixel_format = PIXEL_FORMAT::UNDEFINED;
    INTERLEAVE_FORMAT   interleave = INTERLEAVE_FORMAT::UNDEFINED;

    Image_Store();  // This is the constructor
    ~Image_Store();  // This is the destructor

	Image_Store(int width_in, int height_in, int planes_in, int max_val_in, PIXEL_FORMAT format, INTERLEAVE_FORMAT interleave_fmt ) ;

    void allocate_memory();
    void allocate_memory(int width_in, int height_in, int planes_in, int max_val_in);
	void set_attributes(int width_in, int height_in, int planes_in, int max_val_in, PIXEL_FORMAT format, INTERLEAVE_FORMAT interleave_fmt);
	void clear_memory();

private :

	int memory_allocated = 0;  // Parameter used to determine how much memory was allocated
};

#endif // _IMAGESTORE_

