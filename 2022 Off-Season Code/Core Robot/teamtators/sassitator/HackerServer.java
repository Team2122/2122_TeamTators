package org.teamtators.sassitator;

import edu.wpi.first.networktables.EntryListenerFlags;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import frc.robot.RobotContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.function.Function;

public class HackerServer implements Debuggable {

    private final HashMap<String, Function<String, String>> hackerServerActions;
    private final Logger logger;
    private boolean debug = false;

    // NetworkTables
    private final String tableKey = "HackerServer";
    private final String numClientsKey = "numClients";
    private final String entryKey = "Client: ";
    private int currentNumClients;
    private final NetworkTableEntry numClients;

    public HackerServer(RobotContainer robotContainer) {
        robotContainer.getDebuggableRegistry().registerDebuggable(this);
        hackerServerActions = new HashMap<>();
        logger = LoggerFactory.getLogger(getClass().getSimpleName());
        addCommandActions(robotContainer);
        addDebugActions(robotContainer);

        debugOn();

        currentNumClients = 0;
        numClients = NetworkTableInstance.getDefault().getTable(tableKey).getEntry(numClientsKey);
        numClients.setString(Integer.toString(currentNumClients));
        logger.info("Created");

        numClients.addListener((notification) -> {

            int readNumClients = Integer.parseInt(numClients.getString(Integer.toString(currentNumClients)));
            if (readNumClients != currentNumClients) {
            if (isDebugging()) {
                System.out.println("New Clients Detected!");
            }
                for (int i = currentNumClients; i <= readNumClients; i++) {
                    currentNumClients++;
                    int num = currentNumClients;
                    NetworkTableEntry entry = NetworkTableInstance.getDefault().getTable(tableKey).getEntry(entryKey + currentNumClients);
                    entry.addListener((entryNotification) -> {
                        String input = entry.getString("");
                        if (input.equals("")) {
                            return;
                        }
                        if (isDebugging()) {
                            logger.info(entryKey + " " + num + " Sent: " + input);
                        }

                        String output = handleInput(input);
                        entry.setString(output);
                        if (isDebugging()) {
                            logger.info(entryKey + " " + num + " Was Sent: " + output);
                        }
                    }, EntryListenerFlags.kUpdate | EntryListenerFlags.kImmediate);

                    if (isDebugging()) {
                        System.out.println("Client Added");
                    }
                }
            }

        }, EntryListenerFlags.kUpdate | EntryListenerFlags.kImmediate);
    }

    public String handleInput(String string) {
        int index = string.indexOf(' ');
        if (index == -1) {
            Function<String, String> function = hackerServerActions.get(string);
            if (function == null) {
                return "Unable To Find Action";
            }
            return function.apply("");
        } else {
            String action = string.substring(0, index).toLowerCase();
            String input = string.substring(index + 1);

            Function<String, String> function = hackerServerActions.get(action);
            if (function == null) {
                return "Unable To Find Action";
            }
            return function.apply(input);
        }
    }

    public void addAction(Function<String, String> function, String key) {
        key = key.toLowerCase();
        if (hackerServerActions.get(key) != null) {
            logger.error("Key Already In Use: " + key);
            return;
        }
        hackerServerActions.put(key, function);
    }

    public void addAction(Function<String, String> function, String... key) {
        for (int i = 0; i < key.length; i++) {
            addAction(function, key[i]);
        }
    }

    private void addCommandActions(RobotContainer robotContainer) {
        // Start Command
        addAction((input) -> {
            Command cmd = robotContainer.getCommands().get(input);
            if (cmd == null) {
                return "Unable To Find Command";
            }
            cmd.schedule(true);
            return "Command Scheduled";
        }, "run", "runcommand", "cmd", "command", "start");

        // Stop Command
        addAction((input) -> {
            Command cmd = robotContainer.getCommands().get(input);
            if (cmd == null) {
                return "Unable To Find Command";
            }
            cmd.cancel();
            return "Command Cancelled";
        }, "cancel", "stop");

    }

    private void addDebugActions(RobotContainer robotContainer) {
        // Debug On
        addAction((input) -> {
            if (input.equalsIgnoreCase("all")) {
                robotContainer.getDebuggableRegistry().turnOnAll();
                return "Debuggables Turned On";
            } else {
                Debuggable debubale = robotContainer.getDebuggableRegistry().get(input);
                if (debubale == null) {
                    return "Unable To Find Debuggable";
                } else {
                    debubale.debugOn();
                    return "Debuggable Turned On";
                }
            }
        }, "debugon", "ondebug");

        // Debug Off
        addAction((input) -> {
            if (input.equalsIgnoreCase("all")) {
                robotContainer.getDebuggableRegistry().turnOffAll();
                return "Debuggables Turned Off";
            } else {
                Debuggable debubale = robotContainer.getDebuggableRegistry().get(input);
                if (debubale == null) {
                    return "Unable To Find Debuggable";
                } else {
                    debubale.debugOff();
                    return "Debuggable Turned Off";
                }
            }
        }, "debugoff", "offdebug");

        // Debug Toggle
        addAction((input) -> {
            if (input.equalsIgnoreCase("all")) {
                robotContainer.getDebuggableRegistry().toggleAll();
                return "Debuggables Toggled";
            } else {
                Debuggable debubale = robotContainer.getDebuggableRegistry().get(input);
                if (debubale == null) {
                    return "Unable To Find Debuggable";
                } else {
                    debubale.debugToggle();
                    return "Debuggable Toggled";
                }
            }
        }, "debugtoggle", "toggledebug");

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
