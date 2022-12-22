#!/usr/bin/env python
import smbus 
from gpiozero import LED

# Note: Make sure to enable i2c 

class TatorVisionHAT:
    def __init__(self):
        self.LEDpins = [17, 27, 22, 23]  # GPIO pins
        self.LEDs = []
        for i in range(len(self.LEDpins)):
            self.LEDs.append(LED(self.LEDpins[i]))

        self.dac = MCP4725()
        
    def set_brightness(self, brightness, persist=False):
        self.dac.set_brightness(brightness, persist)

    def get_brightness(self):
        return self.dac.get_brightness()
    
    def LEDon(self, ledIndex):
        self.LEDs[ledIndex].on()
        
    def LEDoff(self, ledIndex):
        self.LEDs[ledIndex].off()

    def allLEDoff(self, ledIndex):
        for led in self.LEDs:
            led.off()
    
    def LED0on(self):
        self.LEDon(0)
    
    def LED0off(self):
        self.LEDoff(0)
        
    def LED1on(self):
        self.LEDon(1)
    
    def LED1off(self):
        self.LEDoff(1)
        
    
class MCP4725:
    """Base functionality for MCP4725 digital to analog converter.
       The DAC is used to set the LED brightness as inputs to the LED drivers"""

    def __init__(self, address=0x66, bus=1):
        """Create an instance of the MCP4725 DAC."""
        self.i2cbus = smbus.SMBus(bus)
        self.i2cAddress = address
        # Register Definitions
        self.writedac = 0x40
        self.writedaceeprom = 0x60

    def set_voltage(self, value, persist=False):
        """Set the output voltage to specified value.  Value is a 12-bit number
        (0-4095) that is used to calculate the output voltage from:
          Vout =  (VDD*value)/4096
        I.e. the output voltage is the VDD reference scaled by value/4096.
        If persist is true it will save the voltage value in EEPROM so it
        continues after reset (default is false, no persistence).
        """
        # Clamp value to an unsigned 12-bit value.
        if value > 4095:
            value = 4095
        if value < 0:
            value = 0
        
        # Generate the register bytes and send them.
        # See datasheet figure 6-2:
        #   https://www.adafruit.com/datasheets/mcp4725.pdf 
        reg_data = [(value >> 4) & 0xFF, (value << 4) & 0xFF]
        if persist:
            self.i2cbus.write_i2c_block_data(self.i2cAddress, self.writedaceeprom, reg_data)
        else:
            self.i2cbus.write_i2c_block_data(self.i2cAddress, self.writedac, reg_data)

    def get_voltage(self):
        """ Read the output voltage that the LED bar is currently configured to.       
            to specified value.0
               value = (Vout*4096)/VDD
        """
        reg_data = self.i2cbus.read_i2c_block_data( self.i2cAddress, self.writedac, 4 )
        
        # print( reg_data )
        
        value = ( reg_data[1] << 4 ) + ( reg_data[2] >> 4 )        
        return_value = ( value - 875 ) / ( 1800 - 875 )
	
        # print( return_value )       
        return round( return_value, 2 )
    
    def mapvalue(self, value, istart, istop, ostart, ostop):
        return int(ostart + (ostop - ostart) * ((value - istart) / (istop - istart)))
    
    def set_brightness(self, brightness, persist=False):
        """
         Brightness value between 0 and 1
         MP2410A uses 0.7V-1.44V on EN/DIM pin for brighness control. Less than 0.7 = off, more than 1.44 = Full on
         DAC is supplied by 3.3V and 12 bits (4095 = 3.3V)
         Therefore we want to limit the range between 875 and 1800
        """
        if brightness < 0:
            brighness = 0
        if brightness > 1:
            brighness = 1
            
        dacVal = self.mapvalue(brightness, 0, 1, 875, 1800 )
	
        #print(dacVal)
        self.set_voltage(dacVal, persist)
        
    def get_brightness( self ):
        return self.get_voltage( )
            

###### Quick test code, recommend rewriting this 
#tatorhat = TatorVisionHAT()
#tatorhat.LED0on()
#brightness = 0
#while (brightness <= 1):
#    tatorhat.set_brightness(brightness)
#    input("Press enter to increment")
#    brightness +=0.1
    
# After you have found the brightness level that you want to use, set it as default with persist=true
# This will write that value to the DAC's non-volatile eeprom and on power-up will use that value. Example:
# tatorhat.set_brightness(0.7, True) 

#tatorhat.LED0off()



