/*
 * Title:        EdgeCloudSim - Task
 * 
 * Description: 
 * Task adds app type, task submission location, mobile device id and host id
 * information to CloudSim's Cloudlet class.
 *               
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

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
	private int loadState=0;
	private int stateID;

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

	
	public void setSubmittedLocation(Location _submittedLocation){
		submittedLocation =_submittedLocation;
	}

	public void setAssociatedDatacenterId(int _datacenterId){
		datacenterId=_datacenterId;
	}
	
	public void setAssociatedHostId(int _hostIndex){
		hostIndex=_hostIndex;
	}

	public void setAssociatedVmId(int _vmIndex){
		vmIndex=_vmIndex;
	}
	
	public void setTaskType(int _type){
		type=_type;
	}

	public int getMobileDeviceId(){
		return mobileDeviceId;
	}
	
	public Location getSubmittedLocation(){
		return submittedLocation;
	}
	
	public int getAssociatedDatacenterId(){
		return datacenterId;
	}
	
	public int getAssociatedHostId(){
		return hostIndex;
	}

	public int getAssociatedVmId(){
		return vmIndex;
	}
	
	public int getTaskType(){
		return type;
	}

	public void setUploadHost(int host){uploadHost = host;}
	public  int getUploadHost(){return uploadHost;}


	public void setUploadPath(List<DefaultEdge> path){uploadPath = path;}
	public void setDownloadPath(List<DefaultEdge> path){downloadPath = path;}

	public  List<DefaultEdge> getUploadPath(){ return uploadPath;}
	public  List<DefaultEdge> getDownloadPath(){ return downloadPath;}

	public int getLoadState(){
		return loadState;
	}
	public void setloadState(boolean flag){
		if(flag) loadState++;
		else loadState=0;//结束
	}

	public void setStateID(int ID){stateID = ID;}
	public  int getStateID(){return stateID;}
}
