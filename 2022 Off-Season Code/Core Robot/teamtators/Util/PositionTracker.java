package org.teamtators.Util;

public class PositionTracker {

    private Vector pos;
    private Vector previousVel;
    private Vector currentVel;

    public PositionTracker(){
        pos = new Vector();
        previousVel = new Vector();
        currentVel = new Vector();
    }

    public Vector updateWithTime(Vector velocity, double timeDelta){
        currentVel.set(velocity);
        previousVel.add(currentVel);
        previousVel.scale(.5);

        previousVel.scale(timeDelta);
        pos.add(previousVel);
        previousVel.set(currentVel);
        return pos;
    }

    public Vector getPosition(){
        return pos;
    }

    public Vector setPosition(Vector newPosition){
        pos.setXY(newPosition.getX(), newPosition.getY());
        return pos;
    }

    @Override
    public String toString() {
        return "PositionTracker{" + "pos=" + pos + '}';
    }
}