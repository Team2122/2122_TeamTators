#include "PipelineConfig.hpp"
#include "Vision_Model.hpp"
#include "morphology.hpp"
#include "conn_components.hpp"
#include "boundary_detection.h"
#include "opencv2/videoio.hpp"
#include "NetworkTables.h"

using namespace cv;


PipelineConfig::~PipelineConfig(void)
{
	// Default Destructor - cleans up
}

PipelineConfig::PipelineConfig(Vision_Pipeline* vision_pipe )
{
	// Default Constructor
	vision_pipeline = vision_pipe;
	this->camera_configuration.camera = NULL ;
	this->camera_configuration.whichcam = CAMERA_IN_USE::UNDEFINED ;

	setDefaultVisionConfiguration();
	setDefaultCameraConfiguration();
	setDefaultSystemConfiguration();
	setDefaultProcessingConfiguration();
}

PipelineConfig::PipelineConfig( Vision_Pipeline * vision_pipe, cv::VideoCapture* camera, CAMERA_IN_USE whichcam )
{
	// Default Constructor when supplied a Camera
	vision_pipeline = vision_pipe;
	this->camera_configuration.camera = camera;
	this->camera_configuration.whichcam = whichcam;

	setDefaultVisionConfiguration( );
	setDefaultCameraConfiguration( );
	setDefaultSystemConfiguration( );
	setDefaultProcessingConfiguration( );
}


void PipelineConfig::setNetworkTableInstance( NetworkTables * my_network_tables) {
	network_table_configuration = my_network_tables;
}


void PipelineConfig::setDefaultVisionConfiguration()
{
	vision_configuration.NT_frame_rate = 11;
	vision_configuration.LED_brightness = 0.25;
	vision_configuration.LED_enabled = false;
}


void PipelineConfig::setDefaultCameraConfiguration( )
{
	// Camera Orientation
	camera_configuration.orientation = SENSOR_ORIENTATION::PORTRAIT;

	// Setting Camera resolution
	camera_configuration.x_resolution = 640;
	camera_configuration.y_resolution = 480;
	camera_configuration.camera_FPS = 15;

	// Setting Exposure Control
	camera_configuration.auto_exposure = false;
	camera_configuration.exposure = 1.0/2048.0;

	// Setting Camera Controls
	camera_configuration.brightness = 0.5;
	camera_configuration.saturation = 0.5;
	camera_configuration.contrast = 0.5;
	camera_configuration.sharpness = 0;

	// Setting Camera Color Balance
	camera_configuration.color_balance_R = 1000;
	camera_configuration.color_balance_B = 1000;
}

void PipelineConfig::setDefaultSystemConfiguration( )
{

	system_configuration.VisionLib_Debug_Mode = false;								// Variable to enable/disable debug
	system_configuration.VisionLib_Capture_Mode = false;							// Variable to enable capture mode
	system_configuration.VisionLib_debug_frame_inc = 4;								// Frame increment, how often to store frames
	system_configuration.VisionLib_debug_frame_counter = 0;							// Counter to keep track of how many frames have been processed
	strcpy( system_configuration.VisionLib_debug_output_file_str, "test_output" ) ; // Setting up the default output string 

	// Processing Performance
	system_configuration.multithreaded = true;
	system_configuration.executor_service = false;
	system_configuration.number_of_threads = 4;

}

