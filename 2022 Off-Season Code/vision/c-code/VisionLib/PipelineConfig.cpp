#include "PipelineConfig.hpp"
#include "Vision_Model.hpp"
#include "morphology.hpp"
#include "conn_components.hpp"
#include "boundary_detection.h"
#include "opencv2/videoio.hpp"
#include "NetworkTables.h"
#include "LED_Control.h"
#include <fstream>
#include "Camera.h"

#define MAX_JSON_STRING_LENGTH 4096
#define stringlength(array) (sizeof(array) / sizeof(*array))
#define MAX_JSON_ELEMENTS 50
#define MAX_STRING_LENGTH 255

struct JSON_element
{
	char* key[MAX_STRING_LENGTH] = { 0 };
	char* value[MAX_STRING_LENGTH] = { 0 };
};

using namespace cv;

std::string& ltrim(std::string& str, const std::string& chars = "\t\n\v\f\r ")
{
	str.erase(0, str.find_first_not_of(chars));
	return str;
}

std::string& rtrim(std::string& str, const std::string& chars = "\t\n\v\f\r ")
{
	str.erase(str.find_last_not_of(chars) + 1);
	return str;
}

std::string& trim(std::string& str, const std::string& chars = "\"\t\n\v\f\r ")
{
	return ltrim(rtrim(str, chars), chars);
}

PipelineConfig::~PipelineConfig(void)
{
	// Default Destructor - cleans up
}

PipelineConfig::PipelineConfig(Vision_Pipeline* vision_pipe)
{
	// Default Constructor
	vision_pipeline = vision_pipe;
	this->camera_configuration.cameraClass = NULL;
	this->camera_configuration.whichcam = CAMERA_IN_USE::UNDEFINED;

	setDefaultVisionConfiguration();
	setDefaultCameraConfiguration();
	setDefaultSystemConfiguration();
	setDefaultFilteringConfiguration();
	setDefaultProcessingConfiguration();
}

void PipelineConfig::setNetworkTableInstance(NetworkTables* my_network_tables) 
{
	network_table_configuration = my_network_tables;
}


void PipelineConfig::setDefaultVisionConfiguration()
{
	vision_configuration.NT_frame_rate = 15;
	vision_configuration.LED_brightness = 0.25;
	vision_configuration.LED_enabled = false;
}


void PipelineConfig::setDefaultCameraConfiguration()
{
	// Camera Orientation
	camera_configuration.orientation = SENSOR_ORIENTATION::LANDSCAPE;

	// Setting Camera resolution
	camera_configuration.x_resolution = 640;
	camera_configuration.y_resolution = 480;
	camera_configuration.camera_FPS = 30;

	// Setting Exposure Control
	camera_configuration.auto_exposure = false;
	camera_configuration.exposure = 2048.0;

	// Setting Camera Controls
	camera_configuration.brightness = 0.5;
	camera_configuration.saturation = 0.5;
	camera_configuration.contrast = 0.5;
	camera_configuration.sharpness = 0;

	// Setting Camera Color Balance
	camera_configuration.color_balance_R = 1000;
	camera_configuration.color_balance_B = 1000;

	camera_configuration.crop[0] = 0;
	camera_configuration.crop[1] = 0;
	camera_configuration.crop[2] = 0;
	camera_configuration.crop[3] = 0;
}

void PipelineConfig::setDefaultSystemConfiguration()
{

	system_configuration.VisionLib_Debug_Mode = false;								// Variable to enable/disable debug
	system_configuration.VisionLib_Capture_Mode = false;							// Variable to enable capture mode
	system_configuration.VisionLib_debug_frame_inc = 4;								// Frame increment, how often to store frames
	system_configuration.VisionLib_debug_frame_counter = 0;							// Counter to keep track of how many frames have been processed
	strcpy(system_configuration.VisionLib_debug_output_file_str, "test_output");	// Setting up the default output string 

	// Processing Performance
	system_configuration.multithreaded = true;
	system_configuration.executor_service = false;
	system_configuration.number_of_threads = 12;

}

void PipelineConfig::setDefaultFilteringConfiguration()
{
	// Define Filtering Parameters
	filtering_configuration.brightness_threshold[0] = 0;
	filtering_configuration.brightness_threshold[1] = 100;

	filtering_configuration.aspect_ratio[0] = 0;
	filtering_configuration.aspect_ratio[1] = 10;

	filtering_configuration.fullness[0] = 0;
	filtering_configuration.fullness[1] = 100;

	filtering_configuration.chroma_filter[0] = 0;
	filtering_configuration.chroma_filter[1] = 128;

	filtering_configuration.percent_area[0] = 0;
	filtering_configuration.percent_area[1] = 25;

	filtering_configuration.color = OBJECTCOLOR::UNDEFINED;
}

void PipelineConfig::setDefaultProcessingConfiguration()
{

	pipeline_configuration.scalefactor = 1.0;
	pipeline_configuration.CLUT_nodes = 17;

	pipeline_configuration.rebuildCLUTneeded = true;

	pipeline_configuration.dynamic_RGB_scaling = true;
	pipeline_configuration.RGB_ClipLow = 0;
	pipeline_configuration.RGB_ClipHigh = 255;

	pipeline_configuration.L_ClipLow = 0;
	pipeline_configuration.L_ClipHigh = 100;

	pipeline_configuration.chroma_ClipLow = 0;
	pipeline_configuration.chroma_ClipHigh = 128;

	pipeline_configuration.hue_ClipLow = 100;
	pipeline_configuration.hue_ClipHigh = 200;

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// NOTE :
	// GIMP uses values between 0 - 100, so they need to be scaled BEFORE shifting
	// Photoshop uses values between 0-255
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	// Set the number of color centers
	pipeline_configuration.Ref_Colors = 1;

	// Colorimetric aims for Green Target
	pipeline_configuration.Ref_L[0] = 55;
	pipeline_configuration.Ref_a[0] = (23 * 2.55) - 128;  // The original values are uint8 encoded, so shifting to a*;
	pipeline_configuration.Ref_b[0] = (71 * 2.55) - 128;  // The original values are uint8 encoded, so shifting to b*;

	//// Colorimetric aims for Green Target
	//pipeline_configuration.Ref_L = 84;
	//pipeline_configuration.Ref_a = (20 * 2.55) - 128;  // The original values are uint8 encoded, so shifting to a*;
	//pipeline_configuration.Ref_b = (75 * 2.55) - 128;  // The original values are uint8 encoded, so shifting to b*;

	//if (shoot_mode == TARGET_DETECTION_TYPE::BALL)
	//{
	//	// GIMP Values here
	//	// Colorimetric aims for Yellow ball
	//	L_ref = 93;
	//	A_ref = (44 * 2.55) - 128;   // The original values are uint8 encoded, so shifting to a*
	//	B_ref = (86 * 2.55) - 128;   // The original values are uint8 encoded, so shifting to b*
	//	threshold = 0.85;
	//}

	// Binarization Thereshold
	pipeline_configuration.threshold = 0.65;

	// Post Binarization Correction
	pipeline_configuration.morphological_operator = MORPHOLOGY_METHOD::OPENING;

	// Connected Components Configuration
	pipeline_configuration.segmentation_method = SEGMENTATION_METHOD::MARK;
	pipeline_configuration.conn_components_connectivity = N_TYPE::N8;

	// Setting the default boundary method
	pipeline_configuration.boundary_method = BOUNDARY_METHOD::DIVIDE_CONQUOR;

	// Target Detection Mode
	pipeline_configuration.segmentation_mode = TARGET_DETECTION_TYPE::HEXAGON;

}


//---------------------------------------------------------------------------------------
// Vision State Parameters
//---------------------------------------------------------------------------------------/

// Setting up the Vision State Entries
void PipelineConfig::setVisionLEDBrightness(double brightness) {
	vision_configuration.LED_brightness = brightness;

	// Need to add code here to call in to set the network table state
	printf("LED Brightness : %g\n", vision_configuration.LED_brightness);
}

void PipelineConfig::setVisionLEDEnabled(bool on_off) {
	vision_configuration.LED_enabled = on_off;

	// Need to add code here to call in to set the network table state
	printf("LED Enabled : %d\n", vision_configuration.LED_enabled);
}

// Retreiving the Vision State Entries
double PipelineConfig::getVisionLEDBrightness() {
	return vision_configuration.LED_brightness;
}

bool PipelineConfig::getVisionLEDEnabled() {
	return vision_configuration.LED_enabled;
}


//---------------------------------------------------------------------------------------
// Debug Mode Parameters
//---------------------------------------------------------------------------------------

void PipelineConfig::setVisionLibDebugMode(bool debug_status)
{
	if (debug_status) {
		system_configuration.VisionLib_Debug_Mode = true;
	}
	else {
		system_configuration.VisionLib_Debug_Mode = false;
	}
	printf("Debug Mode : %d\n", system_configuration.VisionLib_Debug_Mode);

}

void PipelineConfig::setVisionLibCaptureMode(bool capture_mode)
{
	if (capture_mode) {
		system_configuration.VisionLib_Capture_Mode = true;
	}
	else {
		system_configuration.VisionLib_Capture_Mode = false;
	}
	printf("Capture Mode : %d\n", system_configuration.VisionLib_Capture_Mode);
}

void PipelineConfig::setVisionLibDebugStr(const char* debug_str)
{
	// Trim the string 
	std::string temp_string = debug_str;
	strcpy(system_configuration.VisionLib_debug_output_file_str, trim(temp_string).c_str());
	printf("Debug String : %s\n", system_configuration.VisionLib_debug_output_file_str);
}

void PipelineConfig::setVisionLibCaptureInc(int frame_inc)
{
	system_configuration.VisionLib_debug_frame_inc = frame_inc;
	printf("Capture Inc : %d\n", system_configuration.VisionLib_debug_frame_inc);
}

void PipelineConfig::incrementVisionLibFrameCounter() {
	system_configuration.VisionLib_debug_frame_counter++;
}

