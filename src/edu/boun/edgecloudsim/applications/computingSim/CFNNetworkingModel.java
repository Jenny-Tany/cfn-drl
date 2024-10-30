package edu.boun.edgecloudsim.applications.computingSim;

import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.edge_client.Task;
import edu.boun.edgecloudsim.network.NetworkModel;
import edu.boun.edgecloudsim.network.topoGraph.GBN;
import edu.boun.edgecloudsim.network.topoGraph.GEANT2;
import edu.boun.edgecloudsim.network.topoGraph.NSFNET;
import edu.boun.edgecloudsim.network.topoGraph.topoGraph;
import edu.boun.edgecloudsim.utils.Location;
import edu.boun.edgecloudsim.utils.SimLogger;
import org.cloudbus.cloudsim.core.CloudSim;
import org.jgrapht.graph.DefaultEdge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class CFNNetworkingModel extends NetworkModel {
    public topoGraph topo;
    public double[] experimentalDelay;
    public int hostNum, routeNodeNum, edgeNum;
    private Map<String, Integer> edgeClients = new HashMap<>();// 记录使用人数
    private Map<String, Integer> edgeIndex = new HashMap<>();
    private Map<Integer, Integer> activeHostUsed = new HashMap<>();// 记录被卸载到主机的人数
    private Map<String, Integer> edgeActive = new HashMap<>();// 预计被使用且未使用完
    private Random rand = new Random();

    public CFNNetworkingModel(int numberOfMobileDevices, String simScenario) {
        super(numberOfMobileDevices, simScenario);
    }

    private static NetworkModel instance = null;

    public static NetworkModel getInstance() {
        return instance;
    }

    @Override
    public void initialize() {
        if (SimSettings.getInstance().getTOPO().equals("NSFNET"))
            topo = new NSFNET();
        else if (SimSettings.getInstance().getTOPO().equals("GBN"))
            topo = new GBN();
        else
            topo = new GEANT2();
        experimentalDelay = topo.experimentalDelay;

        hostNum = topo.getHostCount();
        routeNodeNum = topo.getNodeCount();
        edgeNum = topo.getEdgeCount();
        instance = this;
        int t = 0;
        for (DefaultEdge edge : topo.getEdges()) {
            edgeClients.put(edge.toString(), 0);
            edgeActive.put(edge.toString(), 0);
            edgeIndex.put(edge.toString(), t++);
        }
        for (int i = 0; i < hostNum; i++)
            activeHostUsed.put(i, 0);

        int numOfApp = SimSettings.getInstance().getTaskLookUpTable().length;
        SimSettings SS = SimSettings.getInstance();
        for (int taskIndex = 0; taskIndex < numOfApp; taskIndex++) {
            if (SS.getTaskLookUpTable()[taskIndex][0] == 0) {
                SimLogger.printLine("Usage percantage of task " + taskIndex + " is 0! Terminating simulation...");
                System.exit(0);
            }
        }
    }

    public topoGraph getTopo() {
        return topo;
    }

    public Map<String, Double> getEdgeBw() {
        Map<String, Double> res = new HashMap<>();
        for (Map.Entry<String, Integer> entry : edgeClients.entrySet()) {
            Integer value = entry.getValue();
            double bw = value > 100? 0 : experimentalDelay[value];
            res.put(entry.getKey(), bw);
        }
        return res;
    }

    public double[] getRouteScore(List<List<DefaultEdge>> routes, boolean haveHost) {
        int size = routes.size();
        if (haveHost)
            size += hostNum;
        double[] scores = new double[size];// 路由网络，主机网络
        int pos = 0;
        for (List<DefaultEdge> route : routes) {
            double score = 0;
            for (DefaultEdge edge : route) {
                int users = edgeClients.get(edge.toString());
                if (users < experimentalDelay.length)
                    score += 1 /*Kb*/ / (experimentalDelay[users] * (double) 3 ) /*Kbps*/;
                else
                    score += 0.001;// 超出使用人数，给与一个较大的时延，100人使用时时延为2.21E-4
            }
            scores[pos++] = score;
        }
        if (haveHost) {
            List<DefaultEdge> edges = topo.getHostEdges();
            for (DefaultEdge edge : edges) {
                int users = edgeClients.get(edge.toString());
                if (users < experimentalDelay.length)
                    scores[pos++] = 1 / (experimentalDelay[edgeClients.get(edge.toString())] * (double) 3 ) /*Kbps*/;
                else
                    scores[pos++] += 0.001;
            }
        }

        return scores;
    }

    public double[] getGraphScore() {
        double[] result = new double[edgeNum + hostNum];
        int t = 0;
        for (Map.Entry<String, Integer> entry : edgeClients.entrySet()) {
            Integer value = entry.getValue();
            result[t++] = value / 100;
        }
        return result;
    }

    public double[] getHostActive() {
        double[] result = new double[hostNum];
        int t = 0;
        for (Map.Entry<Integer, Integer> entry : activeHostUsed.entrySet()) {
            Integer value = entry.getValue();
            result[t++] = value / 100;
        }
        return result;
    }

    public double[] getEdgeActive() {
        double[] result = new double[edgeNum + hostNum];
        int t = 0;
        for (Map.Entry<String, Integer> entry : edgeActive.entrySet()) {
            Integer value = entry.getValue();
            result[t++] = value / 100;
        }
        return result;
    }

    public int getEdgeIndex(String t) {
        return edgeIndex.get(t);
    }

    /**
     * source device is always mobile device in our simulation scenarios!
     */
    @Override
    public double getUploadDelay(Task task) throws Exception {
        int sourceDeviceId = task.getMobileDeviceId();// 设备ID
        Location accessPointLocation = SimManager.getInstance().getMobilityModel().getLocation(sourceDeviceId, CloudSim.clock());// 获取移动设备位置
        int sourceId = accessPointLocation.getServingWlanId();// 根据位置决定的wlan
        if (sourceId > routeNodeNum)
            System.out.println(sourceId + "error, over router node");

//        int destDeviceId = rand.nextInt(routeNodeNum, routeNodeNum + topo.instance.getHostCount());// 在这里给controller决策
//        List<DefaultEdge> edgeList = topo.getShotestPath(sourceId, destDeviceId);// 暂时使用最短路

        List<DefaultEdge> edgeList = SimManager.getInstance().getEdgeOrchestrator().getRouteToOffload(task);// 选中设备id
        double delay = getRoutedelay(edgeList, task.getCloudletFileSize());

        // 任务开始上传，记录开始时间
        task.startExecution();

        return delay;
    }

    /**
     * destination device is always mobile device in our simulation scenarios!
     */
    @Override
    public double getDownloadDelay(Task task) {
        int host = task.getUploadHost();
        int sourceId = host + routeNodeNum;
        int sourceDeviceId = task.getMobileDeviceId();
        Location accessPointLocation = SimManager.getInstance().getMobilityModel().getLocation(sourceDeviceId, CloudSim.clock());// 获取移动设备位置
        int destDeviceId = accessPointLocation.getServingWlanId();// 任务返回
//        List<DefaultEdge> edgeList = topo.getShotestPath(sourceId, destDeviceId);// 暂时使用最短路
        List<DefaultEdge> upPath = task.getUploadPath();
        List<DefaultEdge> edgeList = new ArrayList<>();
        for (int i = upPath.size() - 1; i >= 0; i--)
            edgeList.add(upPath.get(i));
        task.setDownloadPath(edgeList);

        double delay = getRoutedelay(edgeList, task.getCloudletFileSize());

        // 任务下载完成，记录完成时间
        task.finishExecution();

        return delay;
    }

    public double getRoutedelay(List<DefaultEdge> edgeList, long filesize) {
//        List<DefaultEdge> edgeList = path.getEdgeList();
        double delay = 0;
        for (DefaultEdge edge : edgeList) {// 遍历路由的边
            int numOfWlanUser = edgeClients.get(edge.toString());
            double taskSizeInKb = filesize * (double) 8; // KB to Kb
            if (numOfWlanUser < experimentalDelay.length) {// 可以承载该路由
                double t = taskSizeInKb /*Kb*/ / (experimentalDelay[numOfWlanUser] * (double) 3 ) /*Kbps*/; // 802.11ac is around 3 times faster than 802.11n
                delay += t;
            } else
                return 0;
        }
        return delay;
    }

    @Override
    public void uploadStarted(List<DefaultEdge> list, DefaultEdge pathFinished, DefaultEdge pathStarted) {// 已完成的路径和开始的路由
        if (pathFinished!= null) {
            String path = pathFinished.toString();
            edgeActive.put(path, edgeActive.get(path) - 1 );// 减少当前路由结束的链路的预计数
            edgeClients.put(path, edgeClients.get(path) - 1);
        }
        if (pathStarted!= null)
            edgeClients.put(pathStarted.toString(), edgeClients.get(pathStarted.toString()) + 1);
        if (pathFinished == null) {// 开始路由，所有路由预计数增加
            for (DefaultEdge edge : list)
                edgeActive.put(edge.toString(), edgeActive.get(edge.toString()) + 1 );
        }
        if (pathStarted == null && list!= null) {// 路由结束，路由过程失败,pathFinished后所有路由预计数减少
            for (DefaultEdge edge : list)
                edgeActive.put(edge.toString(), edgeActive.get(edge.toString()) - 1 );// 减少当前路由结束的链路的预计数
        }
//        for (DefaultEdge edge : path) edgeClients.put(edge.toString(), edgeClients.get(edge.toString())+1);
//        if(pathStarted!=null&&pathFinished!=null)
//            System.out.println(edgeClients.get(pathFinished.toString())+" "+edgeClients.get(pathStarted.toString()));
//        if(destDeviceId>hostNum){
//            SimLogger.printLine("Error - unknoqn device id in FuzzyExperimentalNetworkModel.uploadStarted(. Terminating simulation...");
//            System.exit(0);
//        }
    }

    public void recordUsed(int host, boolean flag) {
        if (flag)
            activeHostUsed.put(host, activeHostUsed.get(host) + 1);
        else
            activeHostUsed.put(host, activeHostUsed.get(host) - 1);
    }

    @Override
    public double getAvgBandwidth() {
        double result = 0;
        for (Integer value : edgeClients.values()) {
            if (value < experimentalDelay.length)
                result += experimentalDelay[value] * (double) 3;// 否则带宽为0
        }
        return result / edgeNum;
    }

    public double getEdgeDelay(DefaultEdge edge, double taskSizeInKb) {
//        double taskSizeInKb = dataSize * (double)8; //KB to Kb
        int numOfWlanUser = edgeClients.get(edge.toString());
        if (numOfWlanUser < experimentalDelay.length)
            return taskSizeInKb /*Kb*/ / (experimentalDelay[numOfWlanUser] * (double) 3 ) /*Kbps*/; // 802.11ac is around 3 times faster than 802.11n
        return -1;
    }

    @Override
    public double getUploadDelayForTraining(int mobileDeviceId, int cloudDatacenterId, Task task) {
        return 0;
    }

    // 检查任务是否在正常进度
    public boolean isTaskOnTrack(Task task) {
        double elapsedTime = CloudSim.clock() - task.getStartTime();
        double remainingWork = task.getCloudletFileSize() - task.getProcessedSize();
        double estimatedCompletionTime = remainingWork / task.getProcessingSpeed();
        return elapsedTime + estimatedCompletionTime <= task.getDeadline();
    }

    // 负载均衡方法
    public void balanceLoad() {
        double maxLoad = 0;
        double minLoad = Double.MAX_VALUE;
        int mostLoadedDevice = -1;
        int leastLoadedDevice = -1;
        double[] hostActive = getHostActive();
        for (int i = 0; i < hostNum; i++) {
            double currentLoad = hostActive[i];
            if (currentLoad > maxLoad) {
                maxLoad = currentLoad;
                mostLoadedDevice = i;
            }
            if (currentLoad < minLoad) {
                minLoad = currentLoad;
                leastLoadedDevice = i;
            }
        }
        if (mostLoadedDevice!= -1 && leastLoadedDevice!= -1) {
            List<Task> tasksOnMostLoaded = getTasksOnDevice(mostLoadedDevice);
            if (!tasksOnMostLoaded.isEmpty()) {
                Task taskToTransfer = tasksOnMostLoaded.get(0);
                taskToTransfer.setUploadHost(leastLoadedDevice);
                try {
                    double newUploadDelay = getUploadDelay(taskToTransfer);
                    taskToTransfer.setUploadDelay(newUploadDelay);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // 获取分配到特定设备的任务列表（这里只是一个示例，需要根据实际任务分配逻辑完善）
    private List<Task> getTasksOnDevice(int deviceId) {
        List<Task> tasks = new ArrayList<>();
        // 根据任务分配逻辑查找分配到该设备的任务
        return tasks;
    }
}