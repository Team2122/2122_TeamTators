#pragma once

#ifndef _PIPELINE_CONFIG_
#define _PIPELINE_CONFIG_

#include "opencv2/videoio.hpp"
#include "Camera.h"

using namespace cv;

enum class CAMERA_IN_USE;
enum class SENSOR_ORIENTATION ;
enum class TARGET_DETECTION_TYPE ;
enum class SEGMENTATION_METHOD;
enum class N_TYPE;
enum class MORPHOLOGY_METHOD;
enum class BOUNDARY_METHOD;
enum class OBJECTCOLOR;

class Vision_Pipeline;
class NetworkTables;
class JSON_element;

struct vision_state {

	double NT_frame_rate;		// Maximum Frame to stream data back to the driver station using Network Tables
	double LED_brightness;		// LED Brightness for Vision System
	bool LED_enabled;			// LED enabled / disabled

};

struct camera_config {

	// Camera Orientation
	SENSOR_ORIENTATION orientation ;
	CAMERA_IN_USE whichcam;
	Camera* cameraClass = NULL;

	// int deviceID;
	// cv::VideoCapture* camera;

	bool cameraReady = false;

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
	//values for croping the image
	double crop[4] = {0, 0, 0, 0};

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

struct filtering_control {

	double brightness_threshold[2] = { 0, 1.0 };
	double aspect_ratio[2] = { 0, 1.0 };
	double fullness[2] = { 0, 1.0 };
	double chroma_filter[2] = { 0, 1.0 };
	double percent_area[2] = { 0, 1.0 };
	OBJECTCOLOR color ;
};

struct processing_pipeline {
	
	double scalefactor;
	int CLUT_nodes;

	// Color Processing Parameters
	bool rebuildCLUTneeded;
	bool dynamic_RGB_scaling;
	double RGB_ClipLow, RGB_ClipHigh;
	double L_ClipLow, L_ClipHigh;
	double chroma_ClipLow, chroma_ClipHigh;
	double hue_ClipLow, hue_ClipHigh;

	// Variable to hold the number of color centers
	int Ref_Colors;

	// Color Difference Target Color 
	double Ref_L[10] = { 0 };
	double Ref_a[10] = { 0 };
	double Ref_b[10] = { 0 };

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
	
	// This is the default destructor
	~PipelineConfig();

	// Set the default fallback attributes
	void setDefaultVisionConfiguration();
	void setDefaultCameraConfiguration();
	void setDefaultSystemConfiguration();
	void setDefaultFilteringConfiguration();
	void setDefaultProcessingConfiguration();

	void setCameraConfiguration( Camera* camInfo, CAMERA_IN_USE whichcam ) ;

	void setCameraCaptureSettings( );
	void setCameraTargetSettings();
	void setCameraFieldSettings();

	void printCameraSupportSettings( );

	void setNetworkTableInstance( NetworkTables * my_network_tables );


	// ---------------------------------------------------------------------------\
	// Interface SET methods
	// ---------------------------------------------------------------------------\

	void incrementVisionLibFrameCounter();

	// Setting up the Vision State Entries
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

	// Setting the Filtering Parameters
	void setBrightnessRange(double low, double high);
	void setAspectRatioRange(double low, double high);
	void setFullnessRange(double low, double high);
	void setChromaRange(double low, double high);
	void setPercentAreaRange(double low, double high);
	void setColorFilter( OBJECTCOLOR objectColor );

	// Setting the Pipeline Parameters
	void setScaleFactor(double scalefactor);
	void setDynamicRGBScaling(bool enableDynamicScaling);
	void setRebuildCLUTFlag( bool rebuildNeeded );

	void setRGB_ClippingLUT(double low_clip, double high_clip);
	void setL_ClippingLUT(double low_clip, double high_clip);
	void setChroma_ClippingLUT(double low_clip, double high_clip);
	void setHue_ClippingLUT(double low_clip, double high_clip);

	void setNumTargetColors(int num_colors);

	void setTargetLab(double L_ref, double a_ref, double b_ref);
	void setTargetLab( int colorID, double L_ref, double a_ref, double b_ref );

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
	void setCameraCropLeft(double cropL);
	void setCameraCropRight(double cropR);
	void setCameraCropTop(double cropT);
	void setCameraCropBottom(double cropB);
	void setShuffleboardEWSState( char* EWS_state );