void PipelineConfig::setMultiThreaded(bool enable_Multithreaded) {
	system_configuration.multithreaded = enable_Multithreaded;
	printf("Enable MultiThreaded : %d \n", system_configuration.multithreaded);
}

void PipelineConfig::setExecutorService(bool enable_ExecutorService) {
	system_configuration.executor_service = enable_ExecutorService;
	printf("Enable Executor Service : %d \n", system_configuration.executor_service);

	if ( enable_ExecutorService == true ) 
	{
		vision_pipeline->enableExecutorService();
	}
	else if ( enable_ExecutorService == false )
	{
		vision_pipeline->disableExecutorService();
	}

}

void PipelineConfig::setNumberOfThreads(int number_of_threads) {
	system_configuration.number_of_threads = number_of_threads;
	printf("Number of Threads : %d \n", system_configuration.number_of_threads);
}

bool PipelineConfig::getVisionLibDebugMode() {
	return system_configuration.VisionLib_Debug_Mode;
}

bool PipelineConfig::getVisionLibCaptureMode() {
	return system_configuration.VisionLib_Capture_Mode;
}

char* PipelineConfig::getVisionLibDebugStr() {
	return system_configuration.VisionLib_debug_output_file_str;
}

int PipelineConfig::getVisionLibCaptureInc() {
	return system_configuration.VisionLib_debug_frame_inc;
}

int PipelineConfig::getVisionLibFrameCounter() {
	return system_configuration.VisionLib_debug_frame_counter;
}

bool PipelineConfig::getMultiThreaded() {
	return system_configuration.multithreaded;
}

bool PipelineConfig::getExecutorService() {
	return system_configuration.executor_service;
}

int PipelineConfig::getNumberOfThreads() {
	return system_configuration.number_of_threads;
}


//---------------------------------------------------------------------------------------
// Filtering Parameters
//---------------------------------------------------------------------------------------

// Setting the Filtering Parameters
void PipelineConfig::setBrightnessRange(double low, double high) {
	filtering_configuration.brightness_threshold[0] = low;
	filtering_configuration.brightness_threshold[1] = high;
	printf("Brightness Range :[  %g , %g ]\n", low, high);
}

void PipelineConfig::setAspectRatioRange(double low, double high) {
	filtering_configuration.aspect_ratio[0] = low;
	filtering_configuration.aspect_ratio[1] = high;
	printf("Aspect Ratio Range :[  %g , %g ]\n", low, high);
}

void PipelineConfig::setFullnessRange(double low, double high) {
	filtering_configuration.fullness[0] = low;
	filtering_configuration.fullness[1] = high;
	printf("Fullness Range :[  %g , %g ]\n", low, high);
}

void PipelineConfig::setChromaRange(double low, double high) {
	filtering_configuration.chroma_filter[0] = low;
	filtering_configuration.chroma_filter[1] = high;
	printf("Chroma Range :[  %g , %g ]\n", low, high);
}

void PipelineConfig::setPercentAreaRange(double low, double high) {
	filtering_configuration.percent_area[0] = low;
	filtering_configuration.percent_area[1] = high;
	printf("Percent Area Range :[  %g , %g ]\n", low, high);
}

void PipelineConfig::setColorFilter( OBJECTCOLOR objectColor ) {
	char objectColorStr[50] = { 0 };
	filtering_configuration.color = objectColor;
	getObjectColorString(objectColor, objectColorStr);
	printf("Object Color : %s\n", objectColorStr );
}


// Retrieving the Filtering Parameters
void PipelineConfig::getBrightnessRange(double* low, double* high) {
	low[0] = filtering_configuration.brightness_threshold[0];
	high[0] = filtering_configuration.brightness_threshold[1];
}

void PipelineConfig::getAspectRatioRange(double* low, double* high) {
	low[0] = filtering_configuration.aspect_ratio[0];
	high[0] = filtering_configuration.aspect_ratio[1];
}

void PipelineConfig::getFullnessRange(double* low, double* high) {
	low[0] = filtering_configuration.fullness[0];
	high[0] = filtering_configuration.fullness[1];
}

void PipelineConfig::getChromaRange(double* low, double* high) {
	low[0] = filtering_configuration.chroma_filter[0];
	high[0] = filtering_configuration.chroma_filter[1];
}

void PipelineConfig::getPercentAreaRange(double* low, double* high) {
	low[0] = filtering_configuration.percent_area[0];
	high[0] = filtering_configuration.percent_area[1];
}

void PipelineConfig::getColorFilter(OBJECTCOLOR* objectColor) {
	objectColor[0] = filtering_configuration.color;
}

void PipelineConfig::getColorFilter( char* colorString ) {
	getObjectColorString(filtering_configuration.color, colorString);
}

//---------------------------------------------------------------------------------------
// Processing Pipeline Parameters
//---------------------------------------------------------------------------------------

void PipelineConfig::setScaleFactor(double scalefactor) {
	printf("Scale Factor : %g \n", scalefactor);
	pipeline_configuration.scalefactor = scalefactor;
}

void PipelineConfig::setDynamicRGBScaling(bool enableDynamic) {
	printf("Dynamic RGB Scaling : %d \n", enableDynamic);
	pipeline_configuration.dynamic_RGB_scaling = enableDynamic;
}

void PipelineConfig::setRebuildCLUTFlag(bool rebuildNeeded) {
	printf("CLUT Rebuild Needed : %d \n", rebuildNeeded);
	pipeline_configuration.rebuildCLUTneeded = rebuildNeeded;
}

void PipelineConfig::setRGB_ClippingLUT(double low_clip, double high_clip) {
	printf("RGB Clipping LUT Control Points :[  %g , %g ]\n", low_clip, high_clip);

	pipeline_configuration.RGB_ClipLow = low_clip;
	pipeline_configuration.RGB_ClipHigh = high_clip;
	pipeline_configuration.rebuildCLUTneeded = true;
}

void PipelineConfig::setL_ClippingLUT(double low_clip, double high_clip) {
	printf("Lightness Clipping LUT Control Points :[  %g , %g ]\n", low_clip, high_clip);

	pipeline_configuration.L_ClipLow = low_clip;
	pipeline_configuration.L_ClipHigh = high_clip;
	pipeline_configuration.rebuildCLUTneeded = true;
}

void PipelineConfig::setChroma_ClippingLUT(double low_clip, double high_clip) {
	printf("Chroma Clipping LUT Control Points :[  %g , %g ]\n", low_clip, high_clip);

	pipeline_configuration.chroma_ClipLow = low_clip;
	pipeline_configuration.chroma_ClipHigh = high_clip;
	pipeline_configuration.rebuildCLUTneeded = true;
}

void PipelineConfig::setHue_ClippingLUT(double low_clip, double high_clip) {
	printf("Hue Clipping LUT Control Points :[  %g , %g ]\n", low_clip, high_clip);

	pipeline_configuration.hue_ClipLow = low_clip;
	pipeline_configuration.hue_ClipHigh = high_clip;
	pipeline_configuration.rebuildCLUTneeded = true;
}

// Setting the values for the Color Targets
void PipelineConfig::setNumTargetColors( int num_colors ) {
	pipeline_configuration.Ref_Colors = num_colors;
}

// Setting the values for the Color Targets
void PipelineConfig::setTargetLab(double L_ref, double a_ref, double b_ref) {
	// Since not ColorID Ref was declared, using the first
	setTargetLab( 0, L_ref, a_ref, b_ref);
}

// Setting the values for the Color Targets
void PipelineConfig::setTargetLab( int colorID, double L_ref, double a_ref, double b_ref) 
{
	printf("Ref # %d\n", colorID );
	printf("Ref L : %g \n", L_ref);
	printf("Ref a : %g \n", a_ref);
	printf("Ref b : %g \n", b_ref);

	pipeline_configuration.Ref_L[colorID] = L_ref;
	pipeline_configuration.Ref_a[colorID] = a_ref;
	pipeline_configuration.Ref_b[colorID] = b_ref;
	pipeline_configuration.rebuildCLUTneeded = true;
}

void PipelineConfig::setTargetL(double L_ref) {
	printf("Ref L : %g \n", L_ref);
	pipeline_configuration.Ref_L[0] = L_ref;
	pipeline_configuration.rebuildCLUTneeded = true;
}

void PipelineConfig::setTargeta(double a_ref) {
	printf("Ref a : %g \n", a_ref);
	pipeline_configuration.Ref_a[0] = a_ref;
	pipeline_configuration.rebuildCLUTneeded = true;
}

void PipelineConfig::setTargetb(double b_ref) {
	printf("Ref b : %g \n", b_ref);
	pipeline_configuration.Ref_b[0] = b_ref;
	pipeline_configuration.rebuildCLUTneeded = true;
}

void PipelineConfig::setThreshold(double threshold) {
	printf("Threshold : %g \n", threshold);
	pipeline_configuration.threshold = threshold;
}

void PipelineConfig::setMorphologyMethod(MORPHOLOGY_METHOD morphological_operator) {
	
	char current_morphology[255];
	pipeline_configuration.morphological_operator = morphological_operator;

	getMorphologyMethodString(current_morphology);

	printf("Morphological Operator : %s \n", current_morphology);
}

void PipelineConfig::setMorphologyMethod(char* morphological_operator)
{
	printf("Morphological Operator : %s \n", morphological_operator);

	if (strstr(morphological_operator, "Erode")) {
		pipeline_configuration.morphological_operator = MORPHOLOGY_METHOD::ERODE;
	}
	else if (strstr(morphological_operator, "Dialate")) {
		pipeline_configuration.morphological_operator = MORPHOLOGY_METHOD::DIALATE;
	}
	else if (strstr(morphological_operator, "Open")) {
		pipeline_configuration.morphological_operator = MORPHOLOGY_METHOD::OPENING;
	}
	else if (strstr(morphological_operator, "Close")) {
		pipeline_configuration.morphological_operator = MORPHOLOGY_METHOD::CLOSING;
	}
	else {
		pipeline_configuration.morphological_operator = MORPHOLOGY_METHOD::DISABLED;
	}
}

