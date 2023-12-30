package frc.robot.constants;

public class WristConstants {
    // public static final double kGoingDownOutput = 0.2;
    // public static final double kGoingUpOutput = -0.2;

    public static final double 
    kWristHoldSpeed = 0.20;

    public static final int kWristMotorID = 39;

    public static final double kDesiredPositionErrorDegrees =  4; // Made this value up for now

    public static final double kTransportDegrees = 36;

    public static final double kDefaultP = .125;

    public enum WristPositions {
        TRANSPORT(kTransportDegrees + 14, -0.01,.025*.7), // -35
        PLACE_HIGH_CONE(kTransportDegrees+163, -0.05,.25*.8),
        PLACE_HIGH_CUBE(kTransportDegrees+136, -0.05,.25*.8),
        PLACE_MID_CONE(kTransportDegrees+175, -0.05,.25*.8),
        PLACE_MID_CUBE(kTransportDegrees+170, -0.05,.25*.8),
        PLACE_LOW_CONE(kTransportDegrees+75, -0.03,.0625*.8),
        PLACE_LOW_CUBE(kTransportDegrees+75, -0.03,.0625*.8), 
        //PLACE_LOW_CONE(kTransportDegrees+118.7214, -0.05),
        //PLACE_LOW_CUBE(kTransportDegrees+118.7214, -0.05),
        SHELF_PICK(kTransportDegrees+ 175, 0.0,.125*.8),
        FLOOR_PICK(kTransportDegrees+125.5, 0.0, .1*.8);
        // FLOOR_PICK(kTransportDegrees+121.8, -0.0);



        double degrees;
        double holdPower;
        double p;

        WristPositions(double degrees, double holdPower, double p) {
            this.degrees = degrees;
            this.holdPower = holdPower;
            this.p = p;
        }

        WristPositions(double degrees, double holdPower) {
            this.degrees = degrees;
            this.holdPower = holdPower;
            this.p = kDefaultP;
        }

        public double getDegrees() {
            return degrees;
        }

        public double getHoldPower() {
            return holdPower;
        }

        public double getP() {
            return p;
        }
    }
}
