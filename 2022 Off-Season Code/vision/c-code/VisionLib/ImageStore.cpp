
#include <cstddef>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "ImageStore.hpp"

// Member functions definitions including constructor and destructor

Image_Store::Image_Store(void)
{
    // printf("I am here inside the ImageStore constructor\n");
}

Image_Store::~Image_Store(void)
{
    // Ensure that the memory cleanup is properly handled
    if (image != nullptr)
    {
        delete(image);
        image = nullptr;
    }
}


Image_Store::Image_Store(int width_in, int height_in, int planes_in, int max_val_in, PIXEL_FORMAT format, INTERLEAVE_FORMAT interleave_fmt)
{
    set_attributes( width_in, height_in, planes_in, max_val_in, format, interleave_fmt);
}


void Image_Store::allocate_memory()
{
    // Verify the initial parameters
    if (width == 0 || height == 0 || planes == 0)
    {
        printf("Image Parameters not defined currently");
        exit(-1);
    }

    int new_memory_alloc_size = width * height * planes;

    // Make sure that we do not leak any memory
    if (image != nullptr)
    {
        // Make sure that we are not short on memory
        if (memory_allocated < new_memory_alloc_size) {            
            delete(image);
            memory_allocated = new_memory_alloc_size;
            image = new uint8_t[memory_allocated];
        }
        else
        {
            // Do nothing right now
        }
    }
    else
    {
        // Allocate the new memory
        memory_allocated = new_memory_alloc_size;
        image = new uint8_t[memory_allocated];
    }

    // Zero out the memory buffer
    this->clear_memory();
}


void Image_Store::allocate_memory( int width_in, int height_in, int planes_in, int max_val_in )
{
    width = width_in;
    height = height_in;
    planes = planes_in;
    max_val = max_val_in;

    this->allocate_memory();
}

void Image_Store::set_attributes(int width_in, int height_in, int planes_in, int max_val_in, PIXEL_FORMAT format, INTERLEAVE_FORMAT interleave_fmt )
{
    width = width_in;
    height = height_in;
    planes = planes_in;
    max_val = max_val_in;
    pixel_format = format;
    interleave = interleave_fmt;
}

void Image_Store::clear_memory()
{

    if (image == NULL) {
        this->allocate_memory();
    }
    else {
        // clean out the memory
        memset(image, 0, memory_allocated);
    }
}
