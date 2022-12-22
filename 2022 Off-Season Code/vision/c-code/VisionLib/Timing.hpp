#pragma once

#ifndef _TIMER_
#define _TIMER_

typedef enum {STOP, START, POLL} TimerFunc_t;
void get_current_time(char*);
int timeit(TimerFunc_t stop_start);

#endif