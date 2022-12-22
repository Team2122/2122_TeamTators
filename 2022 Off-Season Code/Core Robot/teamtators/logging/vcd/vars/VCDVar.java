package org.teamtators.logging.vcd.vars;

import org.teamtators.logging.FileBuffer;
import org.teamtators.Util.SimFriendlyFileBuffer;

/** VCDVar --- The base abstract class defining a sampled vcd variable. 
  * @author Kareem Ahmad
  */

public abstract class VCDVar {

    private static int id_index = 0;

    // reasonable to assume id is less than 3 chars: 125000 vars
    // possibly reasonable to assume id is less than 2 chars: 2500 vars
    // running on second assumption. must increment all 

    private static String nextId() {
        ++id_index;
        // supported characters are ( I think 33..126 )
        StringBuilder idGen = new StringBuilder();
        int id_building = id_index;
        int id_range = 127 - 33;
        int id_start = 33;
        int id_char = 0;
        while (id_building != 0) {
            id_char = (id_building % id_range) + id_start;
            id_building = id_building / id_range;
            idGen.append((char) id_char);
        }
        return idGen.toString();
    }

    protected String identifier;
    private String type;
    private String name;
    private int width; // width of data in bits
    private int dumpw; // maximum size of dumped string 

    /** creates a VCDVar instance 
      * @param _name  User-visible name of the variable. (To be displayed in waveforms)
      * @param _type  The vcd-type of the variable. See the IEEE 1364 Verilog LRM (Section 18.2.3.7, pg 334) for all values. The most common are {real, wire, reg}.
      * @param _width The maximum width of the variable data in bits. 
      * @param _dumpw Maximum width of the string to be created by getSample() ignoring size of the identifier and the following space.
      */
    public VCDVar(String _name, String _type, int _width, int _dumpw) {
        identifier = nextId() + " ";
        name = _name;
        type = _type;
        width = _width;
        if (id_index < 50) {
            dumpw = _dumpw + 2;
        } else if (id_index < 2500) {
            dumpw = _dumpw + 3;
        } else {
            dumpw = _dumpw + 4;
        }
    }

    // Getters

    /** Get the vcd id of this variable, to be used in getSample().
      * Do NOT override this function.
      * @return identifier
      */
    final public String getId() {
        return identifier;
    }

    final public int getDumpW() {
        return dumpw;
    }

    final public String getDefString() {
        // $var (5) type (? + 1) width(<=3 + 1) id (? + 1) name (?) $end (5)
        int len = 16 + type.length() + name.length() + identifier.length();
        StringBuilder b = new StringBuilder(len);
        b.append("$var ");
        b.append(type);
        b.append(" ");
        b.append(width);
        b.append(" ");
        b.append(identifier);
        b.append(name);
        b.append(" $end\n");
        return b.toString();
    }

    /** Creates and returns a StringBuilder of the correct size for getSample().
      * You may use this for efficient string concatenation.
      * Do NOT override this function.
      * @return StringBuilder
      */
    final public StringBuilder getBuilder() {
        return new StringBuilder(dumpw);
    }

    /** Checks if the variable has a new sample available (data has changed since last sample) 
      * @return true if variable has changed, false otherwise
      */
    abstract public boolean hasSample();

    /** Gets the compressed vcd string form of the variable's latest value.
      * For purposes of optimization, you may assume that this is called after hasSample() 
      * where hasSample() returned true. 
      * @return the compressed vcd String form of the variable's latest value
      */
    abstract public String getSample();

    /** Can be overridden for efficiency later 
      * TODO  @Kareem update children for efficiency
      */
    public void appendSample(SimFriendlyFileBuffer b) {
        b.write(getSample());
    };

}


