/*
 * Title:        EdgeCloudSim - Simulation Logger
 * 
 * Description: 
 * SimLogger is responsible for storing simulation events/results
 * in to the files in a specific format.
 * Format is decided in a way to use results in matlab efficiently.
 * If you need more results or another file format, you should modify
 * this class.
 * 
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package edu.boun.edgecloudsim.utils;

import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.core.SimSettings.NETWORK_DELAY_TYPES;
import edu.boun.edgecloudsim.utils.SimLogger.NETWORK_ERRORS;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

public class SimLogger {
	public static enum TASK_STATUS {
		CREATED, UPLOADING, PROCESSING, DOWNLOADING, COMLETED, REJECTED_DUE_TO_VM_CAPACITY, REJECTED_DUE_TO_BANDWIDTH, UNFINISHED_DUE_TO_BANDWIDTH, UNFINISHED_DUE_TO_MOBILITY
	}
	
	public static enum NETWORK_ERRORS {
		LAN_ERROR, MAN_ERROR, WAN_ERROR, NONE
	}

	private static boolean fileLogEnabled;
	private static boolean printLogEnabled;
	private String filePrefix;
	private String outputFolder;
	private Map<Integer, LogItem> taskMap;
	private LinkedList<VmLoadLogItem> vmLoadList;

	private static SimLogger singleton = new SimLogger();

	/*
	 * A private Constructor prevents any other class from instantiating.
	 */
	private SimLogger() {
		fileLogEnabled = false;
		printLogEnabled = false;
	}

	/* Static 'instance' method */
	public static SimLogger getInstance() {
		return singleton;
	}

	/*
	Baris Yamansavascilar Start
	 */
	public double getServiceTime(int taskId){
		if(taskMap.get(taskId).getStatus() == SimLogger.TASK_STATUS.COMLETED){
			return taskMap.get(taskId).getServiceTime();
		}
		return -9999; //which means an error
	}

	public double getProcessingTime(int taskId){
		if(taskMap.get(taskId).getStatus() == SimLogger.TASK_STATUS.COMLETED){
			return taskMap.get(taskId).getServiceTime() - taskMap.get(taskId).getNetworkDelay();
		}
		return -9999; //which means an error
	}

	/*
	Baris Yamansavascilar End
	 */
	public static void enableFileLog() {
		fileLogEnabled = true;
	}

	public static void enablePrintLog() {
		printLogEnabled = true;
	}

	public static boolean isFileLogEnabled() {
		return fileLogEnabled;
	}

	public static void disablePrintLog() {
		printLogEnabled = false;
	}

	private void appendToFile(BufferedWriter bw, String line) throws IOException {
		bw.write(line);
		bw.newLine();
	}

	public static void printLine(String msg) {
		if (printLogEnabled)
			System.out.println(msg);
	}

	public static void print(String msg) {
		if (printLogEnabled)
			System.out.print(msg);
	}

	public void simStarted(String outFolder, String fileName) {
		filePrefix = fileName;
		outputFolder = outFolder;
		taskMap = new HashMap<Integer, LogItem>();
		vmLoadList = new LinkedList<VmLoadLogItem>();
	}

	public void addLog(int taskId, int taskType, int taskLenght, int taskInputType,
			int taskOutputSize) {
		// printLine(taskId+"->"+taskStartTime);
		taskMap.put(taskId, new LogItem(taskType, taskLenght, taskInputType, taskOutputSize));
	}

	public void taskStarted(int taskId, double time) {
		taskMap.get(taskId).taskStarted(time);
	}

	public void setUploadDelay(int taskId, double delay, NETWORK_DELAY_TYPES delayType) {
		taskMap.get(taskId).setUploadDelay(delay, delayType);
	}

	public void setDownloadDelay(int taskId, double delay, NETWORK_DELAY_TYPES delayType) {
		taskMap.get(taskId).setDownloadDelay(delay, delayType);
	}
	
	public void taskAssigned(int taskId, int datacenterId, int hostId, int vmId, int vmType) {
		taskMap.get(taskId).taskAssigned(datacenterId, hostId, vmId, vmType);
	}

	public void taskExecuted(int taskId) {
		taskMap.get(taskId).taskExecuted();
	}

	public void taskEnded(int taskId, double time) {
		taskMap.get(taskId).taskEnded(time);
	}

	public void rejectedDueToVMCapacity(int taskId, double time, int vmType) {
		taskMap.get(taskId).taskRejectedDueToVMCapacity(time, vmType);
	}

	public void rejectedDueToBandwidth(int taskId, double time, int vmType, NETWORK_DELAY_TYPES delayType) {
		taskMap.get(taskId).taskRejectedDueToBandwidth(time, vmType, delayType);
	}

	public void failedDueToBandwidth(int taskId, double time, NETWORK_DELAY_TYPES delayType) {
		taskMap.get(taskId).taskFailedDueToBandwidth(time, delayType);
	}

	public void failedDueToMobility(int taskId, double time) {
		taskMap.get(taskId).taskFailedDueToMobility(time);
	}

	public void addVmUtilizationLog(double time, double loadOnEdge,double bwOnNetwork,double reward) {
		vmLoadList.add(new VmLoadLogItem(time, loadOnEdge,bwOnNetwork,reward));
	}


	public void simStopped() throws IOException {
		int numOfAppTypes = SimSettings.getInstance().getTaskLookUpTable().length;

		File successFile = null, failFile = null, vmLoadFile = null, locationFile = null;
		FileWriter successFW = null, failFW = null, vmLoadFW = null, locationFW = null;
		BufferedWriter successBW = null, failBW = null, vmLoadBW = null;

		// Save generic results to file for each app type. last index is average of all app types
		File[] genericFiles = new File[numOfAppTypes + 1];
		FileWriter[] genericFWs = new FileWriter[numOfAppTypes + 1];
		BufferedWriter[] genericBWs = new BufferedWriter[numOfAppTypes + 1];

		// extract following values for each app type. last index is average of all app types
		int[] uncompletedTask = new int[numOfAppTypes + 1];
		int[] completedTask = new int[numOfAppTypes + 1];
		int[] failedTask = new int[numOfAppTypes + 1];

		int logSize = vmLoadList.size()+1;
		double logInterval = SimSettings.getInstance().getVmLoadLogInterval();
		int[] completedTaskPeriod = new int[logSize];//某段时间完成的
		int[] uncompletedTaskPeriod = new int[logSize];
		int[] failedTaskPeriod = new int[logSize];

		double[] networkDelay = new double[numOfAppTypes + 1];

		double[] serviceTime = new double[numOfAppTypes + 1];

		double[] processingTime = new double[numOfAppTypes + 1];

		int[] failedTaskDueToVmCapacity = new int[numOfAppTypes + 1];

		double[] cost = new double[numOfAppTypes + 1];
		int[] failedTaskDuetoBw = new int[numOfAppTypes + 1];
		int[] failedTaskDuetoMobility = new int[numOfAppTypes + 1];

		// open all files and prepare them for write
		if (fileLogEnabled) {
			if (SimSettings.getInstance().getDeepFileLoggingEnabled()) {
				successFile = new File(outputFolder, filePrefix + "_SUCCESS.log");
				successFW = new FileWriter(successFile, true);
				successBW = new BufferedWriter(successFW);

				failFile = new File(outputFolder, filePrefix + "_FAIL.log");
				failFW = new FileWriter(failFile, true);
				failBW = new BufferedWriter(failFW);
			}

			vmLoadFile = new File(outputFolder, filePrefix+".log");
			vmLoadFW = new FileWriter(vmLoadFile, true);
			vmLoadBW = new BufferedWriter(vmLoadFW);

			for (int i = 0; i < numOfAppTypes + 1; i++) {
				String fileName = "ALL_APPS_GENERIC.log";

				if (i < numOfAppTypes) {
					// if related app is not used in this simulation, just discard it
					if (SimSettings.getInstance().getTaskLookUpTable()[i][0] == 0)
						continue;

					fileName = SimSettings.getInstance().getTaskName(i);
				}

				genericFiles[i] = new File(outputFolder, filePrefix + "_" + fileName+".log");
				genericFWs[i] = new FileWriter(genericFiles[i], true);
				genericBWs[i] = new BufferedWriter(genericFWs[i]);
			}

			if (SimSettings.getInstance().getDeepFileLoggingEnabled()) {
				appendToFile(successBW, "#auto generated file!");
				appendToFile(failBW, "#auto generated file!");
			}
		}
		// extract the result of each task and write it to the file if required
		for (Map.Entry<Integer, LogItem> entry : taskMap.entrySet()){
			Integer key = entry.getKey();
			LogItem value = entry.getValue();
//			if (value.getVmType() == -1)//虚拟机种类未定义
//				System.out.println("——————————————————————————error————————————————————————");

			if (value.isInWarmUpPeriod()) continue;
			int timePeriod = (int)(value.getTaskStartTime()/logInterval);//任务找到虚拟机了才有开始时间
			if (value.getStatus() == SimLogger.TASK_STATUS.COMLETED) {//统计某段时间内未完成任务数据
				completedTask[value.getTaskType()]++;
				completedTaskPeriod[timePeriod]++;
			}
			else if(value.getStatus() == SimLogger.TASK_STATUS.CREATED ||
					value.getStatus() == SimLogger.TASK_STATUS.UPLOADING ||
					value.getStatus() == SimLogger.TASK_STATUS.PROCESSING ||
					value.getStatus() == SimLogger.TASK_STATUS.DOWNLOADING)
			{
				uncompletedTask[value.getTaskType()]++;
				uncompletedTaskPeriod[timePeriod]++;
			}
			else {
				failedTask[value.getTaskType()]++;
				failedTaskPeriod[timePeriod]++;
			}

			if (value.getStatus() == SimLogger.TASK_STATUS.COMLETED) {
				cost[value.getTaskType()] += value.getCost();
				serviceTime[value.getTaskType()] += value.getServiceTime();
				networkDelay[value.getTaskType()] += value.getNetworkDelay();
				processingTime[value.getTaskType()] += (value.getServiceTime() - value.getNetworkDelay());


				if (fileLogEnabled && SimSettings.getInstance().getDeepFileLoggingEnabled())
					appendToFile(successBW, value.toString(key));
			} else if (value.getStatus() == SimLogger.TASK_STATUS.REJECTED_DUE_TO_VM_CAPACITY) {
				failedTaskDueToVmCapacity[value.getTaskType()]++;

				if (fileLogEnabled && SimSettings.getInstance().getDeepFileLoggingEnabled())
					appendToFile(failBW, value.toString(key));
			} else if (value.getStatus() == SimLogger.TASK_STATUS.REJECTED_DUE_TO_BANDWIDTH
					|| value.getStatus() == SimLogger.TASK_STATUS.UNFINISHED_DUE_TO_BANDWIDTH) {
				failedTaskDuetoBw[value.getTaskType()]++;

				if (fileLogEnabled && SimSettings.getInstance().getDeepFileLoggingEnabled())
					appendToFile(failBW, value.toString(key));
			} else if (value.getStatus() == SimLogger.TASK_STATUS.UNFINISHED_DUE_TO_MOBILITY) {
				failedTaskDuetoMobility[value.getTaskType()]++;
				if (fileLogEnabled && SimSettings.getInstance().getDeepFileLoggingEnabled())
					appendToFile(failBW, value.toString(key));
			}
		}

		// calculate total values
		uncompletedTask[numOfAppTypes] = IntStream.of(uncompletedTask).sum();
		completedTask[numOfAppTypes] = IntStream.of(completedTask).sum();
		failedTask[numOfAppTypes] = IntStream.of(failedTask).sum();

		networkDelay[numOfAppTypes] = DoubleStream.of(networkDelay).sum();
		serviceTime[numOfAppTypes] = DoubleStream.of(serviceTime).sum();
		processingTime[numOfAppTypes] = DoubleStream.of(processingTime).sum();

		failedTaskDueToVmCapacity[numOfAppTypes] = IntStream.of(failedTaskDueToVmCapacity).sum();

		cost[numOfAppTypes] = DoubleStream.of(cost).sum();
		failedTaskDuetoBw[numOfAppTypes] = IntStream.of(failedTaskDuetoBw).sum();
		failedTaskDuetoMobility[numOfAppTypes] = IntStream.of(failedTaskDuetoMobility).sum();
//算力资源比其他资源稀缺得多，报文封装成本
		// calculate server load
		double totalVmLoadOnEdge = 0;
		int t = 0;
		for (VmLoadLogItem entry : vmLoadList) {
			totalVmLoadOnEdge += entry.getEdgeLoad();
			if (fileLogEnabled){//隔一段时间就会记录一次
				appendToFile(vmLoadBW, entry.toString() + SimSettings.DELIMITER+
						completedTaskPeriod[t] + SimSettings.DELIMITER+
						failedTaskPeriod[t] + SimSettings.DELIMITER+
						uncompletedTaskPeriod[t] + SimSettings.DELIMITER+
						(completedTaskPeriod[t]-failedTaskPeriod[t]) );//成功任务-失败任务=奖励
				t++;
			}
		}

		if (fileLogEnabled) {
			for (int i = 0; i < numOfAppTypes + 1; i++) {
				if (i < numOfAppTypes) {
					// if related app is not used in this simulation, just discard it
					if (SimSettings.getInstance().getTaskLookUpTable()[i][0] == 0)
						continue;
				}

				// check if the divisor is zero in order to avoid division by zero problem
				double _serviceTime = (completedTask[i] == 0) ? 0.0 : (serviceTime[i] / (double) completedTask[i]);
//				double _networkDelay = (completedTask[i] == 0) ? 0.0 : (networkDelay[i] / ((double) completedTask[i] - (double)completedTaskOnMobile[i]));
				double _networkDelay = (completedTask[i] == 0) ? 0.0 : (networkDelay[i] / ((double) completedTask[i]));
				double _processingTime = (completedTask[i] == 0) ? 0.0 : (processingTime[i] / (double) completedTask[i]);
				double _vmLoadOnEdge = (vmLoadList.size() == 0) ? 0.0 : (totalVmLoadOnEdge / (double) vmLoadList.size());
				double _cost = (completedTask[i] == 0) ? 0.0 : (cost[i] / (double) completedTask[i]);

				// write generic results
				// check if the divisor is zero in order to avoid division by zero problem
				String genericResult1 = Integer.toString(completedTask[i]) + SimSettings.DELIMITER//完成任务
						+ Integer.toString(failedTask[i]) + SimSettings.DELIMITER//失败任务
						+ Integer.toString(uncompletedTask[i]) + SimSettings.DELIMITER//未完成任务
						+ Integer.toString(failedTaskDuetoBw[i]) + SimSettings.DELIMITER//由于带宽未完成
						+ Integer.toString(failedTaskDueToVmCapacity[i]) + SimSettings.DELIMITER//由于算力未完成
						+ Integer.toString(failedTaskDuetoMobility[i])+ SimSettings.DELIMITER//由于移动未完成（正常情况为0）
						+ Double.toString(_serviceTime) + SimSettings.DELIMITER//服务时间
						+ Double.toString(_processingTime) + SimSettings.DELIMITER//处理时间 = 服务时间-网络时延
						+ Double.toString(_networkDelay) + SimSettings.DELIMITER//网络时延
						+ Double.toString(_cost) + SimSettings.DELIMITER//暂无
						+ Double.toString(_vmLoadOnEdge) + SimSettings.DELIMITER;//边缘总负载

				appendToFile(genericBWs[i], genericResult1);
			}

			// close open files
			if (SimSettings.getInstance().getDeepFileLoggingEnabled()) {
				successBW.close();
				failBW.close();
			}
			vmLoadBW.close();
			for (int i = 0; i < numOfAppTypes + 1; i++) {
				if (i < numOfAppTypes) {
					// if related app is not used in this simulation, just discard it
					if (SimSettings.getInstance().getTaskLookUpTable()[i][0] == 0)
						continue;
				}
				genericBWs[i].close();
			}
		}

		// clear related collections (map list etc.)
		taskMap.clear();
		vmLoadList.clear();
	}

}

