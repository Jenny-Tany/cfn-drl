package edu.boun.edgecloudsim.applications.deepLearning;

public class MemoryItem {


    private edu.boun.edgecloudsim.applications.deepLearning.DeepEdgeState state;
    private edu.boun.edgecloudsim.applications.deepLearning.DeepEdgeState nextState;
    private double value;
    private int action;
    private boolean isDone;

    public MemoryItem(edu.boun.edgecloudsim.applications.deepLearning.DeepEdgeState state, edu.boun.edgecloudsim.applications.deepLearning.DeepEdgeState nextState, double value, int action, boolean isDone){
        this.state = state;
        this.nextState = nextState;
        this.value = value;
        this.action = action;
        this.isDone = isDone;
    }

    public edu.boun.edgecloudsim.applications.deepLearning.DeepEdgeState getState() {
        return state;
    }

    public edu.boun.edgecloudsim.applications.deepLearning.DeepEdgeState getNextState() {
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

    public void setState(edu.boun.edgecloudsim.applications.deepLearning.DeepEdgeState state) {
        this.state = state;
    }

    public void setNextState(edu.boun.edgecloudsim.applications.deepLearning.DeepEdgeState nextState) {
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
