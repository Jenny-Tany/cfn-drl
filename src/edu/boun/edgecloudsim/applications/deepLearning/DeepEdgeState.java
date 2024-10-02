package edu.boun.edgecloudsim.applications.deepLearning;

import org.cloudbus.cloudsim.core.CloudSim;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.ArrayList;
import java.util.List;

public class DeepEdgeState {


    private List<Double> wanBw;
    private double wanDelay;
    private double manBw;
    private double manDelay; // normally this was not implemented in original DeepEdge
    private double taskReqCapacity;
    private double wlanID; // of mobile device
    private double availVMInWlanEdge; // implemented in original DeepEdge
    private ArrayList<Double> availVmInEdge; //new (14)
    private List<Double>  nearestEdgeHostId;
    private double delaySensitivity;
    private double mostAvailEdgeID; // implemented in original solution. It requires preprocessing.

    private double dataUplad;
    private double dataDownload;
    private double taskLength;
    private int featureCount;
    private int stateId;


    private double wlanDelay;


    private double[] activeManTaskCount;
    private double activeWanTaskCount;
    private double[] numberOfWlanOffloadedTask;
    private double numberOfManOffloadedTask;
    private double numberOfWanOffloadedTask;
    private double timestampOfState;

    private static int counterForId = 1; //initialized from 1 to provide correspondance with task ids
    private final double EPISODE_SIZE = 75000;

    public DeepEdgeState(int stateSize){
        this.timestampOfState = CloudSim.clock() / 300;
        this.stateId = counterForId;
        counterForId++;
        if (counterForId > EPISODE_SIZE){
            counterForId = 1;
        }

        this.featureCount = stateSize;
    }

    public INDArray getState(){
        INDArray stateInfo = Nd4j.zeros(1, this.featureCount);
//一行，特征数列，包装特征
        stateInfo.putScalar(0, getManDelay());
        stateInfo.putScalar(1, getTaskReqCapacity());
        stateInfo.putScalar(2, getWlanID());
        stateInfo.putScalar(3, getDelaySensitivity());
        stateInfo.putScalar(4, getNumberOfManOffloadedTask());
        stateInfo.putScalar(5, getNumberOfWanOffloadedTask());
        int count = 6;
        for(int i=0;i<wanBw.size();i++) stateInfo.putScalar(count+i, wanBw.get(i));
        count+=wanBw.size();//链路数+主机数

        for(int i=0;i<activeManTaskCount.length;i++) stateInfo.putScalar(count+i, activeManTaskCount[i]);
        count+=activeManTaskCount.length;//主机数

        for(int i=0;i<numberOfWlanOffloadedTask.length;i++) stateInfo.putScalar(count+i, numberOfWlanOffloadedTask[i]);
        count+=numberOfWlanOffloadedTask.length;//链路数+主机数

        for(int i=0;i<availVmInEdge.size();i++) stateInfo.putScalar(count+i, availVmInEdge.get(i));//主机数
        return stateInfo;
    }


    public void setManBw(double manBw) {
        this.manBw = manBw;
    }

    public int getStateId(){
        return this.stateId;
    }

    public List<Double> getWanBw() {
        return wanBw;
    }

    public double getWanDelay() {
        return wanDelay;
    }

    public double getManBw() {
        return manBw;
    }

    public double getManDelay() {
        return manDelay;
    }

    public double getTaskReqCapacity() {
        return taskReqCapacity;
    }

    public double getWlanID() {
        return wlanID;
    }

    public double getAvailVMInWlanEdge() {
        return availVMInWlanEdge;
    }

    public ArrayList<Double> getAvailVmInEdge() {
        return availVmInEdge;
    }

    public List<Double> getNearestEdgeHostId() {
        return nearestEdgeHostId;
    }

    public double getDelaySensitivity() {
        return delaySensitivity;
    }

    public double getMostAvailEdgeID() {
        return mostAvailEdgeID;
    }

    public double getDataUplad() {
        return dataUplad;
    }

    public double getDataDownload() {
        return dataDownload;
    }

    public double getTaskLength() {
        return taskLength;
    }

    public double getTimestampOfState() {
        return timestampOfState;
    }

    public double getWlanDelay() {
        return wlanDelay;
    }

    public void setWlanDelay(double wlanDelay) {
        this.wlanDelay = wlanDelay;
    }

    public void setWanDelay(double wanDelay) {
        this.wanDelay = wanDelay;
    }

    public void setManDelay(double manDelay) {
        this.manDelay = manDelay;
    }

    public double[] getActiveManTaskCount() {
        return activeManTaskCount;
    }

    public void setActiveManTaskCount(double[] activeManTaskCount) {
        this.activeManTaskCount = activeManTaskCount;
    }

    public double getActiveWanTaskCount() {
        return activeWanTaskCount;
    }

    public void setActiveWanTaskCount(double activeWanTaskCount) {
        this.activeWanTaskCount = activeWanTaskCount;
    }

    public double[] getNumberOfWlanOffloadedTask() {
        return numberOfWlanOffloadedTask;
    }

    public void setNumberOfWlanOffloadedTask(double[] numberOfWlanOffloadedTask) {
        this.numberOfWlanOffloadedTask = numberOfWlanOffloadedTask;
    }

    public double getNumberOfManOffloadedTask() {
        return numberOfManOffloadedTask;
    }

    public void setNumberOfManOffloadedTask(double numberOfManOffloadedTask) {
        this.numberOfManOffloadedTask = numberOfManOffloadedTask;
    }

    public double getNumberOfWanOffloadedTask() {
        return numberOfWanOffloadedTask;
    }

    public void setNumberOfWanOffloadedTask(double numberOfWanOffloadedTask) {
        this.numberOfWanOffloadedTask = numberOfWanOffloadedTask;
    }

    public void setWanBw(List<Double> wanBw) {
        this.wanBw = wanBw;
    }

    public void setTaskReqCapacity(double taskReqCapacity) {
        this.taskReqCapacity = taskReqCapacity;
    }

    public void setWlanID(double wlanID) {
        this.wlanID = wlanID;
    }

    public void setAvailVmInEdge(ArrayList<Double> availVmInEdge) {
        this.availVmInEdge = availVmInEdge;
    }

    public void setNearestEdgeHostId(List<Double> nearestEdgeHostId) {
        this.nearestEdgeHostId = nearestEdgeHostId;
    }

    public void setDelaySensitivity(double delaySensitivity) {
        this.delaySensitivity = delaySensitivity;
    }

}
