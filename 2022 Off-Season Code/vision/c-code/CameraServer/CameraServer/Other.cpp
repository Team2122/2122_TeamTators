#include <iostream>
#include "opencv2/videoio.hpp"
#include "VisionLib.hpp"

#include <thread>
#include <vector>

#include "networktables/NetworkTableInstance.h"
#include "networktables/NetworkTableEntry.h"

#include "arm-linux-gnueabihf\sys\unistd.h"
#include "time.h"

#include "cameraserver/CameraServer.h"
#include "cscore_oo.h"
#include "cscore_c.h"

#include "TargetImage.hpp"
#include "LED_Control.h"

int csViewVersion();
int normalNonViewVersion();
int hybridViewVersion();
int networkTableConnectionTest();
int takePicture();
int setDeltaAngle();

{

    // //testNetworkTables();

    // std::cout << "Testing NetworkTables\n";
    // nt::NetworkTableInstance::GetDefault().GetTable(wpi::Twine("vz"))->GetEntry(wpi::Twine("TEST")).SetBoolean(true);
    // std::cout << "Result: " << nt::NetworkTableInstance::GetDefault().GetTable(wpi::Twine("vz"))->GetEntry(wpi::Twine("TEST")).GetBoolean(false) << "\n";
    // if (nt::NetworkTableInstance::GetDefault().GetTable(wpi::Twine("vz"))->GetEntry(wpi::Twine("TEST")).GetBoolean(false)) {
    //     std::cout << "Test Passed\n";
    // } else {
    //     std::cout << "Test Failed\n";
    // }

    // First, Make sure that we are using the correction version of
    // Verify the version of VISIONLIB
    // test_VisionLib_Version();

    // csViewVersion();
    // normalNonViewVersion();
    // hybridViewVersion();
    // takePicture();
    // setDeltaAngle();
    // printNetworkTableInfo();
    // legacyCodeView(debug_info_state);

    //    /*
        // std::cout << "\tCreating Camera\n";
        // cv::VideoCapture* cam0 = new cv::VideoCapture(0, cv::CAP_V4L2);
        // std::cout << "Object Created";

        // // Set up the camera settings for the turret camera
        // std::cout << "Setting Camera Settings\n";
        // setCameraSupportSettings(cam0, CAMERA_IN_USE::TURRETCAM);

        // // Figure out which settings the camera supports
        // std::cout << "Printing Camera Settnigs\n";
        // printCameraSupportSettings(cam0, CAMERA_IN_USE::TURRETCAM);

        // std::cout << "\tFinished Creating Camera\n";

        // cam0->open(0);

        // if (cam0->isOpened()) {
        //     std::cout << "Camera # 0 is open\nBeginning Stream\n";
        //     //std::thread* stream0 = new std::thread(targetCam, cam0);
        //     //std::thread stream0(targetCam, cam0);
        //     //targetCam(cam0);
        //     //AverysTargetVersionSingleTest(cam0);
        //     // std::thread reader(readBuffer, cam0);
        //     // reader.detach();
        //     // AverysTargetVersion(cam0);
        //     // CameraServer.
        // }
        // else {
        //     std::cout << "Camera # 0 was not able to be opened\n";
        // }


     //   */

        /*
        cv::VideoCapture* cam1 = new cv::VideoCapture(1, cv::CAP_V4L2);

        // Set up the camera settings for the ball camera
        setCameraSupportSettings(cam1, CAMERA_IN_USE::BALLCAM);

        // Figure out which settings the camera supports
        printCameraSupportSettings(cam1, CAMERA_IN_USE::BALLCAM);

        cam1->open(1);

        if (cam1->isOpened()) {
            std::cout << "Camera # 1 is open\nBeginning Stream";
            //std::thread* stream0 = new std::thread(targetCam, cam0);
            //std::thread stream1(groundCam, cam1);
            groundCam(cam1);
        }
        else {
            std::cout << "Camera # 1 was not able to be opened\nHey Jacob, sometimes it says this because it is being stupid. Try power cycling the robot \nand if that doesn't work leve the bot on and power cycle the pi only. If this doesn't \nwork I'm not sure what you should do.";
        }
    //    */

    //testAverysCode();
}



