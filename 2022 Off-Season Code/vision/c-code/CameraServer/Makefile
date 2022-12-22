CXX=arm-raspbian10-linux-gnueabihf-g++
CC=arm-raspbian10-linux-gnueabihf-gcc

CROSSCOMPILER_LOC = "../../../../raspbian10"

INCLUDE_LOC = {CROSSCOMPILER_LOC}"\sys-root\usr\include"

OPENCVLIB_LOC = ../../../lib/wpilib_cameraserver_2021_3_1
VISIONLIB_LOC = ../VisionLib
APRILTAGLIB_LOC = ../AprilTagLib
APRILTAGLIBCOMM_LOC = ../AprilTagLib/common

VISIONLIB=VISIONLIB.o
APRILTAGLIB=AprilTagLib.o

APPLICATION=tatorvision
APPLICATION_DEBUG=tatorvision_Debug

OUTDIR:=./Cameraserver/Out
VISIONDIR:=VisionLibOut
APRILDIR:=AprilTagLibOut

DEPS_CFLAGS=-I ${OPENCVLIB_LOC}/include -I ${OPENCVLIB_LOC}/include/opencv2 -I $(VISIONLIB_LOC) \
			-I ${APRILTAGLIB_LOC} -I ${APRILTAGLIBCOMM_LOC} -I ${INCLUDE_LOC} -fPIC -o2 -pthread

DEPS_LIBS=-L ${CROSSCOMPILER_LOC}sys-root/lib -L ${OPENCVLIB_LOC}/lib \
		  -l wpilibc -l wpiHal -l cameraserver -l ntcore -l cscore -l opencv_dnn -l opencv_highgui -l opencv_ml \
		  -l opencv_objdetect -l opencv_shape -l opencv_stitching -l opencv_superres -l opencv_videostab -l opencv_calib3d \
		  -l opencv_videoio -l opencv_imgcodecs -l opencv_features2d -l opencv_video -l opencv_photo -l opencv_imgproc \
		  -l opencv_flann -l opencv_core -l wpiutil -l atomic

VISIONLIB_OBJECTS = $(addprefix $(VISIONDIR)/,Vision_Model.o Vision_Pipeline.o pnm.o ImageStore.o TargetLocation.o ExecutorService.o \
				  Timing.o Morphology.o visionlib_version.o NetworkTables.o CLUT_Table.o LED_Control.o \
				  annotate.o LUT_1D.o conn_components.o boundary_detection.o scale_image.o segment_filtering.o \
				  PipelineConfig.o Camera.o )

APRILTAGLIB_C_OBJECTS = $(addprefix $(APRILDIR)/,apriltag.o apriltag_pose.o apriltag_quad_thresh.o tag36h11.o )

APRILTAGLIB_CPP_OBJECTS = $(addprefix $(APRILDIR)/,AprilTagPipeline.o )

APRILTAGLIBCOMM_C_OBJECTS = $(addprefix $(APRILDIR)/,g2d.o getopt.o homography.o image_u8.o image_u8x3.o image_u8x4.o \
							matd.o pam.o pjpeg.o pjpeg-idct.o pnm.o string_util.o svd22.o time_util.o \
							unionfind.o workerpool.o zarray.o zhash.o zmaxheap.o )

APRILTAGLIBCOMM_CPP_OBJECTS = $(addprefix $(APRILDIR)/,pthreads_cross.o )

CAMSERVER_OBJECTS = $(addprefix $(OUTDIR)/,CameraServer.o Utilities.o VisionLoop.o sha1.o SocketServer.o )

build: ${APPLICATION}

debug: DEPS_CFLAGS += -g3
debug: ${APPLICATION}

${OUTDIR}:
	mkdir "${OUTDIR}"

${VISIONDIR}:
	mkdir "${VISIONDIR}"

${APRILDIR}:
	mkdir "${APRILDIR}"

${APPLICATION}: ${OUTDIR} $(VISIONLIB) $(APRILTAGLIB) $(CAMSERVER_OBJECTS)
	${CXX} $(CAMSERVER_OBJECTS) $(APRILTAGLIB) $(VISIONLIB) $(DEPS_LIBS) -Wl,--unresolved-symbols=ignore-in-shared-libs -pthread -o $(APPLICATION)

${VISIONLIB}: ${VISIONDIR} $(VISIONLIB_OBJECTS)
	${CXX} $(VISIONLIB_OBJECTS) -shared -o $(VISIONLIB)

${VISIONLIB_OBJECTS}: $(VISIONDIR)/%.o: ${VISIONLIB_LOC}/%.cpp
	$(CXX) -c $^ $(DEPS_CFLAGS) -std=c++17 -o $@

${APRILTAGLIB}: ${APRILDIR} ${APRILTAGLIB_C_OBJECTS} ${APRILTAGLIB_CPP_OBJECTS} ${APRILTAGLIBCOMM_C_OBJECTS} ${APRILTAGLIBCOMM_CPP_OBJECTS} 
	$(CXX) ${APRILTAGLIB_C_OBJECTS} ${APRILTAGLIB_CPP_OBJECTS} ${APRILTAGLIBCOMM_C_OBJECTS} ${APRILTAGLIBCOMM_CPP_OBJECTS} -shared -o $(APRILTAGLIB)

${APRILTAGLIB_C_OBJECTS}: $(APRILDIR)/%.o: ${APRILTAGLIB_LOC}/%.c
	$(CC) -c $^ $(DEPS_CFLAGS) -std=gnu99 -o $@

${APRILTAGLIB_CPP_OBJECTS}: $(APRILDIR)/%.o: ${APRILTAGLIB_LOC}/%.cpp
	$(CXX) -c $^ $(DEPS_CFLAGS) -std=c++17 -o $@ 

${APRILTAGLIBCOMM_C_OBJECTS}: $(APRILDIR)/%.o: ${APRILTAGLIBCOMM_LOC}/%.c
	$(CC) -c $^ $(DEPS_CFLAGS) -std=gnu99 -o $@

${APRILTAGLIBCOMM_CPP_OBJECTS}: $(APRILDIR)/%.o: ${APRILTAGLIBCOMM_LOC}/%.cpp
	$(CXX) -c $^ $(DEPS_CFLAGS) -std=c++17 -o $@

${CAMSERVER_OBJECTS}: $(OUTDIR)/%.o: CameraServer/%.cpp
	$(CXX) -c $^ $(DEPS_CFLAGS) -std=c++17 -o $@

clean:
	rm *.o
	rm $(OUTDIR)/*.o
	rm $(VISIONDIR)/*.o
	rm $(APRILDIR)/*.o
	rm $(APPLICATION)
