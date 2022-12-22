#include "annotate.h"

using namespace cv;


int annotateTargetImage(Vision_Pipeline* pipeline_data)
{
	// Set up the pointers to be able to re-use the code
	Image_Store*	input_image = pipeline_data->scaled_image;
	Image_Store*	segmented_image = pipeline_data->thresholded_out;
	TargetLocation* target_LOC = pipeline_data->target_LOC;
	BoundingBox*	TargetBoundary = pipeline_data->target_bounding_box;

	double target_distance = target_LOC->getDistance();
	double bot_angle = target_LOC->getBotAngle();
	double turret_angle = target_LOC->getTurretAngle();

	// set-up the data
	cv::Mat original = cv::Mat(input_image->height, input_image->width, CV_8UC3, input_image->image);
	cv::Mat greyImg = cv::Mat(segmented_image->height, segmented_image->width, CV_8U, segmented_image->image);

	std::vector<cv::Mat> colorImage(3);
	colorImage.at(0) = greyImg; //for blue channel
	colorImage.at(1) = greyImg; //for green channel
	colorImage.at(2) = greyImg; //for red channel

	cv::Mat seg_img;
	cv::merge(colorImage, seg_img);

	// Draw the Bounding Box

	cv::Point2i  TL = Point(TargetBoundary->left, TargetBoundary->top);
	cv::Point2i  TR = Point(TargetBoundary->right, TargetBoundary->top);
	cv::Point2i  BL = Point(TargetBoundary->left, TargetBoundary->bottom);
	cv::Point2i  BR = Point(TargetBoundary->right, TargetBoundary->bottom);

	draw_dashed_line(seg_img, TL, TR, 5, 3);
	draw_dashed_line(seg_img, TR, BR, 5, 3);
	draw_dashed_line(seg_img, BR, BL, 5, 3);
	draw_dashed_line(seg_img, BL, TL, 5, 3);

	// Outline the target

	TL = Point(target_LOC->top_left_x, target_LOC->top_left_y);
	TR = Point(target_LOC->top_right_x, target_LOC->top_right_y);
	BL = Point(target_LOC->bottom_left_x, target_LOC->bottom_left_y);
	BR = Point(target_LOC->bottom_right_x, target_LOC->bottom_right_y);

	cv::Point2i  MT = (TL + TR) / 2.0;
	cv::Point2i  MB = (BL + BR) / 2.0;
	cv::Point2i  MM = (MT + MB) / 2.0;

	// Draw the Lines
	cv::line(seg_img, TL, TR, Scalar(0, 0, 255), 1, LINE_4);
	cv::line(seg_img, TR, BR, Scalar(0, 0, 255), 1, LINE_4);
	cv::line(seg_img, BR, BL, Scalar(0, 0, 255), 1, LINE_4);
	cv::line(seg_img, BL, TL, Scalar(0, 0, 255), 1, LINE_4);

	// Draw the Circle
	cv::circle(seg_img, TL, 3, Scalar(255, 0, 0), -1, LINE_8, 0);
	cv::circle(seg_img, TR, 3, Scalar(255, 0, 0), -1, LINE_8, 0);
	cv::circle(seg_img, BL, 3, Scalar(255, 0, 0), -1, LINE_8, 0);
	cv::circle(seg_img, BR, 3, Scalar(255, 0, 0), -1, LINE_8, 0);
	cv::circle(seg_img, MT, 3, Scalar(0, 255, 0), -1, LINE_8, 0);

	cv::Mat combined;

	// Draw the dividing Line
	cv::Point2i T = Point(0, 0);
	cv::Point2i B = Point(0, input_image->height);
	cv::line(seg_img, T, B, Scalar(255, 255, 255), 1, LINE_4);

	blendImages(original, seg_img, original);

	// Show the images Side by Side
	cv::Point2i Loc_1 = Point((int)(input_image->width / 3.0), 10);
	cv::Point2i Loc_2 = Point((int)(4 * input_image->width / 3.0), 10);

	cv::Point2i Loc_3 = Point((int)((2 * input_image->width) * (11 / 20.0)), (int)(input_image->height * (6 / 40.0)));
	cv::Point2i Loc_4 = Point((int)((2 * input_image->width) * (11 / 20.0)), (int)(input_image->height * (8 / 40.0)));
	cv::Point2i Loc_5 = Point((int)((2 * input_image->width) * (11 / 20.0)), (int)(input_image->height * (10 / 40.0)));
	cv::Point2i Loc_6 = Point((int)((2 * input_image->width) * (11 / 20.0)), (int)(input_image->height * (12 / 40.0)));

	cv::hconcat(original, seg_img, combined);
	cv::putText(combined, "Segmented Image", Loc_1, cv::FONT_HERSHEY_DUPLEX, 0.4, Scalar(255, 255, 255));
	cv::putText(combined, "Annotated Image", Loc_2, cv::FONT_HERSHEY_DUPLEX, 0.4, Scalar(255, 255, 255));

	// Annotate the image
	char annotations[FILENAME_MAX] = "";
	sprintf(annotations, "Bot Distance : %g ft", target_distance);
	cv::putText(combined, annotations, Loc_3, cv::FONT_HERSHEY_SIMPLEX, 0.4, Scalar(255, 255, 255));

	sprintf(annotations, "TM(X,Y) : %u %u", (int)MT.x, (int)MT.y);
	cv::putText(combined, annotations, Loc_4, cv::FONT_HERSHEY_SIMPLEX, 0.4, Scalar(255, 255, 255));

	sprintf(annotations, "Bot Angle    : %g deg", bot_angle);
	cv::putText(combined, annotations, Loc_5, cv::FONT_HERSHEY_SIMPLEX, 0.4, Scalar(255, 255, 255));

	sprintf(annotations, "Turret Angle : %g deg", turret_angle);
	cv::putText(combined, annotations, Loc_6, cv::FONT_HERSHEY_SIMPLEX, 0.4, Scalar(255, 255, 255));

	if (0)
	{
		// Display the image for debugging purposes
		std::string greyArrWindow = "Color Image";
		cv::namedWindow(greyArrWindow, cv::WINDOW_AUTOSIZE);
		cv::imshow(greyArrWindow, combined);

		cv::waitKey(0);
		cv::destroyAllWindows();
	}

	// Convert from BGR to RGB to display
	// cv::cvtColor(combined, combined, cv::COLOR_RGB2BGR);

	// Now that we have successfuly overlaid the lines on the image, we need to 
	// Copy the image data back into the ImageStore, so we will create a new 
	// image store to hold the data
	// Since its on the stack, it will be deleted when this function exits.

	// Make sure that we do not leak memory
	if (pipeline_data->annotated_image != NULL)
		delete pipeline_data->annotated_image;

	pipeline_data->annotated_image = new Image_Store(combined.cols, combined.rows, 3, 255, PIXEL_FORMAT::RGB, INTERLEAVE_FORMAT::PIXEL_INTERLEAVED);
	pipeline_data->annotated_image->allocate_memory();

	// Copy over the image data
	memcpy((pipeline_data->annotated_image)->image, combined.data, (combined.rows * combined.cols * 3));

	return EXIT_SUCCESS;

}


