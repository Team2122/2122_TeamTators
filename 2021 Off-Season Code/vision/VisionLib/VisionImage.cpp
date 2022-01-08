#include "VisionImage.hpp"

VisionImage::VisionImage() {

}

VisionImage::VisionImage(Image_Store* image, double scaleFactor) { // Use for testing purposes

	this->image = image;
	mat = getMat(image);

	double effSF = sqrt(scaleFactor);
	scaled_image = new Image_Store();
	scaled_image->width = (int)(image->width * effSF);
	scaled_image->height = (int)(image->height * effSF);
	scaled_image->planes = image->planes;
	scaled_image->pixel_format = PIXEL_FORMAT::RGB;
	scaled_image->interleave = INTERLEAVE_FORMAT::PIXEL_INTERLEAVED;
	scaled_image->max_val = 255;
	scaled_image->allocate_memory();

	scaled_mat = getMat(scaled_image);

	create(scaleFactor);
}

VisionImage::VisionImage(cv::Mat* mat, double scaleFactor) { // Give an uninitialized cv::Mat to the camera and then place it here

	this->mat = mat;
	image = getImage_Store(mat);

	double effSF = sqrt(scaleFactor);
	int scaledHeight = (int)(mat->rows * effSF);
	int scaledWidth = (int)(mat->cols * effSF);
	scaled_mat = new cv::Mat(scaledHeight, scaledWidth, CV_8UC3);

	scaled_image = getImage_Store(scaled_mat);

	create(scaleFactor);
}

void VisionImage::create(double scaleFactor) {

	this->scaleFactor = scaleFactor;
	sideScaleFactor = sqrt(scaleFactor);
	effSF = sqrt(1 / scaleFactor);
	threshold = 100;
	maximumSegments = 250;
	pixelSegScoreFactor = .2;

	targetColor = new Color(10.2, 175.95, 55.08);

	interestBox = new BoundingBox();
	interestBox->top = 0;
	interestBox->left = 0;
	interestBox->bottom = image->height - 1;
	interestBox->right = image->width - 1;

	segments = new std::vector<Segment*>();

	pConfig = new PConfig();
	pConfig->box = interestBox;
	pConfig->image = image;
	pConfig->scaled = scaled_image;
	pConfig->segments = segments;
	pConfig->previousValues = new uint8_t[scaled_image->width + 1];
	pConfig->color = targetColor;
	pConfig->threshold = threshold;
	pConfig->scaleFactor = scaleFactor;

}

VisionImage::~VisionImage() {

	delete image;
	delete mat;
	delete scaled_image; // Here
	delete scaled_mat;

	delete targetColor;
	delete interestBox;
	delete segments;
	delete pConfig;
}

Image_Store* VisionImage::getImage_Store(cv::Mat* mat) {
	Image_Store* image = new Image_Store();

	image->planes = mat->channels();
	image->width = mat->cols;
	image->height = mat->rows;

	image->pixel_format = PIXEL_FORMAT::BGR;
	image->interleave = INTERLEAVE_FORMAT::PIXEL_INTERLEAVED;
	
	image->image = mat->data;

	return image;
}

cv::Mat* VisionImage::getMat(Image_Store* image) {
	cv::Mat* mat = new cv::Mat(image->height, image->width, CV_8UC3);

//	delete mat->data;
	mat->data = image->image;
	
	return mat;
}

void VisionImage::prepareSegmentation(std::vector<Segment*>* segments) {

	for (int i = 0; i < segments->size(); i++) {
		delete segments->at(i);
	}

	segments->clear();
	Segment* segment = new Segment();
	segment->segmentID = 0;
	segment->merged = true;
	segments->push_back(segment);
}

void VisionImage::prepareSegmentation(std::vector<Segment*>* segments, uint8_t* previousValues, int size) {
	prepareSegmentation(segments); 
	memset(previousValues, 0, sizeof(uint8_t) * (size));
}

void VisionImage::setSegmentIDs(std::vector<Segment*>* segments, int newID, int oldID) {

	if (newID == oldID) {
		return;
	}

	for (int i = 0; i < segments->size(); i++) {

		if (segments->at(i)->segmentID == oldID) {
			segments->at(i)->segmentID = newID;
			segments->at(i)->merged = true;
		}

	}
}

void VisionImage::setSegmentIDs(std::vector<FutureMerge*>* futureMerges, int newID, int oldID) {
	for (int i = 0; i < futureMerges->size(); i++) {

		if (futureMerges->at(i)->ID1 == oldID) {
			futureMerges->at(i)->ID1 = newID;
		}

		if (futureMerges->at(i)->ID2 == oldID) {
			futureMerges->at(i)->ID2 = newID;
		}

	}
}

bool VisionImage::checkBoundingBoxOverlap(BoundingBox* b1, BoundingBox* b2) {

	if ((b1->top >= b2->top && b1->top <= b2->bottom) || (b1->bottom >= b2->top && b1->bottom <= b2->bottom)) {
		if ((b1->left >= b2->left && b1->left <= b2->right) || (b1->right >= b2->left && b1->right <= b2->right)) {
			return true;
		}
	}

	if ((b2->top >= b1->top && b2->top <= b1->bottom) || (b1->bottom >= b1->top && b2->bottom <= b1->bottom)) {
		if ((b2->left >= b1->left && b2->left <= b1->right) || (b2->right >= b1->left && b2->right <= b1->right)) {
			return true;
		}
	}

	return false;

}

VisionImage::BoundingBox* VisionImage::combineBoundingBoxes(BoundingBox* b1, BoundingBox* b2) {

	BoundingBox* box = new BoundingBox();

	if (b1->right > b2->right) {
		box->right = b1->right;
	}
	else {
		box->right = b2->right;
	}

	if (b1->left < b2->left) {
		box->left = b1->left;
	}
	else {
		box->left = b2->left;
	}

	if (b1->bottom > b2->bottom) {
		box->bottom = b1->bottom;
	}
	else {
		box->bottom = b2->bottom;
	}

	if (b1->top < b2->top) {
		box->top = b1->top;
	}
	else {
		box->top = b2->top;
	}

	return box;
}

