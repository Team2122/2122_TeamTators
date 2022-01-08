
#include "pnm.hpp"

#include <cstddef>
#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <math.h>
#include <string> 
#include <string.h> 

#include <chrono>
#include <ctime>

using namespace std;

// Define local function
int   get_string( FILE* filein, char* header_data );
int   parse_string(char* buffer, void* result);
char* trim_space(char* str);

// Member functions definitions including constructor and destructor;

pnm::pnm(void)
{

	// Create an image store struct to contain the data
	this->image_data = new(Image_Store);

}

pnm::~pnm(void)
{
	// Ensure that the memory cleanup is properly handled

	if (image_data != nullptr)
	{
		delete(image_data);
		image_data = nullptr;
	}

}


int pnm::write()
{
	// todo: throw error if no image_data instance
	FILE* fileout;
	int i = 0, j = 0, cntr = 0;

	fileout = fopen(this->file_path, "wb");

	if (NULL == fileout)
	{
		printf("Unable to open file to write");
		exit(-1);
	}

	// Read the image into memory
	i = 0; j = 0;
	int pix_counter = 0;


	if ((this->image_data->pixel_format == PIXEL_FORMAT::BINARY))
	{
		if (this->PNM_format == PNM_FORMAT::ASCII)
			fprintf(fileout, "P1\n");
		else
			fprintf(fileout, "P4\n");
	}
	else if (this->image_data->pixel_format == PIXEL_FORMAT::GRAY)
	{
		if (this->PNM_format == PNM_FORMAT::ASCII)
			fprintf(fileout, "P2\n");
		else
			fprintf(fileout, "P5\n");
	}
	else if (this->image_data->pixel_format == PIXEL_FORMAT::RGB)
	{
		if (this->PNM_format == PNM_FORMAT::ASCII)
			fprintf(fileout, "P3\n");
		else
			fprintf(fileout, "P6\n");
	}
	else 
	{
		printf("pnm::write() : This pixel format is not implemented!\n");
		exit(-1);
	}

	// Write out the header information	
	fprintf(fileout, "%d\n", this->image_data->width);
	fprintf(fileout, "%d\n", this->image_data->height);

	if ( (this->image_data->pixel_format == PIXEL_FORMAT::GRAY) ||
		 (this->image_data->pixel_format == PIXEL_FORMAT::RGB) )
		fprintf(fileout, "%d\n", this->image_data->max_val);

	// Now stream the data

	int image_pixels = this->image_data->width * this->image_data->height * this->image_data->planes;

	if (this->PNM_format == PNM_FORMAT::ASCII)
	{
		if (this->image_data->pixel_format == PIXEL_FORMAT::BINARY)
		{
			// Run through the data writing it out as ASCII values
			for (i = 0; i < (image_pixels); i++)
			{
				fprintf(fileout, "%d", this->image_data->image[i]);
				
				// Since this is a binary image, it is writing out the ith pixel value in ASCII format
				// The MOD operator just tells the file writer to write out after every Nth character
				// Using 64 because it is divisible by 8
				if (((i + 1) % 64) == 0)
					fprintf(fileout, "\n");
			}
		}
		else
		{
			// Run through the data writing it out as ASCII values
			for (i = 0; i < (image_pixels); i++)
			{
				fprintf(fileout, "%d ", this->image_data->image[i]);

				// Since this is a Gray or RGB image, it is writing out the ith pixel value in ASCII format
				// The MOD operator just tells the file writer to write out after every Nth character
				// Using 21, because each pixel value can be up to 3 characters when written in ASCII
				if (((i + 1) % 21) == 0)
					fprintf(fileout, "\n");
			}
		}
	}
	else
	{
		if (this->image_data->pixel_format == PIXEL_FORMAT::BINARY)
		{
			// Write out the binary stream
			uint8 pixval;

			int pixel_counter = 0;
			int packed_pixels = (int)ceil((double)(this->image_data->width * this->image_data->height * this->image_data->planes) / 8.0);

			for (long pixel_inc = 0; pixel_inc < packed_pixels; pixel_inc++)
			{
				pixval = 0;
				for (int loopbit = 7; loopbit >= 0; loopbit -= 1)
				{
					// Make sure that the number of pixels does not go over the end or the buffer
					if (pixel_counter > (this->image_data->width * this->image_data->height * this->image_data->planes))
						break;

					pixval += (this->image_data->image[pixel_counter++] << loopbit);
				}
				fprintf(fileout, "%c", pixval);
			}
		}
		else
		{
			// Write out the binary stream
			fwrite(this->image_data->image, sizeof(uint8), image_pixels, fileout);
		}

	}

	// close the file
	fclose(fileout);

	// Return success
	return(EXIT_SUCCESS);

}