int annotateDriverStationTargetImage(Vision_Pipeline* pipeline_data)
{
	// Set up the pointers to be able to re-use the code
	Image_Store* input_image = pipeline_data->scaled_image;
	Image_Store* segmented_image = pipeline_data->thresholded_out;
	Image_Store* driver_station_image = pipeline_data->driver_station;

	TargetLocation* target_LOC = pipeline_data->target_LOC;
	BoundingBox* TargetBoundary = pipeline_data->target_bounding_box;

	// Make sure that we do not leak memory		
	if (driver_station_image->image == NULL)
	{
		// Since the pointer is NULL, make sure we allocate the memory
		pipeline_data->driver_station = new Image_Store();
		driver_station_image = pipeline_data->driver_station;

		// Make sure that we have allocated the memory
		driver_station_image->set_attributes(input_image->width, input_image->height, 3, 255, PIXEL_FORMAT::RGB, INTERLEAVE_FORMAT::PIXEL_INTERLEAVED);
		driver_station_image->allocate_memory();
	}

	// Make sure that we can paint the location correctly
	if (target_LOC->checkValidity())
	{
		double target_distance = target_LOC->getDistance();
		double bot_angle = target_LOC->getBotAngle();
		double turret_angle = target_LOC->getTurretAngle();

		// set-up the data
		cv::Mat seg_MonoImg = cv::Mat(segmented_image->height, segmented_image->width, CV_8U, segmented_image->image);

		std::vector<cv::Mat> colorImage(3);
		colorImage.at(0) = seg_MonoImg; //for blue channel
		colorImage.at(1) = seg_MonoImg; //for green channel
		colorImage.at(2) = seg_MonoImg; //for red channel

		// Now merge the image planes
		cv::Mat merged_seg_img;
		cv::merge(colorImage, merged_seg_img);

		cv::Mat orig_image = cv::Mat(input_image->height, input_image->width, CV_8UC3, input_image->image);
		cv::Mat annot_image = cv::Mat(driver_station_image->height, driver_station_image->width, CV_8UC3, driver_station_image->image);

		blendImages(orig_image, merged_seg_img, annot_image);

		// Draw the Bounding Box
		cv::Point2i  TL = Point(TargetBoundary->left, TargetBoundary->top);
		cv::Point2i  TR = Point(TargetBoundary->right, TargetBoundary->top);
		cv::Point2i  BL = Point(TargetBoundary->left, TargetBoundary->bottom);
		cv::Point2i  BR = Point(TargetBoundary->right, TargetBoundary->bottom);

		draw_dashed_line(annot_image, TL, TR, 5, 3);
		draw_dashed_line(annot_image, TR, BR, 5, 3);
		draw_dashed_line(annot_image, BR, BL, 5, 3);
		draw_dashed_line(annot_image, BL, TL, 5, 3);

		// Outline the target
		TL = Point(target_LOC->top_left_x, target_LOC->top_left_y);
		TR = Point(target_LOC->top_right_x, target_LOC->top_right_y);
		BL = Point(target_LOC->bottom_left_x, target_LOC->bottom_left_y);
		BR = Point(target_LOC->bottom_right_x, target_LOC->bottom_right_y);

		cv::Point2i  MT = (TL + TR) / 2.0;
		cv::Point2i  MB = (BL + BR) / 2.0;
		cv::Point2i  MM = (MT + MB) / 2.0;

		// Draw the Lines
		cv::line(annot_image, TL, TR, Scalar(0, 0, 255), 1, LINE_4);
		cv::line(annot_image, TR, BR, Scalar(0, 0, 255), 1, LINE_4);
		cv::line(annot_image, BR, BL, Scalar(0, 0, 255), 1, LINE_4);
		cv::line(annot_image, BL, TL, Scalar(0, 0, 255), 1, LINE_4);

		// Draw the Circle
		cv::circle(annot_image, TL, 3, Scalar(255, 0, 0), -1, LINE_8, 0);
		cv::circle(annot_image, TR, 3, Scalar(255, 0, 0), -1, LINE_8, 0);
		cv::circle(annot_image, BL, 3, Scalar(255, 0, 0), -1, LINE_8, 0);
		cv::circle(annot_image, BR, 3, Scalar(255, 0, 0), -1, LINE_8, 0);
		cv::circle(annot_image, MT, 3, Scalar(0, 255, 0), -1, LINE_8, 0);

		// Show the images Side by Side
		cv::Point2i Loc_1 = Point((int)((input_image->width) * (3 / 20.0)), (int)(input_image->height * (3 / 60.0)));
		cv::Point2i Loc_2 = Point((int)((input_image->width) * (3 / 20.0)), (int)(input_image->height * (5 / 60.0)));
		cv::Point2i Loc_3 = Point((int)((input_image->width) * (3 / 20.0)), (int)(input_image->height * (7 / 60.0)));
		//cv::Point2i Loc_4 = Point((int)((input_image->width) * (3 / 20.0)), (int)(input_image->height * (9 / 60.0)));

		// Annotate the image
		char annotations[FILENAME_MAX] = "";

		sprintf(annotations, "Bot Distance : %g ft", target_distance);
		cv::putText(annot_image, annotations, Loc_1, cv::FONT_HERSHEY_SIMPLEX, 0.4, Scalar(255, 255, 255));

		sprintf(annotations, "TM(X,Y)      : %u %u", (int)MT.x, (int)MT.y);
		cv::putText(annot_image, annotations, Loc_2, cv::FONT_HERSHEY_SIMPLEX, 0.4, Scalar(255, 255, 255));

		sprintf(annotations, "Turret Angle : %g deg", turret_angle);
		cv::putText(annot_image, annotations, Loc_3, cv::FONT_HERSHEY_SIMPLEX, 0.4, Scalar(255, 255, 255));

		// Convert from BGR to RGB to display
		cv::cvtColor(annot_image, annot_image, cv::COLOR_BGR2RGB);

		return EXIT_SUCCESS;
	}
	else
	{
		// Location was not valid, so not attempting to annotate
		// Convert from BGR to RGB to display

		cv::Mat orig_image = cv::Mat(input_image->height, input_image->width, CV_8UC3, input_image->image);
		cv::Mat annot_image = cv::Mat(driver_station_image->height, driver_station_image->width, CV_8UC3, driver_station_image->image);

		cv::cvtColor(orig_image, annot_image, cv::COLOR_BGR2RGB);

		return EXIT_SUCCESS;
	}
}


