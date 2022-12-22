package org.teamtators.logging.vcd;

import java.util.List;
import java.util.ArrayList;
import java.util.function.Supplier;
import org.teamtators.logging.vcd.vars.*;
import org.teamtators.logging.FileBuffer;
import org.teamtators.Util.SimFriendlyFileBuffer;

/* How to use:
scope = new VCDScope(name, type);
scope.register*

Each scope should be either
    - child top another scope : parentScope.registerScope()
    - bound to a VCD          : VCD.registerScope()
*/

public class VCDScope {

    // -------------- PRIVATE API ----------- //

    private List<VCDVar>   vcd_vars;
    private List<VCDScope> vcd_scopes;
    private String         vcd_type;
    private String         vcd_name;

    public VCDScope(String name, String type) { 
        vcd_vars   = new ArrayList<VCDVar>();
        vcd_scopes = new ArrayList<VCDScope>();
        vcd_name   = name;
        vcd_type   = type;
    }

    public VCDScope(String name) {
        this(name, "module");
    }

    public final void writeVCDConfig(SimFriendlyFileBuffer buffer) {
        buffer.write("$scope ");
        buffer.write(vcd_type);
        buffer.write(" ");
        buffer.write(vcd_name);
        buffer.write(" $end\n");
        
        for (VCDVar vvar : vcd_vars)
            buffer.write(vvar.getDefString());

        for (VCDScope scope : vcd_scopes)
            scope.writeVCDConfig(buffer);

        buffer.write("$upscope $end\n");
    }

    public final void sampleVCD(SimFriendlyFileBuffer buffer) {
        for (VCDVar vvar : vcd_vars)
            if (vvar.hasSample())
                buffer.write(vvar.getSample());

        for (VCDScope scope : vcd_scopes)
            scope.sampleVCD(buffer);
    }

    // -------------- PUBLIC API ------------ //

    /** Function to override. 
      * Should register all VCD variables and subscopes
      */
    //public abstract void register();

    // Do NOT Override:

    public final void registerWire(String name, int width, Supplier<Long> sampler) {
        vcd_vars.add(new VCDWire(name, sampler, width));
    }
    public final void registerBoolean(String name, Supplier<Boolean> sampler) {
        vcd_vars.add(new VCDBoolean(name, sampler));
    }
    public final void registerInt(String name, Supplier<Integer> sampler) {
        vcd_vars.add(new VCDInt(name, sampler));
    }
    public final void registerLong(String name, Supplier<Long> sampler) {
        //vcd_vars.add(new VCDLong(name, sampler));
        registerWire(name, 64, sampler);
    }
    public final void registerFloat(String name, Supplier<Float> sampler) {
        vcd_vars.add(new VCDFloat(name, sampler));
    }
    public final void registerDouble(String name, Supplier<Double> sampler) {
        vcd_vars.add(new VCDDouble(name, sampler));
    }
    public final void registerString(String name, int chars, Supplier<String> sampler) {
        vcd_vars.add(new VCDString(name, sampler, chars));
    }
    public final void registerScope(VCDScope scope, String name, String type) {
        vcd_scopes.add(scope);
    }

    public VCDVar getVCDVar(int index) {
        return vcd_vars.get(index);
    }

}

