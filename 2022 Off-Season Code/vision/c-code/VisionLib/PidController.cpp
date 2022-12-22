#include "PidController.h"

PidController::PidController(double k_p, double k_i, double k_d, double (*in_func)(void), void (*out_func)(double)) {
	input_func = in_func;
	output_func = out_func;
	last_error = 0;
	p = k_p;
	i = k_i;
	d = k_d;
	goal = 0;
}

PidController::~PidController() {

}
double PidController::get_error() {
	return goal - input_func();
}
void PidController::update(double delta) {
	double error = get_error();
	i_error += error;
	d_error = 0.2 * (d_error) + 0.8 * (error - last_error) / delta; // 20% smoothing
	double change = p * error - d * d_error + i * i_error;
	output_func(change);
}
double PidController::set_goal(double g) {
	goal = g;
	return 0;
}