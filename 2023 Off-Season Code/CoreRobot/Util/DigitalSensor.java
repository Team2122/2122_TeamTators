package common.Util;

import edu.wpi.first.wpilibj.DigitalInput;

/**
 * An abstraction for either PNP or NPN digital sensors
 *
 * Essentially allows for inverting the sensor reading
 */
public class DigitalSensor extends DigitalInput{
    private Type type = Type.PNP;

    public DigitalSensor(int channel, Type type) {
        this(channel);
        setType(type);
    }

    public DigitalSensor(int channel) {
        super(channel);
    }

    /**
     * @return the value from a digital input channel, taking into account the type
     */
    public boolean get() {
        boolean value = getRaw();
        switch (type) {
            case NPN:
                return !value;
            case PNP:
            default:
                return value;
        }
    }

    /**
     * @return The raw signal from the DigitalInput
     */
    public boolean getRaw() {
        return super.get();
    }

    /**
     * @return type of the digital sensor, PNP or NPN
     */
    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        if (type != null) {
            this.type = type;
        }
    }

    public enum Type {
        PNP, NPN
    }
}