class VmLoadLogItem {
	private double time;
	private double vmLoadOnEdge;
	private double bwOnNetwork;
	private double reward;

	VmLoadLogItem(double _time, double _vmLoadOnEdge,double _bwOnNetwork,double _reward) {
		time = _time;
		vmLoadOnEdge = _vmLoadOnEdge;
		bwOnNetwork = _bwOnNetwork;
		reward = _reward;
	}

	public double getEdgeLoad() {
		return vmLoadOnEdge;
	}
	
	public String toString() {
		return time + 
				SimSettings.DELIMITER + vmLoadOnEdge +
				SimSettings.DELIMITER + bwOnNetwork +
				SimSettings.DELIMITER + reward
				;
	}
}

class LogItem {
	private SimLogger.TASK_STATUS status;
	private SimLogger.NETWORK_ERRORS networkError;
	private int datacenterId;
	private int hostId;
	private int vmId;
	private int vmType=-1;
	private int taskType;
	private int taskLenght;
	private int taskInputType;
	private int taskOutputSize;
	private double taskStartTime;
	private double taskEndTime;
	private double lanUploadDelay;
	private double manUploadDelay;
	private double wanUploadDelay;
	private double lanDownloadDelay;
	private double manDownloadDelay;
	private double wanDownloadDelay;
	private double bwCost;
	private double cpuCost;
	private boolean isInWarmUpPeriod;