int annotateDriverStationBallCamImage (Vision_Pipeline* pipeline_data)
{
	// Set up the pointers to be able to re-use the code
	Image_Store* input_image = pipeline_data->scaled_image;
	Image_Store* segmented_image = pipeline_data->thresholded_out;
	Image_Store* driver_station_image = pipeline_data->driver_station;

	cv::Mat scaledColorImage = cv::Mat(input_image->height, input_image->width, CV_8UC3, input_image->image );
	cv::Mat grayImage = cv::Mat(input_image->height, input_image->width, CV_8U);

	cv::cvtColor(scaledColorImage, grayImage, cv::COLOR_BGR2GRAY);

	// Make sure that we do not leak memory		
	if (driver_station_image->image == NULL)
	{
		// Since the pointer is NULL, make sure we allocate the memory
		pipeline_data->driver_station = new Image_Store();
		driver_station_image = pipeline_data->driver_station;
	}

	// Make sure that we have allocated the memory
	driver_station_image->set_attributes(input_image->width, input_image->height, 3, 255, PIXEL_FORMAT::RGB, INTERLEAVE_FORMAT::PIXEL_INTERLEAVED);
	driver_station_image->allocate_memory();

	// Now merge the image planes
	cv::Mat annot_image = cv::Mat(driver_station_image->height, driver_station_image->width, CV_8UC3, driver_station_image->image);
	cv::cvtColor(grayImage, annot_image, cv::COLOR_GRAY2RGB);

	// Draw the Circles
	for (int loop_ball = 0; loop_ball < pipeline_data->object_location.objects_found; loop_ball++)
	{
		cv::Scalar OBJECTCOLOR;

		switch (pipeline_data->object_location.object_color[loop_ball] )
		{
			case OBJECTCOLOR::RED:
				OBJECTCOLOR = Scalar(0, 0, 255);
				break;
			case OBJECTCOLOR::GREEN:
				OBJECTCOLOR = Scalar(0, 255, 0);
				break;
			case OBJECTCOLOR::BLUE:
				OBJECTCOLOR = Scalar(255, 0, 0);
				break;
			case OBJECTCOLOR::YELLOW:
				OBJECTCOLOR = Scalar(0, 255, 255);
				break;
			default :
				OBJECTCOLOR = Scalar(255, 255, 255);
				break;
		}
				
		cv::Point2i ball_loc = Point(pipeline_data->object_location.center_location[loop_ball].x, pipeline_data->object_location.center_location[loop_ball].y);
		cv::circle(annot_image, ball_loc, 10, OBJECTCOLOR, -1, LINE_8, 0);
	}

	if (0)
	{
		// Display the image for debugging purposes
		cv::namedWindow("Color Image", cv::WINDOW_AUTOSIZE);
		cv::imshow("Color Image", scaledColorImage);

		cv::namedWindow("GreyScale Image", cv::WINDOW_AUTOSIZE);
		cv::imshow("GreyScale Image", grayImage);

		cv::namedWindow("Annotated Image", cv::WINDOW_AUTOSIZE);
		cv::imshow("Annotated Image", annot_image);

		cv::waitKey(0);
		cv::destroyAllWindows();
	}

	return EXIT_SUCCESS;
}


