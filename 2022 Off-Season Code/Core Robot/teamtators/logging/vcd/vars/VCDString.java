package org.teamtators.logging.vcd.vars;

import java.util.function.Supplier;

public class VCDString extends VCDVar {

    private Supplier<String> sampler;
    private String lastval;
    private int chars;

    public VCDString(String _name, Supplier<String> sampler, int _chars) {
        super(_name, "reg", _chars * 8, _chars + 2); //s{chars} !
        this.chars = _chars;
        this.sampler = sampler;
    }

    public boolean hasSample() {
        String value = sampler.get();
        if (value != lastval) {
            lastval = value;
            return true;
        }
        return false;
    }

    public String getSample() {
        StringBuilder b = getBuilder();
        b.append("s");
        if (lastval.length() > chars) {
            b.append(lastval.substring(0, chars));
        } else {
            b.append(lastval);
        }
        b.append(" ");
        b.append(identifier);
        return b.toString();
    }

}

