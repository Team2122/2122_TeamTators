package org.teamtators.sassitator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;

public class DebuggableRegistry implements Debuggable {

    private HashMap<String, Debuggable> registry;
    private boolean debug = false;
    private final Logger logger;

    public DebuggableRegistry() {
        registry = new HashMap<>();
        registry.put(getClass().getSimpleName(), this);
        logger = LoggerFactory.getLogger(getClass().getSimpleName());
    }

    public Debuggable get(String name) {
        return registry.get(name);
    }

    public void registerDebuggable(String name, Debuggable debuggable) {
        registry.put(debuggable.getClass().getSimpleName(), debuggable);
    }

    public void registerDebuggable(Debuggable debuggable) {
        registerDebuggable(debuggable.getClass().getSimpleName(), debuggable);
    }

    public void removeDebuggable(String name) {
        Debuggable debuggable = registry.remove(name);
    }

    public int size() {
        return registry.size();
    }

    public void turnOn(String key) {
        Debuggable debuggable = registry.get(key);
        if (debuggable == null) {
            logger.error("Could Not Find Debuggable");
            return;
        }
        debuggable.debugOn();
        if (debug) {
            logger.info(key + " Turned On");
        }
    }

    public void turnOff(String key) {
        Debuggable debuggable = registry.get(key);
        if (debuggable == null) {
            logger.error("Could Not Find Debuggable");
            return;
        }
        debuggable.debugOff();
        if (debug) {
            logger.info(key + " Turned Off");
        }
    }

    public void toggle(String key) {
        Debuggable debuggable = registry.get(key);
        if (debuggable == null) {
            logger.error("Could Not Find Debuggable");
            return;
        }
        debuggable.debugToggle();
        if (debug) {
            logger.info(key + " Toggled");
        }
    }

    public void turnOnAll() {
        ArrayList<Debuggable> arrayList = new ArrayList<>(registry.values());
        for (int i = 0; i < registry.size(); i++) {
            arrayList.get(i).debugOn();
        }
        if (debug) {
            logger.info("All Turned On");
        }
    }

    public void turnOffAll() {
        ArrayList<Debuggable> arrayList = new ArrayList<>(registry.values());
        for (int i = 0; i < registry.size(); i++) {
            arrayList.get(i).debugOff();
        }
        if (debug) {
            logger.info("All Turned Off");
        }
    }

    public void toggleAll() {
        ArrayList<Debuggable> arrayList = new ArrayList<>(registry.values());
        for (int i = 0; i < registry.size(); i++) {
            arrayList.get(i).debugToggle();
        }
        if (debug) {
            logger.info("All Toggled");
        }
    }

    @Override
    public void debugOn() {
        debug = true;
    }

    @Override
    public void debugOff() {
        debug = false;
    }

    @Override
    public void debugToggle() {
        debug = !debug;
    }

    @Override
    public boolean isDebugging() {
        return debug;
    }
}
