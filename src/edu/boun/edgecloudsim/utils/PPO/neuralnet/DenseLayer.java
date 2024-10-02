package edu.boun.edgecloudsim.utils.PPO.neuralnet;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

public class DenseLayer {
    private final Matrix weights;
    private final Matrix biases;
    private Matrix m;
    private Matrix v;
    private final Matrix accUpdates;
    private final Matrix accBiasUpdates;
    private final Matrix nonActivatedState;
    private final Matrix prevInput;
    //Adaptive moment estimation
    private double beta_1 = 0.9D;
    private double beta_2 = 0.999D;
    private double updateIteration = 1;
    private double nonZero = 0.000000001D;
    private int accumulationNum = 0;
    private int activationType = 0;

    private Function<Double, Double> activationFunction = (a) -> a < 0 ? 0 : a; //Default is ReLU
    private Function<Double, Double> activationFunctionDer = (a) -> a < 0 ? 0 : 1D; //Default is ReLU

    public DenseLayer(int inputSize, int ownSize) {
        this.weights = new Matrix(inputSize, ownSize);

        m = this.weights.copy();
        v = this.weights.copy();

        this.biases = new Matrix(ownSize, 1);
        this.nonActivatedState = new Matrix(ownSize, 1);
        this.prevInput = new Matrix(inputSize, 1);
        this.accUpdates = weights.copy();
        this.accBiasUpdates = biases.copy();

        //Initialize weights and biases
        Random random = ThreadLocalRandom.current();
        this.weights.apply((a) -> random.nextGaussian() / Math.sqrt(inputSize));
        this.biases.apply((a) -> random.nextGaussian() / Math.sqrt(inputSize));
    }

    public double getUpdateIteration() {
        return updateIteration;
    }

    public Matrix getWeights() {
        return weights;
    }

    public Matrix getBiases() {
        return biases;
    }

    public Matrix getM() {
        return m;
    }

    public Matrix getV() {
        return v;
    }

    public void setM(Matrix m) {
        this.m = m;
    }

    public void setV(Matrix v) {
        this.v = v;
    }

    public void setWeights(Matrix weights) {
        this.weights.copyExternal(weights);
    }

    public void setBiases(Matrix biases) {
        this.biases.copyExternal(biases);
    }

    //Pretty much just for convenience
    public DenseLayer setActivationFunctionSigmoid() {

        this.activationFunction = (a) -> 1D / (1D + Math.exp(-a));
        this.activationFunctionDer = (a) -> {
            double s = 1D / (1D + Math.exp(-a));
            return s * (1D - s);
        };

        activationType = 1;

        return this;
    }

    public DenseLayer setActivationFunctionLinear() {
        this.activationFunction = (a) -> a;
        this.activationFunctionDer = (a) -> 1D;

        activationType = 2;

        return this;
    }

    public int getActivationType() {
        return activationType;
    }

    public Matrix propagate(Matrix input) {
        this.prevInput.copyExternal(input);
        input = input.vDot(this.weights).add(biases);
        this.nonActivatedState.copyExternal(input);
        return input.apply(activationFunction);
    }

    public Matrix getActivatedState() {
        return this.nonActivatedState.copy().apply(activationFunction);
    }


    public Matrix backPropagate(Matrix input) {
        input.multiply(this.nonActivatedState.copy().apply(activationFunctionDer));
        this.accBiasUpdates.add(input);
        this.accUpdates.add(input.vInterMultiply(this.prevInput));
        accumulationNum++;

        return input.vDot(this.weights.copy().transpose());
    }

    public void updateParams(double learningRate) {
        if (accumulationNum == 0)
            return;

        //Update using Adam method
        m.multiply(beta_1).add(accUpdates.copy().multiply(1D - beta_1));
        Matrix g2 = accUpdates;
        g2.apply((t) -> Math.pow(t, 2));
        v.multiply(beta_2).add(g2.multiply(1D - beta_2));
        Matrix m_hat = m.divide(1D - Math.pow(beta_1, updateIteration));
        Matrix v_hat = v.divide(1D - Math.pow(beta_2, updateIteration));
        v_hat.apply(Math::sqrt).add(nonZero);
        m_hat.divide(v_hat).multiply(learningRate / accumulationNum);

        this.weights.add(m_hat);

        this.biases.add(this.accBiasUpdates.multiply(learningRate / accumulationNum));

        accUpdates.multiply(0);
        accBiasUpdates.multiply(0);
        accumulationNum = 0;
        updateIteration++;
    }

    public void resetUpdateIteration() {
        updateIteration = 1;
    }

    public void setUpdateIteration(double updateIteration) {
        this.updateIteration = updateIteration;
    }
}

