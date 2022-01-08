package org.teamtators.common.config.helpers;

import com.revrobotics.ColorMatch;
import edu.wpi.first.wpilibj.util.Color;

public class ColorConfig implements ConfigHelper<Color> {
    public double r;
    public double g;
    public double b;

    @Override
    public Color create() {
        return ColorMatch.makeColor(r, g, b);
    }
}