int annotateDriverStationHubImage(Vision_Pipeline* pipeline_data)
{
	// Set up the pointers to be able to re-use the code
	Image_Store* input_image = pipeline_data->scaled_image;
	Image_Store* segmented_image = pipeline_data->thresholded_out;
	Image_Store* driver_station_image = pipeline_data->driver_station;
	TargetLocation* target_LOC = pipeline_data->target_LOC;

	double target_distance = target_LOC->getDistance();
	double turret_angle = target_LOC->getTurretAngle();
	double currentFPS = 1000.0 / pipeline_data->timing_total_processing ;

	Location target_location;
	target_LOC->getTargetCenter(&target_location);
		
	cv::Point2i MT;
	MT.x = target_location.x;
	MT.y = target_location.y;

	cv::Mat scaledColorImage = cv::Mat(input_image->height, input_image->width, CV_8UC3, input_image->image);
	cv::Mat grayImage = cv::Mat(input_image->height, input_image->width, CV_8U);

	cv::cvtColor(scaledColorImage, grayImage, cv::COLOR_BGR2GRAY);

	// Make sure that we do not leak memory		
	if (driver_station_image->image == NULL)
	{
		// Since the pointer is NULL, make sure we allocate the memory
		pipeline_data->driver_station = new Image_Store();
		driver_station_image = pipeline_data->driver_station;
	}

	// Make sure that we have allocated the memory
	driver_station_image->set_attributes(input_image->width, input_image->height, 3, 255, PIXEL_FORMAT::RGB, INTERLEAVE_FORMAT::PIXEL_INTERLEAVED);
	driver_station_image->allocate_memory();

	// Now merge the image planes
	cv::Mat annot_image = cv::Mat(driver_station_image->height, driver_station_image->width, CV_8UC3, driver_station_image->image);
	cv::cvtColor(grayImage, annot_image, cv::COLOR_GRAY2RGB);

	// printf("Top Left	 : %d %d\n", target_LOC->top_left_x, target_LOC->top_left_y);
	// printf("Bottom Right : %d %d\n", target_LOC->bottom_right_x, target_LOC->bottom_right_y);

	// Outline the target
	Point TL = Point(target_LOC->top_left_x, target_LOC->top_left_y);
	Point TR = Point(target_LOC->top_right_x, target_LOC->top_right_y);
	Point BL = Point(target_LOC->bottom_left_x, target_LOC->bottom_left_y);
	Point BR = Point(target_LOC->bottom_right_x, target_LOC->bottom_right_y);

	// Draw the outline
	cv::line(annot_image, TL, Point(BR.x, TL.y), Scalar(255, 255, 0), 1, LINE_4);
	cv::line(annot_image, Point(BR.x, TL.y), BR, Scalar(255, 255, 0), 1, LINE_4);
	cv::line(annot_image, BR, Point(TL.x, BR.y), Scalar(255, 255, 0), 1, LINE_4);
	cv::line(annot_image, Point(TL.x, BR.y), TL, Scalar(255, 255, 0), 1, LINE_4);

	// Draw the Circle
	cv::circle(annot_image, MT, 3, Scalar(0, 255, 0), -1, LINE_8, 0);

	// Show the images Side by Side
	cv::Point2i Loc_1 = Point((int)((input_image->width) * (1 / 20.0)), (int)(input_image->height * (48 / 60.0)));
	cv::Point2i Loc_2 = Point((int)((input_image->width) * (1 / 20.0)), (int)(input_image->height * (51 / 60.0)));
	cv::Point2i Loc_3 = Point((int)((input_image->width) * (1 / 20.0)), (int)(input_image->height * (54 / 60.0)));
	cv::Point2i Loc_4 = Point((int)((input_image->width) * (1 / 20.0)), (int)(input_image->height * (57 / 60.0)));

	// Annotate the image
	char annotations[FILENAME_MAX] = "";

	double rounded_distance = round(target_distance * 1000);
	rounded_distance /= 1000;

	//std::cout << rounded_distance << std::endl;

	if (target_LOC->checkValidity())
	{
		sprintf(annotations, "Bot Distance : %5.5g ft", rounded_distance);
		cv::putText(annot_image, annotations, Loc_1, cv::FONT_HERSHEY_SIMPLEX, 0.4, Scalar(255, 255, 255));

		sprintf(annotations, "Turret Angle : %5.3g deg", turret_angle);
		cv::putText(annot_image, annotations, Loc_2, cv::FONT_HERSHEY_SIMPLEX, 0.4, Scalar(255, 255, 255));

		sprintf(annotations, "TM(X,Y)      : %u %u", (int)MT.x, (int)MT.y);
		cv::putText(annot_image, annotations, Loc_3, cv::FONT_HERSHEY_SIMPLEX, 0.4, Scalar(255, 255, 255));

		sprintf(annotations, "FPS          : %4.2g", currentFPS);
		cv::putText(annot_image, annotations, Loc_4, cv::FONT_HERSHEY_SIMPLEX, 0.4, Scalar(255, 255, 255));
	}

	return EXIT_SUCCESS;
}