void VisionImage::initializeSegment(Segment* segment, int segmentID, int x, int y) {
	segment->segmentID = segmentID;

	segment->box = new BoundingBox();
	segment->box->left = x;
	segment->box->right = x;
	segment->box->top = y;
	segment->box->bottom = y;

	segment->imagePointList = new std::vector<ImagePoint>();
	segment->scaledPointList = new std::vector<ImagePoint>();
}

void VisionImage::setFinalSegmentValues(std::vector<Segment*>* segments) {
	Segment* segmentUsing;
	Segment* segmentMerging;
	for (int i = 0; i < segments->size(); i++) {

		if (segments->at(i)->segmentID == 0) {
			continue;
		}

		if (segments->at(i)->merged) { // Add this segment to another

			segmentMerging = segments->at(i);
			segmentUsing = segments->at(segmentMerging->segmentID);

			segmentUsing->totalPixels += segmentMerging->totalPixels;
			segmentUsing->totalDistance += segmentMerging->totalDistance;
			segmentUsing->box = combineBoundingBoxes(segmentUsing->box, segmentMerging->box);

			segmentUsing->imagePointList->insert(segmentUsing->imagePointList->end(), segmentMerging->imagePointList->begin(), segmentMerging->imagePointList->end());
			segmentUsing->scaledPointList->insert(segmentUsing->scaledPointList->end(), segmentMerging->scaledPointList->begin(), segmentMerging->scaledPointList->end());

		}
	}

	for (int i = 0; i < segments->size(); i++) {
		if (!segments->at(i)->merged) {
			if (segments->at(i)->segmentID != 0) {
				segmentUsing = segments->at(i);
				segmentUsing->averageDistance = segmentUsing->totalPixels / segmentUsing->totalDistance;
			}
			else {
				segments->erase(segments->begin() + i);
			}
		}
	}

}

void VisionImage::trimSegments(std::vector<Segment*>* segments) {

	for (int i = 0; i < segments->size(); i++) {

		if (segments->at(i)->merged) {
			auto position = segments->begin();
			position += i;
			segments->erase(position);
			i--;
		}
		else {
			int cat = 0;
		}

	}

}

void VisionImage::mergeSegments(std::vector<Segment*>* segments) {

	Segment* mergeTo;
	Segment* toMerge;

	for (int i = 0; i < segments->size() - 1; i++) {

		mergeTo = segments->at(i);

		for (int g = i; g < segments->size(); g++) {

			toMerge = segments->at(g);

			if (mergeTo->segmentID == toMerge->segmentID) {

				mergeTo->totalPixels += toMerge->totalPixels;
				mergeTo->totalDistance += toMerge->totalDistance;
				mergeTo->box = combineBoundingBoxes(mergeTo->box, toMerge->box);

				mergeTo->imagePointList->insert(mergeTo->imagePointList->end(), toMerge->imagePointList->begin(), toMerge->imagePointList->end());
				mergeTo->scaledPointList->insert(mergeTo->scaledPointList->end(), toMerge->scaledPointList->begin(), toMerge->scaledPointList->end());

			}

		}

	}
}

double VisionImage::defaultScoreSegment(Segment* segment) {

	//segment->segScore = segment->averageDistance - (segment->totalPixels * pixelSegScoreFactor);
	segment->segScore = segment->totalPixels;
	
	return segment->segScore;
}

void VisionImage::scoreSegments() { // Probably going to change this
	for (int i = 0; i < segments->size(); i++) {
		segments->at(i)->segScore = defaultScoreSegment(segments->at(i));
	}
}

void VisionImage::scoreSegments(std::vector<Segment*>* segments, std::function<double(Segment*)>* scoringMethod) {
	for (int i = 0; i < segments->size(); i++) {
		segments->at(i)->segScore = (*scoringMethod)(segments->at(i));
	}
}

std::vector<VisionImage::Segment*>* VisionImage::getBestSegments(int num) {
	return getBestSegments(segments, num);
}

std::vector<VisionImage::Segment*>* VisionImage::getBestSegments(std::vector<Segment*>* segments, int num) {

	std::vector<Segment*>* bestSegments = new std::vector<Segment*>;

	for (int i = 0; bestSegments->size() <= num && i < segments->size(); i++) {
		bestSegments->push_back(getBestSegment(segments));
		i--;
	}
	
	return bestSegments;
}

VisionImage::Segment* VisionImage::getBestSegment(std::vector<Segment*>* segments) { // Should this remove it?

	int maxLoc = 0;
	for (int i = 1; i < segments->size(); i++) {

		if (segments->at(i)->segScore > segments->at(maxLoc)->segScore) {
			maxLoc = i;
		}

	}

	auto pos = segments->begin();
	pos += maxLoc;

	Segment* segment = segments->at(maxLoc);

	segments->erase(pos);

	return segment;
}

void VisionImage::colorSegment(Image_Store* image, std::vector<ImagePoint>* points, Color* color) {

	int value;

	for (int i = 0; i < points->size(); i++) {

		value = points->at(i).i * 3;

		image->image[value] = (uint8_t) color->r;
		image->image[value + 1] = (uint8_t) color->g;
		image->image[value + 2] = (uint8_t) color->b;

	}

}

void VisionImage::setInterestBox(BoundingBox* interestBox) {

	pConfig->box = interestBox;
	this->interestBox = interestBox;

}

int VisionImage::n4Process() {
	return n4ProcessC(pConfig);
}

int VisionImage::n4ProcessC(PConfig* pConfig) {
	return n4Process(pConfig->box, pConfig->image, pConfig->scaled, pConfig->segments, pConfig->previousValues, pConfig->color, pConfig->threshold, pConfig->scaleFactor);
}


