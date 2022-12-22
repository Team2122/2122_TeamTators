package org.teamtators.logging.vcd;

import java.time.LocalDateTime;
//import java.lang.System;
import java.util.List;
import java.util.ArrayList;
import org.teamtators.logging.FileBuffer;
import org.teamtators.Util.SimFriendlyFileBuffer;
import org.teamtators.logging.vcd.vars.VCDVar;

import frc.robot.constants.LoggingConstants;

public class VCD {
    /** A class for sampling and generating vcd files from live data */

    // VCD Variables
    
    private SimFriendlyFileBuffer buffer;
    //private float version = 1.0;
    //private String tscale = "1ms";

    // Sampling Variables

    private List<VCDScope> vcd_scopes;

    //private boolean configured = false; // configuration complete
    private boolean locked     = false; // header dumped
    private boolean dumping    = false; // dumping
    private long    last_time  = -1;
    //private long    initial_time = 0;

    // Constructor

    public VCD() {
        vcd_scopes = new ArrayList<VCDScope>();
        //initial_time = System.currentTimeMillis();
    }

    // API

    public void configure(String vcdfile, int buffsize) {
        System.out.println("configuring vcd");
        buffer  = new SimFriendlyFileBuffer(vcdfile, buffsize, LoggingConstants.simMode);
        locked  = false;
        dumping = false;
        last_time = -1;
    }

    public void registerScope(VCDScope scope) {
        if (locked == true) return; // TODO warning
        vcd_scopes.add(scope);
    }

    /** Locks the vcd, and begins the dumping process.
      */
    public void dumpVars(long time) {
        System.out.println("dumpvars");
        if (locked == false) {
            System.out.println("locked was false -> true");
            locked = true;
            //initial_time = System.currentTimeMillis();

            writeHeader();
            writeTime(time);
            buffer.write("$dumpvars $end\n");
        }
    }

    /** Enables vcd variable dumping: ie sampled data is now valid.
      */
    public void dumpOn(long time) {
        if (locked == false) return; // TODO warning
        if (dumping == false) {
            dumping = true;
            writeTime(time);
            buffer.write("$dumpon $end\n");
        }
    }

    /** Disables vcd variable dumping: ie sampled data is now invalid.
      * Optimization: disallows sampling data after being called until dumpOn()
      */
    public void dumpOff(long time) {
        dumping = false;
        writeTime(time);
        buffer.write("$dumpoff $end\n");
        buffer.flush();
    }

    /** Sample all registered scopes if dumping has started*/
    public void sample(long time) {
        if (locked == false) return; // TODO warning

        writeTime(time); 

        for (VCDScope scope : vcd_scopes) {
            scope.sampleVCD(buffer);
        }
        buffer.write("\n");
    }

    /** Sample a single scope */
    public void sampleScope(VCDScope scope, long time) {
        if (locked == false) return; // TODO warning

        writeTime(time); 

        scope.sampleVCD(buffer);
    }

    /** Sample a single scope */
    public void sampleVar(VCDVar vvar, long time) {
        if (locked == false) return; // TODO warning

        if (vvar.hasSample()) {
            writeTime(time); 
            buffer.write(vvar.getSample());
        }
    }

    // VCD-H Definition

    private void writeTime(long time) {
        if (time == last_time) return;
        buffer.write("#");
        buffer.write(time);
        buffer.write(" ");
        last_time = time;
    }

    private void writeHeader() {
        buffer.write("$date ");
        buffer.write(LocalDateTime.now().toString());
        buffer.write(" $end\n$version Robot VCDGen v1.0 $end\n$timescale 1ms $end\n");

        for (VCDScope scope : vcd_scopes) {
            scope.writeVCDConfig(buffer);
        }
    }

}

/* Usage 

   register_module("thisModule")
   foreach child: children.register()
   register_float("myFloat", getFloat)
   register_bool("myBool"  , getFloat)
   register_enum("myEnum"  , Enum, getFloat)
   register_wire("myFloat" , 8, getFloat)
   register_int("myInt"    , getFloat)

   sample_start("thisModule")
   sample_int("name", ""

   logger.sample("module")
*/
