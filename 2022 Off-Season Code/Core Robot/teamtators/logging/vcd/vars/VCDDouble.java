package org.teamtators.logging.vcd.vars;

import java.util.function.Supplier;

public class VCDDouble extends VCDVar {

    private Supplier<Double> sampler;
    private double lastval;

    public VCDDouble(String _name, Supplier<Double> sampler) {
        super(_name, "real", 64, 26); //r{-}{16}e{-}{5} ! 
        this.sampler = sampler;
    }

    public boolean hasSample() {
        double value = sampler.get();
        if (value != lastval) {
            lastval = value;
            return true;
        }
        return false;
    }

    public String getSample() {
        StringBuilder b = getBuilder(); 
        b.append("r");
        b.append(lastval);
        b.append(" ");
        b.append(identifier);
        return b.toString();
        //return "r" + lastval + " " + identifier;
    }

}

