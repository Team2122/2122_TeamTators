package org.teamtators.logging.vcd.vars;

import java.util.function.Supplier;

public class VCDFloat extends VCDVar {

    private Supplier<Float> sampler;
    private float lastval;

    public VCDFloat(String _name, Supplier<Float> sampler) {
        super(_name, "real", 32, 15); //r{-}{7}e{-}{3} !
        this.sampler = sampler;
    }

    public boolean hasSample() {
        float value = sampler.get();
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


