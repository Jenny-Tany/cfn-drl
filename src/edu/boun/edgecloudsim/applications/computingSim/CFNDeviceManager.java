package edu.boun.edgecloudsim.applications.computingSim;

import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.core.SimSettings.NETWORK_DELAY_TYPES;
import edu.boun.edgecloudsim.edge_client.CpuUtilizationModel_Custom;
import edu.boun.edgecloudsim.edge_client.MobileDeviceManager;
import edu.boun.edgecloudsim.edge_client.Task;
import edu.boun.edgecloudsim.network.NetworkModel;
import edu.boun.edgecloudsim.utils.Location;
import edu.boun.edgecloudsim.utils.SimLogger;
import edu.boun.edgecloudsim.utils.TaskProperty;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;
import org.jgrapht.graph.DefaultEdge;

import java.util.List;

// 模拟边缘计算环境中，移动设备与边缘服务器、数据中心之间任务卸载和传输的流程。
// 实现了移动设备和边缘服务器之间的任务提交、处理、网络延迟处理以及任务失败等逻辑。
public class CFNDeviceManager extends MobileDeviceManager{
    private static final int BASE = 100000; //start from base in order not to conflict cloudsim tag!

    private static final int UPDATE_MM1_QUEUE_MODEL = BASE + 1;
    private static final int REQUEST_UPLOAD_RECEIVED_BY_ROUTE_NODE = BASE + 2;
    private static final int REQUEST_DOWNLOAD_RECEIVED_BY_ROUTE_NODE = BASE + 3;
    private static final int REQUEST_RECEIVED_BY_EDGE_DEVICE = BASE + 4;
    private static final int RESPONSE_RECEIVED_BY_MOBILE_DEVICE = BASE + 6;

    private static final double MM1_QUEUE_MODEL_UPDATE_INTEVAL = 5; //seconds

    private int taskIdCounter=0;
    private static CFNDeviceManager instance = null;