int VisionImage::n4Process(BoundingBox* interestBox, Image_Store* image, Image_Store* scaled, std::vector<Segment*>* segments, uint8_t* previousValues, Color* targetColor, double threshold, double scaleFactor) {

	// Beginning Algorithm
	prepareSegmentation(segments, previousValues, scaled->width + 1);

	// Scale Factor
	double effSF = sqrt(1 / scaleFactor);
	double sideScaleFactor = sqrt(scaleFactor);

	// Thresholding Variables
	double rDiff = 0, bDiff = 0, gDiff = 0;
	double totalDiff = 0;
	double sqrdThresh = threshold * threshold;

	// Segmentation Variables
	uint8_t up = 0, left = 0;
	int IDCounter = 1;
	int usingID = IDCounter;
	int prevVal = 0;
	Segment* segment;

	// Dimension Variables
	int image_width = image->width; // In Pixels
	int image_height = image->height; // In Pixels
	int scaled_width = scaled->width; // In Pixels
	int scaled_height = scaled->height; // In Pixels

	// Navigation Variables
	double image_x = interestBox->left + 1; // In Pixels
	double image_y = interestBox->top + 1; // In Pixels
	int image_i = (int) ( (image_y * image->width) + image_x ); // In Pixels
	int image_value = (int) image_i * 3; // In Values

	int xBound = interestBox->right; // In Pixels
	int yBound = interestBox->bottom; // In Pixels
	int iBound = (yBound * image_width) + xBound; // In Pixels
	//int widthBound = interestBox->right - interestBox->left - 1; // In Pixels
	//int heightBound = interestBox->bottom - interestBox->top - 1; // In Pixels
	int imageXStart = interestBox->left + 1;

	int scaled_x = (int) ( interestBox->left * sideScaleFactor ) ; // In Pixels
	int scaled_y = (int) ( interestBox->top * sideScaleFactor ) ; // In Pixels
	int scaled_i = (scaled_y * scaled->width) + scaled_x; // In Pixels
	int scaled_value = scaled_i * 3; // In Values

	int scaledXStart = (int) ( interestBox->left * sideScaleFactor ) ;

	while (true) {

		rDiff = image->image[image_value] - targetColor->r;
		gDiff = image->image[image_value + 1] - targetColor->g;
		bDiff = image->image[image_value + 2] - targetColor->b;
		totalDiff = (rDiff * rDiff) + (gDiff * gDiff) + (bDiff * bDiff);

		if (totalDiff < sqrdThresh) {

			prevVal = previousValues[scaled_x];
			if (prevVal == 0) {
				left = 0;
			}
			else {
				left = segments->at(prevVal)->segmentID;
			}

			prevVal = previousValues[scaled_x + 1];
			if (prevVal == 0) {
				up = 0;
			}
			else {
				up = segments->at(prevVal)->segmentID;
			}

			if (up != 0 && left != 0) { // Merge Segments
				usingID = up;
				if (up != left) {
					setSegmentIDs(segments, up, left); // Check to make sure they are not already merged
				}
			}
			else if (up != 0) { // Part of Up Segment
				usingID = up;
			}
			else if (left != 0) { // Part of Left Segment
				usingID = left;
			}
			else { // Create New Segment
				usingID = IDCounter;
				IDCounter++;
				Segment* segment = new Segment();
				initializeSegment(segment, usingID, (int) image_x, (int) image_y);
				segments->push_back(segment);
				
				if (IDCounter > maximumSegments) {
					// Put Deletion Code Here
					std::cout << "Too Many Segments!";
					return 1;
				}

			}

			segment = segments->at(usingID);
			segment->imagePointList->push_back(ImagePoint((int) image_x, (int) image_y, (int) image_i));
			segment->scaledPointList->push_back(ImagePoint(scaled_x, scaled_y, scaled_i));
			segment->totalPixels++;
			segment->totalDistance += sqrt(totalDiff);

			if (image_x > segment->box->right) {
				segment->box->right = (int) image_x;
			}
			else if (image_x < segment->box->left) {
				segment->box->left = (int) image_x;
			}

			if (image_y > segment->box->bottom) {
				segment->box->bottom = (int) image_y;
			}
			else if (image_y < segment->box->top) {
				segment->box->top = image_y;
			}

			previousValues[scaled_x + 1] = usingID;

			scaled->image[scaled_value] = 0;
			scaled->image[scaled_value + 1] = 255;
			scaled->image[scaled_value + 2] = 0;

			image->image[image_value] = usingID;

		}
		else {

			previousValues[scaled_x + 1] = 0;

			scaled->image[scaled_value] = (uint8_t) ( image->image[image_value] * .5 );
			scaled->image[scaled_value + 1] = (uint8_t) (image->image[image_value + 1] * .5 );
			scaled->image[scaled_value + 2] = (uint8_t) (image->image[image_value + 2] * .5 );

			image->image[image_value] = 0;

		}

		//image->image[image_value] = 255; // Enable this and write the original image to see scaling pattern
		//image->image[image_value + 1] = 255;
		//image->image[image_value + 2] = 255;

		// Incrementation
		image_x += effSF;
		scaled_x++;

		if ((int) image_x > xBound) {
			image_y += effSF;
			scaled_y++;

			//image_x = interestBox->left + 1;
			//scaled_x = 0;
			image_x = imageXStart;
			scaled_x = scaledXStart;
		}

		image_i = (((int)image_y) * image_width) + (int)(image_x);
		image_value = image_i * 3;
		scaled_i = (scaled_y * scaled_width) + scaled_x;
		scaled_value = scaled_i * 3;
		
		// End of box
		if (image_i > iBound) {
			break;
		}
	}

	setFinalSegmentValues(segments);
	trimSegments(segments);

	return 0;
}

