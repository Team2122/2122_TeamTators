package org.teamtators.common.math;

import com.fasterxml.jackson.annotation.JsonSetter;
import edu.wpi.first.wpilibj.util.Units;

/**
 * @author Alex Mikhalev
 */
public class Translation2d {
    private double x;
    private double y;

    public Translation2d(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Translation2d(Translation2d translation) {
        this.x = translation.x;
        this.y = translation.y;
    }

    public Translation2d() {
    }

    @JsonSetter("x")
    public void setX(double x) {
        this.x = x;
    }

    @JsonSetter("y")
    public void setY(double y) {
        this.y = y;
    }

    public static Translation2d zero() {
        return new Translation2d(0.0, 0.0);
    }

    public static Translation2d nan() {
        return new Translation2d(Double.NaN, Double.NaN);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public Translation2d withX(double x) {
        return new Translation2d(x, this.y);
    }

    public Translation2d withY(double y) {
        return new Translation2d(this.x, y);
    }

    public boolean isNaN() {
        return Double.isNaN(x) || Double.isNaN(y);
    }

    public Translation2d rotateBy(Rotation rotation) {
        return new Translation2d(this.x * rotation.cos() - this.y * rotation.sin(),
                this.x * rotation.sin() + this.y * rotation.cos());
    }

    public Rotation getDirection() {
        return new Rotation(this.y, this.x);
    }

    public double getMagnitude() {
        return Math.hypot(this.x, this.y);
    }

    public Translation2d add(Translation2d other) {
        return new Translation2d(this.x + other.x, this.y + other.y);
    }

    public Translation2d sub(Translation2d other) {
        return new Translation2d(this.x - other.x, this.y - other.y);
    }

    public Translation2d neg() {
        return new Translation2d(-this.x, -this.y);
    }

    public Translation2d scale(double scalar) {
        return new Translation2d(this.x * scalar, this.y * scalar);
    }

    public double getDistance(Translation2d other) {
        return Math.sqrt((this.x - other.x) * (this.x - other.x)) + ((this.y - other.y) * (this.y - other.y));
    }

    public double dot(Translation2d other) {
        return this.x * other.x + this.y * other.y;
    }

    public Translation2d interpolate(Translation2d other, double amount) {
        return new Translation2d(this.x + (other.x - this.x) * amount, this.y + (other.y - this.y) * amount);
    }

    @Override
    public String toString() {
        return "<" + x + ", " + y + ">";
    }

    public double cross(Translation2d other) {
        return this.x * other.y - this.y * other.x;
    }

    public boolean epsilonEquals(Translation2d other) {
        return Epsilon.isEpsilonEqual(getX(), other.getX()) &&
                Epsilon.isEpsilonEqual(getY(), other.getY());
    }

    public edu.wpi.first.wpilibj.geometry.Translation2d toWpiLibTranslation() {
        return new edu.wpi.first.wpilibj.geometry.Translation2d(Units.inchesToMeters(this.getX()), Units.inchesToMeters(this.getY()));
    }

    public static Translation2d fromWpiLibTranslation(edu.wpi.first.wpilibj.geometry.Translation2d translation) {
        return new Translation2d(Units.metersToInches(translation.getX()), Units.metersToInches(translation.getY()));
    }
}