void PipelineConfig::setSegmentationMethod(SEGMENTATION_METHOD segmentation_method) {
	printf("Segmentation Method : %d \n", segmentation_method);
	pipeline_configuration.segmentation_method = segmentation_method;
}

void PipelineConfig::setSegmentationMethod(char* connectivity)
{
	printf("Segmentation Method : %s \n", connectivity);

	if (strstr(connectivity, "Mark")) {
		pipeline_configuration.segmentation_method = SEGMENTATION_METHOD::MARK;
	}
	else if (strstr(connectivity, "Jacob")) {
		pipeline_configuration.segmentation_method = SEGMENTATION_METHOD::JACOB;
	}
}

void PipelineConfig::setConnectComponentsConnectivity(N_TYPE connectivity) {
	printf("Connected Components Connectivity : %d \n", connectivity);
	pipeline_configuration.conn_components_connectivity = connectivity;
}

void PipelineConfig::setConnectComponentsConnectivity(char* connectivity)
{
	printf("Connected Components Connectivity : %s \n", connectivity);

	if (strstr(connectivity, "N4")) {
		pipeline_configuration.conn_components_connectivity = N_TYPE::N4;
	}
	else if (strstr(connectivity, "N8")) {
		pipeline_configuration.conn_components_connectivity = N_TYPE::N8;
	}
}

void PipelineConfig::setBoundaryDetectionMethod(BOUNDARY_METHOD boundary_method) {
	printf("Boundary Detection Method : %d \n", boundary_method);
	pipeline_configuration.boundary_method = boundary_method;
}

void PipelineConfig::setBoundaryDetectionMethod(char* boundary_method)
{
	printf("Boundary Detection Method : %s \n", boundary_method);

	if (strstr(boundary_method, "Divide")) {
		pipeline_configuration.boundary_method = BOUNDARY_METHOD::DIVIDE_CONQUOR;
	}
	else if (strstr(boundary_method, "ParaBound")) {
		pipeline_configuration.boundary_method = BOUNDARY_METHOD::PARABOUND;
	}
	else if (strstr(boundary_method, "Legacy")) {
		pipeline_configuration.boundary_method = BOUNDARY_METHOD::LEGACY;
	}

}

void PipelineConfig::setTargetType(TARGET_DETECTION_TYPE target_type) {
	printf("Target Type : %d \n", target_type);
	pipeline_configuration.segmentation_mode = target_type;
}

void PipelineConfig::setTargetType(char* target_type)
{

	printf("Target Type : %s \n", target_type);

	if (strstr(target_type, "Hexagon")) {
		pipeline_configuration.segmentation_mode = TARGET_DETECTION_TYPE::HEXAGON;
	}
	else if (strstr(target_type, "Ball")) {
		pipeline_configuration.segmentation_mode = TARGET_DETECTION_TYPE::BALL;
	}
	else if (strstr(target_type, "Hub")) {
		pipeline_configuration.segmentation_mode = TARGET_DETECTION_TYPE::HUB;
	}
	else {
		pipeline_configuration.segmentation_mode = TARGET_DETECTION_TYPE::UNDEFINED;
	}
}


// Retrieving the Color Target Values

double PipelineConfig::getScaleFactor() {
	return pipeline_configuration.scalefactor;
}

bool PipelineConfig::getDynamicRGBScaling() {
	return pipeline_configuration.dynamic_RGB_scaling;
}

bool PipelineConfig::getRebuildCLUTNeeded() {
	return pipeline_configuration.rebuildCLUTneeded;
}

void PipelineConfig::getRGB_ClippingLUT(double* low_clip, double* high_clip) {
	low_clip[0] = pipeline_configuration.RGB_ClipLow;
	high_clip[0] = pipeline_configuration.RGB_ClipHigh;
}

void PipelineConfig::getL_ClippingLUT(double* low_clip, double* high_clip) {
	low_clip[0] = pipeline_configuration.L_ClipLow;
	high_clip[0] = pipeline_configuration.L_ClipHigh;
}

void PipelineConfig::getChroma_ClippingLUT(double* low_clip, double* high_clip) {
	low_clip[0] = pipeline_configuration.chroma_ClipLow;
	high_clip[0] = pipeline_configuration.chroma_ClipHigh;
}

void PipelineConfig::getHue_ClippingLUT(double* low_clip, double* high_clip) {
	low_clip[0] = pipeline_configuration.hue_ClipLow;
	high_clip[0] = pipeline_configuration.hue_ClipHigh;
}

double PipelineConfig::getTargetL() {
	return getTargetL( 0 );
}

double PipelineConfig::getTargetA() {
	return getTargetA( 0 );
}

double PipelineConfig::getTargetB() {
	return getTargetB( 0 );
}

double PipelineConfig::getTargetL( int colorID ) {
	return pipeline_configuration.Ref_L[colorID];
}

double PipelineConfig::getTargetA( int colorID ) {
	return pipeline_configuration.Ref_a[colorID];
}

double PipelineConfig::getTargetB( int colorID ) {
	return pipeline_configuration.Ref_b[colorID];
}

int PipelineConfig::getNumTargetColors() {
	return pipeline_configuration.Ref_Colors;
}

double PipelineConfig::getThreshold() {
	return pipeline_configuration.threshold;
}

MORPHOLOGY_METHOD PipelineConfig::getMorphologyMethod() {
	return pipeline_configuration.morphological_operator;
}

void PipelineConfig::getMorphologyMethodString(char* returnStr) {
	switch (pipeline_configuration.morphological_operator)
	{
	case MORPHOLOGY_METHOD::CLOSING:
		sprintf(returnStr, "Close");
		break;
	case MORPHOLOGY_METHOD::OPENING:
		sprintf(returnStr, "Open");
		break;
	case MORPHOLOGY_METHOD::ERODE:
		sprintf(returnStr, "Erode");
		break;
	case MORPHOLOGY_METHOD::DIALATE:
		sprintf(returnStr, "Dialate");
		break;
	default:
		sprintf(returnStr, "Off");
		break;
	}
}

SEGMENTATION_METHOD PipelineConfig::getSegmentationMethod() {
	return pipeline_configuration.segmentation_method;
}

void PipelineConfig::getSegmentationMethodString(char* returnStr) {
	switch (pipeline_configuration.segmentation_method)
	{
	case SEGMENTATION_METHOD::JACOB:
		sprintf(returnStr, "Jacob");
		break;
	case SEGMENTATION_METHOD::MARK:
	default:
		sprintf(returnStr, "Mark");
	}
}

N_TYPE PipelineConfig::getConnectComponentsConnectivity() {
	return pipeline_configuration.conn_components_connectivity;
}

void PipelineConfig::getConnectComponentsConnectivityString(char* returnStr) {
	switch (pipeline_configuration.conn_components_connectivity)
	{
	case N_TYPE::N4:
		sprintf(returnStr, "N4");
		break;
	case N_TYPE::N8:
	default:
		sprintf(returnStr, "N8");
	}
}

BOUNDARY_METHOD PipelineConfig::getBoundaryDetectionMethod() {
	return pipeline_configuration.boundary_method;
}

void PipelineConfig::getBoundaryDetectionMethodString(char* returnStr) {
	switch (pipeline_configuration.boundary_method)
	{
	case BOUNDARY_METHOD::LEGACY:
		sprintf(returnStr, "Legacy");
		break;
	case BOUNDARY_METHOD::PARABOUND:
		sprintf(returnStr, "Parabound");
		break;
	case BOUNDARY_METHOD::DIVIDE_CONQUOR:
	default:
		sprintf(returnStr, "Divide");
	}
}

TARGET_DETECTION_TYPE PipelineConfig::getTargetType() {
	return pipeline_configuration.segmentation_mode;
}

void PipelineConfig::getTargetTypeString(char* returnStr) {
	switch (pipeline_configuration.segmentation_mode)
	{
	case TARGET_DETECTION_TYPE::HUB:
		sprintf(returnStr, "Hub");
		break;
	case TARGET_DETECTION_TYPE::BALL:
		sprintf(returnStr, "Ball");
		break;
	case TARGET_DETECTION_TYPE::HEXAGON:
		sprintf(returnStr, "Hexagon");
		break;
	default:
		sprintf(returnStr, "Undefined");
	}
}


void PipelineConfig::estimatePixelLabValues(double Pixel_X, double Pixel_Y, char* client_msg)
{
	if (vision_pipeline == NULL) {
		printf("Vision Pipleine Pointer not setup");
		client_msg[0] = 0;
		return;
	}

	// Extract the values from the image
	if (vision_pipeline->scaled_image != NULL)
	{

		// Image Dimensions
		int width = vision_pipeline->scaled_image->width;
		int height = vision_pipeline->scaled_image->height;
		int planes = vision_pipeline->scaled_image->planes;
		int offset = planes * ((Pixel_Y * width) + Pixel_X);

		Pixel_X = range(Pixel_X, 0.0, (double)width - 1);
		Pixel_Y = range(Pixel_Y, 0.0, (double)height - 1);

		uint8_t RGB[3] = { 0 };
		double  Lab[3] = { 0 };
		double  LCh[3] = { 0 };

		RGB[0] = vision_pipeline->scaled_image->image[offset++];
		RGB[1] = vision_pipeline->scaled_image->image[offset++];
		RGB[2] = vision_pipeline->scaled_image->image[offset];

		printf("RGB Co-Ordinates     R : %u  G : %u  B : %u\n", RGB[0], RGB[1], RGB[2]);

		// Run the image through the pipeline
		vision_pipeline->CLUT_table->color_process_rgb_pixel(RGB, Lab, LCh);

		printf("CIELAB Co-Ordinates  L : %g  a : %g  b : %g\n", Lab[0], Lab[1], Lab[2]);
		printf("CIELCh Co-Ordinates  L : %g  C : %g  h : %g\n", LCh[0], LCh[1], LCh[2]);

		// Construct the JSON Response
		sprintf(client_msg, "{ \"type\" : \"pixelColorValues\" ");
		sprintf(client_msg, "%s, \"RGBValues\" : \"[ %u %u %u ]\" ", client_msg, RGB[0], RGB[1], RGB[2]);
		sprintf(client_msg, "%s, \"CIELabValues\" : \"[ %g %g %g ]\" ", client_msg, round(Lab[0]), round(Lab[1]), round(Lab[2]));
		sprintf(client_msg, "%s, \"CIELChValues\" : \"[ %g %g %g ]\" }", client_msg, round(LCh[0]), round(LCh[1]), round(LCh[2]));

	}
	else
	{
		client_msg[0] = 0;
	}
}


