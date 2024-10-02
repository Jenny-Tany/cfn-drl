package edu.boun.edgecloudsim.utils.PPO.neuralnet;

import java.util.Arrays;
import java.util.function.Function;

public class Matrix {
    private int[] dimensions;
    private double[] dat;

    public Matrix(int[] dimensions) {
        this.dimensions = dimensions;
        this.dat = new double[MathUtils.multiplyAll(dimensions)];
    }

    public Matrix(int sizeX, int sizeY) {
        this(new int[]{sizeX, sizeY});
    }

    public static Matrix wrap(double[] data, int[] dimensions) {
        Matrix m = new Matrix(dimensions);
        m.dat = Arrays.copyOf(data, data.length);
        return m;
    }

    public static Matrix wrap(double[] data) {
        Matrix m = new Matrix(new int[] {data.length, 1});
        m.dat = Arrays.copyOf(data, data.length);
        return m;
    }

    public void copyExternal(Matrix matrix) {

        if (matrix.dat.length != this.dat.length) {
            //Resize
            this.dat = new double[matrix.dat.length];
        }

        //Copy data
        System.arraycopy(matrix.dat, 0, this.dat, 0, this.dat.length);

        //Copy dimensions
        System.arraycopy(matrix.dimensions, 0, this.dimensions, 0, matrix.dimensions.length);
    }

    public double[] getData() {
        return dat;
    }

    public int[] getDimensions() {
        return this.dimensions;
    }

    public Matrix multiply(Matrix other) {
        if (other.dat.length != this.dat.length) {
            throw new IllegalArgumentException("Cannot multiply matrices of different lengths");
        }
        for (int i = 0; i < dat.length; i++) {
            dat[i] *= other.dat[i];
        }
        return this;
    }

    public Matrix vDot(Matrix other) {

        double[] arr = new double[other.dat.length / other.dimensions[0]];

        int counter = 0;
        for(int i = 0; i < arr.length; i++) {
            double sum = 0;
            for (double v : dat) {
                sum += v * other.dat[counter++];
            }
            arr[i] = sum;
        }

        return Matrix.wrap(arr);
    }

    public Matrix divide(Matrix other) {
        if (other.dat.length != this.dat.length) {
            throw new IllegalArgumentException("Cannot divide matrices of different lengths");
        }
        for (int i = 0; i < dat.length; i++) {
            dat[i] /= other.dat[i];
        }
        return this;
    }

    public Matrix add(Matrix other) {
        if (other.dat.length != this.dat.length) {
            throw new IllegalArgumentException("Cannot add matrices of different lengths (" + Arrays.toString(this.dimensions) + ", " + Arrays.toString(other.dimensions) + ")");
        }
        for (int i = 0; i < dat.length; i++) {
            dat[i] += other.dat[i];
        }
        return this;
    }

    public Matrix subtract(Matrix other) {
        if (other.dat.length != this.dat.length) {
            throw new IllegalArgumentException("Cannot add matrices of different lengths");
        }
        for (int i = 0; i < dat.length; i++) {
            dat[i] -= other.dat[i];
        }
        return this;
    }

    public Matrix multiply(double value) {
        for (int i = 0; i < dat.length; i++) {
            dat[i] *= value;
        }
        return this;
    }

    public Matrix divide(double value) {
        for (int i = 0; i < dat.length; i++) {
            dat[i] /= value;
        }
        return this;
    }

    public Matrix add(double value) {
        for (int i = 0; i < dat.length; i++) {
            dat[i] += value;
        }
        return this;
    }

    public Matrix vInterMultiply(Matrix other) {
        double[] arr = new double[other.dimensions[0] * this.dimensions[0]];

        int c = 0;
        for(int i = 0; i < this.dat.length; i++) {
            for(int l = 0; l < other.dat.length; l++) {
                arr[c++] = other.dat[l] * this.dat[i];
            }
        }

        return Matrix.wrap(arr, new int[] {other.dimensions[0], this.dimensions[0]});
    }

    public Matrix copy() {
        Matrix matrix = new Matrix(Arrays.copyOf(this.dimensions, this.dimensions.length));
        System.arraycopy(dat, 0, matrix.dat, 0, dat.length);
        return matrix;
    }
// TODO
//    public double[] getColumn(int index) {
//        int columnSize = dimensions[]
//    }
//
//    public double[] getRow(int index) {
//
//    }

    public double getElement(int x, int y) {
        return dat[x * dimensions[0] + y];
    }

    public Matrix apply(Function<Double, Double> function) {
        for (int i = 0; i < dat.length; i++) {
            dat[i] = function.apply(dat[i]);
        }
        return this;
    }
    
    public double avg() {
        return MathUtils.avg(dat);
    }

    public Matrix transpose() {
        double[] dataCopy = Arrays.copyOf(this.dat, this.dat.length);
        int oldDimX = dimensions[0];
        dimensions[0] = dimensions[1];
        dimensions[1] = oldDimX;
        for(int i = 0; i < dataCopy.length; i++) {
            int x = i % dimensions[0];
            int y = (i - x) / dimensions[0];
            this.dat[i] = dataCopy[x * oldDimX + y];
        }
        return this;
    }

    public double total() {
        double sum = 0;
        for(int i = 0; i < dat.length; i++) {
            sum += dat[i];
        }
        return sum;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for(int i = 0; i < dat.length; i++) {
            builder.append(dat[i]).append(", ");
            if(i % dimensions[0] == dimensions[0]-1) {
                builder.append("\n");
            }
        }
        return builder.toString();
    }
}