void VisionImage::initializePConfigs(int num, std::vector<PConfig*>* threadConfigs, PConfig* model) {

	for (int i = 0; i < threadConfigs->size(); i++) {
		delete threadConfigs->at(i);
	}

	if (threadConfigs->size() > 0) {
		threadConfigs->clear();
	}

	for (int i = 0; i < num; i++) {

		PConfig* config = new PConfig();

		config->box = model->box;

		config->image = model->image;
		config->scaled = model->scaled;

		config->segments = new std::vector<Segment*>;
		config->previousValues = new uint8_t[model->scaled->width + 1];

		config->color = model->color;
		config->threshold = model->threshold;

		config->scaleFactor = model->scaleFactor;

		threadConfigs->push_back(config);
	}

}

void VisionImage::setBoundingBoxes(BoundingBox* interestBox, std::vector<PConfig*>* threadConfigs) {

	double scaleFactor = threadConfigs->at(0)->scaleFactor;
	double sideScaleFactor = sqrt(scaleFactor);

	int numThreads = (int) threadConfigs->size();
	int start = (int)(interestBox->top * sideScaleFactor);
	int end = (int)(interestBox->bottom * sideScaleFactor);

	int step = (end - start) / numThreads;
	int barrier = start;

	PConfig* config;
	for (int i = 0; i < threadConfigs->size(); i++) {

		config = threadConfigs->at(i);

		BoundingBox* newBox = new BoundingBox();
		config->box = newBox;
		
		newBox->left = interestBox->left;
		newBox->right = interestBox->right;

		newBox->top = (int) (((int) barrier) / sideScaleFactor) + 1;
		barrier += step;
		newBox->bottom = (int)(((int)barrier) / sideScaleFactor) + 1;

	}

	threadConfigs->at(0)->box->top = interestBox->top;
	threadConfigs->at(threadConfigs->size() - 1)->box->bottom = interestBox->bottom;

}

int VisionImage::n4MultithreadProcess() {
	return n4MultithreadProcess(interestBox, threadConfigs, pConfig, segments);
}

int VisionImage::n4MultithreadProcess(BoundingBox* interestBox, std::vector<PConfig*>* threadConfigs, PConfig* model, std::vector<Segment*>* segments) {

	//std::function<int(N4PConfig*)> lambda_arg = [this] (N4PConfig* config) {
	//	return n4ProcessC(config); 
	//};
	
	std::function<int()>* lambda;

	for (int i = 0; i < threadConfigs->size(); i++) {
		PConfig* config = threadConfigs->at(i);

		lambda = new std::function<int()>();
		*lambda = [this, config]() {
			return n4ProcessC_SubPro(config);
		};

		executorService->addLambdaTask(lambda);

		//executorService->addTask(this, &VisionImage::n4ProcessC, config);

	}

	//(*lambda)();

	executorService->registerThisThread(true);

	double effSF = sqrt(1 / scaleFactor);
	double image_i;

	Image_Store* image = threadConfigs->at(0)->image;
	
	int levelID, lowerID;
	std::vector<Segment*>* upperSegments;
	std::vector<Segment*>* lowerSegments;
	std::vector<FutureMerge*>* futureMerges = new std::vector<FutureMerge*>();

	int prevTotalSize = 0;
	int totalSize = 0;

	for (int i = 0; i < threadConfigs->size() - 1; i++) {

		upperSegments = threadConfigs->at(i)->segments;
		lowerSegments = threadConfigs->at(i + 1)->segments;

		image_i = (((int) threadConfigs->at(i)->box->bottom) * image->width) + interestBox->left + 1;

		totalSize += (int) upperSegments->size();

		for (double pos = interestBox->left + 1; (int) pos < interestBox->right; pos += effSF) {

			levelID = upperSegments->at(image->image[((int) image_i) * 3])->segmentID;
			lowerID = lowerSegments->at(image->image[(((int) image_i) + image->width) * 3])->segmentID;

			if (levelID != 0 && lowerID != 0) {
				// Probably running way too many times

				futureMerges->push_back(new FutureMerge(levelID + prevTotalSize, lowerID + totalSize));

			}

			image_i += effSF;
		}

		prevTotalSize += (int) upperSegments->size();

	}

	// Merge Segments Together
	totalSize = 0;

	for (int i = 1; i < threadConfigs->size(); i++) {
		
		totalSize += (int) threadConfigs->at(i - 1)->segments->size();

		for (int g = 1; g < threadConfigs->at(i)->segments->size(); g++) {

			threadConfigs->at(i)->segments->at(g)->segmentID += (totalSize);

		}

	}

	for (int i = 0; i < threadConfigs->size(); i++) {

		segments->insert(segments->end(), threadConfigs->at(i)->segments->begin(), threadConfigs->at(i)->segments->end());

	}

	for (int i = 0; i < futureMerges->size(); i++) {
		if (futureMerges->at(i)->ID1 != futureMerges->at(i)->ID2) {
			setSegmentIDs(segments, futureMerges->at(i)->ID1, futureMerges->at(i)->ID2);
			setSegmentIDs(futureMerges, futureMerges->at(i)->ID1, futureMerges->at(i)->ID2);
		}
	}

	setFinalSegmentValues(segments);
	//mergeSegments(segments);
	trimSegments(segments);

	return 0;
}

int VisionImage::n4ProcessC_SubPro(PConfig* pConfig) {
	return n4Process_SubPro(pConfig->box, pConfig->image, pConfig->scaled, pConfig->segments, pConfig->previousValues, pConfig->color, pConfig->threshold, pConfig->scaleFactor);
}

