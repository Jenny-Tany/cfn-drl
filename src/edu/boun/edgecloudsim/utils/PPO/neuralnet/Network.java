package edu.boun.edgecloudsim.utils.PPO.neuralnet;

import java.util.ArrayList;
import java.util.List;

public class Network {
    private double learningRate = 0.01D;
    private final List<DenseLayer> layers = new ArrayList<>();

    public void addLayer(DenseLayer layer) {
        this.layers.add(layer);
    }

    public double[] propagate(double[] input) {
        Matrix current = Matrix.wrap(input);
        for(DenseLayer layer : layers) {
            current = layer.propagate(current);
        }
        return current.getData();
    }

    public List<DenseLayer> getLayers() {
        return layers;
    }

    public double train(double[][] trainingExamples, double[][] trainingLabels, int batchSize) {
        assert trainingExamples.length == trainingLabels.length;

        int counter = 0;

        double loss = 0;

        for(int i = 0; i < trainingExamples.length; i++) {

            double[] example = trainingExamples[i];
            double[] label = trainingLabels[i];

            Matrix out = Matrix.wrap(propagate(example));

            loss += Matrix.wrap(label).copy().subtract(out).total();

            Matrix current = Matrix.wrap(label).subtract(out);

            for (int j = layers.size()-1; j >= 0; j--) {
                DenseLayer layer = layers.get(j);
                current = layer.backPropagate(current);
            }

            counter++;
            if(counter == batchSize) {
                layers.forEach((l) -> l.updateParams(learningRate));
                counter = 0;
            }
        }

        loss /= trainingExamples.length;

        return loss;
    }

    public double getLearningRate() {
        return this.learningRate;
    }

    public void setLearningRate(double value) {
        this.learningRate = value;
    }
}
