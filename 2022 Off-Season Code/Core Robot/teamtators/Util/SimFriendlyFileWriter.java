package org.teamtators.Util;

import java.io.FileWriter;
import java.io.IOException;

public class SimFriendlyFileWriter extends FileWriter {

    private boolean simMode;

    public SimFriendlyFileWriter(String filepath, boolean append, boolean simMode) throws IOException {
        super(filepath, append);
        this.simMode = simMode;
    }

    public void write(String str) throws IOException {
        if (simMode) {
            if (Math.random() > .96) {
                System.out.println("Writing " + str);
            }
        } else {
            super.write(str);
        }
    }

    public void close() throws IOException {
        if (simMode) {
            if (Math.random() > .96) {
                System.out.println("Closing file");
            }
        } else {
            super.close();
        }
    }
}
