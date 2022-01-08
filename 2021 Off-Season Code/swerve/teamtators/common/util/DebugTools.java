package org.teamtators.common.util;

import org.teamtators.common.control.Timer;

import java.util.HashMap;
import java.util.Map;

public class DebugTools {
    public static void notImplemented () {
        throw new Error("not implemented");
    }

    public static boolean periodic (double periodicSeconds) {
        var data = lazyInit(getUniqueId(), () -> {}, periodicSeconds);
        data.update();
        return data.doRun();
    }
    public static void periodic(Runnable code, double periodicSeconds) {
        var data = lazyInit(getUniqueId(), code, periodicSeconds);
        data.update();
    }
    public static void once(Runnable code) {
        var data = lazyInit(getUniqueId(), code, Double.MAX_VALUE);
        data.update();
    }

    protected static Timer timer = new Timer();
    private static Map<String, DebugData> debugs = new HashMap<>();

    private static DebugData lazyInit (String uniqueId, Runnable code, double periodicTime) {
        if (!timer.isRunning()) {
            timer.start();
        }
        if (debugs.containsKey(uniqueId)) {
            return debugs.get(uniqueId);
        } else {
            var data = DebugData.create(code, periodicTime);
            debugs.put(uniqueId, data);
            return data;
        }
    }
    private static String getUniqueId () {
        var element = Thread.currentThread().getStackTrace()[3]; // hardcoded depth
        return String.format("L%d:C%s", element.getLineNumber(), element.getClassName());
    }

    private static class DebugData {
        public double lastTime;
        public int timesCalled;
        public double periodicTime;
        public boolean hasUpdated = false;
        public Runnable code;
        public static DebugData create (Runnable code, double periodicTime) {
            var d = new DebugData();
            d.lastTime = timer.get();
            d.timesCalled = 0;
            d.periodicTime = periodicTime;
            d.code = code;
            return d;
        }
        public boolean doRun () {
            return !hasUpdated || timer.get() - lastTime > periodicTime;
        }
        public void update () {
            if (doRun()) {
                code.run();
                lastTime = timer.get();
                hasUpdated = true;
                timesCalled ++;
            }
        }
    }
}
