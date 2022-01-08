package org.teamtators.common.math;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.base.Objects;

/**
 * @author Alex Mikhalev
 */
public class Rotation {
    private double sin;
    private double cos;

    public Rotation(double sin, double cos) {
        this.sin = sin;
        this.cos = cos;
        this.normalize();
    }

    public Rotation(Rotation other) {
        this.sin = other.sin;
        this.cos = other.cos;
    }

    public Rotation() {
        this(identity());
    }

    public static Rotation identity() {
        return new Rotation(0.0, 1.0);
    }

    public static Rotation fromRadians(double radians) {
        return new Rotation(Math.sin(radians), Math.cos(radians));
    }

    public static Rotation fromDegrees(double degrees) {
        return fromRadians(Math.toRadians(degrees));
    }

/*
    public void setRadians(double radians) {
        this.sin = Math.sin(radians);
        this.cos = Math.cos(radians);
        normalize();
    }

    public void setDegrees(double degrees) {
        setDegrees(Math.toRadians(degrees));
    }
*/
    @JsonSetter("sin")
    public void setSin(double sin) {
        this.sin = sin;
    }

    @JsonSetter("cos")
    public void setCos(double cos) {
        this.cos = cos;
    }


    public double sin() {
        return sin;
    }

    public double cos() {
        return cos;
    }

    public double tan() {
        return sin / cos;
    }

    public double csc() {
        return 1 / sin;
    }

    public double sec() {
        return 1 / cos;
    }

    public double cot() {
        return cos / sin;
    }

    public double toRadians() {
        return Math.atan2(sin, cos);
    }

    public double toPositiveRadians() {
        double radians = toRadians();
        if (radians < 0) {
            radians += Math.PI * 2;
        }
        return radians;
    }

    public double toDegrees() {
        return Math.toDegrees(this.toRadians());
    }

    public double toPositiveDegrees() {
        return Math.toDegrees(this.toPositiveRadians());
    }

    public Rotation neg() {
        return new Rotation(-this.sin, this.cos);
    }

    public Rotation add(Rotation other) {
        return new Rotation(this.sin * other.cos + this.cos * other.sin,
                this.cos * other.cos - this.sin * other.sin);
        //sin(a + b), and cos(a + b)
    }

    public Rotation sub(Rotation other) {
        return new Rotation(this.sin * other.cos - this.cos * other.sin,
                this.cos * other.cos + this.sin * other.sin);
    }

    public Rotation mult(double scaleNum) {
        return Rotation.fromRadians(scaleNum * toRadians());
    }

    /**
     * @return the rotation plus 90 degrees (counter clockwise in mathematical coordinates)
     */
    public Rotation ccwNormal() {
        return new Rotation(this.cos, -this.sin);
    }

    /**
     * @return the rotation minus 90 degrees (clockwise in mathematical coordinates)
     */
    public Rotation cwNormal() {
        return new Rotation(-this.cos, this.sin);
    }

    public Rotation inverse() {
        return new Rotation(-this.sin, -this.cos);
    }

    public Rotation complement() {
        return new Rotation(this.sin, -this.cos);
    }

    /**
     * @return a vector pointing in the direction of the rotation with length 1
     */
    public Translation2d toTranslation() {
        return new Translation2d(this.cos, this.sin);
    }

    /**
     * @param length the length of the resultant vector
     * @return   a vector pointing in the direction of the rotation with the specified length
     */
    public Translation2d toTranslation(double length) {
        return toTranslation().scale(length);
    }

    /**
     * Checks if this rotation is between the smallest angle formed between start and end
     * @param start
     * @param end
     * @return
     */
    public boolean isBetween(Rotation start, Rotation end) {
        double a = end.sub(start).toRadians();
        double b = this.sub(start).toRadians();
        if (a * b > 0 && Math.abs(b) < Math.abs(a)) {
            return true;
        }
        return false;
    }

    public void normalize() {
        double hyp = Math.hypot(sin, cos);
        if (hyp != 0) {
            this.sin /= hyp;
            this.cos /= hyp;
        }
    }

    @Override
    public String toString() {
        return toDegrees() + "\u00B0";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Rotation rotation = (Rotation) o;
        return Double.compare(rotation.toRadians(), toRadians()) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(toRadians());
    }

    public boolean isParallel(Rotation yaw) {
        return epsilonEquals(yaw) || epsilonEquals(yaw.inverse());
    }

    public boolean epsilonEquals(Rotation other) {
        return Epsilon.isEpsilonZero(sub(other).toRadians());
    }

    public edu.wpi.first.wpilibj.geometry.Rotation2d toWpiLibRotation () {
        return new edu.wpi.first.wpilibj.geometry.Rotation2d(this.cos, this.sin);
    }
    public static Rotation fromWpiLibRotation (edu.wpi.first.wpilibj.geometry.Rotation2d rotation) {
        return new Rotation(rotation.getSin(), rotation.getCos());
    }
}
