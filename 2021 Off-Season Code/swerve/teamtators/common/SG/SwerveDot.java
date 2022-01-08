package org.teamtators.common.SG;

import SplineGenerator.GUI.DisplayGraphics;
import SplineGenerator.GUI.Displayable;
import SplineGenerator.Util.DPoint;

import java.util.function.Supplier;

public class SwerveDot implements Displayable {
    private Supplier<DPoint> pointSupplier;
    private DPoint pos;

    public SwerveDot(Supplier<DPoint> pointSupplier){
        this.pointSupplier = pointSupplier;
        pos = pointSupplier.get();
    }

    public void update(){
        pos = pointSupplier.get();
    }

    public void display(DisplayGraphics displayGraphics){
        update();
        displayGraphics.paintPoint(pos.clone());
    }

    @Override
    public String toString() {
        return "SwerveDot{" + "position=" + pos + '}';
    }
}
