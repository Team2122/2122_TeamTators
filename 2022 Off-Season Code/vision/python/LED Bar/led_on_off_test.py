#import libraries
import RPi.GPIO as GPIO
import time

#GPIO Basic initialization
GPIO.setmode(GPIO.BCM)
GPIO.setwarnings(False)

#Use a variable for the Pin to use
#If you followed my pictures, it's port 7 => BCM 4

led_0 = 17
led_1 = 18

#Initialize your pin
GPIO.setup(led_0,GPIO.OUT)
GPIO.setup(led_1,GPIO.OUT)

#Turn on the LED
print "LED on"
GPIO.output(led_0,1)
GPIO.output(led_1,1)
#Wait 5s
time.sleep(5)

#Turn off the LED
print "LED off"
GPIO.output(led_0,0)
GPIO.output(led_1,0)