	LogItem(int _taskType, int _taskLenght, int _taskInputType, int _taskOutputSize) {
		taskType = _taskType;
		taskLenght = _taskLenght;
		taskInputType = _taskInputType;
		taskOutputSize = _taskOutputSize;
		networkError = NETWORK_ERRORS.NONE;
		status = SimLogger.TASK_STATUS.CREATED;
		taskEndTime = 0;
	}
	
	public void taskStarted(double time) {
		taskStartTime = time;
		status = SimLogger.TASK_STATUS.UPLOADING;
		
		if (time < SimSettings.getInstance().getWarmUpPeriod())
			isInWarmUpPeriod = true;
		else
			isInWarmUpPeriod = false;
	}
	
	public void setUploadDelay(double delay, NETWORK_DELAY_TYPES delayType) {
		if(delayType == NETWORK_DELAY_TYPES.WLAN_DELAY)
			lanUploadDelay = delay;
		else if(delayType == NETWORK_DELAY_TYPES.MAN_DELAY)
			manUploadDelay = delay;
		else if(delayType == NETWORK_DELAY_TYPES.WAN_DELAY)
			wanUploadDelay = delay;
	}
	
	public void setDownloadDelay(double delay, NETWORK_DELAY_TYPES delayType) {
		if(delayType == NETWORK_DELAY_TYPES.WLAN_DELAY)
			lanDownloadDelay = delay;
		else if(delayType == NETWORK_DELAY_TYPES.MAN_DELAY)
			manDownloadDelay = delay;
		else if(delayType == NETWORK_DELAY_TYPES.WAN_DELAY)
			wanDownloadDelay = delay;
	}
	
