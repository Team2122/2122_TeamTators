package org.teamtators.logging;

import java.io.*;

import org.teamtators.Util.SimFriendlyFileWriter;

import frc.robot.constants.LoggingConstants;

public class FileBuffer {
    /** A class for buffering writes to files */

    //String[] buffer;
    protected StringBuilder buffer;
    //int buffer_limit; // maximum size of the buffer in characters
    //int buffer_index; // index of the next item to write, also current size
    String filepath;

    public FileBuffer(String filepath, int buffer_limit) {
        this.filepath = filepath;
        buffer = new StringBuilder(buffer_limit);
    }

    /** Queues data to be written to buffer, and flushes buffer if it fills.
     * @return true if buffer was flushed, false otherwise
     */
    public boolean write(String data) {
        if (buffer.length() + data.length() > buffer.capacity()) {
            flush();
        }

        buffer.append(data);
        return buffer.length() == data.length();
    }

    public void write(int    data) { write(String.valueOf(data)); }
    public void write(long   data) { write(String.valueOf(data)); }
    public void write(float  data) { write(String.valueOf(data)); }
    public void write(double data) { write(String.valueOf(data)); }
    
    public void writeH(int  data)  { write(Integer.toHexString(data)); }
    public void writeH(long data)  { write(   Long.toHexString(data)); }


    /** Checks if buffer is empty.
      * @return boolean
      */
    public boolean isEmpty() {
        return buffer.length() == 0;
    }

    /** Dump buffer contents to file.
      * @return None
      */
    public void flush() {
        //FileOutputStream file = null;
        SimFriendlyFileWriter file = null;

        try {
            //file = new FileOutputStream(filepath);
            file = new SimFriendlyFileWriter(filepath, true, LoggingConstants.simMode);
            file.write(buffer.toString());
            if (file != null) file.close();
        } catch (IOException e) {
            //e.printStackTrace();
            System.out.println(String.format("Unable to create file %s", filepath));
        }

        buffer.setLength(0);
    }
}
