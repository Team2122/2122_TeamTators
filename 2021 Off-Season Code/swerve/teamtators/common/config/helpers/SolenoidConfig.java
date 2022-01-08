package org.teamtators.common.config.helpers;

import edu.wpi.first.wpilibj.Solenoid;

/**
 * Example mapping:
 * exampleSolenoid: {channel: 0}
 */

public class SolenoidConfig implements ConfigHelper<Solenoid> {
    private int channel;
    private int canID = 0;

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public void setCanID(int canID){
        this.canID = canID;
    }
    public int getCanID(){
        return canID;
    }

    public Solenoid create() {
        return new Solenoid(canID, channel);
    }
}
