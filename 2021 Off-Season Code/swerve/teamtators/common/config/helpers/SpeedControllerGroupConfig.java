package org.teamtators.common.config.helpers;

import edu.wpi.first.wpilibj.SpeedController;
import org.teamtators.common.hw.SpeedControllerGroup;

import java.util.ArrayList;

@SuppressWarnings("serial")
public class SpeedControllerGroupConfig extends ArrayList<SpeedControllerConfig>
        implements ConfigHelper<SpeedControllerGroup> {
    //public boolean followMaster = false;

    public SpeedControllerGroup create() {
        SpeedController[] controllers = new SpeedController[this.size()];
        for (int i = 0; i < controllers.length; i++) {
            controllers[i] = this.get(i).create();

            /*if(followMaster && i > 0 && controllers[i] instanceof IMotorController) {
                //((IMotorController) controllers[i]).follow((IMotorController) controllers[0]);
            } else {

            }*/
        }
        return new SpeedControllerGroup(controllers);
    }
}
