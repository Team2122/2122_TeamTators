//
// Created by eddie on 6/25/2021.
//

#include "Tester.hpp"
#include <cassert>

Tester::Tester(std::filesystem::path* pathToDirectory, std::function<int(std::filesystem::path*)>* function)
{
	this->pathToDirectory = pathToDirectory;
	this->executorService = new ExecutorService(ExecutorService::SleepType::NOSLEEP);
	this->function_normal = function;
	functionType = FunctionType::NORMAL;

	create();
}

Tester::Tester(std::filesystem::path* pathToDirectory, std::function<int(std::filesystem::path*)>* function, ExecutorService* executorService)
{
	this->pathToDirectory = pathToDirectory;
	this->executorService = executorService;
	this->function_normal = function;
	functionType = FunctionType::NORMAL;

	create();
}

Tester::Tester(std::filesystem::path* pathToDirectory, std::function<int(std::filesystem::path*, std::string* message)>* function) {


	this->pathToDirectory = pathToDirectory;
	this->executorService = new ExecutorService(ExecutorService::SleepType::NOSLEEP);
	this->function_string = function;
	functionType = FunctionType::STRING;

	create();
}

Tester::Tester(std::filesystem::path* pathToDirectory, std::function<int(std::filesystem::path*, std::string* messages)>* function, ExecutorService* executorService) {

	this->pathToDirectory = pathToDirectory;
	this->executorService = executorService;
	this->function_string = function;
	functionType = FunctionType::STRING;

	create();
}

Tester::~Tester()
{
	delete executorService;
	delete pathToDirectory;
	delete paths; // delete paths the right way!
	delete paths_mutex;

	if (fileExtension != nullptr) {
		delete fileExtension;
	}
}

void Tester::create() {
	paths = new std::queue<std::filesystem::path*>();
	paths_mutex = new std::mutex();
}

void Tester::setPathQueue() {
	int i = 0;
	for (std::filesystem::directory_entry path : std::filesystem::recursive_directory_iterator(*pathToDirectory)) {
		if (!path.path().extension().empty()) {
			if (certainFiles) {
				if (path.path().extension().string() == *fileExtension) {
					paths->push(new std::filesystem::path(path.path()));
					i++;
				}
			}
			else {
				paths->push(new std::filesystem::path(path.path()));
				i++;
			}
		}
		else
		{
			std::cout << "File Path is empty" ;
		}
	}
	std::cout << "Paths Found: " << i << "\n";
}

void Tester::addAllTasks() {
	int limit = paths->size();

	if (functionType == FunctionType::NORMAL) {
		for (int i = 0; i < limit; i++) {
			executorService->addLambdaTask(&lambdaRun_normal);
		}
	}
	else if (functionType == FunctionType::STRING) {
		for (int i = 0; i < limit; i++) {
			executorService->addLambdaTask(&lambdaRun_string);
		}
	}

	std::cout << "Tasks Added: " << limit << "\n";
}

void Tester::start() {
	addAllTasks();
}

void Tester::setFiletype(std::string* fileExtension) {
	certainFiles = true;
	this->fileExtension = fileExtension;
}

void Tester::clearFiletype() {
	certainFiles = false;
}

bool Tester::isFinished() {
	return executorService->allTasksComplete();
}

void Tester::safePrint(std::string message) {
	executorService->safePrint(message);
}

void Tester::createDataFile(std::string* data, std::filesystem::path* path) {

	path->replace_extension("");
	path->replace_filename(path->filename().string() + dataFileName + ".txt");
	//std::cout << "\nPath: " << path->string() << "\n";

	std::ofstream* file = new std::ofstream(*path);

	*file << *data;

	file->close();
	delete file;
}

void Tester::createDataFile(std::string* data) {

	std::filesystem::path* path = new std::filesystem::path(pathToDirectory->string());
	path->replace_extension("");
	path->replace_filename(path->filename().string() + dataFileName + ".txt");

	std::ofstream* file = new std::ofstream(*path);

	*file << *data;

	file->close();
	delete file;
}

std::filesystem::path* Tester::getPairedFilePath(std::filesystem::path* path) {
	return getPairedFilePath(path, defaultPairedNameExtension, defaultPairedFileExtension);
}

std::filesystem::path* Tester::getPairedFilePath(std::filesystem::path* path, std::string nameExtension, std::string fileExtension) {

	std::filesystem::path* pairedPath = new std::filesystem::path(path->string());
	pairedPath->replace_extension("");
	pairedPath->replace_filename(pairedPath->filename().string() + nameExtension + fileExtension);

	std::cout << "Paired File Path: " << pairedPath->string() << "\n";

	return pairedPath;
}

std::ifstream* Tester::getPairedFile(std::filesystem::path* path) {
	return new std::ifstream(*getPairedFilePath(path));
}

std::ifstream* Tester::getPairedFile(std::filesystem::path* path, std::string nameExtension, std::string fileExtension) {
	return new std::ifstream(*getPairedFilePath(path, nameExtension, fileExtension));
}

std::vector<std::string*>* Tester::getPairedFileLines(std::filesystem::path* path) {
	return getPairedFileLines(path, defaultPairedNameExtension, defaultPairedFileExtension);
}