	public void taskAssigned(int _datacenterId, int _hostId, int _vmId, int _vmType) {//任务完成
		status = SimLogger.TASK_STATUS.PROCESSING;
		datacenterId = _datacenterId;
		hostId = _hostId;
		vmId = _vmId;
		vmType = _vmType;
	}

	public void taskExecuted() {
		status = SimLogger.TASK_STATUS.DOWNLOADING;
	}

	public void taskEnded(double time) {
		taskEndTime = time;
		status = SimLogger.TASK_STATUS.COMLETED;
	}

	public void taskRejectedDueToVMCapacity(double time, int _vmType) {//算力不够
		vmType = _vmType;
		taskStartTime = time;
		taskEndTime = time;
		status = SimLogger.TASK_STATUS.REJECTED_DUE_TO_VM_CAPACITY;
	}

	public void taskRejectedDueToBandwidth(double time, int _vmType, NETWORK_DELAY_TYPES delayType) {//任务上传带宽不够
		vmType = _vmType;
		taskEndTime = time;
		status = SimLogger.TASK_STATUS.REJECTED_DUE_TO_BANDWIDTH;

	}

	public void taskFailedDueToBandwidth(double time, NETWORK_DELAY_TYPES delayType) {//任务返回带宽不够
		taskEndTime = time;
		status = SimLogger.TASK_STATUS.UNFINISHED_DUE_TO_BANDWIDTH;

	}

