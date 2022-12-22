#!/usr/bin/env python3
import tatorVisionHat as TatorVision
import sys

# Print all arguments
# print ('Argument List:', str(sys.argv))

###### Quick test code, recommend rewriting this 
tatorhat = TatorVision.TatorVisionHAT()

# After you have found the brightness level that you want to use, set it as default with persist=true
# This will write that value to the DAC's non-volatile eeprom and on power-up will use that value. Example:

tatorhat.set_brightness( float(sys.argv[1]), True )

