package edu.boun.edgecloudsim.applications.deepLearning;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.distribution.UniformDistribution;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction;

import java.io.File;
import java.io.IOException;
import java.util.*;
//菜单栏中选择File，选中Settings，在搜索栏中输入：Excludes，
// 找到Excludes选项，点击右上角+，添加要排除的文件，之后编译时就不会对已经排除的Java文件进行编译
// 深度双重Q学习（Double Deep Q-Network, DDQN） 的智能体用于强化学习任务。
// 使用神经网络作为Q值函数的逼近器，来选择最优的动作，从而最大化累积的奖励。
// 关键组件包括Q网络（用于评估当前状态的动作价值）和目标网络（用于生成目标Q值以更新Q网络），并通过一个经验回放机制存储状态转换和奖励信息。
public class DDQNAgent {
    private MultiLayerNetwork qNetwork; // 主要的Q网络，用于选择动作
    private MultiLayerNetwork targetNetwork; //  目标网络，用于生成训练目标。目标网络通过主网络的参数周期性地更新（通过 syncTargetNetwork()），以减少训练过程中的非稳定性。
    private final double DISCOUNT_FACTOR = 0.8; // 用于未来奖励的折扣率，值为0.8。它决定了智能体对未来奖励的重视程度
    private double totalReward;
    private final double LEARNING_RATE = 0.0001; //控制神经网络的权重更新步长
    private double epsilon = 1; // 探索率，控制智能体采取随机动作的概率
    private final double MIN_EPSILON = 0.1; // 探索率最小值，用于控制智能体Never explore
    private final double EPSILON_FACTOR = 0.99; // 探索率衰减率，用于控制探索的步长
    private final int MEMORY_SIZE = 1000000; // 存储状态转换和奖励信息的经验回放池的大小，值为1000000
    private final int BATCH_SIZE = 4; // 每次训练时从经验池中采样的大小
    private final double TAU = 0.01; // 用于目标网络软更新的参数，表示主网络参数对目标网络影响的权重
    private final int C_SYNC = 10;// 目标网络参数更新步长，用于控制目标网络参数的更新，值为10，表示每隔多少次动作后将同步目标网络
    private ArrayList<edu.boun.edgecloudsim.applications.deepLearning.MemoryItem> memory; //it needs a new memoryItem structure
    private int numberOfActions;
    private int counterForEpsilon;

    private static edu.boun.edgecloudsim.applications.deepLearning.DDQNAgent instance = null;

    private double reward;
    private double avgQValue;
    private int actionCount;

    // 添加成员变量用于跟踪任务完成情况
    private int completedTasks;
    private int totalTasks;

    // 添加新的成员变量用于跟踪任务完成率和平均奖励
    private double taskCompletionRate;
    private double averageReward;
    private double targetCompletionRate = 0.8; // 目标任务完成率，可以根据实际情况调整
    private double targetAverageReward = 5.0; // 目标平均奖励，可以根据实际情况调整
    private double increaseFactor = 1.05; // 当任务完成率低或平均奖励低时增加探索率的因子，可以根据实际情况调整


    public DDQNAgent(int stateSize,int numberOfEdgeServers){
        // 初始化新添加的成员变量
        completedTasks = 0;
        totalTasks = 0;

        this.counterForEpsilon = 0;
        totalReward = 0;
        this.numberOfActions = numberOfEdgeServers; // +1 comes from the cloud server
        memory = new ArrayList<edu.boun.edgecloudsim.applications.deepLearning.MemoryItem>();
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .updater(new Adam(LEARNING_RATE))
                .seed(1234)
                .miniBatch(false)
                .list()
                .layer(new DenseLayer.Builder()
                        .nIn(stateSize)
                        .nOut(64)
                        .activation(Activation.RELU)
                        // random initialize weights with values between 0 and 1
                        .weightInit(new UniformDistribution(-1, 1))
                        .build())
                .layer(new DenseLayer.Builder()
                        .nIn(64)
                        .nOut(64)
                        .activation(Activation.RELU)
                        // random initialize weights with values between 0 and 1
                        .weightInit(new UniformDistribution(-1, 1))
                        .build())
                .layer(new OutputLayer.Builder(LossFunction.MSE) //create hidden layer
                        .nIn(64)
                        .nOut(this.numberOfActions)
                        .activation(Activation.IDENTITY)
                        .weightInit(new UniformDistribution(-1, 1))
                        .build())
                .build();

        qNetwork = new MultiLayerNetwork(conf);
        targetNetwork = new MultiLayerNetwork(conf);
        qNetwork.init();
        targetNetwork.init();

        reward = 0;
        this.actionCount = 0;
        this.avgQValue = 0;
        syncTargetNetwork();
        instance = this;

    }
    // 更新任务完成状态的方法
    public void updateTaskCompletionStatus(boolean isTaskCompleted) {
        totalTasks++;
        if (isTaskCompleted) {
            completedTasks++;
        }
    }

