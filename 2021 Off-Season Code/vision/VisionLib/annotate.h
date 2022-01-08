#pragma once

#ifndef _ANNOTATE_IMAGES_
#define _ANNOTATE_IMAGES_

#include "VisionLib.hpp"

int annotateDriverStationBallCamImage(Vision_Pipeline* pipeline_data);
int annotateDriverStationTargetImage(Vision_Pipeline* pipeline_data);

int annotateBallCamImage(Vision_Pipeline* pipeline_data);
int annotateTargetImage(Vision_Pipeline* pipeline_data);

void blendImages(cv::Mat orig_1, cv::Mat orig_2, cv::Mat output);
void draw_dashed_line(cv::Mat img, cv::Point2d p1, cv::Point2d p2, int length_on, int length_off);

#endif // _ANNOTATE_IMAGES_