package edu.boun.edgecloudsim.edge_client;

import edu.boun.edgecloudsim.utils.Location;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.UtilizationModel;
import org.jgrapht.graph.DefaultEdge;

import java.util.List;

public class Task extends Cloudlet {
	private Location submittedLocation;
	private int type;
	private int mobileDeviceId;
	private int hostIndex;
	private int vmIndex;
	private int datacenterId;
	private List<DefaultEdge> uploadPath;
	private List<DefaultEdge> downloadPath;
	private int uploadHost;
	private int loadState = 0;
	private int stateID;


	// 用于记录任务开始执行时间
	private double startTime;

	// 用于记录任务执行完成时间
	private double finishTime;

	// 任务的截止时间
	private double deadline;

	// 已处理的数据大小
	private long processedSize;

	// 任务处理速度
	private double processingSpeed;

	// 任务上传延迟
	private double uploadDelay;

	public Task(int _mobileDeviceId, int cloudletId, long cloudletLength, int pesNumber,
				long cloudletFileSize, long cloudletOutputSize,
				UtilizationModel utilizationModelCpu,
				UtilizationModel utilizationModelRam,
				UtilizationModel utilizationModelBw) {
		super(cloudletId, cloudletLength, pesNumber, cloudletFileSize,
				cloudletOutputSize, utilizationModelCpu, utilizationModelRam,
				utilizationModelBw);

		mobileDeviceId = _mobileDeviceId;
	}

	public void setSubmittedLocation(Location _submittedLocation) {
		submittedLocation = _submittedLocation;
	}

	public void setAssociatedDatacenterId(int _datacenterId) {
		datacenterId = _datacenterId;
	}

	public void setAssociatedHostId(int _hostIndex) {
		hostIndex = _hostIndex;
	}

	public void setAssociatedVmId(int _vmIndex) {
		vmIndex = _vmIndex;
	}

	public void setTaskType(int _type) {
		type = _type;
	}

	public int getMobileDeviceId() {
		return mobileDeviceId;
	}

	public Location getSubmittedLocation() {
		return submittedLocation;
	}

	public int getAssociatedDatacenterId() {
		return datacenterId;
	}

	public int getAssociatedHostId() {
		return hostIndex;
	}

	public int getAssociatedVmId() {
		return vmIndex;
	}

	public int getTaskType() {
		return type;
	}

	public void setUploadHost(int host) {
		uploadHost = host;
	}

	public int getUploadHost() {
		return uploadHost;
	}

	public void setUploadPath(List<DefaultEdge> path) {
		uploadPath = path;
	}

	public void setDownloadPath(List<DefaultEdge> path) {
		downloadPath = path;
	}

	public List<DefaultEdge> getUploadPath() {
		return uploadPath;
	}

	public List<DefaultEdge> getDownloadPath() {
		return downloadPath;
	}

	public int getLoadState() {
		return loadState;
	}

	public void setloadState(boolean flag) {
		if (flag)
			loadState++;
		else
			loadState = 0; // 结束
	}

	public void setStateID(int ID) {
		stateID = ID;
	}

	public int getStateID() {
		return stateID;
	}

	// 假设任务复杂度与任务类型相关，这里简单返回任务类型作为复杂度
	public double getTaskComplexity() {
		return type;
	}

	// 在任务开始执行时设置开始时间
	public void startExecution() {
		startTime = System.currentTimeMillis();
	}

	// 获取任务开始时间
	public double getStartTime() {
		return startTime;
	}

	// 在任务执行完成时设置完成时间并计算执行时间
	public void finishExecution() {
		finishTime = System.currentTimeMillis();
	}

	// 获取任务执行时间（单位：秒，这里只是一个示例，你可以根据实际需求调整单位和计算方式）
	public double getExecutionTime() {
		if (startTime == 0 || finishTime == 0) {
			return 0; // 如果任务未开始或未完成，返回0
		}
		return (finishTime - startTime) / 1000.0;
	}

	// 设置任务的截止时间
	public void setDeadline(double deadline) {
		this.deadline = deadline;
	}

	// 获取任务的截止时间
	public double getDeadline() {
		return deadline;
	}

	// 设置已处理的数据大小
	public void setProcessedSize(long processedSize) {
		this.processedSize = processedSize;
	}

	// 获取已处理的数据大小
	public long getProcessedSize() {
		return processedSize;
	}

	// 设置任务处理速度
	public void setProcessingSpeed(double processingSpeed) {
		this.processingSpeed = processingSpeed;
	}

	// 获取任务处理速度
	public double getProcessingSpeed() {
		return processingSpeed;
	}

	// 设置任务上传延迟
	public void setUploadDelay(double uploadDelay) {
		this.uploadDelay = uploadDelay;
	}

	// 获取任务上传延迟
	public double getUploadDelay() {
		return uploadDelay;
	}
}