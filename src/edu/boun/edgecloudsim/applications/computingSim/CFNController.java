package edu.boun.edgecloudsim.applications.computingSim;

import edu.boun.edgecloudsim.applications.deepLearning.DeepMobileDeviceManager;
import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.edge_client.CpuUtilizationModel_Custom;
import edu.boun.edgecloudsim.edge_client.Task;
import edu.boun.edgecloudsim.edge_orchestrator.EdgeOrchestrator;
import edu.boun.edgecloudsim.edge_server.EdgeVM;
import edu.boun.edgecloudsim.network.FCL_definition;
import edu.boun.edgecloudsim.network.topoGraph.topoGraph;
import edu.boun.edgecloudsim.utils.PPO.GameState;
import edu.boun.edgecloudsim.utils.PPO.PPOPlayer;
import edu.boun.edgecloudsim.utils.SimLogger;
import net.sourceforge.jFuzzyLogic.FIS;
import org.antlr.runtime.RecognitionException;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEvent;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.jgrapht.graph.DefaultEdge;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.io.File;
import java.io.IOException;
import java.util.*;

// 在模拟环境中协调边缘设备的任务卸载
// 该类涉及了不同的卸载决策策略（例如 DDQN、PPO、Shortest Path 等），并使用深度学习和模糊逻辑等技术来优化任务的分配
public class CFNController extends EdgeOrchestrator {


    private int numberOfHost; //used by load balancer
    private FIS fis1 = null;
    private FIS fis2 = null;
    private FIS fis3 = null;

    private MultiLayerNetwork m_agent = null;
    private ArrayList<Double> trainHost=new ArrayList<>(),resetHost= new ArrayList<>();
    private double[] trainEdge,resetEdge;
    private boolean agentTraining = true;
    private final int EPISODE_SIZE = 100000;//75%(75000)
    private double totalReward = 0;
    private HashMap<Integer, HashMap<Integer, Integer>> taskToStateActionPair = new HashMap<>();
    private HashMap<Integer, Memory> stateIDToMemoryItemPair = new HashMap<>();
    private int routeNodeNum,hostNum,edgeNum;
    public CFNController(String _policy, String _simScenario) {
        super(_policy, _simScenario);
    }
    private DeepMobileDeviceManager deepEdgeManager;

