#!/usr/bin/env python3
#
# This is a NetworkTables server (eg, the robot or simulator side).
#

import time, sys
import tatorVisionHat as TatorVision
from networktables import NetworkTables

# As a client to connect to a robot
NetworkTables.initialize(server='roborio-2122-frc.local')

# Quick test code, recommend rewriting this 
tatorhat = TatorVision.TatorVisionHAT()
sd = NetworkTables.getTable("visionTable")

LED_state = tatorhat.get_brightness()

print( "\tReceived LED Brightness Value : " + str(LED_state) )
sd.putNumber( "visionBrightness", LED_state )
