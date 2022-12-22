#include "Camera.h"


Camera::Camera( int cameraID ) 
{
	// Create the connection to the camera
	cam = new cv::VideoCapture(cameraID, cv::CAP_V4L2);
	this->cameraID = cameraID;
}

Camera::~Camera()
{
	// Delete the camera stream
	if (cam != NULL) {
		delete cam;
	}
}


void Camera::printCameraSupportSettings()
{
	// Create a pointer to current camera
	Camera* currentCamera = this;

	if (currentCamera == NULL) {
		// We do not have a camera configured yet, so cannot run this code		
		printf("Cannot configure camera. Have you set it up yet?\n");
		return;
	}

	printf("Camera ID     : %d\n", cameraID);
	printf("Width         : %f\n", currentCamera->cam->get(cv::CAP_PROP_FRAME_WIDTH));
	printf("Height        : %f\n", currentCamera->cam->get(cv::CAP_PROP_FRAME_HEIGHT));

	int fourcc = (int)currentCamera->cam->get(cv::CAP_PROP_FOURCC);
	printf("Video Format  : %c%c%c%c\n", fourcc & 255, (fourcc >> 8) & 255, (fourcc >> 16) & 255, (fourcc >> 24) & 255);

	// get the current camera settings 
	printf("Frame Rate    : %f\n", currentCamera->cam->get(cv::CAP_PROP_FPS));
	printf("Auto Exposure : %f\n", currentCamera->cam->get(cv::CAP_PROP_AUTO_EXPOSURE));
	printf("Exposure      : %f\n", currentCamera->cam->get(cv::CAP_PROP_EXPOSURE));
	printf("ISO Speed     : %f\n", currentCamera->cam->get(cv::CAP_PROP_ISO_SPEED));
	printf("Brightness    : %f\n", currentCamera->cam->get(cv::CAP_PROP_BRIGHTNESS));
	printf("Contrast      : %f\n", currentCamera->cam->get(cv::CAP_PROP_CONTRAST));
	printf("Sharpness     : %f\n", currentCamera->cam->get(cv::CAP_PROP_SHARPNESS));
	printf("Saturation    : %f\n", currentCamera->cam->get(cv::CAP_PROP_SATURATION));
	printf("Roll          : %f\n", currentCamera->cam->get(cv::CAP_PROP_ROLL));
	printf("Red Balance   : %f\n", currentCamera->cam->get(cv::CAP_PROP_WHITE_BALANCE_RED_V));
	printf("Blue Balance  : %f\n", currentCamera->cam->get(cv::CAP_PROP_WHITE_BALANCE_BLUE_U));
	printf("Buffer Size  : %f\n", currentCamera->cam->get(cv::CAP_PROP_BUFFERSIZE));

}


//
// ===================================================================================
// ===================================================================================
//

PiCamera::PiCamera(int cameraID) : Camera(cameraID)
{
	// Rest of constructor
	// Occurs AFTER the base contructor
}

void PiCamera::setBrightness(double brightness)
{
	//double low_val = 0;
	//double high_val = 100;
	//double default_value = 50 ;

	cam->set(cv::CAP_PROP_BRIGHTNESS, brightness );		// Black when 0
}

void PiCamera::setContrast(double contrast)
{
	//double low_val = -100;
	//double high_val = 100;
	//double default_value = 0 ;

	cam->set(cv::CAP_PROP_CONTRAST, contrast);			// Black when 0
}

void PiCamera::setSaturation(double saturation)
{
	//double low_val = -100;
	//double high_val = 100;
	//double default_value = 0 ;

	cam->set(cv::CAP_PROP_SATURATION, saturation);		// Grayscale when 0
}

void PiCamera::setSharpness(double sharpness)
{
	//double low_val = -100;
	//double high_val = 100;
	//double default_value = 0 ;

	cam->set(cv::CAP_PROP_SHARPNESS, sharpness);					// 
}

void PiCamera::setColorBalanceR(double colorBalanceR)
{
	cam->set(cv::CAP_PROP_WHITE_BALANCE_RED_V, colorBalanceR);		// 
}

