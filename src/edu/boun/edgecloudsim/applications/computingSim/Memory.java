package edu.boun.edgecloudsim.applications.computingSim;

public class Memory {


    private CFNState state;
    private CFNState nextState;
    private double value;
    private int action;
    private boolean isDone;

    public Memory(CFNState state, CFNState nextState, double value, int action, boolean isDone){
        this.state = state;
        this.nextState = nextState;
        this.value = value;
        this.action = action;
        this.isDone = isDone;
    }

    public CFNState getState() {
        return state;
    }

    public CFNState getNextState() {
        return nextState;
    }

    public double getValue() {
        return value;
    }

    public int getAction() {
        return action;
    }

    public boolean isDone() {
        return isDone;
    }

    public void setState(CFNState state) {
        this.state = state;
    }

    public void setNextState(CFNState nextState) {
        this.nextState = nextState;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public void setAction(int action) {
        this.action = action;
    }

    public void setDone(boolean done) {
        isDone = done;
    }

}
