package edu.boun.edgecloudsim.applications.computingSim;

import edu.boun.edgecloudsim.core.SimSettings;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class DQNAgent {
    private MultiLayerNetwork qNetwork;
    private MultiLayerNetwork targetNetwork;
    private double DISCOUNT_FACTOR = 0.95;//0.8
    private double totalReward;
    private double LEARNING_RATE = 0.00005;//0.0001
    private double epsilon = 1;
    private final double MIN_EPSILON = 0.1;
    private final double EPSILON_FACTOR = 0.999;//0.99
    private final int MEMORY_SIZE = 1000000;
    private int BATCH_SIZE = 4;
    private final double TAU = 0.01;
    private final int C_SYNC = 10;
    private ArrayList<Memory> memory; //it needs a new memoryItem structure
    private int numberOfActions;
    private int counterForEpsilon;
    private static DQNAgent instance = null;

    private double reward;
    private double avgQValue;
    private int actionCount,featureCount;


    public DQNAgent(int featureCount){
        this.counterForEpsilon = 0;
        totalReward = 0;

        this.numberOfActions = SimSettings.getInstance().getActionSize();

//        this.DISCOUNT_FACTOR = SimSettings.getInstance().getLearningRate();
        int hidden = 64;
        memory = new ArrayList<Memory>();
//        含两个隐藏层和一个输出层的神经网络，其中隐藏层使用ReLU激活函数，输出层使用恒等激活函数，并使用Adam优化算法进行权重更新
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .updater(new Adam(LEARNING_RATE))
                .seed(1234)
                .miniBatch(false)
                .list()
                .layer(new DenseLayer.Builder()
                        .nIn(featureCount)//输入层数量state.feture
                        .nOut(hidden)
                        .activation(Activation.RELU)//负值不应过多
                        // random initialize weights with values between 0 and 1
                        .weightInit(new UniformDistribution(-1, 1))//权重初始化的范围应该与输入数据的范围相匹配
                        .build())
                .layer(new DenseLayer.Builder()
                        .nIn(hidden)
                        .nOut(hidden)
                        .activation(Activation.RELU)
                        // random initialize weights with values between 0 and 1
                        .weightInit(new UniformDistribution(-1, 1))
                        .build())
                .layer(new OutputLayer.Builder(LossFunction.MSE) //create hidden layer
                        .nIn(hidden)
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

    public static DQNAgent getInstance(){
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

    public int DoAction(CFNState state){
        this.actionCount++;
        Random rand = new Random();
        double randomNumber = rand.nextFloat();
        this.counterForEpsilon++;
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

    public void DDQN(CFNState state, CFNState nextState, double reward, int action, boolean isDone){

        Memory memoryItem = new Memory(state, nextState, reward, action, isDone);
        ArrayList<Memory> memoryItems = new ArrayList<>();
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

        for (Memory item:memoryItems) {
            INDArray target = this.qNetwork.output(item.getState().getState());
            if (item.isDone()){
                //System.out.println("Shape: " + target.shape());
                target.putScalar(item.getAction(), item.getValue());//奖励设为目标Q值
            }
            else{
                int argmaxOfQNetworkForNextState = this.qNetwork.output(item.getNextState().getState()).argMax().getInt();//最大索引
                INDArray targetNetworkOutput = this.targetNetwork.output(item.getNextState().getState());//下一个状态的输出
                double targetValue = item.getValue() + this.DISCOUNT_FACTOR * targetNetworkOutput.getDouble(argmaxOfQNetworkForNextState);//计算目标Q
                this.avgQValue += this.qNetwork.output(item.getNextState().getState()).max().getDouble();//监控平均Q
                target.putScalar(item.getAction(), targetValue);//设置目标Q
            }
            this.qNetwork.fit(item.getState().getState(), target);

        }
        if (this.counterForEpsilon % C_SYNC == 0){
            syncTargetNetwork();
        }

    }

    private ArrayList<Memory> getRandomMemoryItems(){
        ArrayList<Memory> memoryItems = new ArrayList<>();
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

    public void saveModel(String outputFolder,String episodeNo, Double reward, Double avgQValue) throws IOException {
        String modelName = "DDQNModel";
//        modelName = modelName + episodeNo + "-"+ reward + "-" + avgQValue;
        modelName = modelName + "-"+SimSettings.getInstance().getTOPO()
                +"-"+SimSettings.getInstance().getLoadSize()
                +"-"+SimSettings.getInstance().getActionSize()
                +"-"+SimSettings.getInstance().getMaxNumOfMobileDev();
//        this.qNetwork.save(new File("model", modelName), false);//model路径下
        this.qNetwork.save(new File(outputFolder, modelName), false);
    }

}

