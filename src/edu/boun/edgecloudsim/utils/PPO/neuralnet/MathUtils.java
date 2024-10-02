/*
 * Copyright (c) 2020. UltraDev
 */

package edu.boun.edgecloudsim.utils.PPO.neuralnet;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class MathUtils {
    /**
     * Random once used for all random utility methods in this class
     */
    private static Random rand = ThreadLocalRandom.current();

    /**
     * Generate a random integer between min and max
     *
     * @param min Minimum value
     * @param max Maxmimum value
     * @return Random integer between min and max
     */
    public static int random(int min, int max) {
        return rand.nextInt((max - min) + 1) + min;
    }


    /**
     * Generate a random double between min and max
     *
     * @param min Minimum value
     * @param max Maxmimum value
     * @return Random double between min and max
     */
    public static double random(double min, double max) {
        return rand.nextDouble() * (max - min) + min;
    }

    /**
     * Round a number randomly, probability of rounding up is the decimal of the number
     *
     * @param i Number
     * @return Rounded number
     */
    public static int roundRand(double i) {
        int am = (int) Math.floor(i);
        double ret = i - am;
        if (ret >= random(0.0, 1.0)) {
            am++;
        }
        return am;
    }

    /**
     * Check whether a random number [0, max] is within range [0, i]
     *
     * @param i   Value
     * @param max Maximum
     * @return {@code true} if the condition is satisfied
     */
    public static boolean isRandom(double i, double max) {
        return (i >= random(0.0, max));
    }

    /**
     * Do not touch
     * Only use this method if you understand what it does!
     *
     * @param min    Minimum value
     * @param max    Maximum value
     * @param weight Weight strength
     * @return Value
     */
    public static double weightedRandom(double min, double max, int weight) {
        double num = min;
        double nmax = max - min;
        for (int i = 0; i < weight; i++) {
            num += Math.random() * (nmax / weight);
        }
        return num;
    }

    /**
     * Get a weighted random integer between two values
     *
     * @param min    Minimum value
     * @param max    Maximum value
     * @param weight Weight
     * @return Weighted random number
     */
    public static int weightedRandomI(int min, int max, int weight) {
        return (int) Math.floor(weightedRandom(min, max, weight));
    }

    /**
     * Round a value to a certain amount of decimal places
     *
     * @param val    Number
     * @param places Decimal places
     * @return Rounded number
     */
    public static double round(double val, int places) {
        double qq = Math.pow(10, places);
        return Math.round(val * qq) / qq;
    }

    public static int clampLoop(int i, int min, int max) {
        if (i < min) {
            return clampLoop(i + 1 + max - min, min, max);
        }
        if (i > max) {
            return clampLoop(i - 1 - max + min, min, max);
        }
        return i;
    }

    /**
     * Generate a low-weighted random integer between min and max
     *
     * @param min    Minimum
     * @param max    Maximum
     * @param weight Weight
     * @return Low-weighted random integer
     */
    public static int lowWeightedInt(int min, int max, double weight) {
        return (int) (weightedDoubleInternal(weight) * (max - min) + min);
    }

    /**
     * Generate a high-weighted random integer between min and max
     *
     * @param min    Minimum
     * @param max    Maximum
     * @param weight Weight
     * @return High-weighted random integer
     */
    public static int highWeightedInt(int min, int max, double weight) {
        return (int) ((1 - weightedDoubleInternal(weight)) * (max - min) + min);
    }

    /**
     * Generate a weighted double
     *
     * @param weight Weight
     * @return Weighted double
     */
    private static double weightedDoubleInternal(double weight) {
        double d = rand.nextDouble();
        return Math.pow(d, weight + 1);
    }

    /**
     * Series approximation of the Lambert W function (product log function)
     *
     * @param z Number
     * @return W(z)
     */
    public static double lw(double z) {
        double S = 0.0;
        for (int n = 1; n <= 100; n++) {
            double Se = S * StrictMath.pow(StrictMath.E, S);
            double S1e = (S + 1) *
                    StrictMath.pow(StrictMath.E, S);
            double PRECISION = 1E-12;
            if (PRECISION > StrictMath.abs((z - Se) / S1e)) {
                return S;
            }
            S -= (Se - z) / (S1e - (S + 2) * (Se - z) / (2 * S + 2));
        }
        return S;
    }

    public static double sumGeo(double a, double r, int n) {
        return a * (1 - Math.pow(r, n)) / (1 - r);
    }

    public static double sumSumGeo(double a, double r, int n) {
        return a * (n - sumGeo(r, r, n)) / (1 - r);
    }

    public static double avg(double[] nums) {
        int size = nums.length;
        double avg = 0;
        for(int i = 0; i < size; i++) {
            avg += (nums[i] - avg) / (i+1);
        }
        return avg;
    }

    public static double avg(int[] nums) {
        int size = nums.length;
        double avg = 0;
        for(int i = 0; i < size; i++) {
            avg += (nums[i] - avg) / (i+1);
        }
        return avg;
    }

    public static double stdDev(double[] nums) {
        double avg = avg(nums);
        double[] dev = new double[nums.length];
        for(int i = 0; i < nums.length; i++) {
            double d = nums[i] - avg;
            dev[i] = d * d;
        }
        return Math.sqrt(avg(dev));
    }

    public static double reRandom(double min, double max, int power) {
        double diff = max - min;
        double hDiff = diff / 2;

        double rand1 = hDiff;
        double rand2;

        for (int i = 0; i < power; i ++) {
            rand2 = rand.nextDouble() * hDiff;
            rand1 = rand1 + random(-rand2, rand2);

            hDiff /= 2;
        }

        return hDiff + min;
    }

    public static int reRandom(int min, int max, int power) {
        return (int) reRandom(min, (double) max, power);
    }

    public static int multiplyAll(int[] ints) {
        int ret = 1;
        for (int i : ints) {
            ret *= i;
        }
        return ret;
    }

    public static int multiplyAll(int[] ints, int[] ints2) {
        int ret = 1;
        for (int i = 0; i < ints.length; i ++) {
            ret *= ints[i] * ints2[i];
        }
        return ret;
    }

    public static int multiplyN(int[] ints, int count) {
        int ret = 1;
        for (int i = 0; i < count; i ++) {
            ret *= ints[i];
        }
        return ret;
    }

    public static int multiplyN(int[] ints, int[] ints2, int count) {
        int ret = 1;
        for (int i = 0; i < count; i ++) {
            ret *= ints[i] * ints2[i];
        }
        return ret;
    }

    public static void main(String[] args) {
        double[] arr = new double[] {0, 1, 1, 10};
        System.out.println("Avg: " + avg(arr) + " dev: " + stdDev(arr));
    }
}
