package edu.boun.edgecloudsim.utils.PPO;

import edu.boun.edgecloudsim.applications.computingSim.CFNState;

public class GameState {

    public double value;
    public double reward;
    public boolean terminal;
    public int playerNum;
    public int featureCount;
    private double taskReqCapacity,delaySensitivity,dataUpload,wlanID;
    private double[] scaledVm,scaledScores,action;

    public GameState(CFNState state) {
        this.featureCount = state.getFeatureCount();
        this.taskReqCapacity = state.getTaskReqCapacity();
        this.delaySensitivity = state.getDelaySensitivity();
        this.dataUpload = state.getDataUpload();
        this.wlanID = state.getWlanID();
        this.scaledVm = state.getScaledVm();
        this.scaledScores =state.getScaledScores();
    }


    public double[] getState(){
        double[] stateInfo = new double[featureCount];//一行，特征数列，包装特征
        stateInfo[0]= taskReqCapacity;
        stateInfo[1]= wlanID;
        stateInfo[2]= delaySensitivity;


        stateInfo[3]=dataUpload/1500;

        for(int i = 4; i < 4+scaledVm.length; i++ ) stateInfo[i]=scaledVm[i-4];
        for(int i = 20 ; i < 20+scaledScores.length ; i++ ) stateInfo[i]= scaledScores[i-20];
        return stateInfo;
    }

    public void setReward(double reward) {
        this.reward = reward;
    }

    public int getPlayerNum() {
        return playerNum;
    }

    public void setPlayerNum(int playerNum) {
        this.playerNum = playerNum;
    }

    public double getReward() {
        return reward;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public double getValue() {
        return value;
    }

    public void setPPOAction(double[] value) {
        this.action = value;
    }

    public double[] getPPOAction() {
        return action;
    }

    public double[] encoded(int playerNum) {//不使用
        return new double[0];
    }

    public double[] encoded() {
        return encoded(playerNum);
    }

    public void setTerminal(boolean terminal) {
        this.terminal = terminal;
    }

    public boolean isTerminal() {
        return terminal;
    }
}
