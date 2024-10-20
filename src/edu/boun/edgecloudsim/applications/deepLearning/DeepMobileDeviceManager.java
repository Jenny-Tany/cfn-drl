package edu.boun.edgecloudsim.applications.deepLearning;

import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.edge_client.CpuUtilizationModel_Custom;
import edu.boun.edgecloudsim.edge_client.MobileDeviceManager;
import edu.boun.edgecloudsim.edge_client.Task;
import edu.boun.edgecloudsim.network.NetworkModel;
import edu.boun.edgecloudsim.utils.SimLogger;
import edu.boun.edgecloudsim.utils.TaskProperty;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.core.SimEvent;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeepMobileDeviceManager extends MobileDeviceManager{
    private static final int BASE = 100000; //start from base in order not to conflict cloudsim tag!

    private static final int UPDATE_MM1_QUEUE_MODEL = BASE + 1;

    private static final double MM1_QUEUE_MODEL_UPDATE_INTEVAL = 5; //seconds
    private boolean dqnTraining = false;

    private final int EPISODE_SIZE = 75000;
    private double totalReward = 0;

    private HashMap<Integer, HashMap<Integer, Integer>> taskToStateActionPair = new HashMap<>();
    private HashMap<Integer, MemoryItem> stateIDToMemoryItemPair = new HashMap<>();

    private static DeepMobileDeviceManager instance = null;
    private  DDQNAgent agent;
    private int stateSize;

    private long lastVisualizationTime = 0;

    public  DeepMobileDeviceManager (int hostNum,int edgeNum) throws Exception{
        stateSize = 6+edgeNum*2+hostNum*4;
        agent = new DDQNAgent(stateSize,hostNum);//主机数

    }

    @Override
    public void initialize() {
        instance = this;
    }

    @Override
    public UtilizationModel getCpuUtilizationModel() {
        return new CpuUtilizationModel_Custom();
    }

    @Override
    public void startEntity() {
        super.startEntity();
        schedule(getId(), SimSettings.CLIENT_ACTIVITY_START_TIME +
                MM1_QUEUE_MODEL_UPDATE_INTEVAL, UPDATE_MM1_QUEUE_MODEL);
    }

    public static DeepMobileDeviceManager getInstance(){
        return instance;
    }

    /**
     * Submit cloudlets to the created VMs.
     *
     * @pre $none
     * @post $none
     */
    protected void submitCloudlets() {
        //do nothing!
    }

    /**
     * Process a cloudlet return event.
     *
     * @param ev a SimEvent object
     * @pre ev != $null
     * @post $none
     */
    protected void processCloudletReturn(SimEvent ev) {//处理任务返回
        NetworkModel networkModel = SimManager.getInstance().getNetworkModel();
        Task task = (Task) ev.getData();
        SimLogger.getInstance().taskExecuted(task.getCloudletId());
    }

    protected void processOtherEvent(SimEvent ev) {//处理数据中心接收任务后的事件
        if (ev == null) {
            SimLogger.printLine(getName() + ".processOtherEvent(): " + "Error - an event is null! Terminating simulation...");
            System.exit(0);
            return;
        }

        NetworkModel networkModel = SimManager.getInstance().getNetworkModel();
    }
    public void submitTask(TaskProperty edgeTask) { }

    public int deicede(Task task,ArrayList<Double> edgeCapacities){
        int nextHopId;

//        DDQNAgent agent = DDQNAgent.getInstance();
        DeepEdgeState currentState = GetFeaturesForAgent(task,edgeCapacities);
        nextHopId = agent.DoAction(currentState);


//为云任务记录状态动作对
        HashMap<Integer, Integer> stateActionP = new HashMap<>();
        stateActionP.put(currentState.getStateId(), nextHopId);
        taskToStateActionPair.put(task.getCloudletId(), stateActionP);
        boolean isDone = false;
        if (task.getCloudletId() == EPISODE_SIZE){//云任务已完成迭代
            isDone = true;
            dqnTraining = false;
            System.out.println("Total Reward devicemanager: " + totalReward);
        }


        // After work
        MemoryItem memoryItem;
        ArrayList<Double> edgeList = currentState.getAvailVmInEdge();
        if (nextHopId < 14 && edgeList.get(nextHopId) == 0){
            memoryItem = new MemoryItem(currentState, null, -1, -10, isDone);
        }else{
            memoryItem = new MemoryItem(currentState, null, -10, -10, isDone);
        }
        // After work
        // 查找前一状态，为当前状态存储前一状态
        stateIDToMemoryItemPair.put(currentState.getStateId(), memoryItem);
        if (stateIDToMemoryItemPair.get(currentState.getStateId()-1) != null){
            MemoryItem previousMemoryItem = stateIDToMemoryItemPair.get(currentState.getStateId()-1);
            previousMemoryItem.setNextState(currentState); //pass by reference!! It is crucial!!
        }
        return nextHopId;
    }

    // 添加可视化方法，接收任务作为参数
    public void visualizeMetrics(Task task) {
        DDQNAgent agent = DDQNAgent.getInstance();

        // 获取任务完成率、平均奖励、网络延迟和算力使用情况等指标
        double taskCompletionRate = agent.getTaskCompletionRate();
        double averageReward = agent.getAvgQvalue();
        double networkDelay = getNetworkDelayForTask(task);
        double cpuUtilization = getCpuUtilizationForTask(task);

        // 创建数据集用于绘图
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        dataset.addValue(taskCompletionRate, "Task Completion Rate", "");
        dataset.addValue(averageReward, "Average Reward", "");
        dataset.addValue(networkDelay, "Network Delay", "");
        dataset.addValue(cpuUtilization, "CPU Utilization", "");

        // 创建图表
        JFreeChart chart = ChartFactory.createBarChart(
                "Metrics Visualization",
                "Metrics",
                "Value",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        // 创建图表面板并显示在窗口中
        ChartPanel chartPanel = new ChartPanel(chart);
        JFrame frame = new JFrame("Metrics Visualization");
        frame.setContentPane(chartPanel);
        frame.setSize(800, 600);
        frame.setVisible(true);
    }

    private double getNetworkDelayForTask(Task task) {
        // 返回任务的网络延迟，根据实际情况实现
        return 0.0;
    }

    private double getCpuUtilizationForTask(Task task) {
        // 返回任务的算力使用情况，根据实际情况实现
        return 0.0;
    }


    // 假设的获取当前模拟时间的方法
    private long getCurrentSimulationTime() {
        // 根据实际情况实现获取当前模拟时间的方法
        return 0;
    }
    //训练DDQN
public void TrainAgentforDeepEdge(Task task, boolean isFailed){

        DDQNAgent agent = DDQNAgent.getInstance();

        double reward;
        if (isFailed){
            reward = -1;
        }
        else{
            reward = 1;
        }
        totalReward += reward;
        agent.setReward(totalReward);

    // 更新任务完成状态
    agent.updateTaskCompletionStatus(!isFailed);

    // 记录其他指标，假设可以通过以下方式获取网络延迟和算力使用情况
    double networkDelay = getNetworkDelayForTask(task);
    double cpuUtilization = getCpuUtilizationForTask(task);

        HashMap<Integer, Integer> pair = taskToStateActionPair.get(task.getCloudletId());

    // 获取当前模拟时间
    long currentTime = getCurrentSimulationTime();

    // 如果距离上次可视化时间超过 2 分钟，则进行可视化
    if (currentTime - lastVisualizationTime >= 2 * 60) {
        visualizeMetrics(task);
        lastVisualizationTime = currentTime;
    }

        //System.out.println("Size of taskToStateActionPair: "+ taskToStateActionPair.size());
        int stateId = pair.entrySet().iterator().next().getKey();
        int selectedAction = pair.entrySet().iterator().next().getValue();

        if (stateIDToMemoryItemPair.get(stateId) != null){
            MemoryItem memoryItem = stateIDToMemoryItemPair.get(stateId); // pass by reference!! vital!!
            memoryItem.setAction(selectedAction);

            if (memoryItem.getValue() == -10){
                memoryItem.setValue(reward);
            }

        }else{
            System.out.println("ERROR!");
        }



        ArrayList<Integer> toBeDeletedIds = new ArrayList<>();
        for (Map.Entry<Integer, MemoryItem> stateIDToMemoryItem: stateIDToMemoryItemPair.entrySet() ){
            int id = stateIDToMemoryItem.getKey();
            MemoryItem item = stateIDToMemoryItem.getValue();

            if (item.getNextState() != null && item.getState() != null && item.getAction() != -10 && item.getValue() != -10){
                //it means that the agent can be trained with this item
                agent.DDQN(item.getState(), item.getNextState(), item.getValue(), item.getAction(), item.isDone());
                toBeDeletedIds.add(id);
            }
        }
        for (Integer id: toBeDeletedIds){
            stateIDToMemoryItemPair.remove(id);
        }
        taskToStateActionPair.remove(task.getCloudletId()); //remove task to state-action pair

    }


    public DeepEdgeState GetFeaturesForAgent(Task task,ArrayList<Double> edgeCapacities){
        Task dummyTask = new Task(0, 0, 0, 0, 128, 128, new UtilizationModelFull(), new UtilizationModelFull(), new UtilizationModelFull());

        DeepEdgeState currentState = new DeepEdgeState(stateSize);
        int numberOfHost = SimSettings.getInstance().getNumOfEdgeHosts();
//获取每种网络的上传延迟
        Map<String, Double> netDelay = SimManager.getInstance().getNetworkModel().getEdgeBw();
        List<Double> wanDelay = new ArrayList<>();
        for (Map.Entry<String, Double> entry : netDelay.entrySet()) wanDelay.add(entry.getValue());
        currentState.setWanBw(wanDelay);
//预测该任务在边缘虚拟机上的CPU利用率
        double taskRequiredCapacity = ((CpuUtilizationModel_Custom)task.getUtilizationModelCpu()).predictUtilization(SimSettings.VM_TYPES.EDGE_VM);
        currentState.setTaskReqCapacity(taskRequiredCapacity/800);

        int wlanID = task.getSubmittedLocation().getServingWlanId();
        currentState.setWlanID((double)wlanID / (numberOfHost - 1));
        //最近的数据中心
        List<Double> nearestEdgeHostId = SimManager.getInstance().getNetworkModel().getTopo().getNodesNearestHost(wlanID,numberOfHost);
        currentState.setNearestEdgeHostId(nearestEdgeHostId);

//记录主机可用容量
        currentState.setAvailVmInEdge(edgeCapacities);

        double delay_sensitivity = SimSettings.getInstance().getTaskLookUpTable()[task.getTaskType()][12];
        currentState.setDelaySensitivity(delay_sensitivity);

        double[] numberOfloadedTask = SimManager.getInstance().getNetworkModel().getEdgeActive();
        currentState.setNumberOfWlanOffloadedTask(numberOfloadedTask);//卸载到每种网络的任务数
        double[] activeTaskCount = SimManager.getInstance().getNetworkModel().getHostActive();
        currentState.setActiveManTaskCount(activeTaskCount);//卸载到-某种网络-但尚未到达目的地的任务数量
        return currentState;
    }
}