int VisionImage::n4Process_SubPro(BoundingBox* interestBox, Image_Store* image, Image_Store* scaled, std::vector<Segment*>* segments, uint8_t* previousValues, Color* targetColor, double threshold, double scaleFactor) {
	// Beginning Algorithm
	prepareSegmentation(segments, previousValues, scaled->width + 1);

	// Scale Factor
	double effSF = sqrt(1 / scaleFactor);
	double sideScaleFactor = sqrt(scaleFactor);

	// Thresholding Variables
	double rDiff = 0, bDiff = 0, gDiff = 0;
	double totalDiff = 0;
	double sqrdThresh = threshold * threshold;

	// Segmentation Variables
	uint8_t up = 0, left = 0;
	int IDCounter = 1;
	int usingID = IDCounter;
	int prevVal = 0;
	Segment* segment;

	// Dimension Variables
	int image_width = image->width; // In Pixels
	int image_height = image->height; // In Pixels
	int scaled_width = scaled->width; // In Pixels
	int scaled_height = scaled->height; // In Pixels

	// Navigation Variables
	double image_x = interestBox->left + 1; // In Pixels
	double image_y = interestBox->top + 1; // In Pixels
	double image_i = (image_y * image->width) + image_x; // In Pixels
	int image_value = (int) ( image_i * 3 ); // In Values

	int xBound = interestBox->right; // In Pixels
	int yBound = interestBox->bottom; // In Pixels
	int iBound = (yBound * image_width) + xBound; // In Pixels
	//int widthBound = interestBox->right - interestBox->left - 1; // In Pixels
	//int heightBound = interestBox->bottom - interestBox->top - 1; // In Pixels
	int imageXStart = interestBox->left + 1;

	int scaled_x = (int) ( interestBox->left * sideScaleFactor ) ; // In Pixels
	int scaled_y = (int) ( interestBox->top * sideScaleFactor ); // In Pixels
	int scaled_i = (scaled_y * scaled->width) + scaled_x; // In Pixels
	int scaled_value = scaled_i * 3; // In Values

	int scaledXStart = (int) ( interestBox->left * sideScaleFactor );

	while (true) {

		rDiff = image->image[image_value] - targetColor->r;
		gDiff = image->image[image_value + 1] - targetColor->g;
		bDiff = image->image[image_value + 2] - targetColor->b;
		totalDiff = (rDiff * rDiff) + (gDiff * gDiff) + (bDiff * bDiff);

		if (totalDiff < sqrdThresh) {

			prevVal = previousValues[scaled_x];
			if (prevVal == 0) {
				left = 0;
			}
			else {
				left = segments->at(prevVal)->segmentID;
			}

			prevVal = previousValues[scaled_x + 1];
			if (prevVal == 0) {
				up = 0;
			}
			else {
				up = segments->at(prevVal)->segmentID;
			}

			if (up != 0 && left != 0) { // Merge Segments
				usingID = up;
				if (up != left) {
					setSegmentIDs(segments, up, left); // Check to make sure they are not already merged
				}
			}
			else if (up != 0) { // Part of Up Segment
				usingID = up;
			}
			else if (left != 0) { // Part of Left Segment
				usingID = left;
			}
			else { // Create New Segment
				usingID = IDCounter;
				IDCounter++;
				Segment* segment = new Segment();
				initializeSegment(segment, usingID, (int) image_x, (int) image_y);
				segments->push_back(segment);

				if (IDCounter > maximumSegments) {
					// Put Deletion Code Here
					std::cout << "Too Many Segments!";
					return 1;
				}

			}

			segment = segments->at(usingID);
			segment->imagePointList->push_back(ImagePoint((int) image_x, (int) image_y, (int) image_i));
			segment->scaledPointList->push_back(ImagePoint(scaled_x, scaled_y, scaled_i));
			segment->totalPixels++;
			segment->totalDistance += sqrt(totalDiff);

			if (image_x > segment->box->right) {
				segment->box->right = (int) image_x;
			}
			else if (image_x < segment->box->left) {
				segment->box->left = (int) image_x;
			}

			if (image_y > segment->box->bottom) {
				segment->box->bottom = (int) image_y;
			}
			else if (image_y < segment->box->top) {
				segment->box->top = image_y;
			}

			previousValues[scaled_x + 1] = usingID;

			scaled->image[scaled_value] = 0;
			scaled->image[scaled_value + 1] = 255;
			scaled->image[scaled_value + 2] = 0;

			image->image[image_value] = usingID;

		}
		else {

			previousValues[scaled_x + 1] = 0;

			scaled->image[scaled_value] = (uint8_t)(image->image[image_value] * .5);
			scaled->image[scaled_value + 1] = (uint8_t)(image->image[image_value + 1] * .5);
			scaled->image[scaled_value + 2] = (uint8_t)(image->image[image_value + 2] * .5);

			image->image[image_value] = 0;

			//image->image[image_value] = 255;
			//image->image[image_value + 1] = 255;
			//image->image[image_value + 2] = 255;

		}

		//image->image[image_value] = 255; // Enable this and write the original image to see scaling pattern
		//image->image[image_value + 1] = 255;
		//image->image[image_value + 2] = 255;

		// Incrementation
		image_x += effSF;
		scaled_x++;

		if ((int)image_x > xBound) {
			image_y += effSF;
			scaled_y++;

			//image_x = interestBox->left + 1;
			//scaled_x = 0;
			image_x = imageXStart;
			scaled_x = scaledXStart;
		}

		image_i = (((int)image_y) * image_width) + (int)(image_x);
		image_value = (int) ( image_i * 3 );
		scaled_i = (scaled_y * scaled_width) + scaled_x;
		scaled_value = scaled_i * 3;

		// End of box
		if (image_i > iBound) {
			break;
		}
	}

	return 0;
}

int VisionImage::n8Process() {
	return n8ProcessC(pConfig);
}

int VisionImage::n8ProcessC(PConfig* pConfig) {
	return n8Process(pConfig->box, pConfig->image, pConfig->scaled, pConfig->segments, pConfig->previousValues, pConfig->color, pConfig->threshold, pConfig->scaleFactor);
}