//---------------------------------------------------------------------------------------
// Camera Attributes
//---------------------------------------------------------------------------------------

Camera* PipelineConfig::getCameraConfiguration() {
	return this->camera_configuration.cameraClass ;
}


SENSOR_ORIENTATION PipelineConfig::getSensorOrientation() {
	return camera_configuration.orientation;
}

void PipelineConfig::getSensorOrientationString(char* returnStr) {
	switch (camera_configuration.orientation)
	{
	case SENSOR_ORIENTATION::PORTRAIT_UD:
		sprintf(returnStr, "270");
		break;
	case SENSOR_ORIENTATION::LANDSCAPE_UD:
		sprintf(returnStr, "180");
		break;
	case SENSOR_ORIENTATION::PORTRAIT:
		sprintf(returnStr, "90");
		break;
	case SENSOR_ORIENTATION::LANDSCAPE:
	default:
		sprintf(returnStr, "0");
	}
}

bool PipelineConfig::getCameraState() {
	return camera_configuration.cameraReady;
}

int PipelineConfig::getCameraXResolution() {
	return camera_configuration.x_resolution;
}

int PipelineConfig::getCameraYResolution() {
	return camera_configuration.y_resolution;
}

int PipelineConfig::getCameraFramesPerSecond() {
	return camera_configuration.camera_FPS;
}

bool PipelineConfig::getCameraAutoExposure() {
	return camera_configuration.auto_exposure;
}

double PipelineConfig::getCameraManualExposure() {
	return camera_configuration.exposure;
}

double PipelineConfig::getCameraBrightness() {
	return camera_configuration.brightness;
}

double PipelineConfig::getCameraSaturation() {
	return camera_configuration.saturation;
}

double PipelineConfig::getCameraContrast() {
	return camera_configuration.contrast;
}

double PipelineConfig::getCameraSharpness() {
	return camera_configuration.sharpness;
}

int PipelineConfig::getCameraColorBalanceR() {
	return camera_configuration.color_balance_R;
}

int PipelineConfig::getCameraColorBalanceB() {
	return camera_configuration.color_balance_B;
}

double PipelineConfig::getCameraCropLeft() {
	return camera_configuration.crop[0];
}

double PipelineConfig::getCameraCropRight() {
	return camera_configuration.crop[1];
}

double PipelineConfig::getCameraCropTop() {
	return camera_configuration.crop[2];
}

double PipelineConfig::getCameraCropBottom() {
	return camera_configuration.crop[3];
}


void PipelineConfig::setSensorOrientation(int orientation)
{
	// Create a pointer to current camera
	Camera* currentCamera = this->camera_configuration.cameraClass;

	// Define a string to hold the rotation command
	char cmd1[100];
	char cmd2[100];

	if (orientation == 0) {
		camera_configuration.orientation = SENSOR_ORIENTATION::LANDSCAPE;
		if (currentCamera != NULL) {
			sprintf(cmd1, "v4l2-ctl -c horizontal_flip=0 -d %d\n", currentCamera->cameraID);
			sprintf(cmd2, "v4l2-ctl -c vertical_flip=0 -d %d\n", currentCamera->cameraID);
		}
	}
	else if (orientation == 90) {
		camera_configuration.orientation = SENSOR_ORIENTATION::PORTRAIT;
		if (currentCamera != NULL) {
			sprintf(cmd1, "v4l2-ctl -c horizontal_flip=0 -d %d\n", currentCamera->cameraID);
			sprintf(cmd2, "v4l2-ctl -c vertical_flip=0 -d %d\n", currentCamera->cameraID);
		}
	}
	else if (orientation == 180) {
		camera_configuration.orientation = SENSOR_ORIENTATION::LANDSCAPE_UD;
		if (currentCamera != NULL) {
			sprintf(cmd1, "v4l2-ctl -c horizontal_flip=1 -d %d\n", currentCamera->cameraID);
			sprintf(cmd2, "v4l2-ctl -c vertical_flip=1 -d %d\n", currentCamera->cameraID);
		}
	}
	else if (orientation == 270) {
		camera_configuration.orientation = SENSOR_ORIENTATION::PORTRAIT_UD;
		if (currentCamera != NULL) {
			sprintf(cmd1, "v4l2-ctl -c horizontal_flip=1 -d %d\n", currentCamera->cameraID);
			sprintf(cmd2, "v4l2-ctl -c vertical_flip=1 -d %d\n", currentCamera->cameraID);
		}
	}

	if (currentCamera != NULL)
	{
		printf(cmd1);
		printf(cmd2);

		system(cmd1);
		system(cmd2);
	}

	printf("Set Orientation : %d\n", orientation);
} 

void PipelineConfig::setSensorOrientation(SENSOR_ORIENTATION orientation) 
{
	switch (orientation) 
	{
		case SENSOR_ORIENTATION::PORTRAIT:
			setSensorOrientation(90);
			break;
		case SENSOR_ORIENTATION::LANDSCAPE_UD:
			setSensorOrientation(180);
			break;
		case SENSOR_ORIENTATION::PORTRAIT_UD:
			setSensorOrientation(270);
			break;
		case SENSOR_ORIENTATION::LANDSCAPE:
		default:
			setSensorOrientation(0);
	}

}

void PipelineConfig::setCameraXResolution(int resX) {
	camera_configuration.x_resolution = resX;
	printf("Set X Resolution : %d\n", resX);
}

void PipelineConfig::setCameraYResolution(int resY) {
	camera_configuration.y_resolution = resY;
	printf("Set Y Resolution : %d\n", resY);
}

void PipelineConfig::setCameraFramesPerSecond(int FPS) {
	camera_configuration.camera_FPS = FPS;
	printf("Set Camera FPS : %d\n", FPS);
}

void PipelineConfig::setCameraAutoExposure(bool auto_exposure) {
	camera_configuration.auto_exposure = auto_exposure;
	printf("Auto Exposure : %d\n", auto_exposure);
}

void PipelineConfig::setCameraManualExposure(double exposure) {
	camera_configuration.exposure = exposure;
	printf("Manual Exposure : %g\n", exposure);
}

void PipelineConfig::setCameraBrightness(double brightness) {
	camera_configuration.brightness = brightness / 100.0;
	printf("Brightness : %f\n", camera_configuration.brightness);
}

void PipelineConfig::setCameraSaturation(double saturation) {
	camera_configuration.saturation = saturation / 100.0;
	printf("Saturation : %f\n", camera_configuration.saturation);
}

void PipelineConfig::setCameraContrast(double contrast) {
	camera_configuration.contrast = contrast / 100.0;
	printf("Contrast : %f\n", camera_configuration.contrast);
}

void PipelineConfig::setCameraSharpness(double sharpness) {
	camera_configuration.sharpness = sharpness / 100.0;
	printf("Sharpness : %f\n", camera_configuration.sharpness);
}

void PipelineConfig::setCameraColorBalanceR(int balanceR) {
	camera_configuration.color_balance_R = balanceR;
	printf("Color Balance R : %d\n", camera_configuration.color_balance_R);
}

void PipelineConfig::setCameraColorBalanceB(int balanceB) {
	camera_configuration.color_balance_B = balanceB;
	printf("Color Balance B : %d\n", camera_configuration.color_balance_B);
}

void PipelineConfig::setCameraCropLeft(double cropL) {
	camera_configuration.crop[0] = cropL;
	printf("Crop left :  %g\n", camera_configuration.crop[0]);
}

void  PipelineConfig::setCameraCropRight(double cropR) {
	camera_configuration.crop[1] = cropR;
	printf("Crop right : %g\n", camera_configuration.crop[1]);
}

void PipelineConfig::setCameraCropTop(double cropT) {
	camera_configuration.crop[2] = cropT;
	printf("Crop top : %g\n", camera_configuration.crop[2]);
}

void PipelineConfig::setCameraCropBottom(double cropB) {
	camera_configuration.crop[3] = cropB;
	printf("Crop bottom : %g\n", camera_configuration.crop[3]);
}

void PipelineConfig::setCameraConfiguration( Camera* camInfo, CAMERA_IN_USE whichcam) {

	// this->camera_configuration.deviceID = camInfo->cameraID;
	// this->camera_configuration.camera = camInfo->cam;
	this->camera_configuration.whichcam = whichcam;
	this->camera_configuration.cameraClass = camInfo;
}

void PipelineConfig::setCameraCaptureSettings()
{
	// Create a pointer to current camera
	Camera* currentCamera = this->camera_configuration.cameraClass;

	if (currentCamera == NULL) {
		// We do not have a camera configured yet, so cannot run this code		
		printf("Cannot configure camera. Have you set it up yet?\n");
		return;
	}

	// Disable the camera until we are done
	camera_configuration.cameraReady = false;

	if (camera_configuration.whichcam == CAMERA_IN_USE::TURRETCAM)
	{
		// printf("Camera X Res : %i\n", camera_configuration.x_resolution);
		// printf("Camera Y Res : %i\n", camera_configuration.y_resolution);

		currentCamera->cam->set(cv::CAP_PROP_FRAME_WIDTH, camera_configuration.x_resolution);
		currentCamera->cam->set(cv::CAP_PROP_FRAME_HEIGHT, camera_configuration.y_resolution);
		currentCamera->cam->set(cv::CAP_PROP_FOURCC, cv::VideoWriter::fourcc('R', 'G', 'B', '3'));
		currentCamera->cam->set(cv::CAP_PROP_ISO_SPEED, 1);
	}
	else // BALLCAM CASE
	{
		currentCamera->cam->set(cv::CAP_PROP_FRAME_WIDTH, camera_configuration.x_resolution);
		currentCamera->cam->set(cv::CAP_PROP_FRAME_HEIGHT, camera_configuration.y_resolution);
		// currentCamera->cam->set(cv::CAP_PROP_FOURCC, cv::VideoWriter::fourcc('Y', 'U', 'Y', 'V'));
		currentCamera->cam->set(cv::CAP_PROP_FOURCC, cv::VideoWriter::fourcc('M', 'J', 'P', 'G'));
	}

	// Set Camera Orientation
	setSensorOrientation(camera_configuration.orientation);

	// Set the buffer size
	currentCamera->cam->set(cv::CAP_PROP_BUFFERSIZE, 1);

	// Re-enable the camera
	camera_configuration.cameraReady = true;
}

