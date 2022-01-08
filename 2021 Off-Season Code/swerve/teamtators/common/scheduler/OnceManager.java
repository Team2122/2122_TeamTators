package org.teamtators.common.scheduler;

import java.util.ArrayList;

public class OnceManager implements RobotStateListener {

    public enum OnceType {
        OnDeploy,
        OnEnable // May Do Later
    }

    private Scheduler scheduler;
    private ArrayList<Command> onces;

    public OnceManager(Scheduler scheduler, ArrayList<Command> onces) {
        this.scheduler = scheduler;
        this.onces = onces;
    }

    @Override
    public void onEnterRobotState(RobotState state) {
        for (int i = 0; i < onces.size(); i++) {
            if (onces.get(i).isValidInState(state) && state != RobotState.AUTONOMOUS) {
                scheduler.startCommand(onces.get(i));
                onces.remove(onces.get(i));
                i--;
            }
        }
        if (onces.size() > 0) {
            System.out.println("Onces Running Again");
        }
    }

}