int VisionImage::n8Process(BoundingBox* interestBox, Image_Store* image, Image_Store* scaled, std::vector<Segment*>* segments, uint8_t* previousValues, Color* targetColor, double threshold, double scaleFactor) {

	// Beginning Algorithm
	prepareSegmentation(segments, previousValues, scaled->width + 1);

	// Scale Factor
	double effSF = sqrt(1 / scaleFactor);
	double sideScaleFactor = sqrt(scaleFactor);

	// Thresholding Variables
	double rDiff = 0, bDiff = 0, gDiff = 0;
	double totalDiff = 0;
	double sqrdThresh = threshold * threshold;

	// Segmentation Variables
	uint8_t up = 0, left = 0, topLeft = 0, topRight = 0;
	int IDCounter = 1;
	int usingID = IDCounter;
	int prevVal = 0;
	Segment* segment;

	// Dimension Variables
	int image_width = image->width; // In Pixels
	int image_height = image->height; // In Pixels
	int scaled_width = scaled->width; // In Pixels
	int scaled_height = scaled->height; // In Pixels

	// Navigation Variables
	double image_x = interestBox->left + 1; // In Pixels
	double image_y = interestBox->top + 1; // In Pixels
	double image_i = (image_y * image->width) + image_x; // In Pixels
	int image_value = (int) ( image_i * 3 ); // In Values

	int xBound = interestBox->right; // In Pixels
	int yBound = interestBox->bottom; // In Pixels
	int iBound = (yBound * image_width) + xBound; // In Pixels
	//int widthBound = interestBox->right - interestBox->left - 1; // In Pixels
	//int heightBound = interestBox->bottom - interestBox->top - 1; // In Pixels
	int imageXStart = interestBox->left + 1;

	int scaled_x = (int) ( interestBox->left * sideScaleFactor ); // In Pixels
	int scaled_y = (int) ( interestBox->top * sideScaleFactor ); // In Pixels
	int scaled_i = (scaled_y * scaled->width) + scaled_x; // In Pixels
	int scaled_value = scaled_i * 3; // In Values

	int scaledXStart = (int) ( interestBox->left * sideScaleFactor );

	while (true) {

		rDiff = image->image[image_value] - targetColor->r;
		gDiff = image->image[image_value + 1] - targetColor->g;
		bDiff = image->image[image_value + 2] - targetColor->b;
		totalDiff = (rDiff * rDiff) + (gDiff * gDiff) + (bDiff * bDiff);

		if (totalDiff < sqrdThresh) {

			prevVal = previousValues[scaled_x];
			if (prevVal == 0) {
				left = 0;
			}
			else {
				left = segments->at(prevVal)->segmentID;
			}

			prevVal = previousValues[scaled_x + 1];
			if (prevVal == 0) {
				up = 0;
			}
			else {
				up = segments->at(prevVal)->segmentID;
			}

			prevVal = previousValues[scaled_x + 2];
			if (prevVal == 0) {
				topRight = 0;
			}
			else {
				topRight = segments->at(prevVal)->segmentID;
			}

			//if (up != 0 && left != 0) { // Merge Segments
			//	usingID = up;
			//	if (up != left) {
			//		setSegmentIDs(segments, up, left); // Check to make sure they are not already merged
			//	}
			//}
			//else if (up != 0) { // Part of Up Segment
			//	usingID = up;
			//}
			//else if (left != 0) { // Part of Left Segment
			//	usingID = left;
			//}
			//else { // Create New Segment
			//	usingID = IDCounter;
			//	IDCounter++;
			//	Segment* segment = new Segment();
			//	initializeSegment(segment, usingID, image_x, image_y);
			//	segments->push_back(segment);

			//	if (IDCounter > maximumSegments) {
			//		// Put Deletion Code Here
			//		std::cout << "Too Many Segments!";
			//		return 1;
			//	}

			//}

			usingID = 0;

			if (up != 0 && left != 0) { // Merge up and left
				usingID = up;
				if (up != left) {
					setSegmentIDs(segments, up, left);
				}
			}
			else if (up != 0) { // Part of up Segment
				usingID = up;
			}
			else if (left != 0) { // Part of left Segment
				usingID = left;
			}

			if (usingID != 0 && topRight != 0) { // Merge usingID and topRight
				if (usingID != topRight) {
					setSegmentIDs(segments, usingID, topRight);
				}
			}
			else if (usingID != 0) { // Part of usingID Segment

			}
			else if (topRight != 0) { // Part of topRight Segment
				usingID = topRight;
			}

			if (topLeft != 0 && usingID != 0) { // Merge topLeft and usingID
				if (up != left) {
					setSegmentIDs(segments, usingID, topLeft);
				}
			}
			else if (topLeft != 0) {
				usingID = topLeft;
			}
			else if (usingID != 0) {
				
			}
			else { // Create New Segment

				usingID = IDCounter;
				IDCounter++;
				Segment* segment = new Segment();
				initializeSegment(segment, usingID, (int) image_x, (int) image_y);
				segments->push_back(segment);

				if (IDCounter > maximumSegments) {
					// Put Deletion Code Here
					std::cout << "Too Many Segments!";
					return 1;
				}

			}

			segment = segments->at(usingID);
			segment->imagePointList->push_back(ImagePoint((int) image_x, (int) image_y, (int) image_i));
			segment->scaledPointList->push_back(ImagePoint(scaled_x, scaled_y, scaled_i));
			segment->totalPixels++;
			segment->totalDistance += sqrt(totalDiff);

			if (image_x > segment->box->right) {
				segment->box->right = (int) image_x;
			}
			else if (image_x < segment->box->left) {
				segment->box->left = (int) image_x;
			}

			if (image_y > segment->box->bottom) {
				segment->box->bottom = (int) image_y;
			}
			else if (image_y < segment->box->top) {
				segment->box->top = image_y;
			}

			previousValues[scaled_x + 1] = usingID;
			topLeft = up;

			scaled->image[scaled_value] = 0;
			scaled->image[scaled_value + 1] = 255;
			scaled->image[scaled_value + 2] = 0;

			image->image[image_value] = usingID;

		}
		else {

			topLeft = previousValues[scaled_x + 1];
			previousValues[scaled_x + 1] = 0;

			scaled->image[scaled_value] = (uint8_t)( image->image[image_value] * .5);
			scaled->image[scaled_value + 1] = (uint8_t)(image->image[image_value + 1] * .5);
			scaled->image[scaled_value + 2] = (uint8_t)(image->image[image_value + 2] * .5);

			image->image[image_value] = 0;

		}

		//image->image[image_value] = 255; // Enable this and write the original image to see scaling pattern
		//image->image[image_value + 1] = 255;
		//image->image[image_value + 2] = 255;

		// Incrementation
		image_x += effSF;
		scaled_x++;

		if ((int)image_x > xBound) {
			image_y += effSF;
			scaled_y++;

			//image_x = interestBox->left + 1;
			//scaled_x = 0;
			image_x = imageXStart;
			scaled_x = scaledXStart;
		}

		image_i = (((int)image_y) * image_width) + (int)(image_x);
		image_value = (int) ( image_i * 3 );
		scaled_i = (scaled_y * scaled_width) + scaled_x;
		scaled_value = scaled_i * 3;

		// End of box
		if (image_i > iBound) {
			break;
		}
	}

	setFinalSegmentValues(segments);
	trimSegments(segments);

	return 0;
}

