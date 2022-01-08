package org.teamtators.common.hw;

import com.revrobotics.ColorMatch;
import com.revrobotics.ColorMatchResult;
import edu.wpi.first.wpilibj.I2C;
import edu.wpi.first.wpilibj.util.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamtators.common.util.Tuple;

import java.util.Map;

public class RevColorSensor {
    private static final Logger logger = LoggerFactory.getLogger(RevColorSensor.class);
    private static final byte i2cAddr = 0x52;
    private static final byte i2cPart = (byte) 0xC2;
    private final I2C i2c;

    private ColorMatch matcher;
    private Map<Color, String> colors;

    public RevColorSensor(I2C.Port port, ProximitySensorMeasurementRegisterValue proxSensorMeasRate,
                          ProximitySensorMeasurementRegisterValue proxSensorMeasRes, int proxSensorPulses,
                          LightSensorMeasurementRegisterValue colorSensorResolution, LightSensorMeasurementRegisterValue colorSensorRate,
                          LightSensorGainRegisterValue colorSensorGain, ProximitySensorRegisterValue proxSensorPulseRate,
                          ProximitySensorRegisterValue proxSensorPulseCurrent, ColorMatch matcher,
                          Map<Color, String> colors) {
        i2c = new I2C(port, i2cAddr);
        init(proxSensorMeasRate, proxSensorMeasRes, proxSensorPulses, colorSensorResolution, colorSensorRate, colorSensorGain, proxSensorPulseRate, proxSensorPulseCurrent);
        this.matcher = matcher;
        this.colors = colors;
    }

    public ColorMatchResult getMatchedColor() {
        return matcher.matchClosestColor(getColor());
    }

    public String getMatchedColorName() {
        return colors.get(getMatchedColor().color);
    }

    public String getMatchedColorName(Color color) {
        return colors.get(color);
    }


    private void init(ProximitySensorMeasurementRegisterValue proxSensorMeasRate, ProximitySensorMeasurementRegisterValue proxSensorMeasRes,
                      int proxSensorPulses, LightSensorMeasurementRegisterValue colorSensorResolution,
                      LightSensorMeasurementRegisterValue colorSensorRate,
                      LightSensorGainRegisterValue colorSensorGain,
                      ProximitySensorRegisterValue proxSensorPulseRate, ProximitySensorRegisterValue proxSensorPulseCurrent) {
        var partIDResult = getPartID();
        if (partIDResult.getA()) {
            logger.error("No Color Sensor connected!");
            return;
        }
        if (partIDResult.getB() != i2cPart) {
            logger.error("Wrong part number!");
            return;
        }

        if (writeByte(Register.control, ControlRegisterValue.EnableColorSensor | ControlRegisterValue.EnableLightSensor | ControlRegisterValue.EnableProximitySensor)) {
            logger.error("Control register write failure!");
            return;
        }

        if (writeByte(Register.cfgProxMeasRate, proxSensorMeasRate.value | proxSensorMeasRes.value)) {
            logger.error("Prox sensor measure rate register write failure!");
            return;
        }

        if (writeByte(Register.cfgPulseCount, proxSensorPulses)) {
            logger.error("Prox sensor pulse register write failure!");
            return;
        }

        if (writeByte(Register.cfgLightMeasRate, colorSensorRate.value | colorSensorResolution.value)) {
            logger.error("Color sensor config register write failure!");
            return;
        }

        if (writeByte(Register.cfgLightMeasGain, colorSensorGain.value)) {
            logger.error("Color sensor gain register write failure!");
            return;
        }

        if (writeByte(Register.cfgProxLED, proxSensorPulseRate.value | proxSensorPulseCurrent.value)) {
            logger.error("Proximity sensor led register write failure!");
            return;
        }

        clearReset(); //clear reset flag

        logger.info(String.format("Connected to Rev Color Sensor (part: 0x%02X)", partIDResult.getB()));
    }

    private Tuple<Boolean, Byte> getPartID() {
        byte[] buf = new byte[1];
        var res = i2c.read(Register.partID, 1, buf);
        return new Tuple<>(res, buf[0]);
    }

    private Tuple<Boolean, Byte> clearReset() {
        byte[] buf = new byte[1];
        var res = i2c.read(Register.control, 1, buf);
        return new Tuple<>(res, buf[0]);
    }

    private boolean writeByte(int register, int data) {
        return i2c.write(register, data);
    }

    private static class Register {
        public static final int control = 0x00;
        public static final int cfgProxLED = 0x01;
        public static final int cfgPulseCount = 0x02;
        public static final int cfgProxMeasRate = 0x03;
        public static final int cfgLightMeasRate = 0x04;
        public static final int cfgLightMeasGain = 0x05;
        public static final int partID = 0x06;
        public static final int status = 0x07;
        public static final int prox = 0x08;
        public static final int ir = 0x0A;
        public static final int g = 0x0D;
        public static final int b = 0x10;
        public static final int r = 0x13;
    }

