#import libraries
import RPi.GPIO as GPIO

#GPIO Basic initialization
GPIO.setmode(GPIO.BCM)
GPIO.setwarnings(False)

#Use a variable for the Pin to use
led_0 = 17
led_1 = 18

#Set the GPIO pins as writeable 
GPIO.setup(led_0, GPIO.OUT )
GPIO.setup(led_1, GPIO.OUT )

#Turn off the LED
print( "LED off" )
GPIO.output(led_0,0)
GPIO.output(led_1,0)