    // 获取任务完成率的方法
    public double getTaskCompletionRate() {
        if (totalTasks > 0) {
            return (double) completedTasks / totalTasks;
        } else {
            return 0.0;
        }
    }

    public static edu.boun.edgecloudsim.applications.deepLearning.DDQNAgent getInstance(){
        return instance;
    }

    public void setReward(double reward) {
        this.reward = reward;
    }

    public double getReward(){
        return this.reward;
    }

    public double getAvgQvalue(){
        if (this.actionCount > 0){
            return this.avgQValue / this.actionCount;
        }
        return this.avgQValue;
    }

    public double getQvalue(){
        return this.avgQValue;
    }

    public void resetQValue(){
        this.avgQValue = 0;
    }

    // Implemented for smooth transition but never used.
    // Instead, syncTargetNetwork() is used
    public void updateTargetNetwork(){
        ArrayList<INDArray> qNetworkWeights = new ArrayList<>();
        //System.out.println("SIZE: "+ qNetwork.getLayers().length);
        int numberOfLayers = qNetwork.getLayers().length;
        for (int i = 0; i < numberOfLayers; i++){
            String param = i + "_W";
            INDArray weightsOfLayer = this.qNetwork.getParam(param);
            qNetworkWeights.add(weightsOfLayer);
        }

        HashMap<Integer, INDArray > weightMap = new HashMap<>();
        int layerCounter = 0;
        for (INDArray weights:qNetworkWeights){
            double [] theweigths = weights.ravel().toDoubleVector();
            int counter = 0;
            INDArray intendedValues = Nd4j.zeros(weights.shape());
            String paramForTarget = layerCounter + "_W";
            INDArray weightsOfTarget = this.targetNetwork.getParam(paramForTarget);
            double [] targetWeights = weightsOfTarget.ravel().toDoubleVector();
            //System.out.println("Target weights: " + targetWeights[0]);
            for (double weigth: theweigths){
                double theNewValue = weigth * TAU + targetWeights[counter] * (1-TAU);
                intendedValues.putScalar(counter, theNewValue);
                counter++;
            }
            weightMap.put(layerCounter, intendedValues);
            layerCounter++;
        }

        for ( Map.Entry<Integer, INDArray>  weightsFromMap: weightMap.entrySet()) {
            int key = weightsFromMap.getKey();
            String param = key + "_W";
            //System.out.println("Param: "+ param);
            this.targetNetwork.setParam(param, weightsFromMap.getValue());

            //INDArray w0_qNetwork = this.targetNetwork.getParam(param);
            //System.out.println("weights for " +param+ " targetNetwork: " + w0_qNetwork);
        }

    }

    private void syncTargetNetwork(){
        this.targetNetwork = this.qNetwork.clone();
    }

