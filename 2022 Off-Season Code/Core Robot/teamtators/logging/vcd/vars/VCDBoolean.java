package org.teamtators.logging.vcd.vars;

import java.util.function.Supplier;

import org.teamtators.logging.FileBuffer;
import org.teamtators.Util.SimFriendlyFileBuffer;

public class VCDBoolean extends VCDVar {

    private Supplier<Boolean> sampler;
    private boolean lastval;

    public VCDBoolean(String _name, Supplier<Boolean> sampler) {
        super(_name, "wire", 1, 1); // 0!
        this.sampler = sampler;
    }

    public boolean hasSample() {
        boolean value = sampler.get();
        if (value != lastval) {
            lastval = value;
            return true;
        }
        return false;
    }

    public String getSample() {
        if (lastval)
            return "1" + identifier;
        return "0" + identifier;
    }

    public void appendSample(SimFriendlyFileBuffer b) {
        if (lastval)
            b.write("1" + identifier);
        b.write("0" + identifier);
    }

}