std::string jsonConfigSettings = "    \"pixel format\": \"RGB3\"    \"width\": 640    \"height\": 480    \"fps\": 30    \"brightness\": .5    \"white balance\": 1000    \"exposure\": 0.001953125    \"properties\": [        {            \"name\": \"Turret Camera\"            \"value\": property value        }    ]";

// https://first.wpi.edu/FRC/roborio/release/docs/cpp/classcs_1_1VideoSource.html#a8efc6ba17b7847d975121cc5c56185a5


int csViewVersion() {

    const wpi::Twine& cameraName = wpi::Twine("Pi Camera Module");
    const wpi::Twine& cameraPath = wpi::Twine("/dev/video0");
    cs::UsbCamera source = cs::UsbCamera(cameraName, cameraPath);
    std::cout << "Camera Created\n";
    //wpi::StringRef jsonSettings = wpi::StringRef(jsonConfigSettings);
    //source.SetConfigJson(jsonSettings);
    cs::VideoMode videoMode = cs::VideoMode(cs::VideoMode::PixelFormat::kMJPEG, 640, 480, 30);
    source.SetVideoMode(videoMode);
    std::cout << "Modes Set\n";

    // std::vector<cs::VideoSource
    // cs::VideoSource::EnumerateSources();

    const wpi::Twine& sinkName = wpi::Twine("Pi Camera Sink");
    cs::CvSink camera = cs::CvSink(sinkName);
    std::cout << "Sink Created\n";
    camera.SetSource(source);
    std::cout << "Source Set\n";

    cv::Mat* view = new cv::Mat();
    camera.GrabFrame(*view);
    std::cout << "Frame Grabbed\n";

    // Setup DriverStation Viewing
    const wpi::Twine& twine = wpi::Twine("VisionOutput");
    cs::CvSource screen = frc::CameraServer::GetInstance()->PutVideo(twine, view->cols, view->rows);
    std::cout << "Screen set\n";

    // Vision Variables
    TargetColor targetColor{};
    TargetLocation* location = new TargetLocation();
    BoundingBox_VM* box = new BoundingBox_VM();

    double scale_factor = 1;
    double effSF = sqrt(1 / scale_factor);
    Image_Store* image = new Image_Store();
    cv::Mat* image_mat = new cv::Mat();
    image->image = image_mat->data;
    image->set_attributes(view->cols, view->rows, view->channels(), 255, PIXEL_FORMAT::BGR, INTERLEAVE_FORMAT::PIXEL_INTERLEAVED);
    Image_Store* scaled = new Image_Store();
    scaled->set_attributes(view->cols * effSF, view->rows * effSF, view->channels(), 255, PIXEL_FORMAT::BGR, INTERLEAVE_FORMAT::PIXEL_INTERLEAVED);
    cv::Mat* scaled_mat = new cv::Mat();
    std::string* string = new std::string("");
    std::vector<cv::Point>* pointList = new std::vector<cv::Point>();
    double threshold = 80;
    double initialAngle = 0;

    std::cout << "Images Created\n";

    // NetworkTable communication
    const wpi::Twine visionTableKey = wpi::Twine("visionTable");
    const wpi::Twine deltaAngleKey = wpi::Twine("deltaAngle");
    const wpi::Twine initialAngleKey = wpi::Twine("initialAngle");
    const wpi::Twine distanceKey = wpi::Twine("distance");
    const wpi::Twine timeReceivedKey = wpi::Twine("timeReceived");
    const wpi::Twine currentTurretAngleKey = wpi::Twine("currentTurretAngle");

    std::cout << "Keys Created\n";

    int time = 0;

    nt::NetworkTableEntry deltaAngleEntry = nt::NetworkTableInstance::GetDefault().GetTable(visionTableKey)->GetEntry(deltaAngleKey);
    nt::NetworkTableEntry initialAngleEntry = nt::NetworkTableInstance::GetDefault().GetTable(visionTableKey)->GetEntry(initialAngleKey);
    nt::NetworkTableEntry distanceEntry = nt::NetworkTableInstance::GetDefault().GetTable(visionTableKey)->GetEntry(distanceKey);
    nt::NetworkTableEntry timeReceivedEntry = nt::NetworkTableInstance::GetDefault().GetTable(visionTableKey)->GetEntry(timeReceivedKey);
    nt::NetworkTableEntry currentTurretAngleEntry = nt::NetworkTableInstance::GetDefault().GetTable(visionTableKey)->GetEntry(currentTurretAngleKey);

    std::cout << "Entries Created\n";



    while (true) {

        delete string;
        string = new std::string("");

        std::cout << "Grabbing Frame\n";

        camera.GrabFrame(*image_mat);
        initialAngle = initialAngleEntry.GetDouble(-1);

        image->image = image_mat->data;

        std::cout << "Running Process\n";
        NewOptimizedProcessWriteEnhancedSelection(image, scaled, scale_factor, threshold, targetColor, box, pointList, location, string);

        std::cout << "Results of Algorithm: \n" << string;

        // Sending Data!
        screen.PutFrame(*scaled_mat);

        if (location->checkValidity()) {
            deltaAngleEntry.SetDouble(location->getDeltaAngle());
            initialAngleEntry.SetDouble(initialAngle);
            distanceEntry.SetDouble(location->getDistanceTransform());
        }
        else {
            std::cout << "Data is Invalid";
        }

        timeReceivedEntry.SetDouble(time++);
    }

    return 0;
}

