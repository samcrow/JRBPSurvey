package org.samcrow.ridgesurvey;

/**
 * Averages several values
 */
public class MovingAverage {

    /**
     * The samples to average
     */
    private final double[] mSamples;

    /**
     * The index of the next sample to replace
     */
    private int mNextSample;

    public MovingAverage(int samples) {
        if (samples < 1) {
            throw new IllegalArgumentException("Cannot average fewer than one samples");
        }
        mSamples = new double[samples];
        mNextSample = 0;
    }

    /**
     * Adds a value to this moving average, replacing another one
     * @param value
     */
    public void add(double value) {
        mSamples[mNextSample] = value;
        mNextSample = (mNextSample + 1) % mSamples.length;
    }

    public double getAverage() {
        double sum = 0;
        for (double value : mSamples) {
            sum += value;
        }
        return sum / (double) mSamples.length;
    }
}
