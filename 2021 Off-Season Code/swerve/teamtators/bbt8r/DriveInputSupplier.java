package org.teamtators.bbt8r;

import java.util.function.Supplier;

public class DriveInputSupplier implements Supplier<DriveInputSupplier.DriveInput> {

    private Supplier<DriveInput> supplier;

    public DriveInputSupplier(Supplier<DriveInput> supplier) {
        setSupplier(supplier);
    }

    public DriveInputSupplier() {
        setSupplier(() -> new DriveInput(0, 0));
    }

    @Override
    public DriveInput get() {
        return supplier.get();
    }

    public Supplier<DriveInput> getSupplier() {
        return supplier;
    }

    public void setSupplier(Supplier<DriveInput> supplier) {
        this.supplier = supplier;
    }

    public static class DriveInput {

        public double right;
        public double left;

        public DriveInput(double right, double left) {
            this.right = right;
            this.left = left;
        }

        public void set(double right, double left) {
            this.right = right;
            this.left = left;
        }

    }

}