int VisionImage::n8MultithreadProcess() {
	return n8MultithreadProcess(interestBox, threadConfigs, pConfig, segments);
}

int VisionImage::n8MultithreadProcess(BoundingBox* interestBox, std::vector<PConfig*>* threadConfigs, PConfig* model, std::vector<Segment*>* segments) {

	std::function<int()>* lambda;

	for (int i = 0; i < threadConfigs->size(); i++) {
		PConfig* config = threadConfigs->at(i);

		lambda = new std::function<int()>();
		*lambda = [this, config]() {
			return n8ProcessC_SubPro(config);
		};

		executorService->addLambdaTask(lambda);

		//executorService->addTask(this, &VisionImage::n4ProcessC, config);

	}

	//(*lambda)();

	executorService->registerThisThread(true);

	double effSF = sqrt(1 / scaleFactor);
	double image_i;

	Image_Store* image = threadConfigs->at(0)->image;

	int levelID, lowerID;
	std::vector<Segment*>* upperSegments;
	std::vector<Segment*>* lowerSegments;
	std::vector<FutureMerge*>* futureMerges = new std::vector<FutureMerge*>();

	int prevTotalSize = 0;
	int totalSize = 0;

	for (int i = 0; i < threadConfigs->size() - 1; i++) {

		upperSegments = threadConfigs->at(i)->segments;
		lowerSegments = threadConfigs->at(i + 1)->segments;

		image_i = (((int)threadConfigs->at(i)->box->bottom) * image->width) + interestBox->left + 1;

		totalSize += (int) upperSegments->size();

		for (double pos = interestBox->left + 1; (int)pos < interestBox->right; pos += effSF) {

			levelID = upperSegments->at(image->image[((int)image_i) * 3])->segmentID;
			lowerID = lowerSegments->at(image->image[(((int)image_i) + image->width) * 3])->segmentID;

			if (levelID != 0 && lowerID != 0) {
				// Probably running way too many times

				futureMerges->push_back(new FutureMerge(levelID + prevTotalSize, lowerID + totalSize));

			}

			image_i += effSF;
		}

		prevTotalSize += (int) upperSegments->size();

	}

	// Merge Segments Together
	totalSize = 0;

	for (int i = 1; i < threadConfigs->size(); i++) {

		totalSize += (int) threadConfigs->at(i - 1)->segments->size();

		for (int g = 1; g < threadConfigs->at(i)->segments->size(); g++) {

			threadConfigs->at(i)->segments->at(g)->segmentID += (totalSize);

		}

	}

	for (int i = 0; i < threadConfigs->size(); i++) {

		segments->insert(segments->end(), threadConfigs->at(i)->segments->begin(), threadConfigs->at(i)->segments->end());

	}

	for (int i = 0; i < futureMerges->size(); i++) {
		if (futureMerges->at(i)->ID1 != futureMerges->at(i)->ID2) {
			setSegmentIDs(segments, futureMerges->at(i)->ID1, futureMerges->at(i)->ID2);
			setSegmentIDs(futureMerges, futureMerges->at(i)->ID1, futureMerges->at(i)->ID2);
		}
	}

	setFinalSegmentValues(segments);
	trimSegments(segments);

	return 0;
}

int VisionImage::n8ProcessC_SubPro(PConfig* pConfig) {
	return n8Process_SubPro(pConfig->box, pConfig->image, pConfig->scaled, pConfig->segments, pConfig->previousValues, pConfig->color, pConfig->threshold, pConfig->scaleFactor);
}

