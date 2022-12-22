package org.teamtators.sassitator;

public interface Debuggable {

    void debugOn();
    void debugOff();
    void debugToggle();

    boolean isDebugging();

}
