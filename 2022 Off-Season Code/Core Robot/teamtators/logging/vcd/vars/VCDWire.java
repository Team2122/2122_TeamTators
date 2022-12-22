package org.teamtators.logging.vcd.vars;

import java.util.function.Supplier;
import org.teamtators.logging.FileBuffer;
import org.teamtators.Util.SimFriendlyFileBuffer;

public class VCDWire extends VCDVar {

    private Supplier<Long> sampler;
    private long lastval;
    private long mask;

    public VCDWire(String _name, Supplier<Long> sampler, int width) {
        super(_name, "wire", width, 3 + width/4); //d{-}{19} !
        this.mask = (1 << width) - 1;
        this.sampler = sampler;
    }

    public boolean hasSample() {
        long value = sampler.get() & mask;
        if (value != lastval) {
            lastval = value;
            return true;
        }
        return false;
    }

    public String getSample() {
        StringBuilder b = getBuilder();
        b.append("h");
        b.append(Long.toHexString(lastval));
        b.append(" ");
        b.append(identifier);
        return b.toString();
        //return "d" + String.valueOf(lastval) + " " + identifier;
    }

    public void appendSample(SimFriendlyFileBuffer b) {
        b.write("h");
        b.write(Long.toHexString(lastval));
        b.write(" ");
        b.write(identifier);
    }

}