std::vector<std::string*>* Tester::getPairedFileLines(std::filesystem::path* path, std::string nameExtension, std::string fileExtension) {

	std::ifstream* file = getPairedFile(path, nameExtension, fileExtension);
	std::string tempLine;
	std::vector<std::string*>* lines = new std::vector<std::string*>();

	while (std::getline(*file, tempLine)) {
		lines->push_back(new std::string(tempLine));
	}

	delete file;
	return lines;
}

std::filesystem::path* Tester::getNextPath() {

	if (paths->size() == 0) {
		return nullptr;
	}

	paths_mutex->lock();

	std::filesystem::path* path = paths->front();
	paths->pop();

	paths_mutex->unlock();

	return path;
}

//Comparator::Comparator() {
//	keys = new std::vector<std::string*>();
//	types = new std::vector<Type>();
//
//	elements = new std::vector<ComparatorElement*>();
//}
//
//Comparator::~Comparator() {
//
//	delete types;
//
//	for (int i = 0; i < keys->size(); i++) {
//		delete keys->at(i);
//	}
//	delete keys;
//
//	for (int i = 0; i < elements->size(); i++) {
//		delete elements->at(i);
//	}
//	delete elements;
//}
//
//void Comparator::addKey(std::string* key, Type type) {
//	keys->push_back(key);
//	types->push_back(type);
//}
//
//void Comparator::removeKey(std::string* key) {
//
//	int pos = getIndex(key);
//
//	if (pos == -1) {
//		return;
//	}
//
//	keys->erase(keys->begin() += pos);
//	types->erase(types->begin() += pos);
//}
//
//ComparatorElement* Comparator::getNewElement() {
//	ComparatorElement* comparatorElement = new ComparatorElement(this, keys, types);
//	comparatorElement->setName(new std::string(std::to_string(elementCount++)));
//
//	return comparatorElement;
//}
//
//int Comparator::getIndex(std::string* key) {
//	
//	int pos = -1;
//
//	for (int i = 0; i < keys->size(); i++) {
//		if (keys->at(i)->compare(*key)) {
//			pos = i;
//			break;
//		}
//	}
//
//	return pos;
//}
//
//Comparator::Type Comparator::getType(std::string* key) {
//	return types->at(getIndex(key));
//}
//
//std::string Comparator::typeToString(Type type) {
//
//	switch (type) {
//	case Type::STRING:
//		return "STRING";
//	case Type::DOUBLE:
//		return "DOUBLE";
//	case Type::COMPAREE:
//		return "COMPAREE";
//	}
//
//	return "Unable To Get String From Type\n";
//}
//
//ComparatorElement::ComparatorElement(Comparator* comparator, std::vector<std::string*>* keys, std::vector<Comparator::Type>* types) {
//	this->comparator = comparator;
//	this->keys = keys;
//	this->types = types;
//
//	std::vector<std::string*>* string_values = new std::vector<std::string*>();
//	std::vector<double>* double_values = new std::vector<double>();
//	std::vector<Comparee*>* comparee_values = new std::vector<Comparee*>();
//
//	std::string* name = new std::string();
//}
//
//ComparatorElement::~ComparatorElement() {
//	
//	for (int i = 0; i < string_values->size(); i++) {
//		delete string_values->at(i);
//	}
//	delete  string_values;
//
//	delete double_values;
//
//	for (int i = 0; i < comparee_values->size(); i++) {
//		comparee_values->at(i);
//	}
//	delete comparee_values;
//}
//
//void ComparatorElement::addValue(std::string* key, std::string* value) {
//	if (comparator->getType(key) != Comparator::Type::STRING) {
//		std::cout << "Type Mismatch in Comparator!\n\tType of Key: " << comparator->typeToString(types->at(comparator->getIndex(key))) << "\n\tType Given: " << comparator->typeToString(Comparator::Type::STRING);
//		throw new _exception();
//	}
//
//	string_values->push_back(value);
//}
//
//void ComparatorElement::addValue(std::string* key, double value) {
//	if (comparator->getType(key) != Comparator::Type::DOUBLE) {
//		std::cout << "Type Mismatch in Comparator!\n\tType of Key: " << comparator->typeToString(types->at(comparator->getIndex(key))) << "\n\tType Given: " << comparator->typeToString(Comparator::Type::DOUBLE);
//		throw new _exception();
//	}
//
//	double_values->push_back(value);
//}
//
//void ComparatorElement::addValue(std::string* key, Comparee* value) {
//	if (comparator->getType(key) != Comparator::Type::COMPAREE) {
//		std::cout << "Type Mismatch in Comparator!\n\tType of Key: " << comparator->typeToString(types->at(comparator->getIndex(key))) << "\n\tType Given: " << comparator->typeToString(Comparator::Type::COMPAREE);
//		throw new _exception();
//	}
//
//	comparee_values->push_back(value);
//}
//
//std::string* ComparatorElement::getName() {
//	return name;
//}
//
//void ComparatorElement::setName(std::string* name) {
//	delete name;
//	this->name = name;
//}