	public void taskFailedDueToMobility(double time) {//一般不发生，忽略移动性
		taskEndTime = time;
		status = SimLogger.TASK_STATUS.UNFINISHED_DUE_TO_MOBILITY;
	}

	public void setCost(double _bwCost, double _cpuCos) {
		bwCost = _bwCost;
		cpuCost = _cpuCos;
	}

	public boolean isInWarmUpPeriod() {
		return isInWarmUpPeriod;
	}

	public double getCost() {
		return bwCost + cpuCost;
	}

	public double getNetworkUploadDelay(NETWORK_DELAY_TYPES delayType) {
		double result = 0;
		if(delayType == NETWORK_DELAY_TYPES.WLAN_DELAY)
			result = lanUploadDelay;
		else if(delayType == NETWORK_DELAY_TYPES.MAN_DELAY)
			result = manUploadDelay;
		else if(delayType == NETWORK_DELAY_TYPES.WAN_DELAY)
			result = wanUploadDelay;
		
		return result;
	}

	public double getNetworkDownloadDelay(NETWORK_DELAY_TYPES delayType) {
		double result = 0;
		if(delayType == NETWORK_DELAY_TYPES.WLAN_DELAY)
			result = lanDownloadDelay;
		else if(delayType == NETWORK_DELAY_TYPES.MAN_DELAY)
			result = manDownloadDelay;
		else if(delayType == NETWORK_DELAY_TYPES.WAN_DELAY)
			result = wanDownloadDelay;
		
		return result;
	}
	