// Now Define the code

int pnm::read()
{
	// todo: if no instance, create image_data
	FILE* filein;
	int i = 0, j = 0, cntr = 0;

	filein = fopen(this->file_path, "rb");

	if (NULL == filein)
	{
		printf("Unable to open file %s\n", this->file_path);
		exit(-1);
	}

	// Set up the parameters needed to read the file

	if (parse_header(filein) == EXIT_FAILURE)
	{
		printf("Unable to parse header");
		exit(-1);
	}

	// Now Allocate the memory needed 
	image_data->allocate_memory();

	//Allocation for the string reader
	char string_buffer[200*6];

	// Read the image into memory
	i = 0; j = 0;
	int pix_counter = 0;

	if (this->PNM_format == PNM_FORMAT::ASCII)
	{
		if (this->image_data->pixel_format == PIXEL_FORMAT::BINARY)
		{
			// First off the Binary Image case	
			while (feof(filein) == 0)
			{
				int img_pix_count = this->image_data->width * this->image_data->height * this->image_data->planes;

				// Read a line of characters
				fgets(string_buffer, sizeof string_buffer, filein);

				for (i = 0; i <= (strlen(string_buffer)); i++)
				{
					if (pix_counter >= img_pix_count)
						break;

					if ((string_buffer[i] == 13) || (string_buffer[i] == 10))
						break;

					// if space or NULL found, assign into new string
					if (string_buffer[i] == '1')
						this->image_data->image[pix_counter++] = 1;
					else
						this->image_data->image[pix_counter++] = 0;

				}
			}
		}
		else 
		{
			// The Gray and RGB cases 
			int img_pix_count = this->image_data->width * this->image_data->height * this->image_data->planes;
			int string_length;

			for (pix_counter = 0; pix_counter < img_pix_count; pix_counter++)
			{
				// printf( "%d\n", pix_counter);
				string_length = get_string(filein, string_buffer);
				this->image_data->image[pix_counter] = stoi(string_buffer);
			}
		}

		// Specify the pixel interleave format
		this->image_data->interleave = INTERLEAVE_FORMAT::PIXEL_INTERLEAVED;
	}
	else if (this->PNM_format == PNM_FORMAT::BINARY)
	{

		if (this->image_data->pixel_format == PIXEL_FORMAT::BINARY)
		{
			// This is the binary case
			// Need to unpack the bytes

			uint8 pixval;
			int pixel_counter = 0;
			int packed_pixels = (int) ceil( ((double) this->image_data->width * (double) this->image_data->height * (double) this->image_data->planes) / 8.0 );

			for (long pixel_inc = 0; pixel_inc < packed_pixels; pixel_inc++)
			{

				pixval = fgetc(filein);

				for (int loopbit = 7; loopbit >= 0; loopbit -= 1)
				{
					// Make sure that the number of pixels does not go over the end or the buffer
					if (pixel_counter > (this->image_data->width * this->image_data->height * this->image_data->planes))
						break;

					if (pixval & (1 << loopbit))
						this->image_data->image[pixel_counter++] = 1;
					else
						this->image_data->image[pixel_counter++] = 0;

				}
			}
		}
		else
		{

			uint8 pixval;
			for (long pixel_inc = 0; pixel_inc < (this->image_data->width * this->image_data->height * this->image_data->planes); pixel_inc++)
			{
				pixval = fgetc(filein);
				this->image_data->image[pixel_inc] = pixval;
			}
		}

		// Specify the pixel interleave format
		this->image_data->interleave = INTERLEAVE_FORMAT::PIXEL_INTERLEAVED;

	}


	// close the file
	fclose(filein);

	// Return success
	return(EXIT_SUCCESS);

}