void PipelineConfig::setDefaultProcessingConfiguration( )
{
	pipeline_configuration.scalefactor = 1.0;
	pipeline_configuration.CLUT_nodes = 17;

	pipeline_configuration.RGB_ClipLow = 0;
	pipeline_configuration.RGB_ClipHigh = 255;

	pipeline_configuration.L_ClipLow = 0;
	pipeline_configuration.L_ClipHigh = 100;

	pipeline_configuration.chroma_ClipLow = 0;
	pipeline_configuration.chroma_ClipHigh = 128;

	pipeline_configuration.hue_ClipLow = 0;
	pipeline_configuration.hue_ClipHigh = 360;

	// Color Processing Parameters	
	build_Clipping_LUT(pipeline_configuration.RGB_clipping_LUT, pipeline_configuration.RGB_ClipLow, pipeline_configuration.RGB_ClipHigh, 255, LUT_1D_LEVELS);
	build_Clipping_LUT(pipeline_configuration.Lightness_LUT, pipeline_configuration.L_ClipLow, pipeline_configuration.L_ClipHigh, 100, LUT_1D_LEVELS);
	build_Clipping_LUT(pipeline_configuration.chroma_LUT, pipeline_configuration.chroma_ClipLow, pipeline_configuration.chroma_ClipHigh, 128, LUT_1D_LEVELS);
	build_Hue_LUT(pipeline_configuration.hue_LUT, pipeline_configuration.hue_ClipLow, pipeline_configuration.hue_ClipHigh, LUT_HUE_LEVELS);

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// NOTE :
	// GIMP uses values between 0 - 100, so they need to be scaled BEFORE shifting
	// Photoshop uses values between 0-255
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	// Colorimetric aims for Green Target
	pipeline_configuration.Ref_L = 55;
	pipeline_configuration.Ref_a = (23 * 2.55) - 128;  // The original values are uint8 encoded, so shifting to a*;
	pipeline_configuration.Ref_b = (71 * 2.55) - 128;  // The original values are uint8 encoded, so shifting to b*;

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
	pipeline_configuration.threshold = 0.6;

	// Post Binarization Correction
	pipeline_configuration.morphological_operator = MORPHOLOGY_METHOD::CLOSING ;

	// Connected Components Configuration
	pipeline_configuration.segmentation_method = SEGMENTATION_METHOD::MARK ;
	pipeline_configuration.conn_components_connectivity = N_TYPE::N8 ;

	// Setting the default boundary method
	pipeline_configuration.boundary_method = BOUNDARY_METHOD::DIVIDE_CONQUOR;

	// Target Detection Mode
	pipeline_configuration.segmentation_mode = TARGET_DETECTION_TYPE::TARGET ;

}


//---------------------------------------------------------------------------------------
// Vision State Parameters
//---------------------------------------------------------------------------------------

// Setting up the Vision State Entries
void PipelineConfig::setVisionNTFrameRate(double frame_rate) {
	vision_configuration.NT_frame_rate = frame_rate;
}

void PipelineConfig::setVisionLEDBrightness(double brightness) {
	vision_configuration.LED_brightness = brightness;
}

void PipelineConfig::setVisionLEDEnabled(bool on_off) {
	vision_configuration.LED_enabled = on_off;
}

// Retreiving the Vision State Entries
double PipelineConfig::setVisionNTFrameRate() {
	return vision_configuration.NT_frame_rate;
}

double PipelineConfig::setVisionLEDBrightness() {
	return vision_configuration.LED_brightness;
}

bool PipelineConfig::setVisionLEDEnabled() {
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
}

void PipelineConfig::setVisionLibCaptureMode(bool capture_mode)
{
	if (capture_mode) {
		system_configuration.VisionLib_Capture_Mode = true;
	}
	else {
		system_configuration.VisionLib_Capture_Mode = false;
	}
}

void PipelineConfig::setVisionLibDebugStr(const char* debug_str)
{
	strcpy(system_configuration.VisionLib_debug_output_file_str, debug_str);
	// printf("\n\tDebug String : %s\n", VisionLib_debug_output_file_str);
}

void PipelineConfig::setVisionLibCaptureInc(int frame_inc)
{
	system_configuration.VisionLib_debug_frame_inc = frame_inc;
	// printf("\n\tDebug Frame Inc : %u\n", VisionLib_debug_frame_inc);
}

void PipelineConfig::incrementVisionLibFrameCounter() {
	system_configuration.VisionLib_debug_frame_counter++;
}

void PipelineConfig::setMultiThreaded(bool enable_Multithreaded) {
	system_configuration.multithreaded = enable_Multithreaded;
}

void PipelineConfig::setExecutorService(bool enable_ExecutorService) {
	system_configuration.executor_service = enable_ExecutorService;
}