void PipelineConfig::setCameraTargetSettings()
{
	// Create a pointer to current camera
	Camera* currentCamera = this->camera_configuration.cameraClass;

	if (currentCamera == NULL) {
		// We do not have a camera configured yet, so cannot run this code		
		printf("Cannot configure camera. Have you set it up yet?\n");
		return;
	}

	// Disable the camera until we are done
	camera_configuration.cameraReady = false;

	currentCamera->setAutoExposure( camera_configuration.auto_exposure );		// Auto Exposure Control
	currentCamera->setExposure( camera_configuration.exposure );				// Set the manual exposure

	currentCamera->cam->set(cv::CAP_PROP_ROLL, 0);

	// Now set the Brightness / Contrast adjustment values 
	currentCamera->setBrightness( camera_configuration.brightness );
	currentCamera->setContrast( camera_configuration.contrast);	
	currentCamera->setSaturation( camera_configuration.saturation);		
	currentCamera->setSharpness( camera_configuration.sharpness);		 

	currentCamera->setColorBalanceR( camera_configuration.color_balance_R );
	currentCamera->setColorBalanceB( camera_configuration.color_balance_B );

	// Re-enable the camera
	camera_configuration.cameraReady = true;
}

void PipelineConfig::setCameraFieldSettings()
{
	// Create a pointer to current camera
	Camera* currentCamera = this->camera_configuration.cameraClass;

	if (currentCamera == NULL) {
		// We do not have a camera configured yet, so cannot run this code		
		printf("Cannot configure camera. Have you set it up yet?\n");
		return;
	}

	// Disable the camera until we are done
	camera_configuration.cameraReady = false;

	// Target Specific attribuites can go here
	if (camera_configuration.whichcam == CAMERA_IN_USE::TURRETCAM)
	{
		// Set the Field Viewing Settings
		currentCamera->setAutoExposure(true);									// Auto Exposure Control
	}
	else // BALLCAM CASE
	{
		currentCamera->setAutoExposure(false);									// Manual Exposure Control
		currentCamera->setExposure( camera_configuration.exposure );			// Set the manual exposure
		currentCamera->setGamma(3);
	}

	// Now set the Brightness / Contrast adjustment values 
	currentCamera->setBrightness(camera_configuration.brightness);
	currentCamera->setContrast(camera_configuration.contrast);
	currentCamera->setSaturation(camera_configuration.saturation);
	currentCamera->setSharpness(camera_configuration.sharpness);

	// Re-enable the camera
	camera_configuration.cameraReady = true;

}

void PipelineConfig::printCameraSupportSettings()
{	
	this->camera_configuration.cameraClass->printCameraSupportSettings();
}

int PipelineConfig::getSystemStateConfiguration(char* message, int buffer_length)
{
	// Start out by setting up the JSON string to be returned 
	char messagestring[MAX_JSON_STRING_LENGTH] = { 0 };
	char tempstring[MAX_JSON_STRING_LENGTH] = { 0 };
	int current_position = 0;
	double temp_L, temp_H;

	// Start by building the string 
	current_position += sprintf(message, "{\n\"type\" : \"updateSystemControls\", \n");


	// Get the Pi State Machine
	getShuffleboardEWSState( tempstring );
	current_position += sprintf( messagestring, "\t\"EWSViewingMode\" : \"%s\", \n", tempstring );
	strcat( message, messagestring );


	// System Commands
	current_position += sprintf(messagestring, "\t\"debugMode\" : \"%d\", \n", getVisionLibDebugMode());
	strcat(message, messagestring);
	current_position += sprintf(messagestring, "\t\"captureMode\" : \"%d\", \n", getVisionLibCaptureMode());
	strcat(message, messagestring);
	current_position += sprintf(messagestring, "\t\"captureIncrement\" : \"%d\", \n", getVisionLibCaptureInc());
	strcat(message, messagestring);
	current_position += sprintf(messagestring, "\t\"debugFilename\" : \"%s\", \n", getVisionLibDebugStr());
	strcat(message, messagestring);
	current_position += sprintf(messagestring, "\t\"multi_thread\" : \"%d\", \n", getMultiThreaded());
	strcat(message, messagestring);
	current_position += sprintf(messagestring, "\t\"executor_service\" : \"%d\", \n", getExecutorService());
	strcat(message, messagestring);
	current_position += sprintf(messagestring, "\t\"numberOfThreads\" : \"%d\", \n", getNumberOfThreads());
	strcat(message, messagestring);
	current_position += sprintf(messagestring, "\t\"visionLEDstatus\" : \"%d\", \n", getVisionLEDEnabled());
	strcat(message, messagestring);
	current_position += sprintf(messagestring, "\t\"visionLEDbrightness\" : \"%g\", \n", getVisionLEDBrightness());
	strcat(message, messagestring);


	// Filtering Commands

	getBrightnessRange(&temp_L, &temp_H);
	current_position += sprintf(messagestring, "\t\"brightness_filter\" : \"[ %g %g ]\", \n", temp_L, temp_H);
	strcat(message, messagestring);

	getPercentAreaRange(&temp_L, &temp_H);
	current_position += sprintf(messagestring, "\t\"pct_area_filter\" : \"[ %g %g ]\", \n", temp_L, temp_H);
	strcat(message, messagestring);
	
	getFullnessRange(&temp_L, &temp_H);
	current_position += sprintf(messagestring, "\t\"fullness_filter\" : \"[ %g %g ]\", \n", temp_L, temp_H);
	strcat(message, messagestring);

	getChromaRange(&temp_L, &temp_H);
	current_position += sprintf(messagestring, "\t\"chroma_filter\" : \"[ %g %g ]\", \n", temp_L, temp_H);
	strcat(message, messagestring);

	getAspectRatioRange(&temp_L, &temp_H);
	current_position += sprintf(messagestring, "\t\"aspect_ratio_filter\" : \"[ %g %g ]\", \n", temp_L, temp_H);
	strcat(message, messagestring);

	getColorFilter( tempstring );
	current_position += sprintf(messagestring, "\t\"color_filter\" : \"%s\", \n", tempstring );
	strcat(message, messagestring);


	// Pipeline Commands

	current_position += sprintf(messagestring, "\t\"scaleFactor\" : \"%g\", \n", getScaleFactor());
	strcat(message, messagestring);

	current_position += sprintf(messagestring, "\t\"dynamic_clipping\" : \"%d\", \n", getDynamicRGBScaling());
	strcat(message, messagestring);

	getRGB_ClippingLUT(&temp_L, &temp_H);
	current_position += sprintf(messagestring, "\t\"rgb_clipping\" : \"[ %g %g ]\", \n", temp_L, temp_H);
	strcat(message, messagestring);

	getL_ClippingLUT(&temp_L, &temp_H);
	current_position += sprintf(messagestring, "\t\"lightness_clipping\" : \"[ %g %g ]\", \n", temp_L, temp_H);
	strcat(message, messagestring);

	getChroma_ClippingLUT(&temp_L, &temp_H);
	current_position += sprintf(messagestring, "\t\"chroma_range\" : \"[ %g %g ]\", \n", temp_L, temp_H);
	strcat(message, messagestring);

	getHue_ClippingLUT(&temp_L, &temp_H);
	current_position += sprintf(messagestring, "\t\"hue_range\" : \"[ %g %g ]\", \n", temp_L, temp_H);
	strcat(message, messagestring);

	current_position += sprintf(messagestring, "\t\"L_Ref\" : \"%g\", \n", getTargetL());
	strcat(message, messagestring);
	current_position += sprintf(messagestring, "\t\"a_Ref\" : \"%g\", \n", getTargetA());
	strcat(message, messagestring);
	current_position += sprintf(messagestring, "\t\"b_Ref\" : \"%g\", \n", getTargetB());
	strcat(message, messagestring);
	current_position += sprintf(messagestring, "\t\"threshold\" : \"%g\", \n", getThreshold());
	strcat(message, messagestring);

	getMorphologyMethodString(tempstring);
	current_position += sprintf(messagestring, "\t\"morphology\" : \"%s\", \n", tempstring);
	strcat(message, messagestring);

	getConnectComponentsConnectivityString(tempstring);
	current_position += sprintf(messagestring, "\t\"conn_comp\" : \"%s\", \n", tempstring);
	strcat(message, messagestring);

	getBoundaryDetectionMethodString(tempstring);
	current_position += sprintf(messagestring, "\t\"boundary_detect\" : \"%s\", \n", tempstring);
	strcat(message, messagestring);

	getTargetTypeString(tempstring);
	current_position += sprintf(messagestring, "\t\"target_detect\" : \"%s\", \n", tempstring);
	strcat(message, messagestring);

	getSegmentationMethodString(tempstring);
	current_position += sprintf(messagestring, "\t\"seg_method\" : \"%s\", \n", tempstring);
	strcat(message, messagestring);


	// Camera Commands

	// current_position += sprintf(messagestring, "\t\"tatorVisionCamera\" : \"%d\", \n", messagestring ) ;
	// strcat(message, messagestring);
	// current_position += sprintf(messagestring, "\t\"tatorVisionResolution\" : \"%d\", \n", messagestring ) ;
	// strcat(message, messagestring);

	current_position += sprintf(messagestring, "\t\"tatorVisionframeRate\" : \"%d\", \n", getCameraFramesPerSecond());
	strcat(message, messagestring);

	getSensorOrientationString(tempstring);
	current_position += sprintf(messagestring, "\t\"orientation\" : \"%s\", \n", tempstring);
	strcat(message, messagestring);

	current_position += sprintf(messagestring, "\t\"auto_exposure\" : \"%d\", \n", getCameraAutoExposure());
	strcat(message, messagestring);
	current_position += sprintf(messagestring, "\t\"exposure_value\" : \"%g\", \n", getCameraManualExposure());
	strcat(message, messagestring);
	current_position += sprintf(messagestring, "\t\"brightness_value\" : \"%g\", \n", 100 * getCameraBrightness());
	strcat(message, messagestring);
	current_position += sprintf(messagestring, "\t\"saturation_value\" : \"%g\", \n", 100 * getCameraSaturation());
	strcat(message, messagestring);
	current_position += sprintf(messagestring, "\t\"contrast_value\" : \"%g\", \n", 100 * getCameraContrast());
	strcat(message, messagestring);
	current_position += sprintf(messagestring, "\t\"sharpness_value\" : \"%g\", \n", 100 * getCameraSharpness());
	strcat(message, messagestring);
	current_position += sprintf(messagestring, "\t\"colorbalanceR_value\" : \"%d\", \n", getCameraColorBalanceR());
	strcat(message, messagestring);
	current_position += sprintf(messagestring, "\t\"colorbalanceB_value\" : \"%d\", \n", getCameraColorBalanceB());
	strcat(message, messagestring);
	current_position += sprintf(messagestring, "\t\"CameraCropLeft\" : \"%f\", \n", getCameraCropLeft());
	strcat(message, messagestring);
	current_position += sprintf(messagestring, "\t\"CameraCropRight\" : \"%f\", \n", getCameraCropRight());
	strcat(message, messagestring);
	current_position += sprintf(messagestring, "\t\"CameraCropTop\" : \"%f\", \n", getCameraCropTop());
	strcat(message, messagestring);
	current_position += sprintf(messagestring, "\t\"CameraCropBottom\" : \"%f\" \n",  getCameraCropBottom());
	strcat(message, messagestring);

	current_position += sprintf(messagestring, " }");
	strcat(message, messagestring);

	// printf("JSON String Length : %d\n", current_position);

	// Override	 for testing 
	// current_position = sprintf(message, "{ \"type\" : \"cameraControl\", \"tatorVisionCamera\" : \"0\", \"tatorVisionResolution\" : \"640x480\", \"tatorVisionframeRate\": \"30\", \"orientation\" : \"90\", \"exposure_value\" : \"4096\" , \"brightness_value\" : \"75\" , \"saturation_value\" : \"50\" , \"contrast_value\" : \"50\" , \"sharpness_value\" : \"0\", \"colorbalanceR_value\" : \"1000\" , \"colorbalanceB_value\" : \"1000\" }");
	// current_position = sprintf( message, "{ \"type\": \"pipelineControl\", \"scaleFactor\" : \"1.0\", \"dynamic_clipping\" : \"no\", \"rgb_clipping\" : \"[0 171]\" , \"lightness_clipping\" : \"[0 100]\" , \"chroma_range\" : \"[25 75]\" , \"hue_range\" : \"[200 300]\" , \"L_Ref\" : \"85.00\", \"a_Ref\" : \"37.0\", \"b_Ref\" : \"45.0\", \"threshold\" : \"0.5\", \"morphology\" : \"Open\", \"conn_comp\" : \"N8\", \"boundary_detect\" : \"Divide\", \"target_detect\" : \"Hexagon\" }");
	// current_position = sprintf( message, "{ \"type\": \"systemControl\", \"debugMode\" : \"on\", \"captureMode\" : \"on\", \"captureIncrement\" : \"4\", \"debugFilename\" : \"EWS_Capture\", \"multi_thread\" : \"on\", \"executor_service\" : \"on\", \"numberOfThreads\" : \"4\", \"visionLEDstatus\" : \"on\", \"visionLEDbrightness\" : \"0.35\" }");

	// return the length of the string
	return current_position;
}


