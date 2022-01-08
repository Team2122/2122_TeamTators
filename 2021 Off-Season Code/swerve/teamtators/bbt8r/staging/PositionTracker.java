package org.teamtators.bbt8r.staging;

public class PositionTracker {

    private Vector pos;

    public PositionTracker(){
        pos = new Vector();
    }

    public Vector updateWithTime(Vector velocity, double timeDelta){
        return pos.add(velocity.scale(timeDelta));
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