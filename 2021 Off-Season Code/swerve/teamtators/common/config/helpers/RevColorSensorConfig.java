package org.teamtators.common.config.helpers;

import com.revrobotics.ColorMatch;
import edu.wpi.first.wpilibj.I2C;
import edu.wpi.first.wpilibj.util.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamtators.common.hw.RevColorSensor;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.teamtators.common.hw.RevColorSensor.LightSensorMeasurementRegisterValue.*;
import static org.teamtators.common.hw.RevColorSensor.ProximitySensorMeasurementRegisterValue.*;
import static org.teamtators.common.hw.RevColorSensor.ProximitySensorRegisterValue.*;

public class RevColorSensorConfig implements ConfigHelper<RevColorSensor> {
    private static final Logger logger = LoggerFactory.getLogger(RevColorSensor.class);
    public I2C.Port port = I2C.Port.kOnboard;
    public Map<String, ColorWrapper> colors;
    public double confidenceLevel = 0.95;

    public RevColorSensor.ProximitySensorMeasurementRegisterValue proxSensorMeasRate = RevColorSensor.ProximitySensorMeasurementRegisterValue.getDefaultRate();
    public RevColorSensor.ProximitySensorMeasurementRegisterValue proxSensorMeasRes = RevColorSensor.ProximitySensorMeasurementRegisterValue.getDefaultResolution();
    public int proxSensorPulses = RevColorSensor.getDefaultPulseCount();
    public RevColorSensor.LightSensorMeasurementRegisterValue colorSensorRate = RevColorSensor.LightSensorMeasurementRegisterValue.getDefaultRate();
    public RevColorSensor.LightSensorMeasurementRegisterValue colorSensorResolution = RevColorSensor.LightSensorMeasurementRegisterValue.getDefaultResolution();
    public RevColorSensor.LightSensorGainRegisterValue colorSensorGain = RevColorSensor.LightSensorGainRegisterValue.getDefault();
    public RevColorSensor.ProximitySensorRegisterValue proxSensorPulseRate = RevColorSensor.ProximitySensorRegisterValue.getDefaultPulseRate();
    public RevColorSensor.ProximitySensorRegisterValue proxSensorPulseCurrent = RevColorSensor.ProximitySensorRegisterValue.getDefaultPulseCurrent();


    @Override
    public RevColorSensor create() {
        Map<Color, String> wpicolor = colors.entrySet().stream().collect(Collectors.toMap(e -> e.getValue().toWpilibColor(), Map.Entry::getKey));
        ColorMatch match = new ColorMatch();
        match.setConfidenceThreshold(confidenceLevel);
        wpicolor.keySet().forEach(match::addColorMatch);
        var colorSensorRateValues = EnumSet.of(Res13b, Res16b, Res17b, Res18b, Res20b);
        if (isInvalid(colorSensorRate, colorSensorRateValues)) {
            logger.error("Invalid color sensor rate value!");
            return null;
        }
        if (isInvalid(colorSensorResolution, EnumSet.complementOf(colorSensorRateValues))) {
            logger.error("Invalid color sensor resolution value!");
            return null;
        }
        var proxSensorPulseRateValues = EnumSet.of(Pulse5mA, Pulse2p5mA, Pulse10mA, Pulse25mA, Pulse50mA, Pulse75mA, Pulse100mA, Pulse125mA);
        if (isInvalid(proxSensorPulseRate, proxSensorPulseRateValues)) {
            logger.error("Invalid proximity sensor pulse rate value!");
            return null;
        }
        if (isInvalid(proxSensorPulseCurrent, EnumSet.complementOf(proxSensorPulseRateValues))) {
            logger.error("Invalid proximity sensor pulse current value!");
            return null;
        }
        var proxSensorMeasRateValues = EnumSet.of(Res8b, Res9b, Res10b, Res11b);
        if (isInvalid(proxSensorMeasRate, proxSensorMeasRateValues)) {
            logger.error("Invalid proximity sensor measurement rate value!");
            return null;
        }
        if (isInvalid(proxSensorMeasRes, EnumSet.complementOf(proxSensorMeasRateValues))) {
            logger.error("Invalid proximity sensor measurement resolution value!");
            return null;
        }

        if(proxSensorPulses < 0 || proxSensorPulses > 255) {
            logger.error("Proximity sensor pulse count value out of bounds!");
            return null;
        }

        return new RevColorSensor(port, proxSensorMeasRate, proxSensorMeasRes, proxSensorPulses, colorSensorResolution, colorSensorRate,
                colorSensorGain, proxSensorPulseRate, proxSensorPulseCurrent, match, wpicolor);
    }

    private <T extends Enum<T>> boolean isInvalid(T t, Set<T> excluded) {
        return excluded.contains(t);
    }

    public static class ColorWrapper {
        public double r;
        public double g;
        public double b;

        public Color toWpilibColor() {
            return new Color(r, g, b);
        }
    }
}
