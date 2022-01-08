//
// Created by eddie on 6/25/2021.
//

#ifndef TATOREYES2_TESTER_HPP
#define TATOREYES2_TESTER_HPP

#include "ExecutorService.hpp"
#include <string>
#include <queue>
#include <filesystem>
#include <fstream>

class Comparator;
class ComparatorElement;
class Comparee;

class Tester {
public:

    Tester(std::filesystem::path* pathToDirectory, std::function<int(std::filesystem::path*)>* function);
    Tester(std::filesystem::path* pathToDirectory, std::function<int(std::filesystem::path*)>* function, ExecutorService* executorService);

    Tester(std::filesystem::path* pathToDirectory, std::function<int(std::filesystem::path*, std::string* message)>* function);
    Tester(std::filesystem::path* pathToDirectory, std::function<int(std::filesystem::path*, std::string* messages)>* function, ExecutorService* executorService);

    ~Tester();

    enum class FunctionType {

        NORMAL,
        STRING

    };

    void start();
    void setFiletype(std::string* fileExtension);
    void clearFiletype();
    void setPathQueue();
    bool isFinished();
    void safePrint(std::string message);
    std::filesystem::path* getPairedFilePath(std::filesystem::path* path);
    std::filesystem::path* getPairedFilePath(std::filesystem::path* path, std::string nameExtension, std::string fileExtension);
    std::ifstream* getPairedFile(std::filesystem::path* path);
    std::ifstream* getPairedFile(std::filesystem::path* path, std::string nameExtension, std::string fileExtension);
    std::vector<std::string*>* getPairedFileLines(std::filesystem::path* path);
    std::vector<std::string*>* getPairedFileLines(std::filesystem::path* path, std::string nameExtension, std::string fileExtension);

    std::filesystem::path* getNextPath();

private:

    bool certainFiles = false;
    std::string* fileExtension;

    ExecutorService* executorService;
    std::filesystem::path* pathToDirectory;

    std::function<int(std::filesystem::path*)>* function_normal;
    std::function<int(std::filesystem::path*, std::string*)>* function_string;

    std::queue<std::filesystem::path*>* paths;
    std::mutex* paths_mutex;

    FunctionType functionType;

    void create();
    void addAllTasks();
    void createDataFile(std::string* data, std::filesystem::path* path);
    void createDataFile(std::string* data);

    std::string defaultPairedFileExtension = ".txt";
    std::string defaultPairedNameExtension = "";

    std::string dataFileName = "_metadata";

    std::function<int()> lambdaRun_normal = [this]() {

        paths_mutex->lock();

        std::filesystem::path* path = paths->front();
        paths->pop();

        paths_mutex->unlock();

        return (*function_normal)(path);
    };

    std::function<int()> lambdaRun_string = [this]() {

        paths_mutex->lock();

        std::filesystem::path* path = paths->front();
        paths->pop();

        paths_mutex->unlock();

        std::string* string = new std::string("");

        int return_value = (*function_string)(path, string);

        createDataFile(string, path);

        return return_value;
    };


};

//class Comparator {
//
//public:
//
//    enum class Type {
//        COMPAREE,
//        DOUBLE,
//        STRING
//    };
//
//    Comparator();
//    ~Comparator();
//
//    void addKey(std::string* key, Type type);
//    void removeKey(std::string* key);
//
//    ComparatorElement* getNewElement();
//
//    int getIndex(std::string* key);
//    Type getType(std::string* key);
//
//    std::string typeToString(Type type);
//
//private:
//
//    std::vector<std::string*>* keys;
//    std::vector<Type>* types;
//
//    int elementCount = 0;
//    std::vector<ComparatorElement*>* elements;
//
//};
//
//class ComparatorElement {
//
//public:
//
//    ComparatorElement(Comparator* comparator, std::vector<std::string*>* keys, std::vector<Comparator::Type>* types);
//    ~ComparatorElement();
//
//    void addValue(std::string* key, std::string* value);
//    void addValue(std::string* key, double value);
//    void addValue(std::string* key, Comparee* value);
//
//    std::string* getName();
//    void setName(std::string* string);
//
//private:
//
//    Comparator* comparator;
//
//    // MUST BE LINKED AT INEXES!
//    std::vector<std::string*>* keys;
//    std::vector<Comparator::Type>* types;
//    std::vector<int>* indexes;
//
//    std::vector<std::string*>* string_values;
//    std::vector<double>* double_values;
//    std::vector<Comparee*>* comparee_values;
//
//    std::string* name;
//
//};
//
//class Comparee {
//
//    Comparee();
//
//    virtual bool operator==(const Comparee comp);
//
//    virtual std::string* toString();
//
//private:
//
//    static int IDCounter;
//    const int ID;
//
//};

#endif // TATOREYES2_TESTER_HPP
