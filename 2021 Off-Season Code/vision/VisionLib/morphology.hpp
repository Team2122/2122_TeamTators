#pragma once

#ifndef _MORPHOLOGY_
#define _MORPHOLOGY_

#include "ImageStore.hpp"
#include "Vision_Pipeline.hpp"
#include <iostream>

enum class MORPHOLOGY_METHOD {
	DIALATE,
	ERODE,
	OPENING,
	CLOSING,
	DISABLED
};

class Morphology
{
	public:

		~Morphology( );  // This is the default destructor
		Morphology( Vision_Pipeline* vision_pipeline );
		
		void dialate();
		void erode();
		void opening();
		void closing();
		void setSE(int* SE);

	private:
		
		bool capture_debug_image = false;
		bool structuring_element[9] = { 1 } ;
		Image_Store * temporary_image = NULL ;

		Vision_Pipeline* vision_pipeline = NULL ;

		void dialate_erode(MORPHOLOGY_METHOD method);
		bool doesPathExist(const std::string& s);
};

#endif
