package org.teamtators.sassitator;

import java.util.ArrayList;
import java.util.function.Supplier;

import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;
import edu.wpi.first.wpilibj.shuffleboard.ShuffleboardTab;

public class ShuffleboardRegister {
    private final ArrayList<Runnable> updaters;
    private final ShuffleboardTab tab;

    public ShuffleboardRegister(String tabName) {
        updaters = new ArrayList<Runnable>();
        tab = Shuffleboard.getTab(tabName);
    }

    public void update() {
        for (int i = 0; i < updaters.size(); i++) {
            updaters.get(i).run();
        }
    }

    public void addData(String title, Supplier<Object> value, int width, int height, int x, int y) {
        NetworkTableEntry networkTableEntry = tab.add(title, value.get())
            .withSize(width, height)
            .withPosition(x, y)
            .getEntry();
        
        updaters.add(() -> networkTableEntry.setValue(value.get()));
    }

    public void addData(String title, Supplier<Object> value, int x, int y) {
        NetworkTableEntry networkTableEntry = tab.add(title, value.get())
            .withSize(2, 1)
            .withPosition(x, y)
            .getEntry();
        
        updaters.add(() -> networkTableEntry.setValue(value.get()));
    }

    public void addData(String title, Supplier<Object> value) {
        NetworkTableEntry networkTableEntry = tab.add(title, value.get())
            .withSize(2, 1)
            .getEntry();
        
        updaters.add(() -> networkTableEntry.setValue(value.get()));
    }
}