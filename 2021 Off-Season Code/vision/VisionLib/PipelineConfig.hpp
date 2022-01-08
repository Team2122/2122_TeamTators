#pragma once

#ifndef _PIPELINE_CONFIG_
#define _PIPELINE_CONFIG_

#include "opencv2/videoio.hpp"

using namespace cv;

enum class CAMERA_IN_USE;
enum class SENSOR_ORIENTATION ;
enum class TARGET_DETECTION_TYPE ;
enum class SEGMENTATION_METHOD;
enum class N_TYPE;
enum class MORPHOLOGY_METHOD;
enum class BOUNDARY_METHOD;

#define LUT_1D_LEVELS 256
#define LUT_HUE_LEVELS 361

class Vision_Pipeline;
class NetworkTables;

struct vision_state {

	double NT_frame_rate;		// Maximum Frame to stream data back to the driver station using Network Tables
	double LED_brightness;		// LED Brightness for Vision System
	bool LED_enabled;			// LED enabled / disabled

};

struct camera_config {

	// Camera Orientation
	SENSOR_ORIENTATION orientation ;
	CAMERA_IN_USE whichcam;
	cv::VideoCapture* camera;

	// Setting Camera resolution
	int x_resolution;
	int y_resolution;
	int camera_FPS;

	// Setting Exposure Control
	bool auto_exposure;
	double exposure;

	// Setting Camera Controls
	double brightness;
	double saturation;
	double contrast;
	double sharpness;
	int color_balance_R;
	int color_balance_B;

};

struct system_control {

	// Debug capabilities
	bool VisionLib_Debug_Mode;									// Variable to enable/disable debug
	bool VisionLib_Capture_Mode;								// Variable to enable capture mode
	char VisionLib_debug_output_file_str[FILENAME_MAX] ;		// Filename string to append in debug
 	int  VisionLib_debug_frame_inc;								// Frame increment, how often to store frames
	int  VisionLib_debug_frame_counter;							// Counter to keep track of how many frames have been processed
	
	// Processing Performance
	bool multithreaded;
	bool executor_service;
	int number_of_threads;
};

struct processing_pipeline {
	
	double scalefactor;
	int CLUT_nodes;

	// Color Processing Parameters
	double RGB_ClipLow, RGB_ClipHigh;
	int RGB_clipping_LUT[LUT_1D_LEVELS];

	double L_ClipLow, L_ClipHigh;
	int Lightness_LUT[LUT_1D_LEVELS];

	double chroma_ClipLow, chroma_ClipHigh;
	int chroma_LUT[LUT_1D_LEVELS];

	double hue_ClipLow, hue_ClipHigh;
	int hue_LUT[LUT_HUE_LEVELS];

	// Color Difference Target Color 
	double Ref_L;
	double Ref_a;
	double Ref_b;

	// Binarization Thereshold
	double threshold;

	// Post Binarization Correction
	MORPHOLOGY_METHOD morphological_operator ;

	// Connected Components Configuration
	SEGMENTATION_METHOD segmentation_method ;
	N_TYPE conn_components_connectivity;

	// Boundard Detection Method
	BOUNDARY_METHOD boundary_method;

	// Target Detection Mode
	TARGET_DETECTION_TYPE segmentation_mode ;

};


class PipelineConfig
{
public :

	// This is the default constructor
	PipelineConfig( Vision_Pipeline* vision_pipe );
	PipelineConfig( Vision_Pipeline* vision_pipe, cv::VideoCapture* camera, CAMERA_IN_USE whichcam );

	// This is the default destructor
	~PipelineConfig();

	// Set the default fallback attributes
	void setDefaultVisionConfiguration();
	void setDefaultCameraConfiguration( );
	void setDefaultSystemConfiguration( );
	void setDefaultProcessingConfiguration( );

	void setCameraConfiguration(cv::VideoCapture* camera, CAMERA_IN_USE whichcam);
	void setCameraVisionSettings( );
	void setCameraVisionSettings( double camera_exposure ) ;
	void setCameraFieldSettings( );

	void printCameraSupportSettings( );

	void setNetworkTableInstance( NetworkTables * my_network_tables );


	// ---------------------------------------------------------------------------\
	// Interface SET methods
	// ---------------------------------------------------------------------------\

	void incrementVisionLibFrameCounter();

	// Setting up the Vision State Entries
	void setVisionNTFrameRate(double frame_rate);
	void setVisionLEDBrightness(double brightness);
	void setVisionLEDEnabled(bool on_off);

