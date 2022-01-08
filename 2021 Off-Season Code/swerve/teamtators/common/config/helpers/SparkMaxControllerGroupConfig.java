package org.teamtators.common.config.helpers;

import org.teamtators.common.hw.SparkMaxControllerGroup;
import org.teamtators.common.hw.TatorSparkMax;

import java.util.ArrayList;

@SuppressWarnings("serial")
public class SparkMaxControllerGroupConfig extends ArrayList<SpeedControllerConfig>
    implements ConfigHelper<SparkMaxControllerGroup>{
        public SparkMaxControllerGroup create(){
            TatorSparkMax[] controllers = new TatorSparkMax[this.size()];
            for (int i = 0; i < controllers.length; i++) {
                controllers[i] = (TatorSparkMax) this.get(i).create();
            }
            return new SparkMaxControllerGroup(controllers);
        }
    }

