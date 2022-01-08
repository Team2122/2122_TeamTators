package org.teamtators.common.math;

/**
 * @author Abby Chen
 * Represents the cubic between each two points
 */
public class Spline {
    private Point startPoint;
    private Point endPoint;

    // The spline form is a + b(x-h) + c(x-h)^2 + d(x-h)^3
    //h = startPoint.x and a = startPoint.y
    private Coefficient a = new Coefficient();
    private Coefficient b = new Coefficient();
    private Coefficient c = new Coefficient();
    private Coefficient d = new Coefficient();

    //If the spline is not surrounded by its fellow splines on both s(pl)ides
    private boolean isEdge;

    //Constructor
    public Spline(Point startPoint, Point endPoint) {
        this.startPoint = startPoint;
        a.setValue(startPoint.getY());
        this.endPoint = endPoint;
    }

    /**
     * @param edge Is it an edge spline???
     */
    public void setIsEdge(boolean edge, boolean last) {
        isEdge = edge;
        if (edge) {
            if(last) {
                endPoint.setSlopeSlope(0.0);
            } else {
                startPoint.setSlopeSlope(0.0);
                c.setValue(0);
            }
        }
    }

    /**
     * @return If is one of the splines on the ends
     */
    public boolean isEdge() {
        return isEdge;
    }

    public double derive(double x) {
        Coefficient[] coes = getCoefficientsFromDerivationAndSubstitution(x - startPoint.getX(), true);

        double derivative = 0;

        derivative += coes[1].getCoefficient() * b.getValue();
        derivative += coes[2].getCoefficient() * c.getValue();
        derivative += coes[3].getCoefficient() * d.getValue();

        return derivative;
    }

    /**
     * Plugs in the endpoint, where x = startpoint.x or endpoint.x
     *
     * @param useStartPoint Determines if x is the startpoint or endpoint
     * @param singleDerive  Determines if we're deriving once (S'(x)) or twice (S"(x))
     * @return The coefficients in front of a, b, c, and d in order
     */
    public Coefficient[] getCoefficientsFromDerivationAndSubstitution(boolean useStartPoint, boolean singleDerive) {
        //The number we get when we do x - h
        double multiplier;

        if (!useStartPoint) {
            multiplier = endPoint.getX() - startPoint.getX();
        } else {
            multiplier = 0;
        }

        return getCoefficientsFromDerivationAndSubstitution(multiplier, singleDerive);
    }

    public Coefficient[] getCoefficientsFromDerivationAndSubstitution(double multiplier, boolean singleDerive) {
        //The coefficients
        Coefficient[] coefficients;

        if (singleDerive) {
            coefficients = derivedCoefficients();
            //Multiplies the "c" value by the multiplier
            coefficients[2].setCoefficient(coefficients[2].getCoefficient() * multiplier);
            //Multiplies the "d" value by the multiplier squared
            coefficients[3].setCoefficient(coefficients[3].getCoefficient() * multiplier * multiplier);
        } else {
            coefficients = doubleDerivedCoefficients();
            //Multiplies the "d" value by the multiplier
            coefficients[3].setCoefficient(coefficients[3].getCoefficient() * multiplier);
        }

        return coefficients;
    }


        /**
         * Takes the derivative of the spline and changes the coefficients accordingly e.g. d(x-h)^3 becomes 3 * d(x-h)^2
         *
         * @return The newly derived coefficients
         */
    private Coefficient[] derivedCoefficients() {
        //DURRRR (a "derived" becomes 0)
        Coefficient aDer = new Coefficient();
        aDer.setCoefficient(0);
        aDer.setValue(a.getValue());

        //b(x-h) becomes b
        Coefficient bDer = new Coefficient();
        bDer.setCoefficient(b.getCoefficient());
        if (b.isSet()) {
            bDer.setValue(b.getValue());
        }

        //c(x-h)^2 becomes 2*c(x-h)
        Coefficient cDer = new Coefficient();
        cDer.setCoefficient(c.getCoefficient() * 2);
        if (c.isSet()) {
            cDer.setValue(c.getValue());
        }

        //d(x-h)^3 becomes 3 * d(x-h)^2
        Coefficient dDer = new Coefficient();
        dDer.setCoefficient(d.getCoefficient() * 3);
        if (d.isSet()) {
            dDer.setValue(d.getValue());
        }

        Coefficient[] coefficients = {aDer, bDer, cDer, dDer};
        return coefficients;
    }