int normalNonViewVersion() {

    std::cout << "\tCreating Camera\n";
    cv::VideoCapture* cam0 = new cv::VideoCapture(0, cv::CAP_V4L2);
    std::cout << "Object Created";

    // Set up the camera settings for the turret camera
    std::cout << "Setting Camera Settings\n";
    setCameraSupportSettings(cam0, CAMERA_IN_USE::TURRETCAM);

    // Figure out which settings the camera supports
    std::cout << "Printing Camera Settnigs\n";
    printCameraSupportSettings(cam0, CAMERA_IN_USE::TURRETCAM);

    std::cout << "\tFinished Creating Camera\n";

    cam0->open(0);

    if (!cam0->isOpened()) {
        std::cout << "Camera could not be opened\n";
        return 1;
    }

    cv::Mat* view = new cv::Mat();
    std::cout << "Trying to read Camera";
    cam0->read(*view);

    // Vision Variables
    TargetColor targetColor{};
    TargetLocation* location = new TargetLocation();
    BoundingBox_VM* box = new BoundingBox_VM();

    Image_Store* image = new Image_Store();
    cv::Mat* image_mat = new cv::Mat();
    image->image = image_mat->data;
    image->set_attributes(view->cols, view->rows, view->channels(), 255, PIXEL_FORMAT::BGR, INTERLEAVE_FORMAT::PIXEL_INTERLEAVED);
    Image_Store* scaled = new Image_Store();
    std::string* string = new std::string("");
    std::vector<cv::Point>* pointList = new std::vector<cv::Point>();
    double threshold = 80;
    double scale_factor = 1;
    double initialAngle;

    // NetworkTable communication
    const wpi::Twine visionTableKey = wpi::Twine("visionTable");
    const wpi::Twine deltaAngleKey = wpi::Twine("deltaAngle");
    const wpi::Twine initialAngleKey = wpi::Twine("initialAngle");
    const wpi::Twine distanceKey = wpi::Twine("distance");
    const wpi::Twine timeReceivedKey = wpi::Twine("timeReceived");
    const wpi::Twine currentTurretAngleKey = wpi::Twine("currentTurretAngle");

    int time = 0;

    nt::NetworkTableEntry deltaAngleEntry = nt::NetworkTableInstance::GetDefault().GetTable(visionTableKey)->GetEntry(deltaAngleKey);
    nt::NetworkTableEntry initialAngleEntry = nt::NetworkTableInstance::GetDefault().GetTable(visionTableKey)->GetEntry(initialAngleKey);
    nt::NetworkTableEntry distanceEntry = nt::NetworkTableInstance::GetDefault().GetTable(visionTableKey)->GetEntry(distanceKey);
    nt::NetworkTableEntry timeReceivedEntry = nt::NetworkTableInstance::GetDefault().GetTable(visionTableKey)->GetEntry(timeReceivedKey);
    nt::NetworkTableEntry currentTurretAngleEntry = nt::NetworkTableInstance::GetDefault().GetTable(visionTableKey)->GetEntry(currentTurretAngleKey);

    while (true) {

        //delete string;
        string = new std::string("");

        std::cout << "Preparing to Grab\n";;

        for (int i = 0; i < 15; i++) {
            cam0->grab();
        }

        std::cout << "Grabbed, Preparing to Read\n";

        cam0->read(*image_mat);
        initialAngle = initialAngleEntry.GetDouble(-1);

        image->image = image_mat->data;

        std::cout << "Read, preparing to Run Process\n";

        NewOptimizedProcessWriteEnhancedSelection(image, scaled, scale_factor, threshold, targetColor, box, pointList, location, string);

        std::cout << "Results of Algorithm: \n" << (*string) << "\n";

        // Sending Data!
        if (location->checkValidity()) {
            deltaAngleEntry.SetDouble(location->getDeltaAngle());
            initialAngleEntry.SetDouble(initialAngle);
            distanceEntry.SetDouble(location->getDistanceTransform());
        }
        else {
            std::cout << "Data is invalid\n";
        }
        timeReceivedEntry.SetDouble(time++);
    }

    return 0;
}

