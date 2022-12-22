package org.teamtators.Util;

import org.teamtators.logging.FileBuffer;

public class SimFriendlyFileBuffer extends FileBuffer {

    private boolean simMode;

    public SimFriendlyFileBuffer(String filepath, int buffer_limit, boolean simMode){
        super(filepath, buffer_limit);
        this.simMode = simMode;
    }

    public boolean write(String data) {
        if (simMode) {
            if (Math.random() > .96) {
                System.out.println("Writing " + data + " to buffer");
            }
            return true;
        } else {
            if (buffer.length() + data.length() > buffer.capacity()) {
                this.flush();
            }
    
            buffer.append(data);
            return buffer.length() == data.length();
        }
    }

    public void flush() {
        if (simMode) {
            System.out.println("Flushed file buffer");
        } else {
            super.flush();
        }
    }
}
