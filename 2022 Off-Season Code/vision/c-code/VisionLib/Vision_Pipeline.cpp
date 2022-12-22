#include "Vision_Pipeline.hpp"
#include <string.h>

// Member functions definitions including constructor and destructor;

Vision_Pipeline::~Vision_Pipeline(void)
{
	if (scaled_image != NULL){
		delete scaled_image;
		scaled_image = NULL;
	}

	if (thresholded_out != NULL){
		delete thresholded_out;
		thresholded_out = NULL;
	}

	if (distance_out != NULL){
		delete distance_out;
		distance_out = NULL;
	}

	if (segmented_imgout != NULL){
		delete segmented_imgout;
		segmented_imgout = NULL;
	}

	if (annotated_image != NULL){
		delete annotated_image;
		annotated_image = NULL;
	}

	if (driver_station != NULL){
		delete driver_station;
		driver_station = NULL;
	}

	if (exec_service != NULL){
		delete exec_service;
		exec_service = NULL;
	}

	if (CLUT_table != NULL){
		delete CLUT_table;
		CLUT_table = NULL;
	}

	if (target_bounding_box != NULL){
		delete target_bounding_box;
		target_bounding_box = NULL;
	}

	if (target_LOC != NULL) {
		delete target_LOC;
		target_LOC = NULL;
	}

	if (object_location.center_location != NULL) {
		delete [] object_location.center_location;
	}

	if (object_location.bounding_box != NULL) {
		delete [] object_location.bounding_box;
	}

	if (object_location.object_color != NULL) {
		delete [] object_location.object_color;
	}

}

// Overloaded Constructor

Vision_Pipeline::Vision_Pipeline( double scalefactor, int CLUT_Nodes, SENSOR_ORIENTATION orientation, TARGET_DETECTION_TYPE target_mode )
{
	// Store the current scalefactor forlater use
	lastKnownScaleFactor = scalefactor;

	// Initialize a new Pipeline Config Object
	this->pipeline_config = new PipelineConfig(this);
	this->pipeline_config->setScaleFactor(scalefactor);
	this->pipeline_config->setSensorOrientation(orientation);
	this->pipeline_config->setTargetType(target_mode);

	this->scaled_image = new Image_Store();
	this->thresholded_out = new Image_Store();
	this->distance_out = new Image_Store();
	this->segmented_imgout = new Image_Store();
	this->annotated_image = new Image_Store();
	this->driver_station = new Image_Store();

	if (pipeline_config->getExecutorService()) {
		enableExecutorService();
	}

	// Set up the CLUT 
	this->CLUT_table = new CLUT_Table( CLUT_Nodes, pipeline_config, 0, 255 );

	// Handling the multiple case
	this->object_location.location_length = MAX_BOUNDING_BOXES;
	this->object_location.bounding_box = new BoundingBox[this->object_location.location_length];
	this->object_location.center_location = new Location[this->object_location.location_length];
	this->object_location.object_color = new OBJECTCOLOR[this->object_location.location_length];
	
	// Handling the single case
	this->target_bounding_box = new BoundingBox;
	this->target_LOC = new TargetLocation;
}

void Vision_Pipeline::setTestPath(char* test_path)
{
	// Copy over the test file path
	strcpy( this->test_path, test_path );
}

void Vision_Pipeline::printTargetLocation()
{
	printf("\tTarget Location Top Left     : [ %d, %d ]\n", this->target_LOC->top_left_x, this->target_LOC->top_left_y);
	printf("\tTarget Location Top Right    : [ %d, %d ]\n", this->target_LOC->top_right_x, this->target_LOC->top_right_y);
	printf("\tTarget Location Bottom Left  : [ %d, %d ]\n", this->target_LOC->bottom_left_x, this->target_LOC->bottom_left_y);
	printf("\tTarget Location Bottom Right : [ %d, %d ]\n", this->target_LOC->bottom_right_x, this->target_LOC->bottom_right_y);
}

void Vision_Pipeline::printObjectLocations()
{
	// Display to standard out
	printf("\t%d Objects Found\n", this->object_location.objects_found);

	for (int loop_ball = 0; loop_ball < this->object_location.objects_found; loop_ball++)
	{
		printf("\t\tObject %d  :\tx=%d\ty=%d\n", loop_ball, this->object_location.center_location[loop_ball].x, this->object_location.center_location[loop_ball].y);
	}
}

void Vision_Pipeline::enableExecutorService()
{
	if (this->exec_service == NULL) 
	{
		this->exec_service = new ExecutorService(this->pipeline_config->getNumberOfThreads(), ExecutorService::SleepType::NOSLEEP);
	}

	if (pipeline_config->getExecutorService() == false) 
	{
		pipeline_config->setExecutorService(true);
	}
}

void Vision_Pipeline::disableExecutorService()
{
	if (this->exec_service != NULL)
	{
		delete this->exec_service;
		this->exec_service = NULL;
	}

	if (pipeline_config->getExecutorService() == true)
	{
		pipeline_config->setExecutorService(false);
	}
}

void Vision_Pipeline::resetVisionPipelineScaleFactor()
{

	// Updating the image store objects because of a changed scalefactor

	if (scaled_image != NULL) {
		delete scaled_image;
		scaled_image = new Image_Store();
	}

	if (scaled_image != NULL) {
		delete thresholded_out;
		thresholded_out = new Image_Store();
	}

	if (scaled_image != NULL) {
		delete distance_out;
		distance_out = new Image_Store();
	}

	if (scaled_image != NULL) {
		delete segmented_imgout;
		segmented_imgout = new Image_Store();
	}

	if (scaled_image != NULL) {
		delete annotated_image;
		annotated_image = new Image_Store();
	}

	if (scaled_image != NULL) {
		delete driver_station;
		driver_station = new Image_Store();
	}

}