    private static class ControlRegisterValue {
        public static final int Off = 0x00;
        public static final int EnableProximitySensor = 0x01;
        public static final int EnableLightSensor = 0x02;
        public static final int EnableColorSensor = 0x04;

        public static int getDefault() {
            return EnableProximitySensor | EnableLightSensor | EnableColorSensor;
        }
    }

    public enum LightSensorGainRegisterValue {
        Gain1x(0x00),
        Gain3x(0x01),
        Gain6x(0x02),
        Gain9x(0x03),
        Gain18x(0x04);
        public final int value;
        LightSensorGainRegisterValue(int val) {
            this.value = val;
        }
        public static LightSensorGainRegisterValue getDefault() {
            return Gain3x;
        }
    }

    public enum LightSensorMeasurementRegisterValue {
        //top half
        Res20b(0x00),
        Res19b(0x10),
        Res18b(0x20),
        Res17b(0x30),
        Res16b(0x40),
        Res13b(0x50),
        //bottom half
        Rate25ms(0x00),
        Rate50ms(0x01),
        Rate100ms(0x02),
        Rate200ms(0x03),
        Rate500ms(0x04),
        Rate1000ms(0x05),
        Rate2000ms(0x07); //0x06, 0x07 == 2000ms
        public final int value;
        LightSensorMeasurementRegisterValue(int val) {
            this.value = val;
        }
        public static LightSensorMeasurementRegisterValue getDefaultResolution() {
            return Res18b;
        }

        public static LightSensorMeasurementRegisterValue getDefaultRate() {
            return Rate100ms;
        }

    }

    public enum ProximitySensorRegisterValue {
        Pulse60kHz(0x18),
        Pulse70kHz(0x20),
        Pulse80kHz(0x28),
        Pulse90kHz(0x30),
        Pulse100kHz(0x38),

        Pulse2p5mA(0x00),
        Pulse5mA(0x01),
        Pulse10mA(0x02),
        Pulse25mA(0x03),
        Pulse50mA(0x04),
        Pulse75mA(0x05),
        Pulse100mA(0x06),
        Pulse125mA(0x07);
        public final int value;
        ProximitySensorRegisterValue(int val) {
            this.value = val;
        }

        public static ProximitySensorRegisterValue getDefaultPulseRate() {
            return Pulse60kHz;
        }

        public static ProximitySensorRegisterValue getDefaultPulseCurrent() {
            return Pulse125mA;
        }
    }

    public enum ProximitySensorMeasurementRegisterValue {
        Res8b(0x00),
        Res9b(0x08),
        Res10b(0x10),
        Res11b(0x18),

        Rate6p5ms(0x01),
        Rate12p5ms(0x02),
        Rate25ms(0x03),
        Rate50ms(0x04),
        Rate100ms(0x05),
        Rate200ms(0x06),
        Rate400ms(0x07);

        public final int value;
        ProximitySensorMeasurementRegisterValue(int value) {
            this.value = value;
        }

        public static ProximitySensorMeasurementRegisterValue getDefaultResolution() {
            return Res8b;
        }

        public static ProximitySensorMeasurementRegisterValue getDefaultRate() {
            return Rate100ms;
        }
    }

    public static int getDefaultPulseCount() {
        return 32;
    }

    private int read11BitRegister(int reg) {
        byte[] raw = new byte[2];

        i2c.read(reg, 2, raw);

        return (((int) raw[0] & 0xFF) | (((int) raw[1] & 0xFF) << 8)) & 0x7FF;
    }

    private int read20BitRegister(int reg) {
        byte[] raw = new byte[3];

        i2c.read(reg, 3, raw);

        return (((int) raw[0] & 0xFF) | (((int) raw[1] & 0xFF) << 8) |
                (((int) raw[2] & 0xFF) << 16)) & 0x03FFFF;
    }

    public int getRawBlue() {
        return read20BitRegister(Register.b);
    }

    public int getRawGreen() {
        return read20BitRegister(Register.g);
    }

    public int getRawRed() {
        return read20BitRegister(Register.r);
    }

    public int getRawIR() {
        return read20BitRegister(Register.ir);
    }

    public int getProximity() {
        return read11BitRegister(Register.prox);
    }

    public Color getColor() {
        double r = getRawRed();
        double g = getRawGreen();
        double b = getRawBlue();
        double normalize = r + g + b;
        return new Color(r / normalize, g / normalize, b / normalize);
    }
}
