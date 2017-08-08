/*
 * Copyright 2017 Sam Crow
 *
 * This file is part of JRBP Survey.
 *
 * JRBP Survey is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JRBP Survey is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JRBP Survey.  If not, see <http://www.gnu.org/licenses/>.
 */

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