void PipelineConfig::setNumberOfThreads(int number_of_threads) {
	system_configuration.number_of_threads = number_of_threads ;
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
// Processing Pipeline Parameters
//---------------------------------------------------------------------------------------

void PipelineConfig::setScaleFactor(double scalefactor) {
	printf("Scale Factor : %g \n", scalefactor );
	pipeline_configuration.scalefactor = scalefactor;
}

void PipelineConfig::setRGB_ClippingLUT(double low_clip, double high_clip) {
	printf("RGB Clipping LUT Control Points :[  %g , %g ]\n", low_clip, high_clip);
	build_Clipping_LUT(pipeline_configuration.RGB_clipping_LUT, low_clip, high_clip, 255, LUT_1D_LEVELS);
}

void PipelineConfig::setL_ClippingLUT(double low_clip, double high_clip) {
	printf("Lightness Clipping LUT Control Points :[  %g , %g ]\n", low_clip, high_clip);
	build_Clipping_LUT(pipeline_configuration.Lightness_LUT, low_clip, high_clip, 100, LUT_1D_LEVELS);
}

void PipelineConfig::setChroma_ClippingLUT(double low_clip, double high_clip) {
	printf("Chroma Clipping LUT Control Points :[  %g , %g ]\n", low_clip, high_clip);
	build_Clipping_LUT(pipeline_configuration.chroma_LUT, low_clip, high_clip, 128, LUT_1D_LEVELS);
}

void PipelineConfig::setHue_ClippingLUT(double low_clip, double high_clip) {
	printf("Hue Clipping LUT Control Points :[  %g , %g ]\n", low_clip, high_clip);
	build_Hue_LUT(pipeline_configuration.hue_LUT, low_clip, high_clip, LUT_HUE_LEVELS);
}

// Setting the values for the Color Targets
void PipelineConfig::setTargetLab(double L_ref, double a_ref, double b_ref) {
	pipeline_configuration.Ref_L = L_ref;
	pipeline_configuration.Ref_a = a_ref;
	pipeline_configuration.Ref_b = b_ref;
}

void PipelineConfig::setTargetL(double L_ref) {
	printf("Ref L : %g \n", L_ref );
	pipeline_configuration.Ref_L = L_ref;
}

void PipelineConfig::setTargeta(double a_ref) {
	printf("Ref L : %g \n", a_ref);
	pipeline_configuration.Ref_a = a_ref;
}

void PipelineConfig::setTargetb(double b_ref) {
	printf("Ref L : %g \n", b_ref);
	pipeline_configuration.Ref_b = b_ref;
}

void PipelineConfig::setThreshold(double threshold) {
	printf("Threshold : %g \n", threshold);
	pipeline_configuration.threshold = threshold;
}

void PipelineConfig::setMorphologyMethod(MORPHOLOGY_METHOD morphological_operator) {
	pipeline_configuration.morphological_operator = morphological_operator;
}

void PipelineConfig::setMorphologyMethod(char* morphological_operator) 
{
	printf("Morphological Operator : %s \n", morphological_operator );

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
	pipeline_configuration.segmentation_method = segmentation_method;
}

void PipelineConfig::setSegmentationMethod(char* connectivity)
{
	if (strstr(connectivity, "Mark")) {
		pipeline_configuration.segmentation_method = SEGMENTATION_METHOD::MARK;
	}
	else if (strstr(connectivity, "Jacob")) {
		pipeline_configuration.segmentation_method = SEGMENTATION_METHOD::JACOB;
	}
}

void PipelineConfig::setConnectComponentsConnectivity(N_TYPE connectivity) {
	pipeline_configuration.conn_components_connectivity = connectivity;
}

void PipelineConfig::setConnectComponentsConnectivity( char* connectivity)
{
	if (strstr(connectivity, "N4")) {
		pipeline_configuration.conn_components_connectivity = N_TYPE::N4;
	} else if (strstr(connectivity, "N8")) {
		pipeline_configuration.conn_components_connectivity = N_TYPE::N8;
	}
}

void PipelineConfig::setBoundaryDetectionMethod( BOUNDARY_METHOD boundary_method ) {
	pipeline_configuration.boundary_method = boundary_method ;
}

void PipelineConfig::setBoundaryDetectionMethod(char* boundary_method) 
{
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
	pipeline_configuration.segmentation_mode = target_type;
}

void PipelineConfig::setTargetType(char* target_type )
{
	if (strstr(target_type, "Hexagon")) {
		pipeline_configuration.segmentation_mode = TARGET_DETECTION_TYPE::TARGET;
	}
	else if (strstr(target_type, "Ball")) {
		pipeline_configuration.segmentation_mode = TARGET_DETECTION_TYPE::BALL;
	}
}


// Retrieving the Color Target Values

double PipelineConfig::getScaleFactor() {
	return pipeline_configuration.scalefactor ;
}

double PipelineConfig::getTargetL() {
	return pipeline_configuration.Ref_L;
}

double PipelineConfig::getTargetA() {
	return pipeline_configuration.Ref_a;
}

double PipelineConfig::getTargetB() {
	return pipeline_configuration.Ref_b;
}

double PipelineConfig::getThreshold() {
	return pipeline_configuration.threshold;
}

MORPHOLOGY_METHOD PipelineConfig::getMorphologyMethod() {
	return pipeline_configuration.morphological_operator;
}

SEGMENTATION_METHOD PipelineConfig::getSegmentationMethod() {
	return pipeline_configuration.segmentation_method;
}

N_TYPE PipelineConfig::getConnectComponentsConnectivity() {
	return pipeline_configuration.conn_components_connectivity;
}

BOUNDARY_METHOD PipelineConfig::getBoundaryDetectionMethod() {
	return pipeline_configuration.boundary_method;
}

TARGET_DETECTION_TYPE PipelineConfig::getTargetType() {
	return pipeline_configuration.segmentation_mode;
}


//---------------------------------------------------------------------------------------
// Camera Attributes
//---------------------------------------------------------------------------------------

SENSOR_ORIENTATION PipelineConfig::getSensorOrientation() {
	return camera_configuration.orientation ;
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

void PipelineConfig::setSensorOrientation( int orientation) {
	if ( orientation == 0) {
		camera_configuration.orientation = SENSOR_ORIENTATION::LANDSCAPE;
	}
	else if ( orientation == 90 ) {
		camera_configuration.orientation = SENSOR_ORIENTATION::PORTRAIT;
	}
	else if ( orientation == 180 ) {
		camera_configuration.orientation = SENSOR_ORIENTATION::LANDSCAPE_UD;
	}
	else if ( orientation == 270 ) {
		camera_configuration.orientation = SENSOR_ORIENTATION::PORTRAIT_UD;
	}
}

void PipelineConfig::setSensorOrientation(SENSOR_ORIENTATION orientation) {
	camera_configuration.orientation = orientation;
}

void PipelineConfig::setCameraXResolution(int resX) {
	camera_configuration.x_resolution = resX;
}

void PipelineConfig::setCameraYResolution(int resY) {
	camera_configuration.y_resolution = resY;
}

void PipelineConfig::setCameraFramesPerSecond(int FPS) {
	camera_configuration.camera_FPS = FPS;
}

void PipelineConfig::setCameraAutoExposure(bool auto_exposure) {
	camera_configuration.auto_exposure = auto_exposure;
}
void PipelineConfig::setCameraManualExposure(double exposure) {
	camera_configuration.exposure = exposure;
}

void PipelineConfig::setCameraBrightness(double brightness) {
	camera_configuration.brightness = brightness / 100.0;
}

void PipelineConfig::setCameraSaturation(double saturation) {
	camera_configuration.saturation = saturation / 100.0;
}

void PipelineConfig::setCameraContrast(double contrast) {
	camera_configuration.contrast = contrast / 100.0;
}

void PipelineConfig::setCameraSharpness(double sharpness) {
	camera_configuration.sharpness = sharpness / 100.0;
}

void PipelineConfig::setCameraColorBalanceR(int balanceR) {
	camera_configuration.color_balance_R = balanceR;
}

void PipelineConfig::setCameraColorBalanceB(int balanceB) {
	camera_configuration.color_balance_B = balanceB ;
}



void PipelineConfig::setCameraConfiguration(cv::VideoCapture* camera, CAMERA_IN_USE whichcam) {
	this->camera_configuration.camera = camera;
	this->camera_configuration.whichcam = whichcam;
}


void PipelineConfig::setCameraVisionSettings( double camera_exposure )
{

	if (this->camera_configuration.camera == NULL) {
		// We do not have a camera configured yet, so cannot run this code		
		printf("Cannot configure camera. Have you set it up yet?\n");
		return;
	}

	// Call the base camera settings
	setCameraVisionSettings( );

	// Update the camera exposure setting 
	camera_configuration.camera->set(cv::CAP_PROP_EXPOSURE, 1.0 / camera_exposure);

	printf("Updated Exposure : %f\n", camera_exposure);

}

void PipelineConfig::setCameraVisionSettings( )
{
	if (this->camera_configuration.camera == NULL) {
		// We do not have a camera configured yet, so cannot run this code		
		printf("Cannot configure camera. Have you set it up yet?\n");
		return;
	}

	// Now set the values 
	camera_configuration.camera->set(cv::CAP_PROP_FRAME_WIDTH, camera_configuration.x_resolution );
	camera_configuration.camera->set(cv::CAP_PROP_FPS, camera_configuration.camera_FPS);

	// Specify that the image should be RGB
	// camera->set(cv::CAP_PROP_CONVERT_RGB, true);

	if (camera_configuration.whichcam == CAMERA_IN_USE::TURRETCAM)
	{
		camera_configuration.camera->set(cv::CAP_PROP_FRAME_HEIGHT, camera_configuration.y_resolution );
		camera_configuration.camera->set(cv::CAP_PROP_FOURCC, cv::VideoWriter::fourcc('R', 'G', 'B', '3'));

		printf("Camera Exposure : %f\n", camera_configuration.exposure);
		camera_configuration.camera->set(cv::CAP_PROP_EXPOSURE, 1 / camera_configuration.exposure );	// Off when AUTO Exposure is on
																										// This is specified in sec
		camera_configuration.camera->set(cv::CAP_PROP_ROLL, 0);
		camera_configuration.camera->set(cv::CAP_PROP_WHITE_BALANCE_RED_V, camera_configuration.color_balance_R );
		camera_configuration.camera->set(cv::CAP_PROP_WHITE_BALANCE_BLUE_U, camera_configuration.color_balance_B );

		camera_configuration.camera->set(cv::CAP_PROP_ISO_SPEED, 1);

	}
	else // BALLCAM CASE
	{
		camera_configuration.camera->set(cv::CAP_PROP_FRAME_HEIGHT, camera_configuration.x_resolution);
		camera_configuration.camera->set(cv::CAP_PROP_FOURCC, cv::VideoWriter::fourcc('M', 'J', 'P', 'G'));
	}

	if (camera_configuration.auto_exposure) {
		camera_configuration.camera->set(cv::CAP_PROP_AUTO_EXPOSURE, 0);						// 0 Auto
	}
	else {
		camera_configuration.camera->set(cv::CAP_PROP_AUTO_EXPOSURE, 0.25);						// 0.25 Manual
	}

	printf("Setting Camera Brightness : %g\n", camera_configuration.brightness);
	printf("Setting Camera Contrast : %g\n", camera_configuration.contrast);
	printf("Setting Camera Saturation : %g\n", camera_configuration.saturation);
	printf("Setting Camera Sharpness : %g\n", camera_configuration.sharpness);

	camera_configuration.camera->set(cv::CAP_PROP_BRIGHTNESS, camera_configuration.brightness);		// Black when 0
	camera_configuration.camera->set(cv::CAP_PROP_CONTRAST, camera_configuration.contrast );		// Black when 0
	camera_configuration.camera->set(cv::CAP_PROP_SATURATION, camera_configuration.saturation );	// Grayscale when 0
	camera_configuration.camera->set(cv::CAP_PROP_SHARPNESS, camera_configuration.sharpness );		// 
	camera_configuration.camera->set(cv::CAP_PROP_BUFFERSIZE, 1);
}

void PipelineConfig::setCameraFieldSettings( )
{
	if (this->camera_configuration.camera == NULL) {
		// We do not have a camera configured yet, so cannot run this code		
		printf("Cannot configure camera. Have you set it up yet?\n");
		return;
	}

	// Now set the values 
	camera_configuration.camera->set(cv::CAP_PROP_FRAME_WIDTH, camera_configuration.x_resolution);
	camera_configuration.camera->set(cv::CAP_PROP_FPS, camera_configuration.camera_FPS);

	// Specify that the image should be RGB
	// camera_configuration.camera->set(cv::CAP_PROP_CONVERT_RGB, true);

	if (camera_configuration.whichcam == CAMERA_IN_USE::TURRETCAM)
	{
		// Camera Specific settings go here
		camera_configuration.camera->set(cv::CAP_PROP_FRAME_HEIGHT, camera_configuration.y_resolution);
		camera_configuration.camera->set(cv::CAP_PROP_FOURCC, cv::VideoWriter::fourcc('R', 'G', 'B', '3'));
		camera_configuration.camera->set(cv::CAP_PROP_ISO_SPEED, 1);
	}
	else
	{
		// Camera Specific settings go here
		camera_configuration.camera->set(cv::CAP_PROP_FRAME_HEIGHT, camera_configuration.y_resolution);
		camera_configuration.camera->set(cv::CAP_PROP_FOURCC, cv::VideoWriter::fourcc('M', 'J', 'P', 'G'));
	}

	if (camera_configuration.auto_exposure) {
		camera_configuration.camera->set(cv::CAP_PROP_AUTO_EXPOSURE, 0);						// 0 Auto
	}
	else {
		camera_configuration.camera->set(cv::CAP_PROP_AUTO_EXPOSURE, 0.25);						// 0.25 Manual
	}

	camera_configuration.camera->set(cv::CAP_PROP_BRIGHTNESS, camera_configuration.brightness);		// Black when 0
	camera_configuration.camera->set(cv::CAP_PROP_CONTRAST, camera_configuration.contrast);			// Black when 0
	camera_configuration.camera->set(cv::CAP_PROP_SATURATION, camera_configuration.saturation);		// Grayscale when 0
	camera_configuration.camera->set(cv::CAP_PROP_SHARPNESS, camera_configuration.sharpness);		// 

}

void PipelineConfig::printCameraSupportSettings( )
{
	if (this->camera_configuration.camera == NULL) {
		// We do not have a camera configured yet, so cannot run this code		
		printf("Cannot configure camera. Have you set it up yet?\n");
		return;
	}

	printf("Width         : %f\n", camera_configuration.camera->get(cv::CAP_PROP_FRAME_WIDTH));
	printf("Height        : %f\n", camera_configuration.camera->get(cv::CAP_PROP_FRAME_HEIGHT));
	printf("Video Format  : %f\n", camera_configuration.camera->get(cv::CAP_PROP_FOURCC));

	// get the current camera settings 
	if (camera_configuration.whichcam == CAMERA_IN_USE::TURRETCAM)
	{
		printf("Frame Rate    : %f\n", camera_configuration.camera->get(cv::CAP_PROP_FPS));
		printf("Auto Exposure : %f\n", camera_configuration.camera->get(cv::CAP_PROP_AUTO_EXPOSURE));
		printf("Exposure      : %f\n", camera_configuration.camera->get(cv::CAP_PROP_EXPOSURE));
		printf("ISO Speed     : %f\n", camera_configuration.camera->get(cv::CAP_PROP_ISO_SPEED));
		printf("Brightness    : %f\n", camera_configuration.camera->get(cv::CAP_PROP_BRIGHTNESS));
		printf("Contrast      : %f\n", camera_configuration.camera->get(cv::CAP_PROP_CONTRAST));
		printf("Sharpness     : %f\n", camera_configuration.camera->get(cv::CAP_PROP_SHARPNESS));
		printf("Saturation    : %f\n", camera_configuration.camera->get(cv::CAP_PROP_SATURATION));
		printf("Roll          : %f\n", camera_configuration.camera->get(cv::CAP_PROP_ROLL));
		printf("Red Balance   : %f\n", camera_configuration.camera->get(cv::CAP_PROP_WHITE_BALANCE_RED_V));
		printf("Blue Balance  : %f\n", camera_configuration.camera->get(cv::CAP_PROP_WHITE_BALANCE_BLUE_U));
	}
	else  // BALLCAM Case
	{
		printf("Frame Rate    : %f\n", camera_configuration.camera->get(cv::CAP_PROP_FPS));
		printf("Auto Exposure : %f\n", camera_configuration.camera->get(cv::CAP_PROP_AUTO_EXPOSURE));
		printf("Brightness    : %f\n", camera_configuration.camera->get(cv::CAP_PROP_BRIGHTNESS));
	}

	printf("Buffer Size  : %f\n", camera_configuration.camera->get(cv::CAP_PROP_BUFFERSIZE));

	//printf("Contrast      : %f\n", camera->get(cv::CAP_PROP_CONTRAST));				// Not supported by Ball Cam
	//printf("Sharpness     : %f\n", camera->get(cv::CAP_PROP_SHARPNESS));			// Not supported by Ball Cam
	//printf("Saturation    : %f\n", camera->get(cv::CAP_PROP_SATURATION));			// Not supported by Ball Cam
	//printf("Exposure      : %f\n", camera->get(cv::CAP_PROP_EXPOSURE));				// Not supported by Ball Cam
	//printf("ISO Speed     : %f\n", camera->get(cv::CAP_PROP_ISO_SPEED));			// Not supported by Ball Cam
	//printf("Roll          : %f\n", camera->get(cv::CAP_PROP_ROLL));					// Not supported by Ball Cam
	//printf("Red Balance   : %f\n", camera->get(cv::CAP_PROP_WHITE_BALANCE_RED_V));	// Not supported by Ball Cam
	//printf("Blue Balance  : %f\n", camera->get(cv::CAP_PROP_WHITE_BALANCE_BLUE_U)); // Not supported by Ball Cam

	//printf("Hue           : %f\n", camera->get(cv::CAP_PROP_HUE));				// Not supported by Turret Cam or Ball Cam
	//printf("Gain          : %f\n", camera->get(cv::CAP_PROP_GAIN));				// Not supported by Turret Cam or Ball Cam
	//printf("Trigger       : %f\n", camera->get(cv::CAP_PROP_TRIGGER));			// Not supported by Turret Cam or Ball Cam
	//printf("Trigger Delay : %f\n", camera->get(cv::CAP_PROP_TRIGGER_DELAY));	// Not supported by Turret Cam or Ball Cam
	//printf("Gamma         : %f\n", camera->get(cv::CAP_PROP_GAMMA));			// Not supported by Turret Cam or Ball Cam
	//printf("Focus         : %f\n", camera->get(cv::CAP_PROP_FOCUS));			// Not supported by Turret Cam or Ball Cam
	//printf("Zoom          : %f\n", camera->get(cv::CAP_PROP_ZOOM));				// Not supported by Turret Cam or Ball Cam
	//printf("Pan           : %f\n", camera->get(cv::CAP_PROP_PAN));				// Not supported by Turret Cam or Ball Cam
	//printf("Tilt          : %f\n", camera->get(cv::CAP_PROP_TILT));				// Not supported by Turret Cam or Ball Cam
	//printf("Iris          : %f\n", camera->get(cv::CAP_PROP_IRIS));				// Not supported by Turret Cam or Ball Cam
	//printf("Temperature   : %f\n", camera->get(cv::CAP_PROP_TEMPERATURE));		// Not supported by Turret Cam or Ball Cam
	//printf("Auto Focus    : %f\n", camera->get(cv::CAP_PROP_AUTOFOCUS));		// Not supported by Turret Cam or Ball Cam
	//printf("Auto WB       : %f\n", camera->get(cv::CAP_PROP_AUTO_WB));			// Not supported by Turret Cam or Ball Cam
	//printf("Rotate        : %f\n", camera->get(cv::CAP_PROP_XI_REGION_MODE));	// Not supported by Turret Cam or Ball Cam


	// Ball Cam
	// --------
	//brightness 0x00980900 (int) : min = 0 max = 100 step = 1 default = 50 value = 50
	//exposure_auto 0x009a0901 (menu) : min = 0 max = 3 default = 2 value = 2



	// Turret Cam
	// ----------

	//User Controls

	//brightness 0x00980900 (int) : min = 0 max = 100 step = 1 default = 50 value = 50 flags = slider
	//contrast 0x00980901 (int) : min = -100 max = 100 step = 1 default = 0 value = 0 flags = slider
	//saturation 0x00980902 (int) : min = -100 max = 100 step = 1 default = 0 value = 0 flags = slider
	//sharpness 0x0098091b (int) : min = -100 max = 100 step = 1 default = 0 value = 0 flags = slider
	//red_balance 0x0098090e (int) : min = 1 max = 7999 step = 1 default = 1000 value = 1000 flags = slider
	//blue_balance 0x0098090f (int) : min = 1 max = 7999 step = 1 default = 1000 value = 1000 flags = slider
	//rotate 0x00980922 (int) : min = 0 max = 360 step = 90 default = 0 value = 0 flags = modify - layout

	//horizontal_flip 0x00980914 (bool) : default = 0 value = 0
	//vertical_flip 0x00980915 (bool) : default = 0 value = 0

	//power_line_frequency 0x00980918 (menu) : min = 0 max = 3 default = 1 value = 1
	//color_effects 0x0098091f (menu) : min = 0 max = 15 default = 0 value = 0
	//color_effects_cbcr 0x0098092a (int) : min = 0 max = 65535 step = 1 default = 32896 value = 32896

	//Camera Controls

	//auto_exposure 0x009a0901 (menu) : min = 0 max = 3 default = 0 value = 1
	//exposure_time_absolute 0x009a0902 (int) : min = 1 max = 10000 step = 1 default = 1000 value = 1
	//exposure_dynamic_framerate 0x009a0903 (bool) : default = 0 value = 0
	//auto_exposure_bias 0x009a0913 (intmenu) : min = 0 max = 24 default = 12 value = 12
	//white_balance_auto_preset 0x009a0914 (menu) : min = 0 max = 10 default = 1 value = 1
	//image_stabilization 0x009a0916 (bool) : default = 0 value = 0
	//iso_sensitivity 0x009a0917 (intmenu) : min = 0 max = 4 default = 0 value = 0
	//iso_sensitivity_auto 0x009a0918 (menu) : min = 0 max = 1 default = 1 value = 1
	//exposure_metering_mode 0x009a0919 (menu) : min = 0 max = 2 default = 0 value = 0
	//scene_mode 0x009a091a (menu) : min = 0 max = 13 default = 0 value = 0

	//Codec Controls

	//video_bitrate_mode 0x009909ce (menu) : min = 0 max = 1 default = 0 value = 0 flags = update
	//video_bitrate 0x009909cf (int) : min = 25000 max = 25000000 step = 25000 default = 10000000 value = 10000000
	//repeat_sequence_header 0x009909e2 (bool) : default = 0 value = 0
	//h264_i_frame_period 0x00990a66 (int) : min = 0 max = 2147483647 step = 1 default = 60 value = 60
	//h264_level 0x00990a67 (menu) : min = 0 max = 11 default = 11 value = 11
	//h264_profile 0x00990a6b (menu) : min = 0 max = 4 default = 4 value = 4

	// Default Pi Settings
	// -------------------
	//Width: 640.000000
	//Height : 480.000000
	//Video Format : 861030210.000000
	//Frame Rate : 30.000000
	//Auto Exposure : 0.000000
	//Exposure : 0.099910
	//ISO Speed : 0.000000
	//Brightness : 0.500000
	//Contrast : 0.500000
	//Sharpness : 0.000000
	//Saturation : 0.500000
	//Roll : 0.000000
	//Red Balance : 1000.000000
	//Blue Balance : 1000.000000
}

// Clipping LUT
void PipelineConfig::build_Clipping_LUT( int* clipping_LUT, double ClipLow, double ClipHigh, double maxValue, int LUT_levels )
{
	int starting_value = (ClipLow / maxValue ) * LUT_levels - 1;
	int ending_value = ( ClipHigh / maxValue ) * LUT_levels - 1;

	for (int i = 0; i < LUT_levels; i++) {
		
		if (i < starting_value) {
			clipping_LUT[i] = 0;
		}
		else if(i >= ending_value) {
			clipping_LUT[i] = maxValue;
		}
		else {
			clipping_LUT[i] = maxValue * ( i - starting_value ) / (ending_value - starting_value) ;
		}
	}

}

// Clipping HUE lut, hue angles are in the range 0-360
void PipelineConfig::build_Hue_LUT(int* clipping_LUT, double hue_ClipLow, double hue_ClipHigh, int LUT_levels )
{
	int starting_value = hue_ClipLow - 1 ;
	int ending_value = hue_ClipHigh - 1;

	for (int i = 0; i < LUT_levels; i++) {

		if (i < starting_value) {
			clipping_LUT[i] = 0;
		}
		else if (i >= ending_value) {
			clipping_LUT[i] = 0;
		}
		else {
			clipping_LUT[i] = i;
		}
	}

}
