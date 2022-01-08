#pragma once

// A PID controller for no particular reason
// thought it might be helpful for automatic exposure calibration
class PidController {
public:
	PidController(double p, double i, double d, double (*input_func)(void), void (*output_func)(double));
	~PidController();

	void update(double delta);
	double get_error();
	double set_goal(double goal);
private:
	double goal;
	double p, i, d;
	double i_error;
	double d_error;
	double last_error;
	double (*input_func)(void);
	void (*output_func)(double);
};