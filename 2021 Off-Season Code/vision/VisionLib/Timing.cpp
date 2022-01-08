#include "Timing.hpp"
#include <chrono>
#include <ctime>
#include <string>
using namespace std;
void get_current_time(char* source) {
	chrono::system_clock::time_point current_time = std::chrono::system_clock::now();
	time_t c_time = chrono::system_clock::to_time_t(current_time);
	source = ctime(&c_time);
	printf(source);
}
/*
START - start the timer
STOP - stop timer, returns microseconds as an int
POLL - return microseconds as an int
*/
int timeit(TimerFunc_t stop_start) {
	static bool started = false;
	static chrono::system_clock::time_point start_time;
	if (stop_start == START) {
		// start the timer
		// printf("starting timer...");
		started = true;
		start_time = chrono::system_clock::now();
		return -1;
	}
	else {
		auto current = chrono::system_clock::now();
		long us = (long) chrono::duration_cast<chrono::microseconds>(current - start_time).count();
		if (started == false) {
			printf("timer hasn't been started!\n");
			return -1;
		}
		if (stop_start == STOP)
			started = false;
		return us;
	}
}