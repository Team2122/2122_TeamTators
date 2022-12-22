#import libraries
import RPi.GPIO as GPIO

#GPIO Basic initialization
GPIO.setmode(GPIO.BCM)
GPIO.setwarnings(False)

#Use a variable for the Pin to use
led_0 = 17
led_1 = 18

#Initialize your pin
GPIO.setup(led_0,GPIO.OUT)
GPIO.setup(led_1,GPIO.OUT)

#Turn on the LED
print( "LED on" )
GPIO.output(led_0,1)
GPIO.output(led_1,1)