void PiCamera::setColorBalanceB(double colorBalanceB)
{
	cam->set(cv::CAP_PROP_WHITE_BALANCE_BLUE_U, colorBalanceB);		// 
}

void PiCamera::setGamma( double gamma )
{
	// Not Supported
}

void PiCamera::setAutoExposure( bool auto_exposure_true  )
{
	if (auto_exposure_true){
		// Set to Auto Exposure
		cam->set(cv::CAP_PROP_AUTO_EXPOSURE, 0);			// Auto Exposure Control
	}
	else {
		// Set to Manual Exposure
		cam->set(cv::CAP_PROP_AUTO_EXPOSURE, 0.25);			// Manual Exposure Control
	}
}

void PiCamera::setExposure(double exposure)
{
	//double low_val = 1;
	//double high_val = 10000;
	//double default_value = 1000 ;

	cam->set(cv::CAP_PROP_EXPOSURE, 1.0 / exposure) ;
}

void PiCamera::setZoom(double zoom)
{
	// Not Supported
}

//
// ===================================================================================
// ===================================================================================
//


ELPCamera::ELPCamera(int cameraID) : Camera(cameraID)
{
	// Rest of constructor
	// Forwards on to the base contructor
}

void ELPCamera::setBrightness(double brightness)
{
	// The range of the value coming in is 0-1

	//double low_val = 0;
	//double high_val = 255;
	//double default_val = 110;

	char cmd[100];
	sprintf(cmd, "v4l2-ctl -d %d -c brightness=%g", cameraID, brightness*255 );
	system(cmd);
}

void ELPCamera::setContrast(double contrast)
{
	// The range of the value coming in is 0-1
	//double low_val = 0;
	//double high_val = 127;
	//double default_val = 32;

	char cmd[100];
	sprintf(cmd, "v4l2-ctl -d %d -c contrast=%g", cameraID, contrast*127);
	system(cmd);
}

void ELPCamera::setSaturation(double saturation)
{
	// The range of the value coming in is 0-1
	//double low_val = 0;
	//double high_val = 127;
	//double default_val = 40;

	char cmd[100];
	sprintf(cmd, "v4l2-ctl -d %d -c saturation=%g", cameraID, saturation*127);
	system(cmd);
}

void ELPCamera::setSharpness(double sharpness)
{
	// The range of the value coming in is 0-1
	//double low_val = 0;
	//double high_val = 15;
	//double default_val = 5;

	char cmd[100];
	sprintf(cmd, "v4l2-ctl -d %d -c sharpness=%g", cameraID, sharpness*15 );
	system(cmd);
}

void ELPCamera::setColorBalanceR(double colorBalanceR)
{
	// Not supported
}

void ELPCamera::setColorBalanceB(double colorBalanceB)
{
	// Not supported
}

void ELPCamera::setGamma(double gamma)
{
	//double low_val = 1;
	//double high_val = 10;
	//double default_val = 7;

	// Define a string to hold the command
	char cmd[100];
	sprintf(cmd, "v4l2-ctl -d %d -c gamma=%g", cameraID, gamma );
	system(cmd);
}

void ELPCamera::setAutoExposure(bool auto_exposure_true)
{
	// Define a string to hold the command
	char cmd[100];

	if (auto_exposure_true) {
		// Set to Auto Exposure
		sprintf( cmd, "v4l2-ctl -d %d -c exposure_auto=3", cameraID );
	}
	else {
		// Set to Manual Exposure
		sprintf(cmd, "v4l2-ctl -d %d -c exposure_auto=1", cameraID );
	}
	system(cmd);
}

void ELPCamera::setExposure(double exposure)
{
	// Value derived from testing 
	// exposure = 160 ;

	// Define a string to hold the command
	// double low_val = 19;
	// double high_val = 5000;
	 
	char cmd[100];
	sprintf(cmd, "v4l2-ctl -d %d -c exposure_absolute=%g", cameraID, exposure );
	system(cmd);
}

void ELPCamera::setZoom(double zoom)
{
	//double low_val = 0;
	//double high_val = 10;

	char cmd[100];
	sprintf(cmd, "v4l2-ctl -d %d -c zoom=%g", cameraID, zoom);
	system(cmd);
}


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