// int hybridViewVersion() {

//     std::cout << "\tCreating Camera\n";
//     cv::VideoCapture* cam0 = new cv::VideoCapture(0, cv::CAP_V4L2);
//     std::cout << "Object Created";

//     // Set up the camera settings for the turret camera
//     std::cout << "Setting Camera Settings\n";
//     setCameraSupportSettings(cam0, CAMERA_IN_USE::TURRETCAM);

//     // Figure out which settings the camera supports
//     std::cout << "Printing Camera Settnigs\n";
//     printCameraSupportSettings(cam0, CAMERA_IN_USE::TURRETCAM);

// 	std::cout << "\tFinished Creating Camera\n";

//     cam0->open(0);

//     if (!cam0->isOpened()) {
//         std::cout << "Camera could not be opened\n";
//         return 1;
//     }

//     cv::Mat* view = new cv::Mat();
//     std::cout << "Trying to read Camera";
//     cam0->read(*view);

//     // New Vision System
//     double scale_factor = .4;
//     double initialAngle = 0;
//     TargetImage* image = new TargetImage(view, scale_factor);
//     VisionImage::Color* targetColor = new VisionImage::Color(137.955, 191.76, 123.93);
//     image->targetColor = targetColor;
//     image->pConfig->color = targetColor;
//     image->interestBox->bottom -= 10;

//     // Setup DriverStation Viewing
//     const wpi::Twine& outputName = wpi::Twine("VisionOutput");
//     cs::CvSource screen = frc::CameraServer::GetInstance()->PutVideo(visionNTEntry, image->scaled_mat->cols, image->scaled_mat->rows);
//     std::cout << "Screen set with dimensions with " << image->scaled_mat->cols << ", " << image->scaled_mat->rows << "\n";

//     // NetworkTable communication
//     const wpi::Twine visionTableKey = wpi::Twine("visionTable");
//     const wpi::Twine deltaAngleKey = wpi::Twine("deltaAngle");
//     const wpi::Twine initialAngleKey = wpi::Twine("initialAngle");
//     const wpi::Twine distanceKey = wpi::Twine("distance");
//     const wpi::Twine timeReceivedKey = wpi::Twine("timeReceived");
//     const wpi::Twine currentAngleKey = wpi::Twine("currentAngle");

//     int time = 0;

//     nt::NetworkTableEntry deltaAngleEntry = nt::NetworkTableInstance::GetDefault().GetTable(visionTableKey)->GetEntry(deltaAngleKey);
//     nt::NetworkTableEntry initialAngleEntry = nt::NetworkTableInstance::GetDefault().GetTable(visionTableKey)->GetEntry(initialAngleKey);
//     nt::NetworkTableEntry distanceEntry = nt::NetworkTableInstance::GetDefault().GetTable(visionTableKey)->GetEntry(distanceKey);
//     nt::NetworkTableEntry timeReceivedEntry = nt::NetworkTableInstance::GetDefault().GetTable(visionTableKey)->GetEntry(timeReceivedKey);
//     nt::NetworkTableEntry currentAngleEntry = nt::NetworkTableInstance::GetDefault().GetTable(visionTableKey)->GetEntry(currentAngleKey);

