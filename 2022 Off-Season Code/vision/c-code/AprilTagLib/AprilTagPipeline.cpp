#include "AprilTagPipeline.h"

#include <string>

#include "tag36h11.h"
#include "tag25h9.h"
#include "tag16h5.h"

#include "tagCircle21h7.h"
#include "tagCircle49h12.h"
#include "tagCustom48h12.h"
#include "tagStandard41h12.h"

#include "common/getopt.h"
#include "common/image_u8.h"
#include "common/pjpeg.h"
#include "common/zarray.h"

#include <opencv2\core\types.hpp>

// using namespace cv;

AprilTagPipeline::AprilTagPipeline( tagID myTagId )
{

    // Settign the private member 
    mytagID = myTagId;

    //Load the Target Family
    switch (mytagID)
    {
        case tagID::TAG_36H11:
        {
            tf = tag36h11_create();
            break;
        }
        case tagID::TAG_16H5:
        {
            tf = tag16h5_create();
            break;
        }
        case tagID::TAG_25H9:
        {
            tf = tag25h9_create();
            break;
        }
        default:
        {
            // Add error logging, tag is not understood
            printf("Tag ID is not supported\n");
            break;
        }
    }

    // Create the Target Detector
    td = apriltag_detector_create();

    // Add the family
    apriltag_detector_add_family(this->td, this->tf);

    td->quad_decimate = 2;
    td->quad_sigma = 0.0;
    td->nthreads = 12;
    td->debug = 0;
    td->refine_edges = 1;

}


AprilTagPipeline::~AprilTagPipeline()
{
    // Default Destructor - cleans up
    apriltag_detector_destroy(td);

    switch (mytagID)
    {
        case tagID::TAG_36H11:
        {
            tag36h11_destroy(tf);
            break;
        }
    }

}

int AprilTagPipeline::processFrame( cv::Mat* currentFrame, cv::Mat* gray )
{

    // Make an image_u8_t header for the Mat data
    image_u8 im = { .width = gray->cols,
        .height = gray->rows,
        .stride = gray->cols,
        .buf = gray->data
    };

    // Call the detector
    detections = apriltag_detector_detect(td, &im);

    // Keep track of targets found 
    int targets = zarray_size(detections);

    for (int i = 0; i < targets; i++) {

        apriltag_detection_t* det;

        // Get the location information
        zarray_get(detections, i, &det);

        // Draw on the figure 
        line(*currentFrame, cv::Point(det->p[0][0], det->p[0][1]),
            cv::Point(det->p[1][0], det->p[1][1]),
            cv::Scalar(0, 0xff, 0), 2);
        line(*currentFrame, cv::Point(det->p[0][0], det->p[0][1]),
            cv::Point(det->p[3][0], det->p[3][1]),
            cv::Scalar(0, 0, 0xff), 2);
        line(*currentFrame, cv::Point(det->p[1][0], det->p[1][1]),
            cv::Point(det->p[2][0], det->p[2][1]),
            cv::Scalar(0xff, 0, 0), 2);
        line(*currentFrame, cv::Point(det->p[2][0], det->p[2][1]),
            cv::Point(det->p[3][0], det->p[3][1]),
            cv::Scalar(0xff, 0, 0), 2);

        std::stringstream ss;
        ss << det->id;
        cv::String text = ss.str();
        int fontface = cv::FONT_HERSHEY_SCRIPT_SIMPLEX;
        double fontscale = 1.0;
        int baseline;
        cv::Size textsize = cv::getTextSize(text, fontface, fontscale, 2, &baseline);
        putText(*currentFrame, text, cv::Point(det->c[0] - textsize.width / 2,
            det->c[1] + textsize.height / 2),
            fontface, fontscale, cv::Scalar(0xff, 0x99, 0), 2);
    }

    //Cleaning up memory
    apriltag_detections_destroy(detections);
    detections = NULL;

    return targets;
}
