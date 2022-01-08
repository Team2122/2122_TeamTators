package org.teamtators.common.config.helpers;

import com.ctre.phoenix.motorcontrol.can.BaseMotorController;
import org.teamtators.common.hw.CtreMotorControllerGroup;

import java.util.ArrayList;

@SuppressWarnings("serial")
public class CtreMotorControllerGroupConfig extends ArrayList<SpeedControllerConfig>
        implements ConfigHelper<CtreMotorControllerGroup> {

    public CtreMotorControllerGroup create() {
        BaseMotorController[] controllers = new BaseMotorController[this.size()];
        for (int i = 0; i < controllers.length; i++) {
            controllers[i] = (BaseMotorController) this.get(i).create();
        }
        return new CtreMotorControllerGroup(controllers);
    }
}