    /**
     * Takes the double derivative (deriving twice) of the spline and changes the coefficients accordingly e.g. d(x-h)^3
     * becomes 3 * d(x-h)^2 which becomes 2 * 3 * d(x-h)
     *
     * @return The newly derived coefficients
     */
    private Coefficient[] doubleDerivedCoefficients() {
        //DURRRR (a "derived" becomes 0)
        Coefficient aDerDer = new Coefficient();
        aDerDer.setCoefficient(0);
        aDerDer.setValue(a.getValue());

        //b derived becomes 0
        Coefficient bDerDer = new Coefficient();
        bDerDer.setCoefficient(0);
        if (b.isSet()) {
            bDerDer.setValue(b.getValue());
        }

        //c(x-h)^2 derived becomes 2 * c (x-h) which becomes 2*c
        Coefficient cDerDer = new Coefficient();
        cDerDer.setCoefficient(c.getCoefficient() * 2);
        if (c.isSet()) {
            cDerDer.setValue(c.getValue());
        }

        //d(x-h)^3 * becomes 3 * d(x-h)^2 which becomes 2 * 3 * d(x-h)
        Coefficient dDerDer = new Coefficient();
        dDerDer.setCoefficient(d.getCoefficient() * 6);
        if (d.isSet()) {
            cDerDer.setValue(d.getValue());
        }

        Coefficient[] coefficients = {aDerDer, bDerDer, cDerDer, dDerDer};
        return coefficients;
    }

    /**
     * Substitutes the endpoint into the original spline form
     *
     * @return the coefficients in front of a, b, c, and d, respectively
     */
    public Coefficient[] substituteEndpointWithoutDeriving() {
        double multiplier = endPoint.getX() - startPoint.getX();

        Coefficient aSub = new Coefficient();
        aSub.setValue(a.getValue());

        Coefficient bSub = new Coefficient();
        bSub.setCoefficient(b.getCoefficient() * multiplier);
        if (b.isSet()) {
            bSub.setValue(b.getValue());
        }

        Coefficient cSub = new Coefficient();
        cSub.setCoefficient(c.getCoefficient() * multiplier * multiplier);
        if (c.isSet()) {
            cSub.setValue(c.getValue());
        }

        Coefficient dSub = new Coefficient();
        dSub.setCoefficient(d.getCoefficient() * multiplier * multiplier * multiplier);
        if (d.isSet()) {
            dSub.setValue(d.getValue());
        }

        Coefficient[] coefficients = {aSub, bSub, cSub, dSub};
        return coefficients;
    }

    /**
     * @return coefficients e,f, g, and i if we put the funciton in the form et^3 + ft^2 + gt +i
     */
    public double[] getCoefficientsWithoutH() {
        double h = startPoint.getX();

        double e = getDValue();
        double f = getCValue() - (3.0 * getDValue() * h);
        double g = (getDValue() * 3.0 * h * h) - (2.0 * getCValue() * h) + getBValue();
        double i = getAValue() - (getBValue() * h) + (getCValue() * h * h) - (getDValue() * h * h * h);

        double[] coesss = {e, f, g, i};

        return coesss;
    }

    public double getAValue() {
        return a.getValue();
    }

    public void setAValue(double value) {
        a.setValue(value);
    }

    public double getBValue() {
        return b.getValue();
    }

    public void setBValue(double value) {
        b.setValue(value);
    }

    public double getCValue() {
        return c.getValue();
    }

    public void setCValue(double value) {
        c.setValue(value);
    }

    public double getDValue() {
        return d.getValue();
    }

    public void setDValue(double value) {
        d.setValue(value);
    }

    public Point getStartPoint() {
        return startPoint;
    }

    public Point getEndPoint() {
        return endPoint;
    }
}