void PipelineConfig::setShuffleboardEWSState(char* EWS_state)
{
	if (strstr(EWS_state, "Target_Detect")) {
		printf("Setting viewing state to TARGET DETECTION Configuration\n");
		network_table_configuration->vision_trackingState_new = VISION_STATE::TARGET_DETECTION;
	}
	else if (strstr(EWS_state, "Ball_Detect")) {
		printf("Setting viewing state to BALL DETECTION Configuration\n");
		network_table_configuration->vision_trackingState_new = VISION_STATE::BALL_DETECTION;
	}
	else if (strstr(EWS_state, "Thresholded")) {
		printf("Setting viewing state to THRESHOLDING Configuration\n");
		network_table_configuration->vision_trackingState_new = VISION_STATE::THRESHOLDING;
	}
	else {
		printf("Setting viewing state to SHUFFLEBOARD Configuration\n");
		network_table_configuration->vision_trackingState_new = VISION_STATE::SHUFFLEBOARD;
	}
}


void PipelineConfig::getShuffleboardEWSState( char* EWS_state )
{
	// Gets the current state of the PI Vision State machine
	switch (network_table_configuration->vision_trackingState_current)
	{
		case VISION_STATE::TARGET_DETECTION:
			sprintf(EWS_state, "Target_Detect");
			break;
		case VISION_STATE::BALL_DETECTION:
			sprintf(EWS_state, "Ball_Detect");
			break;
		case VISION_STATE::THRESHOLDING:
			sprintf(EWS_state, "Thresholded");
			break;
		case VISION_STATE::SHUFFLEBOARD:
		default :
			sprintf(EWS_state, "Shuffleboard");
			break;
	}
}


double PipelineConfig::range(double inval, double LB, double UB)
{
	inval = (inval < LB ? LB : inval);
	return inval > UB ? UB : inval;
}

bool PipelineConfig::writeToFile(char* pathToFile, char* data)
{
	// File Pointer
	// FILE* file = fopen("C:\\Users\\Tators 03\\random", "w");
	FILE* file = fopen(pathToFile, "w");

	if (file == NULL) {
		return false;
	}

	// Write out the stream to file
	fprintf(file, "%s", data);
	fclose(file);

	return true;
}


bool PipelineConfig::readFromFile(char* pathToFile)
{
	return readFromFile( pathToFile, NULL, 0 );
}


bool PipelineConfig::readFromFile(char* pathToFile, char* output, int outstr_length )
{
	char tmp_output_stream[MAX_JSON_STRING_LENGTH] = { 0 };
	char* output_stream = tmp_output_stream;

	int character = 0;
	int counter = 0;
	int output_str_len = MAX_JSON_STRING_LENGTH;

	// File Pointer
	FILE* file = fopen(pathToFile, "r");
	if (output != NULL) {
		output_stream = output;
 		output_str_len = outstr_length;
	}

	if (NULL == file) {
		printf("File can't be opened: %s \n", pathToFile);
		return EXIT_FAILURE;
	}

	// Get the first character of the string
	character = fgetc(file);

	do {
		output_stream[counter++] = character ;
		character = fgetc(file);
	} while ((character != EOF) && (character < output_str_len));

	fclose(file);
	return EXIT_SUCCESS;
	
}


void PipelineConfig::saveCurrentConfigurationState( char* filename )
{
	// Create a temporary buffer
	char temporary_str[MAX_JSON_STRING_LENGTH] = { 0 };

	// Call in store the current state
	saveCurrentConfigurationState( filename, temporary_str, MAX_JSON_STRING_LENGTH );
}


void PipelineConfig::saveCurrentConfigurationState( char* filename, char* return_buffer, int buffer_length )
{
	// Grab the system configuration
	getSystemStateConfiguration( return_buffer, buffer_length );

	// Write the confiuration to disk
	writeToFile(filename, return_buffer);
}


