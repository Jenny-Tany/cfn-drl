/*
 * Title:        EdgeCloudSim - Network Model
 * 
 * Description: 
 * NetworkModel is an abstract class which is used for calculating the
 * network delay from device to device. For those who wants to add a
 * custom Network Model to EdgeCloudSim should extend this class and
 * provide a concreate instance via ScenarioFactory
 *               
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package edu.boun.edgecloudsim.network;

import edu.boun.edgecloudsim.edge_client.Task;
import edu.boun.edgecloudsim.network.topoGraph.topoGraph;
import org.jgrapht.graph.DefaultEdge;

import java.util.List;
import java.util.Map;

public abstract class NetworkModel {
	protected int numberOfMobileDevices;
	protected String simScenario;

	public NetworkModel(int _numberOfMobileDevices, String _simScenario){
		numberOfMobileDevices=_numberOfMobileDevices;
		simScenario = _simScenario;
	};
	/**
	* initializes custom network model
	*/
	public abstract void initialize();
	public abstract topoGraph getTopo();
	public abstract Map<String, Double> getEdgeBw();
	public abstract double[] getRouteScore(List<List<DefaultEdge>> routes,boolean haveHost);
	public abstract double[] getGraphScore();
	public abstract double[] getHostActive();
	public abstract double[] getEdgeActive();
	public abstract int getEdgeIndex(String t);
		/**
        * calculates the upload delay from source to destination device
        */
	public abstract double getUploadDelay(Task task) throws Exception;

    /**
    * calculates the download delay from source to destination device
    */
	public abstract double getDownloadDelay(Task task);
	public abstract void recordUsed(int host,boolean tag);
    /**
    * Mobile device manager should inform network manager about the network operation
    * This information may be important for some network delay models
    */
	public abstract void uploadStarted(List<DefaultEdge> handleList,DefaultEdge pathFinished,DefaultEdge pathStarted);
	public abstract double getEdgeDelay(DefaultEdge edge,double filesize);


	public abstract double getUploadDelayForTraining(int mobileDeviceId, int cloudDatacenterId, Task task);

	public abstract double getAvgBandwidth();

}
