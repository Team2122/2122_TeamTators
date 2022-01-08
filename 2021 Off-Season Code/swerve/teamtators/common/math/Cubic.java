package org.teamtators.common.math;

import java.util.ArrayList;

/**
 * Form of ax^3 + bx^2 + cx + d (= 0 if solving)
 */
public class Cubic {
    private double a, b, c, d;
    //private double delta;
    //private double d0;
    //private double d1;
    //private ComplexNumber xi;

    public Cubic(double a, double b, double c, double d) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;

        //d0 = getDiscriminant0();
        //d1 = getDiscriminant1();
        //delta = getDiscriminant();

        //xi = new ComplexNumber(-0.5, 0.5 * Math.cbrt(3.0));
    }

    public double getY(double x) {
        return a * x * x * x + b * x * x + c * x + d;
    }

    /**
     * @return the roots of the cubic
     */
    public ArrayList<Double> solve() {
        double f = ((3.0 * (c / a)) - ((b * b) / (a * a))) / 3.0;

        double g1 = (2.0 * (b * b * b)) / (a * a * a);
        double g2 = (9.0 * b * c) / (a * a);
        double g3 = (27.0 * d) / a;
        double g = (g1 - g2 + g3) / 27.0;

        double h = ((g * g) / 4.0) + ((f * f * f) / 27.0);

        System.out.println(h);

        if(h > 0.0) {
            //System.out.println("Getting One Real Root");
            return getOneRealRoot(f, g, h);
        } else if(Epsilon.isEpsilonZero(f) && Epsilon.isEpsilonZero(g) && Epsilon.isEpsilonZero(h)) {
            //System.out.println("Getting triple root");
            return getTripleRoot();
        } else {
            //System.out.println("Getting Three real roots");
            return getThreeRealRoots(f, g, h);
        }
    }

    private ArrayList<Double> getOneRealRoot(double f, double g, double h) {
        double r = -(g / 2.0) + Math.sqrt(h);
        double s = Math.cbrt(r);
        double t = -(g / 2.0) - Math.sqrt(h);
        double u = Math.cbrt(t);

        double solution = (s + u) - (b / (3.0 * a));

        ArrayList<Double> solutions = new ArrayList<>();
        solutions.add(solution);

        return solutions;
    }

    private ArrayList<Double> getTripleRoot() {
        double solution = -Math.cbrt(d / a);

        ArrayList<Double> solutions = new ArrayList<>();
        solutions.add(solution);

        return solutions;
    }

    private ArrayList<Double> getThreeRealRoots(double f, double g, double h) {
        double i = Math.sqrt(((g * g) / 4.0) - h);
        double j = Math.cbrt(i);
        double k = Math.acos(-(g / (2.0 * i)));
        double l = -j;
        double m = Math.cos(k / 3.0);
        double n = Math.sqrt(3.0) * Math.sin(k / 3.0);
        double p = -(b / (3.0 * a));

        double solution1 = 2.0 * j * Math.cos(k / 3.0) - (b / (3.0 * a));
        double solution2 = l * (m + n) + p;
        double solution3 = l * (m - n) + p;

        ArrayList<Double> solutions = new ArrayList<>();
        solutions.add(solution1);
        solutions.add(solution2);
        solutions.add(solution3);

        return solutions;
    }

    /*
    /**
     * Puts cubic in form of t^3 + pt + qt = 0

    public class Depressed {
        public double p;
        public double q;

        public void getDepressed() {
            setP();
            setQ();
        }

        private void setP() {
            p = ((3.0 * a * c) - (b * b)) / (3.0 * a * a);
        }

        private void setQ() {
            q = ((2 * Math.pow(b, 3.0)) - (9.0 * a * b * c) + (27.0 * a * a * d)) / (27.0 * Math.pow(a, 3.0));
        }
    }
    */

}