int pnm::parse_header(FILE* filein)
{
	// Allocation for the string reader
	char string_buffer[2000];
	char format_str[10];
	int string_length = 0;

	// Start to parse the header info

	// File format
	string_length = get_string(filein, string_buffer);
	strcpy(format_str, string_buffer);

	// Image Width
	string_length = get_string(filein, string_buffer);
	this->image_data->width = stoi(string_buffer);

	// Image Height
	string_length = get_string(filein, string_buffer);
	this->image_data->height = stoi(string_buffer);

	if ((strcmp(format_str, "P2") == 0) ||
		(strcmp(format_str, "P3") == 0) ||
		(strcmp(format_str, "P5") == 0) ||
		(strcmp(format_str, "P6") == 0))
	{
		// Max Value
		string_length = get_string(filein, string_buffer);
		this->image_data->max_val = stoi(string_buffer);
	}
	else
		this->image_data->max_val = 1;


	// Now parse the header
	// https://en.wikipedia.org/wiki/Netpbm#File_formats see table in Description
	
	if (strcmp(format_str, "P1") == 0)
	{
		this->PNM_format = PNM_FORMAT::ASCII;
		this->image_data->pixel_format = PIXEL_FORMAT::BINARY;
		this->image_data->planes = 1;
	}
	else if (strcmp(format_str, "P2") == 0)
	{
		this->PNM_format = PNM_FORMAT::ASCII;
		this->image_data->pixel_format = PIXEL_FORMAT::GRAY;
		this->image_data->planes = 1;
	}
	else if (strcmp(format_str, "P3") == 0)
	{
		this->PNM_format = PNM_FORMAT::ASCII;
		this->image_data->pixel_format = PIXEL_FORMAT::RGB;
		this->image_data->planes = 3;
	}
	else if (strcmp(format_str, "P4") == 0)
	{
		this->PNM_format = PNM_FORMAT::BINARY;
		this->image_data->pixel_format = PIXEL_FORMAT::BINARY;
		this->image_data->planes = 1;
	}
	else if (strcmp(format_str, "P5") == 0)
	{
		this->PNM_format = PNM_FORMAT::BINARY;
		this->image_data->pixel_format = PIXEL_FORMAT::GRAY;
		this->image_data->planes = 1;
	}
	else if (strcmp(format_str, "P6") == 0)
	{
		this->PNM_format = PNM_FORMAT::BINARY;
		this->image_data->pixel_format = PIXEL_FORMAT::RGB;
		this->image_data->planes = 3;
	}

	if (this->image_data->max_val > 255)
	{
		printf("Can ony handle uint8 images at this time");
		exit(-1);
	}

	return (EXIT_SUCCESS);

}


// Sub-Function used to parse the string from the file

int parse_string(char* buffer, void* result)
{

	// The size of header_string MUST match that of result
	// otherwise an exception fault will occur 

	// Define the reurn parameter
	int cntr = 0;
	int i = 0, j = 0;

	char header_strings[200][6];

	//Process through the string
	for (i = 0; i <= (strlen(buffer)); i++)
	{
		// if space or NULL found, assign into new string
		if (buffer[i] == 32 || buffer[i] == 10)
		{
			header_strings[cntr][j] = '\0';
			cntr++;  //for next word
			j = 0;    //for next word, init index to 0
		}
		else
		{
			header_strings[cntr][j] = buffer[i];
			j++;
		}
	}

	memcpy(result, header_strings, sizeof header_strings);

	return cntr;

}


char* trim_space(char* str) {
	char* end;
	/* skip leading whitespace */
	while (isspace(*str)) {
		str = str + 1;
	}
	/* remove trailing whitespace */
	end = str + strlen(str) - 1;
	while (end > str && isspace(*end)) {
		end = end - 1;
	}
	/* write null character */
	*(end + 1) = '\0';
	return str;
}

int get_string( FILE* filein, char* header_data )
{
	int counter = 0;
	char read_char;	

	// Read a character
	read_char = fgetc(filein);

	// Process through any end of line characters
	while ((read_char == 10) || (read_char == 13) )
		read_char = fgetc(filein);

	// Now start on the string
	while ( !(( read_char == 10 ) || (read_char == 13) || (read_char == 32 )) )
	{

		if (read_char == 35)
		{
			read_char = fgetc(filein);

			// Read to the end of the line
			while( read_char != 10 )
				read_char = fgetc(filein);

			read_char = fgetc(filein);
		}
		if ( read_char < 0 )
			break;

		// Start looking through the file
		header_data[counter++] = read_char;
		read_char = fgetc(filein);
	}

	// Insert End of Line Character
	header_data[counter++] = 0;

	return{ counter };

}

int write_image_store(Image_Store* image_data, PNM_FORMAT format, const char* filepath) {
	
	pnm writer;
	writer.file_path = (char*)filepath;

	Image_Store* g = writer.image_data;
	writer.image_data = image_data;
	writer.PNM_format = format;

	int result = writer.write();

	if (result != 0) {
		printf("Unable to write image\n");
		return -1;
	}

	writer.image_data = g;

	return 0;
}