//     image->executorService = new ExecutorService(3, ExecutorService::SleepType::NOSLEEP);

//     image->threadConfigs = new std::vector<VisionImage::PConfig*>();
// 	image->initializePConfigs(10, image->threadConfigs, image->pConfig);
// 	image->setBoundingBoxes(image->interestBox, image->threadConfigs);

//     VisionImage::Segment* bestSegment;
//     bool targetFound = false;

//     while (true) {

//         auto start_time = std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count();

//         auto grabStartTime = std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count();
//         cam0->read(*image->mat);
//         auto grabEndTime = std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count();

//         std::cout << "Grab and Read Time: " << (grabEndTime - grabStartTime) << "\n";

//         initialAngle = currentAngleEntry.GetDouble(-1);

//         image->image->image = image->mat->data;

//         auto processStartTime = std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count();
//         image->n4MultithreadProcess();
//         // image->n4Process();
//         image->scoreSegments();

//         std::vector<VisionImage::Segment*>* bestSegments = image->getBestSegments(3);
//         if (bestSegments->size() > 0) {
//             bestSegment = bestSegments->at(0);
//             targetFound = true;
//         }
//         auto processEndTime = std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count();

//         std::cout << "Process Time: " << processEndTime - processStartTime << "\n";

//         auto coloringStartTime = std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count();
//         if (targetFound) {
//             image->colorSegment(image->scaled_image, bestSegment->scaledPointList, new VisionImage::Color(255, 255, 255));
//         }
//         auto coloringEndTime = std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count();

//         std::cout << "Coloring Time: " << coloringEndTime - coloringStartTime << "\n";

//         auto streamingStartTime = std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count();
//         // Sending Data!
//         screen.PutFrame(*image->scaled_mat);
//         auto streamingEndTime = std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count();

//         std::cout << "Streaming Time: " << streamingEndTime - streamingStartTime << "\n";

//         auto ntTablesStartTime = std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count();
//         if (targetFound) {
//             deltaAngleEntry.SetDouble(image->getDeltaAngle(bestSegment->box));
//             std::cout << "Delta Angle Entry: " << deltaAngleEntry.GetDouble(-1000) << "\n";
//             std::cout << "Initial Angle Entry: " << initialAngleEntry.GetDouble(-1000) << "\n";
//             std::cout << "Target Angle: " << deltaAngleEntry.GetDouble(-1000) + initialAngleEntry.GetDouble(-1000) << "\n";
//             initialAngleEntry.SetDouble(initialAngle);
//             distanceEntry.SetDouble(0);
//         } else {
//             std::cout << "Data is invalid\n";
//         }
//         timeReceivedEntry.SetDouble(time++);
//         auto ntTablesEndTime = std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count();

//         std::cout << "NetworkTable Send Time: " << ntTablesEndTime - ntTablesStartTime << "\n";

//         targetFound = false;

//         auto end_time = std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count();

//         std::cout << "Total Time Taken: " << end_time - start_time << "\n";
//     }

//     return 0;
// }

int writeTest() {

    const wpi::Twine& cameraName = wpi::Twine("Pi Camera Module");
    const wpi::Twine& cameraPath = wpi::Twine("/dev/video0");

    cs::UsbCamera source = cs::UsbCamera(cameraName, cameraPath);
    std::cout << "Camera Created\n";

    //wpi::StringRef jsonSettings = wpi::StringRef(jsonConfigSettings);
    //source.SetConfigJson(jsonSettings);

    cs::VideoMode videoMode = cs::VideoMode(cs::VideoMode::PixelFormat::kMJPEG, 640, 480, 30);
    source.SetVideoMode(videoMode);
    std::cout << "Modes Set\n";

    const wpi::Twine& sinkName = wpi::Twine("Pi Camera Sink");
    cs::CvSink camera = cs::CvSink(sinkName);
    std::cout << "Sink Created\n";

    camera.SetSource(source);
    std::cout << "Source Set\n";

    cv::Mat* view = new cv::Mat();
    camera.GrabFrame(*view);
    std::cout << "Frame Grabbed\n";

    pnm* image = new pnm();

    image->image_data->set_attributes(view->cols, view->rows, 3, 255, PIXEL_FORMAT::BGR, INTERLEAVE_FORMAT::PIXEL_INTERLEAVED);
    image->image_data->image = view->data;

    // char* path = "Runtime_Capture//CscoreTest.ppm";
    // image->file_path = path;

    image->write();

    return 0;
}

