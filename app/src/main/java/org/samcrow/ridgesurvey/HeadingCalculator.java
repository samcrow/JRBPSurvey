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

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

/**
 * Retrieves information from sensors and calculates the heading of the device
 */
public class HeadingCalculator {
    private static final String TAG = HeadingCalculator.class.getSimpleName();

    /**
     * Period between samples, in microseconds
     */
    private static final int SAMPLING_PERIOD = 100000;

    public interface HeadingListener {
        /**
         * Called when a new heading value is available
         * @param heading the new heading in degrees, in the range [0, 360)
         */
        void headingUpdated(double heading);
    }

    /**
     * The sensor manager
     */
    private final SensorManager mSensorManager;

    /**
     * The event listener
     */
    private final HeadingCalculationListener mListener;
    /**
     * If sensor updates are enabled
     */
    private boolean mEnabled;

    /**
     * The acceleration sensor
     */
    private final Sensor mAccelSensor;
    /**
     * The magnetism sensor
     */
    private final Sensor mMagnetSensor;

    /**
     * The heading listener, or null for none
     */
    private HeadingListener mHeadingListener;


    public HeadingCalculator(Context context) {
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mListener = new HeadingCalculationListener();
        // Check for sensors
        mAccelSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (mAccelSensor == null || mMagnetSensor == null) {
            Log.i(TAG, "Device does not support acceleration and magnetic field sensing");
        }
        mHeadingListener = null;
        mEnabled = false;
    }

    public void pause() {
        if (mEnabled) {
            mSensorManager.unregisterListener(mListener);
            mEnabled = false;
        }
    }

    public void resume() {
        if (!mEnabled && isAvailable()) {
            mSensorManager.registerListener(mListener, mAccelSensor, SAMPLING_PERIOD);
            mSensorManager.registerListener(mListener, mMagnetSensor, SAMPLING_PERIOD);
            mEnabled = true;
        }
    }

    public boolean isAvailable() {
        return mAccelSensor != null && mMagnetSensor != null;
    }

    public void setHeadingListener(HeadingListener listener) {
        mHeadingListener = listener;
    }

    private class HeadingCalculationListener implements SensorEventListener {

        /**
         * Gravity
         */
        private float[] mGravity;
        /**
         * Magnetism
         */
        private float[] mGeomagnetic;

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                mGravity = event.values;
            }
            if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                mGeomagnetic = event.values;
            }
            if (mGravity != null && mGeomagnetic != null) {
                float[] R = new float[9];
                float[] I = new float[9];
                boolean success = SensorManager.getRotationMatrix(R, I, mGravity,
                        mGeomagnetic);
                if (success) {
                    float orientation[] = new float[3];
                    SensorManager.getOrientation(R, orientation);
                    final float azimuth = orientation[0];
                    final double headingDegrees = (Math.toDegrees(azimuth) + 360.0) % 360.0;
                    if (mHeadingListener != null) {
                        mHeadingListener.headingUpdated(headingDegrees);
                    }
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    }
}