    public int DoAction(edu.boun.edgecloudsim.applications.deepLearning.DeepEdgeState state){
        this.actionCount++;
        Random rand = new Random();
        double randomNumber = rand.nextFloat();
        this.counterForEpsilon++;

        // add
        double explorationRate = epsilon;
        if (taskCompletionRate < targetCompletionRate || averageReward < targetAverageReward) {
            explorationRate = Math.max(explorationRate * increaseFactor, MIN_EPSILON);
        } else {
            explorationRate = Math.max(explorationRate * EPSILON_FACTOR, MIN_EPSILON);
        }



        if (randomNumber <= this.epsilon){
            if (this.epsilon > MIN_EPSILON){
                this.epsilon = Math.max(this.epsilon * EPSILON_FACTOR, MIN_EPSILON);
            }
            return rand.nextInt(this.numberOfActions);
        }
        else{
            INDArray output = this.qNetwork.output(state.getState());
            //int action = output.argMax().getInt();
            return output.argMax().getInt();
        }
    }

    public void DDQN(edu.boun.edgecloudsim.applications.deepLearning.DeepEdgeState state, edu.boun.edgecloudsim.applications.deepLearning.DeepEdgeState nextState, double reward, int action, boolean isDone){

        edu.boun.edgecloudsim.applications.deepLearning.MemoryItem memoryItem = new edu.boun.edgecloudsim.applications.deepLearning.MemoryItem(state, nextState, reward, action, isDone);
        ArrayList<edu.boun.edgecloudsim.applications.deepLearning.MemoryItem> memoryItems = new ArrayList<>();
        if (this.memory.size() >= MEMORY_SIZE){
            this.memory.remove(0);
        }

        this.memory.add(memoryItem);

        if (this.memory.size() > BATCH_SIZE){
            memoryItems = getRandomMemoryItems();
        }
        else{
            memoryItems = this.memory;
        }

        for (edu.boun.edgecloudsim.applications.deepLearning.MemoryItem item:memoryItems) {
            INDArray target = this.qNetwork.output(item.getState().getState());
            if (item.isDone()){
                //System.out.println("Shape: " + target.shape());
                target.putScalar(item.getAction(), item.getValue());
            }
            else{
                int argmaxOfQNetworkForNextState = this.qNetwork.output(item.getNextState().getState()).argMax().getInt();
                INDArray targetNetworkOutput = this.targetNetwork.output(item.getNextState().getState());
                double targetValue = item.getValue() + this.DISCOUNT_FACTOR * targetNetworkOutput.getDouble(argmaxOfQNetworkForNextState);
                this.avgQValue += this.qNetwork.output(item.getNextState().getState()).max().getDouble();//训练网络
                target.putScalar(item.getAction(), targetValue);
            }
            this.qNetwork.fit(item.getState().getState(), target);

        }
        if (this.counterForEpsilon % C_SYNC == 0){
            syncTargetNetwork();
        }

    }

    private ArrayList<edu.boun.edgecloudsim.applications.deepLearning.MemoryItem> getRandomMemoryItems(){
        ArrayList<edu.boun.edgecloudsim.applications.deepLearning.MemoryItem> memoryItems = new ArrayList<>();
        ArrayList<Integer> selectedNumbers = new ArrayList<>();
        Random rand = new Random();

        for (int i=0; i < this.BATCH_SIZE; i++){
            int randomNumber = rand.nextInt(this.memory.size());

            while (selectedNumbers.contains(randomNumber)){
                randomNumber = rand.nextInt(this.memory.size());
            }

            selectedNumbers.add(randomNumber);
            memoryItems.add(this.memory.get(randomNumber));
        }
        return memoryItems;
    }

    public void saveModel(String episodeNo, Double reward, Double avgQValue) throws IOException {
        String modelName = "DDQNModel-";
        modelName = modelName + episodeNo + "-"+ reward + "-" + avgQValue;
        this.qNetwork.save(new File("model", modelName), false);
    }

    // add methods to update performance metrics
    // 添加方法用于更新任务完成率和平均奖励
    public void updatePerformanceMetrics(double newReward) {
        totalReward += newReward;
        averageReward = totalReward / actionCount;

        // 假设可以通过某种方式获取当前任务完成率，这里只是一个简单的示例
        taskCompletionRate = Math.random() * 0.5 + 0.5;
    }

}