void PipelineConfig::parseJSONCommand(char* client_msg, size_t msg_length)
{
	// Prepare JSON Objects
	JSON_element jsonCommands[MAX_JSON_ELEMENTS];
	int jsonCount = 0;

	// Copy the pointer for reference
	char* base_offset = client_msg;
	int current_offset = 0;

	//printf("Message from Client: %s\r\n", (base_offset + current_offset));

	char* openPos = strstr(base_offset, "{");
	char* closePos = strstr(base_offset, "}");
	char* commaPos = strstr(base_offset, ",");
	char* colonPos = strstr(base_offset, ":");

	//printf("Base Pointer : %x\n", base_offset);
	//printf("Open Pointer : %x\n", openPos);
	//printf("Close Pointer : %x\n", closePos);
	//printf("Comma Pointer : %x\n", commaPos);
	//printf("Colon Pointer : %x\n", colonPos);

	int commaLength = commaPos - base_offset;
	int endLength = closePos - base_offset;

	//printf("Comma Position : %d\n", commaLength);
	//printf("End Brace Position : %d\n", endLength);

	if (openPos == 0) {
		// Could not find a valid stream
		memset(client_msg, 0, msg_length);
		sprintf(client_msg, "{ }");
		return;
	}

	// Itterate through the pairs
	while ( (int) colonPos > 0)
	{
		char* openQ = openPos + 1;
		int str_length = colonPos - openQ;

		//printf("OpenPos  : %x\n", openQ);
		//printf("ColonPos : %x\n", colonPos);

		// Extract the first key value 
		memcpy(jsonCommands[jsonCount].key, openQ, str_length);

		// printf("String 1 : %s\n", jsonCommands[jsonCount].key);

		// Extract the second key value 
		openQ = colonPos + 1;

		if (commaPos < openQ) {
			str_length = closePos - openQ;
		}
		else {
			str_length = commaPos - openQ;
		}

		//printf("StrLen : %d\n", str_length);
		memcpy(jsonCommands[jsonCount].value, openQ, str_length);

		//printf("String 2 : %s\n", jsonCommands[jsonCount].value);

		//printf("Current Position : %d\n", ( openQ + str_length ) - client_msg );
		//printf("CommaPos : %d\n", commaPos - client_msg);
		jsonCount++;

		if ((jsonCount >= (MAX_JSON_ELEMENTS - 1)) || (commaPos == NULL)) {
			break;
		}
		else {

			//printf("OpenPos : %x\n", openPos);
			//printf("ClosePos : %x\n", closePos);
			//printf("CommaPos : %x\n", commaPos);

			//Reset the pointer
			openPos = commaPos;

			// Look for another delimeter
			commaPos = strstr(openPos + 1, ",");
			colonPos = strstr(openPos + 1, ":");
		}
	}

	// Set up temporary strings to hold the attributes
	char value_0[MAX_STRING_LENGTH] = { 0 };
	char value_1[MAX_STRING_LENGTH] = { 0 };

	char key_0[MAX_STRING_LENGTH] = { 0 };
	char key_1[MAX_STRING_LENGTH] = { 0 };

	// Extract the strings
	sprintf(value_0, "%s", jsonCommands[0].value);
	sprintf(value_1, "%s", jsonCommands[1].value);

	sprintf(key_0, "%s", jsonCommands[0].key);
	sprintf(key_1, "%s", jsonCommands[1].key);

	//printf(" I am HERE \n" );
	//printf(" Value : %s \n", value_0 );

	if (strcmp(key_0, "type"))
	{
		if (strstr(value_0, "tatorVisionState"))
		{
			if (debug_mode) {
				printf("Vision Connection Request\n");
			}
			returnStatusConfig(client_msg, msg_length);
		}
		else if (strstr(value_0, "tatorEWSViewingState"))
		{
			if (debug_mode) {
				printf("EWS Viewing Request\n");
			}

			// Pass the state to the config class
			setShuffleboardEWSState( value_1 );

			memset(client_msg, 0, msg_length);
			sprintf(client_msg, "{ }");
		}
		else if (strstr(value_0, "tatorGetCIELabPixelValues"))
		{
			if (debug_mode) {
				printf("CIELAB Pixel Conversion Request\n");
			}

			if (strstr( key_1, "X_Y_Coordinates"))
			{
				char low_value[MAX_STRING_LENGTH];
				char high_value[MAX_STRING_LENGTH];

				char* startPos = strstr(value_1, "[") + 1;
				char* spacePos = strstr(startPos, " ") + 1;
				char* endPos = strstr(value_1, "]");

				memcpy(low_value, startPos, (spacePos - startPos));
				memcpy(high_value, spacePos, (endPos - spacePos));

				// Call in to run the pixel through the pipeline
				estimatePixelLabValues((double)std::stoi(low_value), (double)std::stoi(high_value), client_msg);
			}
			else 
			{
				memset(client_msg, 0, msg_length);
				sprintf(client_msg, "{ }");
			}
		}
		else if (strstr(value_0, "saveConfiguration"))
		{
			printf("Save Configuration\n");
			char config_file[MAX_STRING_LENGTH] = "./pipelineConfig.txt";
			saveCurrentConfigurationState(config_file);
		}
		else if (strstr(value_0, "resetConfiguration"))
		{
			// Reset the system configuration
			setDefaultVisionConfiguration();
			setDefaultCameraConfiguration();
			setDefaultSystemConfiguration();
			setDefaultFilteringConfiguration();
			setDefaultProcessingConfiguration();

			// Now return the current state of the pipeline config to the EWS
			returnStatusConfig(client_msg, msg_length);

		}
		else if (strstr(value_0, "loadConfiguration"))
		{
			// Read in the configuration
			char config_file[MAX_STRING_LENGTH] = "./pipelineConfig.txt";
			char pipeline_config[MAX_JSON_STRING_LENGTH] = { 0 };

			printf("Load Configuration\n");
			readFromFile(config_file, pipeline_config, MAX_JSON_STRING_LENGTH);
			parseJSONCommand(pipeline_config, MAX_JSON_STRING_LENGTH);

			// Now return the current state of the pipeline config to the EWS
			returnStatusConfig(client_msg, msg_length);
		}
		else
		{
			// Clear the return buffer
			memset(client_msg, 0, msg_length);
			sprintf(client_msg, "{ }");

			// Now act on the JSON Command
			updateConfigClass(jsonCommands, jsonCount);
		}
	}

}
void PipelineConfig::updateConfigClass(JSON_element* jsonElements, int numElements)
{
	// Set up temporary strings to hold the attributes
	char value[MAX_STRING_LENGTH] = { 0 };
	char key[MAX_STRING_LENGTH] = { 0 };

	// Extract the strings
	sprintf(value, "%s", jsonElements[0].value);
	sprintf(key, "%s", jsonElements[0].key);

	if (strcmp(key, "type"))
	{
		if (debug_mode) {
			printf("Connection : %s\n", value);
		}

		if (strstr(value, "systemControl"))
		{
			updateSystemConfig(jsonElements, numElements);
		}
		else if (strstr(value, "pipelineControl"))
		{
			updatePipelineConfig(jsonElements, numElements);
		}
		else if (strstr(value, "cameraControl"))
		{
			updateCameraConfig(jsonElements, numElements);
		}
		else if (strstr(value, "filteringControl"))
		{
			updateFilteringConfig(jsonElements, numElements);
		}
		else if (strstr(value, "updateSystemControls"))
		{
			// Run through all of the sub system controls
			updateSystemConfig(jsonElements, numElements);
			updatePipelineConfig(jsonElements, numElements);
			updateCameraConfig(jsonElements, numElements);
			updateFilteringConfig(jsonElements, numElements);
		}
	}
}

void PipelineConfig::updateCameraConfig(JSON_element* jsonElements, int numElements)
{

	// Now start to work with the elements
	for (int i = 1; i < numElements; i++)
	{
		char value[MAX_STRING_LENGTH] = { 0 };
		char key[MAX_STRING_LENGTH] = { 0 };

		sprintf(value, "%s", jsonElements[i].value);
		sprintf(key, "%s", jsonElements[i].key);

		std::string temp_string = value;

		if (debug_mode) {
			printf(" Key : %s\n", key);
			printf(" Value : %s\n", value);
		}

		if (strstr(key, "tatorVisionCamera")) {
		}
		else if (strstr(key, "tatorVisionResolution")) {
		}
		else if (strstr(key, "tatorVisionframeRate")) {
			// Trim the string 
			strcpy(value, trim(temp_string).c_str());
			setCameraFramesPerSecond(std::stoi(value));
		}
		else if (strstr(key, "orientation")) {
			// Trim the string 
			strcpy(value, trim(temp_string).c_str());
			setSensorOrientation(std::stoi(value));
		}
		else if (strstr(key, "auto_exposure")) {
			if (strstr(value, "yes") || strstr(value, "1") ) {
				setCameraAutoExposure(true);
			}
			else {
				setCameraAutoExposure(false);
			}
		}
		else if (strstr(key, "exposure_value")) {
			// Trim the string 
			strcpy(value, trim(temp_string).c_str());
			setCameraManualExposure(std::stod(value));
		}
		else if (strstr(key, "brightness_value")) {
			// Trim the string 
			strcpy(value, trim(temp_string).c_str());
			setCameraBrightness(std::stod(value));
		}
		else if (strstr(key, "saturation_value")) {
			// Trim the string 
			strcpy(value, trim(temp_string).c_str());
			setCameraSaturation(std::stod(value));
		}
		else if (strstr(key, "contrast_value")) {
			// Trim the string 
			strcpy(value, trim(temp_string).c_str());
			setCameraContrast(std::stod(value));
		}
		else if (strstr(key, "sharpness_value")) {
			// Trim the string 
			strcpy(value, trim(temp_string).c_str());
			setCameraSharpness(std::stod(value));
		}
		else if (strstr(key, "colorbalanceR_value")) {
			// Trim the string 
			strcpy(value, trim(temp_string).c_str());
			setCameraColorBalanceR(std::stoi(value));
		}
		else if (strstr(key, "colorbalanceB_value")) {
			// Trim the string 
			strcpy(value, trim(temp_string).c_str());
			setCameraColorBalanceB(std::stoi(value));
		}
		else if (strstr(key, "CameraCropLeft")) {
			// Trim the string 
			strcpy(value, trim(temp_string).c_str());
			setCameraCropLeft(std::stod(value));
		}
		else if (strstr(key, "CameraCropRight")) {
			// Trim the string 
			strcpy(value, trim(temp_string).c_str());
			setCameraCropRight(std::stod(value));
		}
		else if (strstr(key, "CameraCropTop")) {
			// Trim the string 
			strcpy(value, trim(temp_string).c_str());
			setCameraCropTop(std::stod(value));
		}
		else if (strstr(key, "CameraCropBottom")) {
			// Trim the string 
			strcpy(value, trim(temp_string).c_str());
			setCameraCropBottom(std::stod(value));
		}
		else {
			if (debug_mode) {
				printf("Unknown Command : %s, %s\n", key, value);
				fflush(stdout);
			}
		}

	}

	// Force the camera to use manual exposure control
	// setCameraAutoExposure(false);

	// Force a refresh on the camera configuration
	setCameraTargetSettings();

	// Print the current settings
	if (debug_mode) {
		printCameraSupportSettings();
	}
}


