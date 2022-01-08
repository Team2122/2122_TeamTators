package org.teamtators.common.SG;

import SplineGenerator.Applied.MotionController;

import java.util.HashMap;

public class SGControllerRegistry {

    private HashMap<String, MotionController> register;

    public SGControllerRegistry() {
        register = new HashMap<>();
    }

    public void addController(String controllerName, MotionController controller) {
        register.put(controllerName, controller);
    }

    public MotionController getController(String controllerName) {
        return register.get(controllerName);
    }

}
