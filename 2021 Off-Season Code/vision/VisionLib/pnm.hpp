#pragma once

#ifndef _PNM_
#define _PNM_


//
// PNM Header File
//

#include "ImageStore.hpp"
#include <stdio.h>

enum class PNM_FORMAT {
	UNDEFINED,
	ASCII,
	BINARY
};

class pnm {

public:

	char* file_path = NULL;
	Image_Store* image_data = NULL;

	PNM_FORMAT			PNM_format = PNM_FORMAT::UNDEFINED;

	pnm();  // This is the constructor
	~pnm();  // This is the destructor

	int write();
	int read();

private:
	int	 parse_header( FILE* filein );

};

int write_image_store(Image_Store* image_data, PNM_FORMAT format, const char* filepath);

#endif //_PNM_