	public double getNetworkDelay(NETWORK_DELAY_TYPES delayType){
		double result = 0;
		if(delayType == NETWORK_DELAY_TYPES.WLAN_DELAY)
			result = lanDownloadDelay + lanUploadDelay;
		else if(delayType == NETWORK_DELAY_TYPES.MAN_DELAY)
			result = manDownloadDelay + manUploadDelay;
		else if(delayType == NETWORK_DELAY_TYPES.WAN_DELAY)
			result = wanDownloadDelay + wanUploadDelay;
		
		return result;
	}
	
	public double getNetworkDelay(){
		return  lanUploadDelay +
				manUploadDelay +
				wanUploadDelay +
				lanDownloadDelay +
				manDownloadDelay +
				wanDownloadDelay;
	}

	public double getTaskStartTime() {return taskStartTime;}

	public double getServiceTime() {
		return taskEndTime - taskStartTime;
	}

	public SimLogger.TASK_STATUS getStatus() {
		return status;
	}

	public SimLogger.NETWORK_ERRORS getNetworkError() {
		return networkError;
	}
	
	public int getVmType() {
		return vmType;
	}

	public int getTaskType() {
		return taskType;
	}

	public String toString(int taskId) {
		String result = taskId + SimSettings.DELIMITER + datacenterId + SimSettings.DELIMITER + hostId
				+ SimSettings.DELIMITER + vmId + SimSettings.DELIMITER + vmType + SimSettings.DELIMITER + taskType
				+ SimSettings.DELIMITER + taskLenght + SimSettings.DELIMITER + taskInputType + SimSettings.DELIMITER
				+ taskOutputSize + SimSettings.DELIMITER + taskStartTime + SimSettings.DELIMITER + taskEndTime
				+ SimSettings.DELIMITER;

		if (status == SimLogger.TASK_STATUS.COMLETED){
			result += getNetworkDelay() + SimSettings.DELIMITER;
			result += getNetworkDelay(NETWORK_DELAY_TYPES.WLAN_DELAY) + SimSettings.DELIMITER;
			result += getNetworkDelay(NETWORK_DELAY_TYPES.MAN_DELAY) + SimSettings.DELIMITER;
			result += getNetworkDelay(NETWORK_DELAY_TYPES.WAN_DELAY);
		}
		else if (status == SimLogger.TASK_STATUS.REJECTED_DUE_TO_VM_CAPACITY)
			result += "1"; // failure reason 1
		else if (status == SimLogger.TASK_STATUS.REJECTED_DUE_TO_BANDWIDTH)
			result += "2"; // failure reason 2
		else if (status == SimLogger.TASK_STATUS.UNFINISHED_DUE_TO_BANDWIDTH)
			result += "3"; // failure reason 3
		else if (status == SimLogger.TASK_STATUS.UNFINISHED_DUE_TO_MOBILITY)
			result += "4"; // failure reason 4
		else
			result += "0"; // default failure reason
		return result;
	}
}
