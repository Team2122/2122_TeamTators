package org.teamtators.common.datalogging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * Utility as an alternative to temporarily commenting out code
 * Code inside the Runnable is ignored, but preserved in text
 * Additionally, a warning can be logged to remind the user of disabled code
 * <DO NOT USE IN COMPETITION>
 * @see Disable#warnOnce(Runnable)
 * @see Disable#main(String[])
 */
public class Disable {
    private static final Logger logger = LoggerFactory.getLogger(Disable.class);
    private static Set<String> calledOnce = new HashSet<>();

    /**
     * Warn about this code being disabled
     *  every time it is encountered
     */
    public static void warnAll (Runnable disabledCode) {
        warnAll(getCodeInformation(), disabledCode);
    }
    public static void warnAll (String name, Runnable disabledCode) {
        warnAll(name, logger, disabledCode);
    }
    public static void warnAll (Logger log, Runnable disabledCode) {
        warnAll(getCodeInformation(), log, disabledCode);
    }
    public static void warnAll (String name, Logger log, Runnable disabledCode) {
        disable(name, log);
    }

    /**
     * Warn once about this code being disabled
     */
    public static void warnOnce (Runnable disabledCode) {
        warnOnce(getCodeInformation(), disabledCode);
    }
    public static void warnOnce (String uniqueName, Runnable disabledCode) {
        warnOnce(uniqueName, logger, disabledCode);
    }
    public static void warnOnce (String uniqueName, Logger log, Runnable disabledCode) {
        if (once(uniqueName))
            disable(uniqueName, log);
    }
    public static void warnOnce (Logger log, Runnable disabledCode) {
        warnOnce(getCodeInformation(), log, disabledCode);
    }


    private static void disable (String name, Logger log) {
        log.warn("{} is disabled!", name);
    }
    private static boolean once (String name) {
        if (calledOnce.contains(name))
            return false;
        calledOnce.add(name);
        return true;
    }
    private static String getCodeInformation() {
        var element = Thread.currentThread().getStackTrace()[3]; // hardcoded depth
        // element.getClassName();
        return String.format("code on line %d in %s", element.getLineNumber(), element.getClassName());
    }

    /**
     * Example usage of common methods
     */
    public static void main (String[] args) {
        System.out.println("running main");
        for (int i = 0; i<3; i++) {
            Disable.warnOnce(() -> {
                // code block
                System.out.println("this code will not run");
            });
            Disable.warnAll(() -> {
                // code block
                System.out.println("this code will not run");
            });
        }
    }
}
