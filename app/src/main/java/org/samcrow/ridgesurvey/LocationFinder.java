/*
 * Copyright (c) 2025 Sam Crow
 *
 * This file is part of JRBPSurvey.
 *
 * JRBPSurvey is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * JRBPSurvey is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Foobar.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package org.samcrow.ridgesurvey;

import android.app.AlertDialog;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;

import java.util.LinkedHashSet;
import java.util.Set;

import androidx.annotation.NonNull;

import org.maplibre.android.geometry.LatLng;

/**
 * Requests and provides location information
 */
public class LocationFinder {
    /**
     * Minimum time between location updates, in milliseconds
     */
    private static final long MIN_TIME = 500;

    /**
     * Minimum movement between location updates, in meters
     */
    private static final float MIN_DISTANCE = 0.0f;
    /**
     * The context
     */
    @NonNull
    private final Context mContext;
    /**
     * The location manager that provides access to location services
     */
    @NonNull
    private final LocationManager mLocationManager;
    /**
     * The location listener that receives updates
     */
    @NonNull
    private final android.location.LocationListener mLocationListener;
    /**
     * The location listeners that get notified with new locations
     */
    @NonNull
    private final Set<LocationListener> mListeners;
    /**
     * If location updates are currently enabled
     */
    private boolean mUpdatesEnabled;

    public LocationFinder(@NonNull Context context) {
        Objects.requireNonNull(context);
        mContext = context;
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        mLocationListener = new MyLocationListener();
        mUpdatesEnabled = false;
        mListeners = new LinkedHashSet<>();
    }

    private void requestLocation() throws SecurityException {
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, MIN_DISTANCE,
                mLocationListener);
    }

    /**
     * Suspends location updates, if they are not suspended
     */
    public void pause() {
        if (mUpdatesEnabled) {
            mUpdatesEnabled = false;
            try {
                mLocationManager.removeUpdates(mLocationListener);
            } catch (SecurityException e) {
                // Ignore
            }
        }
    }

    /**
     * Starts or resumes location updates, if they are not active
     */
    public void resume() {
        if (!mUpdatesEnabled) {
            try {
                requestLocation();
                mUpdatesEnabled = true;
            } catch (SecurityException e) {
                // TODO: Request permission?
                new AlertDialog.Builder(mContext)
                        .setTitle("Location not available")
                        .setMessage("Please give the application permission to access your location")
                        .setNeutralButton(android.R.string.ok, null)
                        .show();
            } catch (IllegalArgumentException e) {
                new AlertDialog.Builder(mContext)
                        .setTitle("GPS location provider not available")
                        .setMessage("Please enable GPS")
                        .setNeutralButton(android.R.string.ok, null)
                        .show();
            }
        }
    }

    public void addListener(LocationListener listener) {
        mListeners.add(listener);
    }

    private void notifyListeners(Location location) {
        final LatLng ll = new LatLng(location.getLatitude(), location.getLongitude());
        final double accuracy = location.getAccuracy();
        for (LocationListener listener : mListeners) {
            listener.newLocation(ll, accuracy);
        }
    }

    public interface LocationListener {
        /**
         * Called when a new location is received
         *
         * @param position the location
         * @param accuracy the approximate accuracy of the location, in meters
         */
        void newLocation(@NonNull LatLng position, double accuracy);
    }

    private class MyLocationListener implements android.location.LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            notifyListeners(location);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    }
}
