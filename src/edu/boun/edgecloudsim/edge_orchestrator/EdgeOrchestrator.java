/*
 * Title:        EdgeCloudSim - Edge Orchestrator
 * 
 * Description: 
 * EdgeOrchestrator is an abstract class which is used for selecting VM
 * for each client requests. For those who wants to add a custom 
 * Edge Orchestrator to EdgeCloudSim should extend this class and provide
 * a concreate instance via ScenarioFactory
 *               
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package edu.boun.edgecloudsim.edge_orchestrator;

import edu.boun.edgecloudsim.applications.computingSim.CFNState;
import edu.boun.edgecloudsim.edge_client.Task;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.SimEntity;
import org.jgrapht.graph.DefaultEdge;

import java.util.List;

public abstract class EdgeOrchestrator extends SimEntity{
	protected String policy;
	protected String simScenario;

	public EdgeOrchestrator(String _policy, String _simScenario){
		super("EdgeOrchestrator");
		policy = _policy;
		simScenario = _simScenario;
	}
	
	/*
	 * initialize edge orchestrator if needed
	 */
	public abstract void initialize() throws Exception;
	public abstract void TrainAgent(Task task, double serviceTime);
	public abstract CFNState GetFeaturesForAgent(Task task);
	/*
	 * decides where to offload
	 */
	public abstract List<DefaultEdge> getRouteToOffload(Task task) throws Exception;
	
	/*
	 * returns proper VM from the edge orchestrator point of view
	 */
	public abstract Vm getVmToOffload(Task task, int deviceId);
}