    public CFNDeviceManager() throws Exception{

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

    public static CFNDeviceManager getInstance(){
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

        int nextEvent = REQUEST_DOWNLOAD_RECEIVED_BY_ROUTE_NODE; // 移动设备下载结果
        NETWORK_DELAY_TYPES delayType = NETWORK_DELAY_TYPES.WLAN_DELAY;
        double delayAll = networkModel.getDownloadDelay(task);
        networkModel.recordUsed(task.getUploadHost(),false);
//        if(delayAll>0)
        if(true)
        {
            Location currentLocation = SimManager.getInstance().getMobilityModel().getLocation(task.getMobileDeviceId(),CloudSim.clock()+delayAll);
            if(!SimSettings.devicesMobile||task.getSubmittedLocation().getServingWlanId() == currentLocation.getServingWlanId())
            {
                List<DefaultEdge> route = task.getDownloadPath();
                DefaultEdge edge = route.get(task.getLoadState());
                double delay = networkModel.getEdgeDelay(edge,task.getCloudletFileSize());
                if(delay<=0) {//在此之前减少主机卸载数
                    recordFaild(-1,task,CloudSim.clock(),delayType);
                    return ;
                }
                networkModel.uploadStarted(route,null,edge);//开始下载
                task.setloadState(true);
                SimLogger.getInstance().setDownloadDelay(task.getCloudletId(), delayAll, delayType);//修改为上传完成后记录

                if(task.getLoadState()==task.getDownloadPath().size()) nextEvent = RESPONSE_RECEIVED_BY_MOBILE_DEVICE;
                schedule(getId(), delay, nextEvent, task);//完成
            }
            else
            {
//                    当任务在移动过程中从一个网络区域（如WLAN）切换到另一个时，可能会导致任务执行的中断或延迟，因为可能需要重新分配资源或重新建立连接。
                SimLogger.getInstance().failedDueToMobility(task.getCloudletId(), CloudSim.clock());
//                SimManager.getInstance().getEdgeOrchestrator().TrainAgent(task, true);
            }
        }
        else recordFaild(-1,task,CloudSim.clock(),delayType);
    }

    protected void recordFaild(int vmType,Task task,double time,NETWORK_DELAY_TYPES delayType){//由于带宽失败
        if(vmType!=-1) SimLogger.getInstance().rejectedDueToBandwidth(task.getCloudletId(), time, vmType, delayType);//上传
        else SimLogger.getInstance().failedDueToBandwidth(task.getCloudletId(), time, delayType);//下载
        SimManager.getInstance().getEdgeOrchestrator().TrainAgent(task, -1);
    }

    protected void processOtherEvent(SimEvent ev) {//处理数据中心接收任务后的事件
        if (ev == null) {
            SimLogger.printLine(getName() + ".processOtherEvent(): " + "Error - an event is null! Terminating simulation...");
            System.exit(0);
            return;
        }

        NetworkModel networkModel = SimManager.getInstance().getNetworkModel();

        switch (ev.getTag()) {
            case UPDATE_MM1_QUEUE_MODEL:
            {
//                ((CFNNetworkingModel)networkModel).updateMM1QueeuModel();
                schedule(getId(), MM1_QUEUE_MODEL_UPDATE_INTEVAL, UPDATE_MM1_QUEUE_MODEL);
                break;
            }
            case REQUEST_UPLOAD_RECEIVED_BY_ROUTE_NODE: {//上传路由
                Task task = (Task) ev.getData();
                List<DefaultEdge> path = task.getUploadPath();//获取dc

                int state = task.getLoadState();
                int nextEvent = REQUEST_UPLOAD_RECEIVED_BY_ROUTE_NODE;
                if(state == path.size()-1) nextEvent = REQUEST_RECEIVED_BY_EDGE_DEVICE;

                DefaultEdge edge = path.get(state);
                double delay = networkModel.getEdgeDelay(edge,task.getCloudletFileSize());
                if(delay<=0) {//失败
                    recordFaild(SimSettings.VM_TYPES.EDGE_VM.ordinal(),task,CloudSim.clock(),NETWORK_DELAY_TYPES.WLAN_DELAY);//上传过程中失败
                    List<DefaultEdge> countList = path.subList(state, path.size());
                    networkModel.uploadStarted(countList,path.get(state-1),null);//释放上一条带宽
                    networkModel.recordUsed(task.getUploadHost(),false);
                    break;
                }
                networkModel.uploadStarted(null,path.get(state-1),edge);
                task.setloadState(true);
                schedule(getId(), delay, nextEvent, task);//指定下一事件延迟
                break;

            }
            case REQUEST_DOWNLOAD_RECEIVED_BY_ROUTE_NODE: {//下载路由
                Task task = (Task) ev.getData();
                List<DefaultEdge> path = task.getDownloadPath();//获取dc

                int state = task.getLoadState();
                int nextEvent = REQUEST_DOWNLOAD_RECEIVED_BY_ROUTE_NODE;
                if(state == path.size()-1) nextEvent = RESPONSE_RECEIVED_BY_MOBILE_DEVICE;
                DefaultEdge edge = path.get(state);
                double delay = networkModel.getEdgeDelay(edge,task.getCloudletFileSize());
                if(delay<=0){
                    recordFaild(-1,task,CloudSim.clock(),NETWORK_DELAY_TYPES.WLAN_DELAY);//下载过程中失败
                    List<DefaultEdge> countList = path.subList(state, path.size());
                    networkModel.uploadStarted(countList,path.get(state-1),null);//释放上一条带宽
                    break;
                }
                networkModel.uploadStarted(null,path.get(state-1),edge);
                task.setloadState(true);
                schedule(getId(), delay, nextEvent, task);//指定下一事件延迟
                break;
            }
            case REQUEST_RECEIVED_BY_EDGE_DEVICE://被计算设备接受后
            {
                Task task = (Task) ev.getData();
                List<DefaultEdge> path = task.getUploadPath();//获取dc
                int state = task.getLoadState();
                networkModel.uploadStarted(null,path.get(state-1),null);
                task.setloadState(false);

                submitTaskToVm(task, SimSettings.VM_TYPES.EDGE_VM);
                break;
            }
            case RESPONSE_RECEIVED_BY_MOBILE_DEVICE://已被移动设备接收后
            {
                Task task = (Task) ev.getData();
                List<DefaultEdge> path = task.getDownloadPath();//获取dc
                int state = task.getLoadState();
                networkModel.uploadStarted(null,path.get(state-1),null);
                task.setloadState(false);

                SimLogger.getInstance().taskEnded(task.getCloudletId(), CloudSim.clock());//任务完成
//                if(SimLogger.getInstance().getServiceTime(task.getCloudletId())<0.0008)
//                    System.out.println(SimLogger.getInstance().getServiceTime(task.getCloudletId()));
                SimManager.getInstance().getEdgeOrchestrator().TrainAgent(task, SimLogger.getInstance().getServiceTime(task.getCloudletId()));
                break;
            }
            default:
                SimLogger.printLine(getName() + ".processOtherEvent(): " + "Error - event unknown by this DatacenterBroker. Terminating simulation...");
                System.exit(0);
                break;
        }
    }

//    创建任务
    public void submitTask(TaskProperty edgeTask) throws Exception {

        //for this given task, for TRAINING, each edge server must be evaluated 对于给定的用于训练的任务，必须评估每个服务器
        NetworkModel networkModel = SimManager.getInstance().getNetworkModel();
        Task task = createTask(edgeTask);//create a task生成任务
        Location currentLocation = SimManager.getInstance().getMobilityModel().getLocation(task.getMobileDeviceId(), CloudSim.clock());//获取task位置
        task.setSubmittedLocation(currentLocation);//set location of the mobile device which generates this task

        double delayAll = networkModel.getUploadDelay(task);//在这里决测路由获取时延
        int vmType = SimSettings.VM_TYPES.EDGE_VM.ordinal();
        int nextEvent = REQUEST_UPLOAD_RECEIVED_BY_ROUTE_NODE;
        NETWORK_DELAY_TYPES delayType = NETWORK_DELAY_TYPES.WLAN_DELAY;

//        if(delayAll>0){//路径可用
        if(true){
            int nextHopId = task.getUploadHost();//决策的主机
            Vm selectedVM = SimManager.getInstance().getEdgeOrchestrator().getVmToOffload(task, nextHopId);//传入nextHopId选择vm

            if(selectedVM != null){
//                System.out.println(selectedVM.getHost().getDatacenter().getId());
                task.setAssociatedDatacenterId(networkModel.getTopo().getHostDatecenter(nextHopId));//set related 数据中心 id
                task.setAssociatedHostId(selectedVM.getHost().getId());//set related host id
                task.setAssociatedVmId(selectedVM.getId());//set related vm id

                getCloudletList().add(task);//添加到列标//bind task to related VM
                bindCloudletToVm(task.getCloudletId(), selectedVM.getId());//绑定至选定虚拟机
                DefaultEdge edge = task.getUploadPath().get(task.getLoadState());
                double delay = networkModel.getEdgeDelay(edge,task.getCloudletFileSize());
                if(delay<=0){
                    recordFaild(vmType,task,CloudSim.clock(),delayType);
                    return ;
                }

                networkModel.uploadStarted(task.getUploadPath(),null,edge);//开始上传
                networkModel.recordUsed(task.getUploadHost(),true);
                task.setloadState(true);
                SimLogger.getInstance().taskStarted(task.getCloudletId(), CloudSim.clock());//记录任务开始时间和时延
                SimLogger.getInstance().setUploadDelay(task.getCloudletId(), delayAll, delayType);//修改为上传完成后记录
                if(task.getUploadPath().size() == task.getLoadState()) nextEvent = REQUEST_RECEIVED_BY_EDGE_DEVICE;
                schedule(getId(), delay, nextEvent, task);//指定下一事件延迟
            }
            else{
                //SimLogger.printLine("Task #" + task.getCloudletId() + " cannot assign to any VM");
//                System.out.println(nextHopId);
//                System.out.println(task.getExecStartTime());
                SimLogger.getInstance().rejectedDueToVMCapacity(task.getCloudletId(), CloudSim.clock(), vmType);//算力资源不够
                SimManager.getInstance().getEdgeOrchestrator().TrainAgent(task, -1);//由于算力失败
            }
        }
        else recordFaild(vmType,task,CloudSim.clock(),delayType);
    }


    private void submitTaskToVm(Task task, SimSettings.VM_TYPES vmType) {
        //SimLogger.printLine(CloudSim.clock() + ": Cloudlet#" + task.getCloudletId() + " is submitted to VM#" + task.getVmId());

        schedule(getVmsToDatacentersMap().get(task.getVmId()), 0, CloudSimTags.CLOUDLET_SUBMIT, task);

        SimLogger.getInstance().taskAssigned(task.getCloudletId(),
                task.getAssociatedDatacenterId(),
                task.getAssociatedHostId(),
                task.getAssociatedVmId(),
                vmType.ordinal());

    }

    private Task createTask(TaskProperty edgeTask){
        UtilizationModel utilizationModel = new UtilizationModelFull(); /*UtilizationModelStochastic*/
        UtilizationModel utilizationModelCPU = getCpuUtilizationModel();

        Task task = new Task(edgeTask.getMobileDeviceId(), ++taskIdCounter,
                edgeTask.getLength(), edgeTask.getPesNumber(),
                edgeTask.getInputFileSize(), edgeTask.getOutputFileSize(),
                utilizationModelCPU, utilizationModel, utilizationModel);

        //set the owner of this task
        task.setUserId(this.getId());
        task.setTaskType(edgeTask.getTaskType());

        if (utilizationModelCPU instanceof CpuUtilizationModel_Custom) {//是实例则set
            ((CpuUtilizationModel_Custom)utilizationModelCPU).setTask(task);
        }
        SimLogger.getInstance().addLog(task.getCloudletId(),
                task.getTaskType(),
                (int)task.getCloudletLength(),
                (int)task.getCloudletFileSize(),
                (int)task.getCloudletOutputSize());
        return task;
    }
}