int networkTableConnectionTest() {

    std::cout << "Configuring NetworkTables\n";
    usleep(3 * microsecond);

    uint team = 2122;
    const char* server = "10.21.22.2";
    nt::NetworkTableInstance ntinst = nt::NetworkTableInstance::GetDefault();

    ntinst.SetServer(server); // DO NOT USE PORT NUMBER, LET IT USE THE DEFAULT
    ntinst.StartClient();

    std::cout << "Attempting to Connect to Network Server\n";
    usleep(15 * microsecond);
    std::cout << "Exiting\n";

    return 0;
}

int takePicture() {

    std::cout << "\tCreating Camera\n";
    cv::VideoCapture* cam0 = new cv::VideoCapture(0, cv::CAP_V4L2);
    std::cout << "Object Created";

    // Set up the camera settings for the turret camera
    std::cout << "Setting Camera Settings\n";
    setCameraSupportSettings(cam0, CAMERA_IN_USE::TURRETCAM);

    // Figure out which settings the camera supports
    std::cout << "Printing Camera Settnigs\n";
    printCameraSupportSettings(cam0, CAMERA_IN_USE::TURRETCAM);

    cv::Mat* mat = new cv::Mat();

    for (int i = 0; i < 25; i++) {
        cam0->read(*mat);

        std::cout << "Read, Creating Stuff\n";

        pnm* image = new pnm();
        image->image_data->image = mat->data;
        image->image_data->set_attributes(mat->cols, mat->rows, mat->channels(), 255, PIXEL_FORMAT::RGB, INTERLEAVE_FORMAT::PIXEL_INTERLEAVED);

        std::string path = std::string(".//Runtime_Capture//" + std::to_string(i) + "image.ppm");
        char* pathArray = new char[path.length() + 1];
        strcpy(pathArray, path.c_str());

        image->file_path = pathArray;
        image->write();

    }

    std::cout << "Finished Writing\n";

    return 0;
}

int setDeltaAngle() {

    // NetworkTable communication
    const wpi::Twine visionTableKey = wpi::Twine("visionTable");
    const wpi::Twine deltaAngleKey = wpi::Twine("deltaAngle");
    const wpi::Twine initialAngleKey = wpi::Twine("initialAngle");
    const wpi::Twine distanceKey = wpi::Twine("distance");
    const wpi::Twine timeReceivedKey = wpi::Twine("timeReceived");
    const wpi::Twine currentTurretAngleKey = wpi::Twine("currentAngle");

    int time = 0;

    nt::NetworkTableEntry deltaAngleEntry = nt::NetworkTableInstance::GetDefault().GetTable(visionTableKey)->GetEntry(deltaAngleKey);
    nt::NetworkTableEntry initialAngleEntry = nt::NetworkTableInstance::GetDefault().GetTable(visionTableKey)->GetEntry(initialAngleKey);
    nt::NetworkTableEntry distanceEntry = nt::NetworkTableInstance::GetDefault().GetTable(visionTableKey)->GetEntry(distanceKey);
    nt::NetworkTableEntry timeReceivedEntry = nt::NetworkTableInstance::GetDefault().GetTable(visionTableKey)->GetEntry(timeReceivedKey);
    nt::NetworkTableEntry currentAngleEntry = nt::NetworkTableInstance::GetDefault().GetTable(visionTableKey)->GetEntry(currentTurretAngleKey);

    double currentAngle = currentAngleEntry.GetDouble(-1);
    if (currentAngle == -1) {
        deltaAngleEntry.SetDouble(0);
        initialAngleEntry.SetDouble(140);
    }
    else {
        deltaAngleEntry.SetDouble(15);
        initialAngleEntry.SetDouble(140);
    }

    return 0;
}