void PipelineConfig::updatePipelineConfig(JSON_element* jsonElements, int numElements)
{

	// Now start to work with the elements
	for (int i = 1; i < numElements; i++)
	{
		char low_value[MAX_STRING_LENGTH] = { 0 };
		char high_value[MAX_STRING_LENGTH] = { 0 };

		char value[MAX_STRING_LENGTH] = { 0 };
		char key[MAX_STRING_LENGTH] = { 0 };

		sprintf(value, "%s", jsonElements[i].value);
		sprintf(key, "%s", jsonElements[i].key);

		std::string temp_string = value;

		if (debug_mode) {
			printf(" Key : %s\n", key);
			printf(" Value : %s\n", value);
		}

		if (strstr(key, "scaleFactor")) {
			// Trim the string 
			strcpy(value, trim(temp_string).c_str());
			setScaleFactor(std::stod(value));
		}
		else if (strstr(key, "dynamic_clipping")) {
			if (strstr(value, "yes") || strstr(value, "1")) {
				setDynamicRGBScaling(true);
			}
			else {
				setDynamicRGBScaling(false);
			}
		}
		else if (strstr(key, "rgb_clipping")) {

			char* startPos = strstr(value, "[") + 2;
			char* spacePos = strstr(startPos, " ") + 1;
			char* endPos = strstr(value, "]");
			memcpy(low_value, startPos, (spacePos - startPos));
			memcpy(high_value, spacePos, (endPos - spacePos));

			setRGB_ClippingLUT((double)std::stoi(low_value), (double)std::stoi(high_value));
		}
		else if (strstr(key, "lightness_clipping")) {
			char* startPos = strstr(value, "[") + 2;
			char* spacePos = strstr(startPos, " ") + 1;
			char* endPos = strstr(value, "]");
			memcpy(low_value, startPos, (spacePos - startPos));
			memcpy(high_value, spacePos, (endPos - spacePos));

			setL_ClippingLUT((double)std::stoi(low_value), (double)std::stoi(high_value));
		}
		else if (strstr(key, "chroma_range")) {
			char* startPos = strstr(value, "[") + 2;
			char* spacePos = strstr(startPos, " ") + 1;
			char* endPos = strstr(value, "]");
			memcpy(low_value, startPos, (spacePos - startPos));
			memcpy(high_value, spacePos, (endPos - spacePos));

			setChroma_ClippingLUT((double)std::stoi(low_value), (double)std::stoi(high_value));
		}
		else if (strstr(key, "hue_range")) {
			char* startPos = strstr(value, "[") + 2;
			char* spacePos = strstr(startPos, " ") + 1;
			char* endPos = strstr(value, "]");
			memcpy(low_value, startPos, (spacePos - startPos));
			memcpy(high_value, spacePos, (endPos - spacePos));

			setHue_ClippingLUT((double)std::stoi(low_value), (double)std::stoi(high_value));
		}
		else if (strstr(key, "L_Ref")) {
			// Trim the string 
			strcpy(value, trim(temp_string).c_str());
			setTargetL(std::stod(value));
		}
		else if (strstr(key, "a_Ref")) {
			// Trim the string 
			strcpy(value, trim(temp_string).c_str());
			setTargeta(std::stod(value));
		}
		else if (strstr(key, "b_Ref")) {
			// Trim the string 
			strcpy(value, trim(temp_string).c_str());
			setTargetb(std::stod(value));
		}
		else if (strstr(key, "threshold")) {
			// Trim the string 
			strcpy(value, trim(temp_string).c_str());
			setThreshold(std::stod(value));
		}
		else {
			if (debug_mode) {
				printf( "Unknown Command : %s, %s \n", key, value);
				fflush(stdout);
			}
		}

	}

}


void PipelineConfig::updateFilteringConfig(JSON_element* jsonElements, int numElements)
{

	// Now start to work with the elements
	for (int i = 1; i < numElements; i++)
	{
		char low_value[MAX_STRING_LENGTH] = { 0 };
		char high_value[MAX_STRING_LENGTH] = { 0 };

		char value[MAX_STRING_LENGTH] = { 0 };
		char key[MAX_STRING_LENGTH] = { 0 };

		sprintf(value, "%s", jsonElements[i].value);
		sprintf(key, "%s", jsonElements[i].key);

		std::string temp_string = value;

		if (debug_mode) {
			printf(" Key : %s\n", key);
			printf(" Value : %s\n", value);
		}

		if (strstr(key, "brightness_filter")) {
			char* startPos = strstr(value, "[") + 2;
			char* spacePos = strstr(startPos, " ") + 1;
			char* endPos = strstr(value, "]");
			memcpy(low_value, startPos, (spacePos - startPos));
			memcpy(high_value, spacePos, (endPos - spacePos));

			setBrightnessRange((double)std::stod(low_value), (double)std::stod(high_value));
		}
		else if (strstr(key, "pct_area_filter")) {
			char* startPos = strstr(value, "[") + 2;
			char* spacePos = strstr(startPos, " ") + 1;
			char* endPos = strstr(value, "]");
			memcpy(low_value, startPos, (spacePos - startPos));
			memcpy(high_value, spacePos, (endPos - spacePos));

			setPercentAreaRange((double)std::stod(low_value), (double)std::stod(high_value));
		}
		else if (strstr(key, "fullness_filter")) {
			char* startPos = strstr(value, "[") + 2;
			char* spacePos = strstr(startPos, " ") + 1;
			char* endPos = strstr(value, "]");
			memcpy(low_value, startPos, (spacePos - startPos));
			memcpy(high_value, spacePos, (endPos - spacePos));

			setFullnessRange((double)std::stod(low_value), (double)std::stoi(high_value));
		}
		else if (strstr(key, "aspect_ratio_filter")) {
			char* startPos = strstr(value, "[") + 2;
			char* spacePos = strstr(startPos, " ") + 1;
			char* endPos = strstr(value, "]");
			memcpy(low_value, startPos, (spacePos - startPos));
			memcpy(high_value, spacePos, (endPos - spacePos));

			setAspectRatioRange((double)std::stod(low_value), (double)std::stod(high_value));
		}
		else if (strstr(key, "chroma_filter")) {
			char* startPos = strstr(value, "[") + 2;
			char* spacePos = strstr(startPos, " ") + 1;
			char* endPos = strstr(value, "]");
			memcpy(low_value, startPos, (spacePos - startPos));
			memcpy(high_value, spacePos, (endPos - spacePos));

			setChromaRange((double)std::stod(low_value), (double)std::stod(high_value));
		}
		else if (strstr(key, "color_filter")) {
			if (strstr(value, "Red")) {
				setColorFilter(OBJECTCOLOR::RED);
			}
			else if (strstr(value, "Green")) {
				setColorFilter(OBJECTCOLOR::GREEN);
			}
			else if (strstr(value, "Blue")) {
				setColorFilter(OBJECTCOLOR::BLUE);
			}
			else if (strstr(value, "Yellow")) {
				setColorFilter(OBJECTCOLOR::YELLOW);
			}
			else {
				setColorFilter(OBJECTCOLOR::UNDEFINED);
			}
		}
			else if (strstr(key, "morphology")) {
			setMorphologyMethod(value);
		}
		else if (strstr(key, "conn_comp")) {
			setConnectComponentsConnectivity(value);
		}
		else if (strstr(key, "seg_method")) {
			setSegmentationMethod(value);
		}
		else if (strstr(key, "boundary_detect")) {
			setBoundaryDetectionMethod(value);
		}
		else if (strstr(key, "target_detect")) {
			setTargetType(value);
		}
		else {
			if (debug_mode) {
				printf("Unknown Command : %s, %s \n", key, value);
				fflush(stdout);
			}
		}

	}

}


void PipelineConfig::updateSystemConfig(JSON_element* jsonElements, int numElements)
{
	// Now start to work with the elements
	for (int i = 1; i < numElements; i++)
	{
		char value[MAX_STRING_LENGTH] = { 0 };
		char key[MAX_STRING_LENGTH] = { 0 };

		char temp[MAX_STRING_LENGTH] = { 0 };

		sprintf(value, "%s", jsonElements[i].value);
		sprintf(key, "%s", jsonElements[i].key);

		std::string temp_string = value;

		if (debug_mode) {
			printf(" Key : %s\n", key);
			printf(" Value : %s\n", value);
		}

		if (strstr(key, "debugMode")) {
			if (strstr(value, "on") || strstr(value, "1")) {
				setVisionLibDebugMode(true);
			}
			else {
				setVisionLibDebugMode(false);
			}
		}
		else if (strstr(key, "captureMode")) {
			if (strstr(value, "on") || strstr(value, "1")) {
				setVisionLibCaptureMode(true);
			}
			else {
				setVisionLibCaptureMode(false);
			}
		}
		else if (strstr(key, "captureIncrement")) {
			// Trim the string 
			strcpy(value, trim(temp_string).c_str());
			setVisionLibCaptureInc(std::stoi(value));
		}
		else if (strstr(key, "debugFilename")) {
			// Trim the string 
			strcpy(value, trim(temp_string).c_str());
			setVisionLibDebugStr(value);
		}
		else if (strstr(key, "multi_thread")) {
			if (strstr(value, "on") || strstr(value, "1")) {
				setMultiThreaded(true);
			}
			else {
				setMultiThreaded(false);
			}
		}
		else if (strstr(key, "executor_service")) {
			if (strstr(value, "on") || strstr(value, "1")) {
				setExecutorService(true);
			}
			else {
				setExecutorService(false);
			}
		}
		else if (strstr(key, "numberOfThreads")) {
			// Trim the string 
			strcpy(value, trim(temp_string).c_str());
			setNumberOfThreads(std::stoi(value));
		}
		else if (strstr(key, "visionLEDstatus")) {
			if (strstr(value, "on") || strstr(value, "1")) {
				setVisionLEDEnabled(true);
				LED_Turn_ON();
			}
			else {
				setVisionLEDEnabled(false);
				LED_Turn_OFF();
			}
		}
		else if (strstr(key, "visionLEDbrightness")) {
			// Trim the string 
			strcpy(value, trim(temp_string).c_str());
			setVisionLEDBrightness(std::stod(value));
		}
		else if (strstr(key, "dynamic_clipping")) {
			if (strstr(value, "yes") || strstr(value, "1")) {
				setDynamicRGBScaling(true);
			}
			else {
				setDynamicRGBScaling(false);
			}
		}
		else {
			if (debug_mode) {
				printf("Unknown Command : %s, %s\n", key, value);
				fflush(stdout);
			}
		}

	}

}


void PipelineConfig::returnStatusConfig(char* client_msg, size_t msg_length)
{
	// Clear the return buffer
	memset(client_msg, 0, msg_length);

	getSystemStateConfiguration(client_msg, msg_length);

}



