package frc.robot.constants;

public class OperatorInterfaceConstants {

    public static final int kDriverControllerPort = 0;
    public static final int kGunnerControllerPort = 1;

    public static double kLowPressureValue = 60;
    public static double kTargetPressure = 90;
    
	// these 'cutoff' values are the point at which the piecewise function
	// used for determining the output generated by the joystick input seperates
	// from a linear function to an exponential function
	// The function will be 0 from (0, 0) to (deadzone, 0)
	// Then linear from (deadzone, 0) to (cutoffX, cutoffY)
	// Then exponential from (cutoffX, cutoffY) to (1, 1)
  
    public static final double maxXDot = 6;
    public static final double maxYDot = 6;
    public static final double maxThetaDot = 6;

    public static final double offset = 0;
    public static final double deadzone = .03;
    public static final double coefficient = 1;

    // The values below are using the new, modified joystick controller algorithm
    // The new algorithm does the following 
    //   zero below the deadzone
    //   linear slope from deadzone to CutOffX
    //   non-linear (depending on exponent) from CutOffX to 1.0
    
    public static final double rotationCutOffX = 0.95;
    public static final double rotationCutOffY = 0.03;
    public static final double rotationExponent = 3;

    //nonfinal because of Shuffleboard control
    public static double translationCutOffX = 0.95;
    public static double translationCutOffY = 0.025;
    public static final double translationExponent = 1;  

}
