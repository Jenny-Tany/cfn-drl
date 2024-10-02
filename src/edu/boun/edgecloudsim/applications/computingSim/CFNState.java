package edu.boun.edgecloudsim.applications.computingSim;

import org.cloudbus.cloudsim.core.CloudSim;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.ArrayList;
import java.util.Arrays;

public class CFNState {


    private boolean dataScale = true;
    private double wanBw;
    private double wanDelay;
    private double manBw;
    private double manDelay; // normally this was not implemented in original DeepEdge
    private double taskReqCapacity;
    private double wlanID; // of mobile device
    private double availVMInWlanEdge; // implemented in original DeepEdge
    private ArrayList<Double> availVmInEdge; //new (14)
    private double nearestEdgeHostId;
    private double delaySensitivity;
    private double mostAvailEdgeID; // implemented in original solution. It requires preprocessing.

    private double dataUpload;
    private double dataDownload;
    private double taskLength;
    private int stateId;
    private double[] routesScore;


    private double wlanDelay;


    private double activeManTaskCount;
    private double activeWanTaskCount;
    private double numberOfWlanOffloadedTask;
    private double numberOfManOffloadedTask;
    private double numberOfWanOffloadedTask;
    private double timestampOfState;
    private double[] scaledVm,scaledScores;
    public int featureCount;
    private static int counterForId = 0; //initialized from 1 to provide correspondance with task ids
    private final double EPISODE_SIZE = 75000;

    public CFNState(){
        this.timestampOfState = CloudSim.clock() / 300;
        this.stateId = counterForId;
        counterForId++;
//        if (counterForId > EPISODE_SIZE)
//            counterForId = 1;
//        featureCount= t==64 ? 52 : 36+SimSettings.getInstance().getActionSize();//?52
//        featureCount= t==64 ? 57 : 41+SimSettings.getInstance().getActionSize();//?52
    }

    public INDArray getState(){
        featureCount = 4+availVmInEdge.size()+routesScore.length;
        INDArray stateInfo = Nd4j.zeros(1, this.featureCount);//一行，特征数列，包装特征
        stateInfo.putScalar(0, getTaskReqCapacity());
        stateInfo.putScalar(1, getWlanID());
        stateInfo.putScalar(2, getDelaySensitivity());

        if(dataScale){
            stateInfo.putScalar(3, getDataUpload()/1500);
            double[] array = new double[availVmInEdge.size()];
            for (int i = 0; i < availVmInEdge.size(); i++) array[i] = availVmInEdge.get(i);
            scaledVm = todataNom(array);
            int t1 = 4;//4
            for(int i = t1; i < t1+availVmInEdge.size(); i++ ) stateInfo.putScalar(i, scaledVm[i-t1]);

            int t2 = t1+availVmInEdge.size();
            scaledScores = todataNom(routesScore);
            for(int i = t2 ; i < t2+scaledScores.length ; i++ ) stateInfo.putScalar(i, scaledScores[i-t2]);
        }else{
            stateInfo.putScalar(3, getDataUpload());
            for(int i = 4; i < 4+availVmInEdge.size(); i++ )
                stateInfo.putScalar(i, getAvailVmInEdge().get(i-4));
            for(int i = 20 ; i < 20+routesScore.length ; i++ ) stateInfo.putScalar(i, routesScore[i-20]);
        }
        return stateInfo;
    }

    private double[] todataScale(double[] data){
        double[] scaledData = new double[data.length];
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;

        // 找到最大值和最小值
        for (double score : data) {
            min = Math.min(min, score);
            max = Math.max(max, score);
        }
        for (int i = 0; i < data.length; i++)  scaledData[i] = (max - data[i]) / (max - min);
//            scaledScores[i] = (min - routesScore[i]) / (max - min);
        return scaledData;

    }

    private double[] todataNom(double[] data){
        double mean = Arrays.stream(data).average().orElseThrow();

        double sumOfSquares = 0.0;
        for (double value : data)
            sumOfSquares += Math.pow(value - mean, 2);

        double standardDeviation = Math.sqrt(sumOfSquares / (data.length - 1));
//        standardDeviation = standardDeviation<1e-10?0.01:standardDeviation;
        if(standardDeviation>1e-10){//如果小于说明data数字全部相等
            double[] standardizedData = new double[data.length];
            for (int i = 0; i < data.length; i++)  standardizedData[i] = (data[i] - mean) / standardDeviation;
            return standardizedData;
        }else return data;


    }

    public void setRoutesScore(double[] routesScore){this.routesScore = routesScore;}

    public void setManBw(double manBw) {
        this.manBw = manBw;
    }

    public int getStateId(){
        return this.stateId;
    }

    public double getWanBw() {
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
    public int getFeatureCount(){return featureCount;}

    public double[] getScaledVm(){
        double[] array = new double[availVmInEdge.size()];
        for (int i = 0; i < availVmInEdge.size(); i++) array[i] = availVmInEdge.get(i);
        scaledVm = todataNom(array);
        return scaledVm;
    }

    public double[] getScaledScores(){
        scaledScores = todataNom(routesScore);
        return scaledScores;
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

    public double getNearestEdgeHostId() {
        return nearestEdgeHostId;
    }

    public double getDelaySensitivity() {
        return delaySensitivity;
    }

    public double getMostAvailEdgeID() {
        return mostAvailEdgeID;
    }

    public double getDataUpload() {
        return dataUpload;
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

    public double getActiveManTaskCount() {
        return activeManTaskCount;
    }

    public void setActiveManTaskCount(double activeManTaskCount) {
        this.activeManTaskCount = activeManTaskCount;
    }

    public double getActiveWanTaskCount() {
        return activeWanTaskCount;
    }

    public void setActiveWanTaskCount(double activeWanTaskCount) {
        this.activeWanTaskCount = activeWanTaskCount;
    }

    public double getNumberOfWlanOffloadedTask() {
        return numberOfWlanOffloadedTask;
    }

    public void setNumberOfWlanOffloadedTask(double numberOfWlanOffloadedTask) {
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

    public void setWanBw(double wanBw) {
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

    public void setNearestEdgeHostId(double nearestEdgeHostId) {
        this.nearestEdgeHostId = nearestEdgeHostId;
    }

    public void setDelaySensitivity(double delaySensitivity) {
        this.delaySensitivity = delaySensitivity;
    }

    public void setDataUpload(double dataUpload) {
        this.dataUpload = dataUpload;
    }
}