int annotateBallCamImage(Vision_Pipeline* pipeline_data)
{
	// Set up the pointers to be able to re-use the code
	Image_Store* input_image = pipeline_data->scaled_image;
	Image_Store* segmented_image = pipeline_data->thresholded_out;

	// set-up the data
	cv::Mat greyImg = cv::Mat(segmented_image->height, segmented_image->width, CV_8U, segmented_image->image);
	cv::Mat original = cv::Mat(input_image->height, input_image->width, CV_8UC3, input_image->image);

	std::vector<cv::Mat> colorImage(3);
	colorImage.at(0) = greyImg; //for blue channel
	colorImage.at(1) = greyImg; //for green channel
	colorImage.at(2) = greyImg; //for red channel

	cv::Mat seg_image;
	cv::merge(colorImage, seg_image);

	// Draw the Circle
	for (int loop_ball = 0; loop_ball < pipeline_data->object_location.objects_found; loop_ball++)
	{
		cv::Scalar OBJECTCOLOR;

		switch (pipeline_data->object_location.object_color[loop_ball])
		{
		case OBJECTCOLOR::RED:
			OBJECTCOLOR = Scalar(255, 0, 0);
			break;
		case OBJECTCOLOR::GREEN:
			OBJECTCOLOR = Scalar(0, 255, 0);
			break;
		case OBJECTCOLOR::BLUE:
			OBJECTCOLOR = Scalar(0, 0, 255);
			break;
		case OBJECTCOLOR::YELLOW:
			OBJECTCOLOR = Scalar(0, 255, 255);
			break;
		default:
			OBJECTCOLOR = Scalar(255, 255, 255);
			break;
		}

		cv::Point2i ball_loc = Point(pipeline_data->object_location.center_location[loop_ball].x, pipeline_data->object_location.center_location[loop_ball].y);
		cv::circle(seg_image, ball_loc, 10, OBJECTCOLOR, -1, LINE_8, 0);
	}

	cv::Mat combined;

	// Draw the dividing Line
	cv::Point2i T = Point(0, 0);
	cv::Point2i B = Point(0, input_image->height);
	cv::line(seg_image, T, B, Scalar(255, 255, 255), 1, LINE_4);

	cv::cvtColor(original, original, cv::COLOR_RGB2BGR);
	blendImages(original, seg_image, original);

	// Show the images Side by Side
	cv::Point2i Loc_1 = Point((int)(input_image->width / 3.0), 10);
	cv::Point2i Loc_2 = Point((int)(4 * input_image->width / 3.0), 10);

	cv::hconcat(original, seg_image, combined);
	cv::putText(combined, "Segmented Image", Loc_1, cv::FONT_HERSHEY_DUPLEX, 0.4, Scalar(255, 255, 255));
	cv::putText(combined, "Annotated Image", Loc_2, cv::FONT_HERSHEY_DUPLEX, 0.4, Scalar(255, 255, 255));

	if (0)
	{
		// Display the image for debugging purposes
		std::string greyArrWindow = "Color Image";
		cv::namedWindow(greyArrWindow, cv::WINDOW_AUTOSIZE);
		cv::imshow(greyArrWindow, combined);

		cv::waitKey(0);
		cv::destroyAllWindows();
	}

	// Now that we have successfuly overlaid the lines on the image, we need to 
	// Copy the image data back into the ImageStore, so we will create a new 
	// image store to hold the data
	// Since its on the stack, it will be deleted when this function exits.

	cv::cvtColor(combined, combined, cv::COLOR_BGR2RGB);

	// Make sure that we do not leak memory
	if (pipeline_data->annotated_image != NULL)
		delete pipeline_data->annotated_image;

	pipeline_data->annotated_image = new Image_Store(combined.cols, combined.rows, 3, 255, PIXEL_FORMAT::RGB, INTERLEAVE_FORMAT::PIXEL_INTERLEAVED);
	pipeline_data->annotated_image->allocate_memory();

	// Copy over the image data
	memcpy((pipeline_data->annotated_image)->image, combined.data, (combined.rows * combined.cols * 3));

	return EXIT_SUCCESS;
}