int VisionImage::n8Process_SubPro(BoundingBox* interestBox, Image_Store* image, Image_Store* scaled, std::vector<Segment*>* segments, uint8_t* previousValues, Color* targetColor, double threshold, double scaleFactor) {

	// Beginning Algorithm
	prepareSegmentation(segments, previousValues, scaled->width + 1);

	// Scale Factor
	double effSF = sqrt(1 / scaleFactor);
	double sideScaleFactor = sqrt(scaleFactor);

	// Thresholding Variables
	double rDiff = 0, bDiff = 0, gDiff = 0;
	double totalDiff = 0;
	double sqrdThresh = threshold * threshold;

	// Segmentation Variables
	uint8_t up = 0, left = 0, topLeft = 0, topRight = 0;
	int IDCounter = 1;
	int usingID = IDCounter;
	int prevVal = 0;
	Segment* segment;

	// Dimension Variables
	int image_width = image->width; // In Pixels
	int image_height = image->height; // In Pixels
	int scaled_width = scaled->width; // In Pixels
	int scaled_height = scaled->height; // In Pixels

	// Navigation Variables
	double image_x = interestBox->left + 1; // In Pixels
	double image_y = interestBox->top + 1; // In Pixels
	double image_i = (image_y * image->width) + image_x; // In Pixels
	int image_value = (int) ( image_i * 3 ); // In Values

	int xBound = interestBox->right; // In Pixels
	int yBound = interestBox->bottom; // In Pixels
	int iBound = (yBound * image_width) + xBound; // In Pixels
	//int widthBound = interestBox->right - interestBox->left - 1; // In Pixels
	//int heightBound = interestBox->bottom - interestBox->top - 1; // In Pixels
	int imageXStart = interestBox->left + 1;

	int scaled_x = (int) ( interestBox->left * sideScaleFactor ) ;		// In Pixels
	int scaled_y = (int) ( interestBox->top * sideScaleFactor );	// In Pixels
	int scaled_i = (scaled_y * scaled->width) + scaled_x;				// In Pixels
	int scaled_value = scaled_i * 3; // In Values

	int scaledXStart = (int) ( interestBox->left * sideScaleFactor );

	while (true) {

		rDiff = image->image[image_value] - targetColor->r;
		gDiff = image->image[image_value + 1] - targetColor->g;
		bDiff = image->image[image_value + 2] - targetColor->b;
		totalDiff = (rDiff * rDiff) + (gDiff * gDiff) + (bDiff * bDiff);

		if (totalDiff < sqrdThresh) {

			prevVal = previousValues[scaled_x];
			if (prevVal == 0) {
				left = 0;
			}
			else {
				left = segments->at(prevVal)->segmentID;
			}

			prevVal = previousValues[scaled_x + 1];
			if (prevVal == 0) {
				up = 0;
			}
			else {
				up = segments->at(prevVal)->segmentID;
			}

			prevVal = previousValues[scaled_x + 2];
			if (prevVal == 0) {
				topRight = 0;
			}
			else {
				topRight = segments->at(prevVal)->segmentID;
			}

			//if (up != 0 && left != 0) { // Merge Segments
			//	usingID = up;
			//	if (up != left) {
			//		setSegmentIDs(segments, up, left); // Check to make sure they are not already merged
			//	}
			//}
			//else if (up != 0) { // Part of Up Segment
			//	usingID = up;
			//}
			//else if (left != 0) { // Part of Left Segment
			//	usingID = left;
			//}
			//else { // Create New Segment
			//	usingID = IDCounter;
			//	IDCounter++;
			//	Segment* segment = new Segment();
			//	initializeSegment(segment, usingID, image_x, image_y);
			//	segments->push_back(segment);

			//	if (IDCounter > maximumSegments) {
			//		// Put Deletion Code Here
			//		std::cout << "Too Many Segments!";
			//		return 1;
			//	}

			//}

			usingID = 0;

			if (up != 0 && left != 0) { // Merge up and left
				usingID = up;
				if (up != left) {
					setSegmentIDs(segments, up, left);
				}
			}
			else if (up != 0) { // Part of up Segment
				usingID = up;
			}
			else if (left != 0) { // Part of left Segment
				usingID = left;
			}

			if (usingID != 0 && topRight != 0) { // Merge usingID and topRight
				if (usingID != topRight) {
					setSegmentIDs(segments, usingID, topRight);
				}
			}
			else if (usingID != 0) { // Part of usingID Segment

			}
			else if (topRight != 0) { // Part of topRight Segment
				usingID = topRight;
			}

			if (topLeft != 0 && usingID != 0) { // Merge topLeft and usingID
				if (up != left) {
					setSegmentIDs(segments, usingID, topLeft);
				}
			}
			else if (topLeft != 0) {
				usingID = topLeft;
			}
			else if (usingID != 0) {

			}
			else { // Create New Segment

				usingID = IDCounter;
				IDCounter++;
				Segment* segment = new Segment();
				initializeSegment(segment, usingID, (int) image_x, (int) image_y);
				segments->push_back(segment);

				if (IDCounter > maximumSegments) {
					// Put Deletion Code Here
					std::cout << "Too Many Segments!";
					return 1;
				}

			}

			segment = segments->at(usingID);
			segment->imagePointList->push_back(ImagePoint((int) image_x, (int) image_y, (int) image_i));
			segment->scaledPointList->push_back(ImagePoint(scaled_x, scaled_y, scaled_i));
			segment->totalPixels++;
			segment->totalDistance += sqrt(totalDiff);

			if (image_x > segment->box->right) {
				segment->box->right = (int) image_x;
			}
			else if (image_x < segment->box->left) {
				segment->box->left = (int) image_x;
			}

			if (image_y > segment->box->bottom) {
				segment->box->bottom = (int) image_y;
			}
			else if (image_y < segment->box->top) {
				segment->box->top = image_y;
			}

			previousValues[scaled_x + 1] = usingID;
			topLeft = up;

			scaled->image[scaled_value] = 0;
			scaled->image[scaled_value + 1] = 255;
			scaled->image[scaled_value + 2] = 0;

			image->image[image_value] = usingID;

		}
		else {

			topLeft = previousValues[scaled_x + 1];
			previousValues[scaled_x + 1] = 0;

			scaled->image[scaled_value] = (uint8_t) (image->image[image_value] * .5);
			scaled->image[scaled_value + 1] = (uint8_t) (image->image[image_value + 1] * .5);
			scaled->image[scaled_value + 2] = (uint8_t) (image->image[image_value + 2] * .5);

			image->image[image_value] = 0;

		}

		//image->image[image_value] = 255; // Enable this and write the original image to see scaling pattern
		//image->image[image_value + 1] = 255;
		//image->image[image_value + 2] = 255;

		// Incrementation
		image_x += effSF;
		scaled_x++;

		if ((int)image_x > xBound) {
			image_y += effSF;
			scaled_y++;

			//image_x = interestBox->left + 1;
			//scaled_x = 0;
			image_x = imageXStart;
			scaled_x = scaledXStart;
		}

		image_i = (((int)image_y) * image_width) + (int)(image_x);
		image_value = (int) ( image_i * 3 ) ;
		scaled_i = (scaled_y * scaled_width) + scaled_x;
		scaled_value = scaled_i * 3;

		// End of box
		if (image_i > iBound) {
			break;
		}
	}

	return 0;
}