package org.teamtators.Util;

import java.io.File;

public class SimFriendlyFile extends File {

    private boolean simMode;

    public SimFriendlyFile(String filepath, boolean simMode) {
        super(filepath);
        this.simMode = simMode;
    }

    public boolean mkdir() {
        if (simMode) {
            System.out.println("Making directory");
            return true;
        } else {
            return super.mkdir();
        }
    }

    public boolean delete() {
        if (simMode) {
            System.out.println("Deleting old file");
            return true;
        } else {
            return super.delete();
        }
    }
}