int annotateHubCamImage(Vision_Pipeline* pipeline_data)
{
	// Set up the pointers to be able to re-use the code
	Image_Store* input_image = pipeline_data->scaled_image;
	Image_Store* segmented_image = pipeline_data->thresholded_out;
	TargetLocation* target_LOC = pipeline_data->target_LOC;

	// set-up the data
	cv::Mat greyImg = cv::Mat(segmented_image->height, segmented_image->width, CV_8U, segmented_image->image);
	cv::Mat original = cv::Mat(input_image->height, input_image->width, CV_8UC3, input_image->image);

	std::vector<cv::Mat> colorImage(3);
	colorImage.at(0) = greyImg; //for blue channel
	colorImage.at(1) = greyImg; //for green channel
	colorImage.at(2) = greyImg; //for red channel

	cv::Mat seg_image;
	cv::merge(colorImage, seg_image);

	// Outline the target
	Point TL = Point(target_LOC->top_left_x, target_LOC->top_left_y);
	Point TR = Point(target_LOC->top_right_x, target_LOC->top_right_y);
	Point BL = Point(target_LOC->bottom_left_x, target_LOC->bottom_left_y);
	Point BR = Point(target_LOC->bottom_right_x, target_LOC->bottom_right_y);

	// Draw the outline
	cv::line(seg_image, TL, Point(BR.x, TL.y), Scalar(255, 255, 0), 1, LINE_4);
	cv::line(seg_image, Point(BR.x, TL.y), BR, Scalar(255, 255, 0), 1, LINE_4);
	cv::line(seg_image, BR, Point(TL.x, BR.y), Scalar(255, 255, 0), 1, LINE_4);
	cv::line(seg_image, Point(TL.x, BR.y), TL, Scalar(255, 255, 0), 1, LINE_4);

	cv::Point  MT = (TL + TR) / 2.0;

	// Draw the Circle
	cv::circle(seg_image, MT, 3, Scalar(0, 255, 0), -1, LINE_8, 0);

	cv::Mat combined;

	// Draw the dividing Line
	cv::Point2i T = Point(0, 0);
	cv::Point2i B = Point(0, input_image->height);
	cv::line(seg_image, T, B, Scalar(255, 255, 255), 1, LINE_4);

	cv::cvtColor(original, original, cv::COLOR_RGB2BGR);
	blendImages(original, seg_image, original);

	// Show the images Side by Side
	cv::Point2i Loc_1 = Point((int)(input_image->width / 3.0), 10);
	cv::Point2i Loc_2 = Point((int)(4 * input_image->width / 3.0), 10);

	cv::hconcat(original, seg_image, combined);
	cv::putText(combined, "Segmented Image", Loc_1, cv::FONT_HERSHEY_DUPLEX, 0.4, Scalar(255, 255, 255));
	cv::putText(combined, "Annotated Image", Loc_2, cv::FONT_HERSHEY_DUPLEX, 0.4, Scalar(255, 255, 255));

	if (0)
	{
		// Display the image for debugging purposes
		std::string greyArrWindow = "Color Image";
		cv::namedWindow(greyArrWindow, cv::WINDOW_AUTOSIZE);
		cv::imshow(greyArrWindow, combined);

		cv::waitKey(0);
		cv::destroyAllWindows();
	}

	// Now that we have successfuly overlaid the lines on the image, we need to 
	// Copy the image data back into the ImageStore, so we will create a new 
	// image store to hold the data
	// Since its on the stack, it will be deleted when this function exits.

	cv::cvtColor(combined, combined, cv::COLOR_BGR2RGB);

	// Make sure that we do not leak memory
	if (pipeline_data->annotated_image != NULL)
		delete pipeline_data->annotated_image;

	pipeline_data->annotated_image = new Image_Store(combined.cols, combined.rows, 3, 255, PIXEL_FORMAT::RGB, INTERLEAVE_FORMAT::PIXEL_INTERLEAVED);
	pipeline_data->annotated_image->allocate_memory();

	// Copy over the image data
	memcpy((pipeline_data->annotated_image)->image, combined.data, (combined.rows * combined.cols * 3));

	return EXIT_SUCCESS;
}


void draw_dashed_line(cv::Mat img, cv::Point2d p1, cv::Point2d p2, int length_on, int length_off)
{
	// Draw a line itterator
	cv::LineIterator it(img, p1, p2);

	for (int i = 0; i < it.count; i++, it++)
	{
		// Draw on pixels
		for (int onpix = 0; onpix < length_on; onpix++)
		{
			if (i >= it.count)
				break;

			(*it)[0] = 255;
			it++;
			i++;
		}

		// Skip the off pixels
		for (int offpix = 0; offpix < length_off; offpix++)
		{
			if (i >= it.count)
				break;
			it++;
			i++;
		}

	}

}

void blendImages(cv::Mat orig_1, cv::Mat orig_2, cv::Mat output)
{
	// Common code to blend images
	cv::addWeighted(orig_1, 0.5, orig_2, 0.5, 0.0, output);
}
