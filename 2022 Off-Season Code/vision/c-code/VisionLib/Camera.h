#pragma once

#include "opencv2/videoio.hpp"


class Camera {
public :

	//Default Constructor
	Camera( int cameraID );
	~Camera();

	// Initalize Virtual Interfaces
	virtual void setBrightness(double brightness)=0;
	virtual void setContrast(double contrast)=0;
	virtual void setSaturation(double saturation) = 0;
	virtual void setSharpness(double sharpness) = 0;
	virtual void setColorBalanceR(double colorBalanceR ) = 0;
	virtual void setColorBalanceB(double colorBalanceB ) = 0;
	virtual void setGamma(double gamma)=0;
	virtual void setAutoExposure(bool auto_exposure_true) =0;
	virtual void setExposure(double exposure)=0;
	virtual void setZoom(double zoom)=0;

	// Public functions
	void printCameraSupportSettings();

	// Information on the current camera
	int cameraID = 0;
	cv::VideoCapture* cam = NULL;

};

class PiCamera : public Camera {

public :
	//Default Constructor
	PiCamera(int cameraID);

	void setBrightness(double brightness);
	void setContrast(double contrast);
	void setSaturation(double saturation);
	void setSharpness(double sharpness);
	void setColorBalanceR(double colorBalanceR);
	void setColorBalanceB(double colorBalanceB);
	void setGamma(double gamma);
	void setAutoExposure(bool auto_exposure_true);
	void setExposure(double exposure);
	void setZoom(double zoom);

};

class ELPCamera : public Camera {

public :
	//Default Constructor
	ELPCamera(int cameraID);

	void setBrightness(double brightness);
	void setContrast(double contrast);
	void setSaturation(double saturation);
	void setSharpness(double sharpness);
	void setColorBalanceR(double colorBalanceR);
	void setColorBalanceB(double colorBalanceB);
	void setGamma(double gamma);
	void setAutoExposure(bool auto_exposure_true);
	void setExposure(double exposure);
	void setZoom(double zoom);

};
