package org.teamtators.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class LookupTable {
    private static final Logger logger = LoggerFactory.getLogger(LookupTable.class);
    // just use doubles for everything
    private ArrayList<Tuple<Double, Double>> table; // ordered map of (key, value) by key
    public static LookupTable createTable (Tuple<Double, Double>[] inputs) {
        if (inputs.length < 2) {
            logger.error("createTable: need at least 2 entries"); // ooh
            throw new Error("LookupTable: createTable: need at least 2 entries");
        }
        var lookupTable = new LookupTable();
        for (var row : inputs) {
            lookupTable.addValue(row.getA(), row.getB());
        }
        return lookupTable;
    }
    public static LookupTable createTable (ArrayList<Tuple<Double, Double>> input) {
        if (input.size() < 2) {
            logger.error("createTable: need at least 2 entries");
            throw new Error("need at least 2 entries");
        }
        var lt = new LookupTable();
        for (var e : input) {
            lt.addValue(e.getA(), e.getB());
        }
        return lt;
    }
    private LookupTable () {
        table = new ArrayList<>();
    }
    public double get (double x) {
        double bottom, top;
        for (int i = 1; i<table.size(); i++) {
            if (x < table.get(i).getA())
                continue;
            bottom = table.get(i-1).getB();
            top = table.get(i).getB();
            return mix(bottom, top, unmix(table.get(i-1).getA(), table.get(i-1).getB(), x));
        }
        return table.get(0).getB();
    }

    // math methods to interpolate between numbers
    private static double unmix (double bottom, double top, double value) {
        return (value - bottom) / (top - bottom);
    }

    /**
     *
     * @param bottom    value
     * @param top       value
     * @param t         value between 0 and 1
     * @return          a value between bottom and top
     */
    private static double mix (double bottom, double top, double t) {
        return bottom + (top - bottom) * t;
    }

    public void addValue (double inValue, double outValue, int index) {
        table.add(index, new Tuple<>(inValue, outValue));
    }
    public void addValue (double inValue, double outValue) {
        table.add(new Tuple<>(inValue, outValue));
    }
}