    @Override
    public void initialize() throws Exception {
        numberOfHost=SimSettings.getInstance().getNumOfEdgeHosts();
        try {
            fis1 = FIS.createFromString(FCL_definition.fclDefinition1, false);
            fis2 = FIS.createFromString(FCL_definition.fclDefinition2, false);
            fis3 = FIS.createFromString(FCL_definition.fclDefinition3, false);
        } catch (RecognitionException e) {
            SimLogger.printLine("Cannot generate FIS! Terminating simulation...");
            e.printStackTrace();
            System.exit(0);
        }
        routeNodeNum = CFNNetworkingModel.getInstance().getTopo().getNodeCount();
        hostNum = CFNNetworkingModel.getInstance().getTopo().getHostCount();
        edgeNum = CFNNetworkingModel.getInstance().getTopo().getEdgeCount()+hostNum;
        if (policy.equals("DDQN")||policy.equals("PPO")){
            for(int i=0;i<hostNum;i++){
                trainHost.add(0.);
                resetHost.add(0.);
            }
            trainEdge = new double[edgeNum];
            resetEdge = new double[edgeNum];
            if(!agentTraining){
                try {
                    final String absolutePath = "sim_results/use/data_size/5/DDQN/DDQNModel";
                    m_agent = MultiLayerNetwork.load(new File(absolutePath), false);//加载已训练好的模型
                }catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else if(policy.equals("deepEdge")){
            deepEdgeManager = new DeepMobileDeviceManager(hostNum,edgeNum);

        }else agentTraining = false;
    }


    /*
     * (non-Javadoc)
     * @see edu.boun.edgecloudsim.edge_orchestrator.EdgeOrchestrator#getDeviceToOffload(edu.boun.edgecloudsim.edge_client.Task)
     *
     * It is assumed that the edge orchestrator app is running on the edge devices in a distributed manner
     */
    @Override
    public List<DefaultEdge> getRouteToOffload(Task task) throws Exception {
        List<DefaultEdge> route=null;
        int wlanID = task.getSubmittedLocation().getServingWlanId(),nextHopId=-1;
        topoGraph topo = SimManager.getInstance().getNetworkModel().getTopo();
        //RODO: return proper host ID
        if(policy.equals("DDQN")){

            if (agentTraining){//如果训练dqn
                DQNAgent agent = DQNAgent.getInstance();
                CFNState currentState = GetFeaturesForAgent(task);
                int action = agent.DoAction(currentState);


                //plan A:4个动作使用一条路由，到达不同host
                if(SimSettings.getInstance().getActionSize()==64){
                    int routeindex = action/4 , hostindex = action%4,dCindex = routeindex/4;
                    nextHopId = topo.getHostFromIndex(dCindex,hostindex);//第dc个数据中心的第host个主机
                    route = topo.getNodesKRoute(wlanID).get(routeindex);
                    DefaultEdge edge = topo.getDcEdge(dCindex,nextHopId);
                    route.add(edge);//把到主机的路由加上
                }else if(SimSettings.getInstance().getActionSize()==16){
                    nextHopId = action+routeNodeNum;
                    route = topo.getShotestPath(wlanID,nextHopId);
                }
                else {//6||16,nsfnet:60修改k
                    route = topo.getKRoute(wlanID,SimSettings.getInstance().getActionSize()).get(action);
                    nextHopId = topo.getEdgeHost(route.get(route.size()-1));
                }


                if (task.getCloudletId() == EPISODE_SIZE){//云任务数量达到迭代完成
//                    isDone = true;
//                    agentTraining = false;
                    System.out.println("Total Reward devicemanager: " + totalReward);
                }

                // After work
                Memory memoryItem;
                ArrayList<Double> edgeList = getAvailVmInEdge();//平均容量
                if ( edgeList.get(nextHopId-routeNodeNum) == 0){//卸载边缘且剩余算力为0
                    memoryItem = new Memory(currentState, null, -1, action, true);//算力不够
                    resetState();//重置状态，释放带宽和算力，在完成任务（路由）后，把数字累加到list，然后再reset的时候加上
                }else{
                    memoryItem = new Memory(currentState, null, -10, action, false);//先不记录奖励
                    //planC修改状态
                    changeState(route,nextHopId-routeNodeNum,task);
                }

                stateIDToMemoryItemPair.put(currentState.getStateId(), memoryItem);// 查找前一状态，为当前状态存储前一状态，等待后续记录
                if (stateIDToMemoryItemPair.get(currentState.getStateId()-1) != null){
                    Memory previousMemoryItem = stateIDToMemoryItemPair.get(currentState.getStateId()-1);
                    previousMemoryItem.setNextState(currentState); //pass by reference!! It is crucial!!
                }
            }
            else{//不训练，只调用
                CFNState currentState = GetFeaturesForAgent(task);//做出决策
                INDArray output = m_agent.output(currentState.getState());
                int action = output.argMax().getInt();

                int routeindex = action/4 , hostindex = action%4,dCindex = routeindex/4;//从wlanID出发的第routeindex条路由,到达4个host
                nextHopId = topo.getHostFromIndex(dCindex,hostindex);//第dc个数据中心的第host个主机
                route = topo.getNodesKRoute(wlanID).get(routeindex);
                DefaultEdge edge = topo.getDcEdge(dCindex,nextHopId);
                route.add(edge);//把到主机的路由加上
            }
        }
        else if(policy.equals("PPO")){
            PPOPlayer agent = PPOPlayer.getInstance();
            CFNState currentState = GetFeaturesForAgent(task);
            currentState.getState();
            GameState state = new GameState(currentState);
            double[] actions = agent.actAndRecord(state,currentState.getStateId());

            route = topo.getShotestPath(wlanID,actions);//根据权重选择
            nextHopId = topo.getEdgeHost(route.get(route.size()-1));
            changeState(route,nextHopId-routeNodeNum,task);

        }
        else if(policy.equals("shortest_RKaction")){//在k条到任意主机的最短路中随机选
            int action = (new Random().nextInt(16));
            route = topo.getKRouteToHost(wlanID).get(action);
            nextHopId = topo.getEdgeHost(route.get(route.size()-1));
        }
        else if(policy.equals("shortest_Rpath")){//OSPF，在最短跳的所有路中随机选
            route = topo.getLatestHost(wlanID);
            nextHopId = topo.getEdgeHost(route.get(route.size()-1));
        }else if(policy.equals("ECMP")){//最短跳作为等价多路径,未完成
            List<List<DefaultEdge>> ecmp = topo.getEcmp(wlanID);
            int out = task.getMobileDeviceId()%ecmp.size();
            route = ecmp.get(out);
            nextHopId = topo.getEdgeHost(route.get(route.size()-1));
        }
        else if(policy.equals("shortest_RHost")){//随机选择主机再计算最短路
            nextHopId = new Random().nextInt(SimSettings.getInstance().getNumOfEdgeHosts())+routeNodeNum;
            route = topo.getShotestPath(wlanID,nextHopId);
        }else if(policy.equals("greedy_host")){//选择利用率最小的主机
            ArrayList<Double>  use = getAvailVmInEdge();
            double max = Double.MIN_VALUE;
            int index = -1,t = 0;
            for(double u:use){
                if(u>max){
                    max = u;
                    index = t;
                }
                t++;
            }
            if(use.get(index) < 0.0001)  index = new Random().nextInt(16);//[0,15]
            nextHopId = index+routeNodeNum;
//            System.out.println(nextHopId+" "+index);
            route = topo.getShotestPath(wlanID,nextHopId);
        }else if(policy.equals("OSPF")){//把权重对应于链路,带宽作为路由代价
            Map<String, Double> weight = SimManager.getInstance().getNetworkModel().getEdgeBw();
            route = topo.getShotestPath(wlanID,weight);//根据权重选择
            nextHopId = topo.getEdgeHost(route.get(route.size()-1));
        }else if(policy.equals("deepEdge")){
            nextHopId = deepEdgeManager.deicede(task,getAvailVmInEdge())+routeNodeNum;
            route = topo.getShotestPath(wlanID,nextHopId);
        }

        task.setUploadPath(route);
        task.setUploadHost(nextHopId-routeNodeNum);//绝对hostid
        return route;
    }


    @Override
    public void TrainAgent(Task task, double serviceTime){
        boolean isFailed = false;
        if(serviceTime==-1) isFailed = true;
        double reward = isFailed ?-10:-Math.log(serviceTime);
        totalReward += reward;
        SimManager.getInstance().setReward(totalReward);

        if(policy.equals("DDQN")) TrainDDQNAgent(task,isFailed,reward);
        else if(policy.equals("PPO")) TrainPPOAgent(task,isFailed,reward);
        else if(policy.equals("deepEdge")) deepEdgeManager.TrainAgentforDeepEdge(task,isFailed);
    }


    public void TrainDDQNAgent(Task task, boolean isFailed,double reward){//任务完成后训练DDQN.记录奖励！
        if(!agentTraining) return ;

        DQNAgent agent = DQNAgent.getInstance();

        agent.setReward(totalReward);

        int stateId = task.getStateID();
        if (stateIDToMemoryItemPair.get(stateId) != null){//记录reward,初始算力为0已经在下面训练过的无法get到，不用管
            Memory memoryItem = stateIDToMemoryItemPair.get(stateId); // pass by reference!! vital!!
            if(memoryItem.getValue()==-10) memoryItem.setValue(reward);
            memoryItem.setDone(isFailed);//任务失败则结束

            List<DefaultEdge> route = task.getUploadPath();//不管是否失败都释放带宽和算力
            for(DefaultEdge edge : route){
                int t = SimManager.getInstance().getNetworkModel().getEdgeIndex(edge.toString());
                resetEdge[t] += 0.01 ;
            }
            int host = task.getUploadHost();
            Double t = resetHost.get(host);
            double cost = task.getCloudletLength()*task.getNumberOfPes();//任务长度*占用率/CPU MIPS
            resetHost.set(host,t+cost);
            if(isFailed) resetState();//重置状态
        }

        ArrayList<Integer> toBeDeletedIds = new ArrayList<>();
        for (Map.Entry<Integer, Memory> stateIDToMemoryItem: stateIDToMemoryItemPair.entrySet() ){
            int id = stateIDToMemoryItem.getKey();
            Memory item = stateIDToMemoryItem.getValue();

            if (item.getNextState() != null && item.getState() != null && item.getValue() != -10){
                //it means that the agent can be trained with this item
                agent.DDQN(item.getState(), item.getNextState(), item.getValue(), item.getAction(), item.isDone());
                toBeDeletedIds.add(id);
            }
        }
        for (Integer id: toBeDeletedIds) stateIDToMemoryItemPair.remove(id);
        taskToStateActionPair.remove(task.getCloudletId()); //remove task to state-action pair
    }

    public void TrainPPOAgent(Task task, boolean isFailed,double reward){
        PPOPlayer agent = PPOPlayer.getInstance();

        List<DefaultEdge> route = task.getUploadPath();//不管是否失败都释放带宽和算力
        for(DefaultEdge edge : route){
            int t = SimManager.getInstance().getNetworkModel().getEdgeIndex(edge.toString());
            resetEdge[t] += 0.01 ;
        }

        int host = task.getUploadHost();
        Double t = resetHost.get(host);
        double cost = task.getCloudletLength()*task.getNumberOfPes();//任务长度*占用率/CPU MIPS
        resetHost.set(host,t+cost);

        agent.record(task.getStateID(),reward, isFailed);//奖励，isdone
        if(isFailed) {
            resetState();//重置状态
        }
    }

    public void changeState(List<DefaultEdge> route,int host,Task task){
        for(DefaultEdge edge : route){
            int t = SimManager.getInstance().getNetworkModel().getEdgeIndex(edge.toString());
            trainEdge[t] -= 0.01 ;
        }
        Double t = trainHost.get(host);
        double cost = task.getCloudletLength()*task.getNumberOfPes();//任务长度*占用率/CPU MIPS
        trainHost.set(host,t-cost);
    }

    public void resetState(){//重置状态，释放带宽和算力，在完成任务（路由）后，把数字累加到resetlist，然后在reset的时候加上
        for(int i=0;i<edgeNum;i++) {
            trainEdge[i] += resetEdge[i];
            resetEdge[i] = 0;
        }
        for(int i=0;i<hostNum;i++){
            double t = trainHost.get(i);
            trainHost.set(i, t + resetHost.get(i) );
            trainHost.set(i,0.);
        }
    }

    public ArrayList<Double>  getAvailVmInEdge(){
        ArrayList<Double> edgeCapacities = new ArrayList<>();
        for(int hostIndex=0; hostIndex<numberOfHost; hostIndex++){//计算负载情况
            List<EdgeVM> vmArray = SimManager.getInstance().getEdgeServerManager().getVmList(hostIndex);
            double totalUtilizationForEdgeServer=0;
            for(int vmIndex=0; vmIndex<vmArray.size(); vmIndex++)
                totalUtilizationForEdgeServer += vmArray.get(vmIndex).getCloudletScheduler().getTotalUtilizationOfCpu(CloudSim.clock());

            double totalCapacity = 100 * vmArray.size();
            double averageCapacity = (totalCapacity - totalUtilizationForEdgeServer)  / vmArray.size();
            double normalizedCapacity = averageCapacity / 100;

            if (normalizedCapacity < 0) normalizedCapacity = 0;
            edgeCapacities.add(normalizedCapacity);
        }
        return edgeCapacities;
    }

    public CFNState GetFeaturesForAgent(Task task){
        CFNState currentState = new CFNState();
        task.setStateID(currentState.getStateId());

        int numberOfHost = SimSettings.getInstance().getNumOfEdgeHosts();

        double taskRequiredCapacity = ((CpuUtilizationModel_Custom)task.getUtilizationModelCpu()).predictUtilization(SimSettings.VM_TYPES.EDGE_VM);
        currentState.setTaskReqCapacity(taskRequiredCapacity/800);//所需算力

        int wlanID = task.getSubmittedLocation().getServingWlanId();
        currentState.setWlanID((double)wlanID / (routeNodeNum - 1));//位置，路由节点数-1

        double delay_sensitivity = SimSettings.getInstance().getTaskLookUpTable()[task.getTaskType()][12];//任务时延敏感
        currentState.setDelaySensitivity(delay_sensitivity);
        double data_upload = SimSettings.getInstance().getTaskLookUpTable()[task.getTaskType()][5];//任务大小
        currentState.setDataUpload(data_upload);


        ArrayList<Double> edgeCapacities = getAvailVmInEdge();
        double[] routesScore;
        currentState.setAvailVmInEdge(edgeCapacities);//负载情况
        List<List<DefaultEdge>> allKRoute=null;
        if(SimSettings.getInstance().getActionSize()==64){
            allKRoute = SimManager.getInstance().getNetworkModel().getTopo().getNodesKRoute(wlanID);//planA 52
            routesScore = SimManager.getInstance().getNetworkModel().getRouteScore(allKRoute,true);
        }
        else if(SimSettings.getInstance().getActionSize()==16||policy.equals("PPO")){//37
            routesScore = SimManager.getInstance().getNetworkModel().getGraphScore();
        }
        else{
            allKRoute = SimManager.getInstance().getNetworkModel().getTopo().getKRoute(wlanID,SimSettings.getInstance().getActionSize());
            routesScore = SimManager.getInstance().getNetworkModel().getRouteScore(allKRoute,false);//k*len(datacenter)=16
        }
        currentState.setRoutesScore(routesScore);//网络情况
        return currentState;
    }

    public CFNState GetFeaturesForUse(Task task){
        CFNState currentState = new CFNState();
        task.setStateID(currentState.getStateId());
        double taskRequiredCapacity = ((CpuUtilizationModel_Custom)task.getUtilizationModelCpu()).predictUtilization(SimSettings.VM_TYPES.EDGE_VM);
        currentState.setTaskReqCapacity(taskRequiredCapacity/800);//所需算力

        int wlanID = task.getSubmittedLocation().getServingWlanId();
        currentState.setWlanID((double)wlanID / (routeNodeNum - 1));//位置，路由节点数-1

        double delay_sensitivity = SimSettings.getInstance().getTaskLookUpTable()[task.getTaskType()][12];//任务时延敏感
        currentState.setDelaySensitivity(delay_sensitivity);
        double data_upload = SimSettings.getInstance().getTaskLookUpTable()[task.getTaskType()][5];//任务大小
        currentState.setDataUpload(data_upload);


        currentState.setAvailVmInEdge(trainHost);//负载情况,16
        currentState.setRoutesScore(trainEdge);//网络情况

        return currentState;
    }

    @Override
    public Vm getVmToOffload(Task task, int deviceId) {//选择最多剩余的虚拟机，如果没有虚拟机可以承载该任务则返回空，任务失败
        Vm selectedVM = null;
        List<EdgeVM> vmArray = SimManager.getInstance().getEdgeServerManager().getVmList(deviceId);

        //Select VM on edge devices via Least Loaded algorithm!
        double selectedVmCapacity = 0; //start with min value
        for(int vmIndex=0; vmIndex<vmArray.size(); vmIndex++){
            double requiredCapacity = ((CpuUtilizationModel_Custom)task.getUtilizationModelCpu()).predictUtilization(vmArray.get(vmIndex).getVmType());
            double targetVmCapacity = (double)100 - vmArray.get(vmIndex).getCloudletScheduler().getTotalUtilizationOfCpu(CloudSim.clock());
            if(requiredCapacity <= targetVmCapacity && targetVmCapacity > selectedVmCapacity){
                selectedVM = vmArray.get(vmIndex);
                selectedVmCapacity = targetVmCapacity;
            }
        }

        return selectedVM;
    }

    @Override
    public void processEvent(SimEvent arg0) {
        // Nothing to do!
    }

    @Override
    public void shutdownEntity() {
        // Nothing to do!
    }

    @Override
    public void startEntity() {
        // Nothing to do!
    }

}