	// ---------------------------------------------------------------------------\
	// Interface GET methods
	// ---------------------------------------------------------------------------\

	Camera* getCameraConfiguration();
	int getSystemStateConfiguration(char* message, int buffer_length);

	// Retreiving the Vision State Entries
	double getVisionLEDBrightness();
	bool  getVisionLEDEnabled();

	// Retrieving the Debug Attributes
	bool getVisionLibDebugMode( );
	bool getVisionLibCaptureMode( );
	char* getVisionLibDebugStr( );
	int getVisionLibCaptureInc( );
	int getVisionLibFrameCounter();

	// Retrieving the Filtering Parameters
	void getBrightnessRange(double* low, double* high);
	void getAspectRatioRange(double* low, double* high);
	void getFullnessRange(double* low, double* high);
	void getChromaRange(double* low, double* high);
	void getPercentAreaRange(double* low, double* high);
	void getColorFilter(OBJECTCOLOR* objectColor);
	void getColorFilter(char* objectColor);

	// Get Pipeline Execution Attributes
	bool getMultiThreaded();
	bool getExecutorService();
	int getNumberOfThreads();

	// Retrieving the Color Target Values
	double getScaleFactor();
	bool getDynamicRGBScaling();
	bool getRebuildCLUTNeeded();

	void getRGB_ClippingLUT(double* low_clip, double* high_clip);
	void getL_ClippingLUT(double* low_clip, double* high_clip);
	void getChroma_ClippingLUT(double* low_clip, double* high_clip);
	void getHue_ClippingLUT(double* low_clip, double* high_clip);

	double getTargetL();
	double getTargetA();
	double getTargetB();

	// Get the values for a specific color ID
	double getTargetL( int colorID );
	double getTargetA( int colorID );
	double getTargetB( int colorID );

	int getNumTargetColors();
	double getThreshold();

	MORPHOLOGY_METHOD getMorphologyMethod();
	SEGMENTATION_METHOD getSegmentationMethod();
	N_TYPE getConnectComponentsConnectivity();
	BOUNDARY_METHOD getBoundaryDetectionMethod();
	TARGET_DETECTION_TYPE getTargetType();

	void getMorphologyMethodString( char* buffer ) ;
	void getSegmentationMethodString( char* buffer );
	void getConnectComponentsConnectivityString( char* buffer );
	void getBoundaryDetectionMethodString( char* buffer );
	void getTargetTypeString( char* buffer );

	void estimatePixelLabValues( double Pixel_X, double Pixel_Y, char* client_msg );

	// Retrieve Camera Attributes
	SENSOR_ORIENTATION getSensorOrientation();
	void getSensorOrientationString(char* buffer);

	bool getCameraState();
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
	double getCameraCropLeft();
	double getCameraCropRight();
	double getCameraCropTop();
	double getCameraCropBottom();

	void parseJSONCommand(char* client_msg, size_t msg_length);
	void returnStatusConfig(char* client_msg, size_t msg_length);
	void updateConfigClass(JSON_element* jsonElements, int numElements);
	void updateSystemConfig(JSON_element* jsonElements, int numElements);
	void updatePipelineConfig(JSON_element* jsonElements, int numElements);
	void updateCameraConfig(JSON_element* jsonElements, int numElements);
	void updateFilteringConfig(JSON_element* jsonElements, int numElements);

	bool readFromFile(char* pathToFile);
	bool readFromFile(char* pathToFile, char* output, int outstr_length);

	bool writeToFile(char* pathToFile, char* data);	
	
	void saveCurrentConfigurationState( char* filename );
	void saveCurrentConfigurationState( char* filename, char* return_buffer, int length );

	void getShuffleboardEWSState(char* EWS_state);


private :

	// Parent Class
	Vision_Pipeline*	vision_pipeline ;
	bool				debug_mode = false;

	// These are the parameters for each of the sections
	camera_config		camera_configuration;
	system_control		system_configuration;
	filtering_control   filtering_configuration;
	processing_pipeline pipeline_configuration;
	vision_state		vision_configuration;
	NetworkTables *		network_table_configuration;

	double range(double inval, double LB, double UB);
	
};

#endif //_PIPELINE_CONFIG_
