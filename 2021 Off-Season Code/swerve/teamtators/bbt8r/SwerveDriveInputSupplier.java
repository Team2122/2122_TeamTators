package org.teamtators.bbt8r;

import java.util.function.Supplier;

public class SwerveDriveInputSupplier implements Supplier<SwerveDriveInputSupplier.SwerveDriveInput> {

    public SwerveDriveInputSupplier(Supplier<SwerveDriveInput> supplier) {
        setSupplier(supplier);
    }

    private Supplier<SwerveDriveInput> supplier;

    public SwerveDriveInputSupplier() {
        setSupplier(() -> new SwerveDriveInput(new double[2] , 0.0));
    }

    @Override
    public SwerveDriveInput get() {
        return supplier.get();
    }

    public Supplier<SwerveDriveInput> getSupplier() {
        return supplier;
    }

    public void setSupplier(Supplier<SwerveDriveInput> supplier) {
        this.supplier = supplier;
    }

    public static class SwerveDriveInput {

        public double[] vector;
        public double rotate;

        public SwerveDriveInput(double[] vector, double left) {
            this.vector = vector;
            this.rotate = left;
        }

        public void set(double[] vector, double left) {
            this.vector = vector;
            this.rotate = left;
        }

    }

}
