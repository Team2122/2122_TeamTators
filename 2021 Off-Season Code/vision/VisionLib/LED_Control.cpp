#include "LED_Control.h"
#include <iostream>
#include <stdio.h>

// Function Definition
double parse_output( char* );

#define BUFSIZE 4096

#ifdef __linux__
	#define _popen popen
	#define _pclose pclose
#endif

void LED_Turn_ON()
{
	char cmd[100];
	std::cout << "Turning LED Bar On\n";
	sprintf(cmd, "python3 /home/pi/LED_Bar/LED_bar_On.py");
	system(cmd);
}

void LED_Turn_OFF()
{
	char cmd[100];
	std::cout << "Turning LED Bar Off\n";
	sprintf(cmd, "python3 /home/pi/LED_Bar/LED_bar_Off.py");
	system(cmd);
}

void LED_get_brightness()
{
	char cmd[100];
	std::cout << "Getting the LED Bar Brightness and setting NetworkTable\n";
	sprintf(cmd, "python3 /home/pi/LED_Bar/LED_get_brightness.py");
	system(cmd);
}

void LED_set_brightness(double brightness)
{
	char cmd[100];
	std::cout << "Setting LED Brightness\n";
	sprintf(cmd, "python3 /home/pi/LED_Bar/LED_set_brightness.py %g", brightness );
	system(cmd);
}

double LED_test_command_line( )
{
	char cmd[100];
	sprintf(cmd, "LED_test_commmand.py");
	double result = parse_output(cmd);

	return result;
}

double parse_output( char* cmd_str ) 
{
	// 
	// sprintf(cmd_str, "dir *.py");

	double result = 0;
	char buf[BUFSIZE];
	FILE* fp;

	// Open the stream
	if ((fp = _popen(cmd_str, "r")) == NULL) 
	{
		printf("Error opening pipe!\n");
		return -1;
	}

	// Parse the stream
	while (fgets(buf, BUFSIZE, fp) != NULL) 
	{
		// Do whatever you want here...
		printf("OUTPUT: %s", buf);

		// Convert the string to 
		result = strtod( buf, NULL );
	}

	// Close the stream
	_pclose(fp);

	return result;
}
