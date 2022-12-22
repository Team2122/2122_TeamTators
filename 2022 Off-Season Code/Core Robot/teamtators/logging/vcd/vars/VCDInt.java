package org.teamtators.logging.vcd.vars;

import java.util.function.Supplier;

public class VCDInt extends VCDVar {

    private Supplier<Integer> sampler;
    private int lastval;

    public VCDInt(String _name, Supplier<Integer> sampler) {
        super(_name, "wire", 32, 13); //d{-}{10} !
        this.sampler = sampler;
    }

    public boolean hasSample() {
        int value = sampler.get();
        if (value != lastval) {
            lastval = value;
            return true;
        }
        return false;
    }

    public String getSample() {
        StringBuilder b = getBuilder();
        b.append("d");
        b.append(lastval);
        b.append(" ");
        b.append(identifier);
        return b.toString();
    }

}