	// Debug interface to enable/disable debugging
	void setVisionLibDebugMode(bool debug_status);
	void setVisionLibCaptureMode(bool capture_mode);
	void setVisionLibDebugStr(const char* debug_str);
	void setVisionLibCaptureInc(int frame_inc);

	void setMultiThreaded( bool enable_Multithreaded );
	void setExecutorService( bool enable_ExecutorService );
	void setNumberOfThreads(int number_of_threads );

	// Setting the Pipeline Parameters
	void setScaleFactor(double scalefactor);

	void setRGB_ClippingLUT(double low_clip, double high_clip);
	void setL_ClippingLUT(double low_clip, double high_clip);
	void setChroma_ClippingLUT(double low_clip, double high_clip);
	void setHue_ClippingLUT(double low_clip, double high_clip);

	void setTargetLab(double L_ref, double a_ref, double b_ref);
	void setTargetL(double L_ref);
	void setTargeta(double a_ref);
	void setTargetb(double b_ref);

	void setThreshold(double threshold);

	void setMorphologyMethod(char* morphological_operator);
	void setMorphologyMethod( MORPHOLOGY_METHOD morphological_operator );

	void setSegmentationMethod(char* segmentation_method);
	void setSegmentationMethod( SEGMENTATION_METHOD segmentation_method );

	void setConnectComponentsConnectivity(char* connectivity);
	void setConnectComponentsConnectivity(N_TYPE connectivity);

	void setBoundaryDetectionMethod(char* boundary_method);
	void setBoundaryDetectionMethod(BOUNDARY_METHOD boundary_method);

	void setTargetType(char* target_type);
	void setTargetType(TARGET_DETECTION_TYPE target_type);

	// Setting Camera Attributes
	void setSensorOrientation( int orientation);
	void setSensorOrientation(SENSOR_ORIENTATION orientation);

	void setCameraXResolution(int resX);
	void setCameraYResolution(int resY);
	void setCameraFramesPerSecond(int FPS);
	void setCameraAutoExposure(bool auto_exposure);
	void setCameraManualExposure(double exposure);
	void setCameraBrightness(double brightness);
	void setCameraSaturation(double saturation);
	void setCameraContrast(double contrast);
	void setCameraSharpness(double sharpness);
	void setCameraColorBalanceR(int balanceR);
	void setCameraColorBalanceB(int balanceB);


	// ---------------------------------------------------------------------------\
	// Interface GET methods
	// ---------------------------------------------------------------------------\

	// Retreiving the Vision State Entries
	double setVisionNTFrameRate();
	double setVisionLEDBrightness();
	bool  setVisionLEDEnabled();

	// Retrieving the Debug Attributes
	bool getVisionLibDebugMode( );
	bool getVisionLibCaptureMode( );
	char* getVisionLibDebugStr( );
	int getVisionLibCaptureInc( );
	int getVisionLibFrameCounter();

	// Set Pipeline Execution Attributes
	bool getMultiThreaded();
	bool getExecutorService();
	int getNumberOfThreads();

	// Retrieving the Color Target Values
	double getScaleFactor();
	double getTargetL();
	double getTargetA();
	double getTargetB();
	double getThreshold();

	MORPHOLOGY_METHOD getMorphologyMethod();
	SEGMENTATION_METHOD getSegmentationMethod();
	N_TYPE getConnectComponentsConnectivity();
	BOUNDARY_METHOD getBoundaryDetectionMethod();
	TARGET_DETECTION_TYPE getTargetType();

	// Retrieve Camera Attributes
	SENSOR_ORIENTATION getSensorOrientation();
	int getCameraXResolution();
	int getCameraYResolution();
	int getCameraFramesPerSecond();
	bool getCameraAutoExposure();
	double getCameraManualExposure();
	double getCameraBrightness();
	double getCameraSaturation();
	double getCameraContrast();
	double getCameraSharpness();
	int getCameraColorBalanceR();
	int getCameraColorBalanceB();


private :

	// Parent Class
	Vision_Pipeline* vision_pipeline ;

	// These are the parameters for each of the sections
	camera_config	camera_configuration;
	system_control  system_configuration;
	processing_pipeline pipeline_configuration;
	vision_state	vision_configuration;
	NetworkTables * network_table_configuration;

	// Code to build Clipping LUTs

	void build_Clipping_LUT(int* LUT, double low_value, double high_value, double maxValue, int num_elements);
	void build_Hue_LUT(int* LUT, double low_value, double high_value, int num_elements);

};

#endif //_PIPELINE_CONFIG_
