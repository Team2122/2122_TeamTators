package org.teamtators.bbt8r.staging;

// Class for polynomial error correction
public class PolyErrorCorrection {
    private double coeff;
    private double power;

    // Constructor coeff is a multiplier, power is the power of the polynomial 
    public  PolyErrorCorrection (double coeff, double power){
        this.coeff = coeff;
        this.power = power;
    }
    
    // Main method of getting change, should not be used if polynomial is of power 2 or 3 as it will be slower
    public double getChange(double distance){
        if(power %2 == 0) {
            return (Math.pow(distance, power) * coeff);
        } else {
            return (-Math.pow(distance, power) * coeff);
        }
    }

    // Method of getting change using a quadratic. 
    // Note: if distance < 0 it will return a negative number using the trick of x/|x| giving a line at -1 when x < 0 amd a line at 1 when x > 0 and infinity at 0
    public double getQuadraticChange(double distance){
        return (distance*distance*distance*coeff)/Math.abs(distance);
    }

    // Method of getting change using a quadratic
    public double getCubicChange(double distance){
        return (distance*distance*distance);
    }

    public double getFancyChange(double distance ,double positiveDomain, int power, double coeff){
        if(Math.abs(distance) < positiveDomain) return getChange(distance);    
        if(power %2 == 0) {
            return (Math.pow(distance, power) * coeff);
        } else {
            return (-Math.pow(distance, power) * coeff);
        }
    
    }
}
