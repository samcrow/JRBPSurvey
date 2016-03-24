package org.samcrow.ridgesurvey;

import android.app.AlertDialog;
import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;

import org.mapsforge.core.model.LatLong;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Requests and provides location information
 */
public class LocationFinder {
    /**
     * Minimum time between location updates, in milliseconds
     */
    private static final long MIN_TIME = 1000;

    /**
     * Minimum movement between location updates, in meters
     */
    private static final float MIN_DISTANCE = 0.0f;

    public interface LocationListener {
        /**
         * Called when a new location is received
         * @param position the location
         * @param accuracy the approximate accuracy of the location, in meters
         */
        void newLocation(@NonNull LatLong position, double accuracy);
    }

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
        final Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        final String providerName = mLocationManager.getBestProvider(criteria, true);
        if (providerName == null) {
            // No provider
            new AlertDialog.Builder(mContext)
                    .setTitle("Location not available")
                    .setMessage("Please ensure that GPS or another location provider is enabled")
                    .show();
        } else {
            // Found provider
            final Location lastLocation = mLocationManager.getLastKnownLocation(providerName);
            if (lastLocation != null) {
                notifyListeners(lastLocation);
            }
            mLocationManager.requestLocationUpdates(providerName, MIN_TIME, MIN_DISTANCE,
                    mLocationListener);
        }
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
                        .setMessage(
                                "Please give the application permission to access your location")
                        .show();
            }
        }
    }

    public void addListener(LocationListener listener) {
        mListeners.add(listener);
    }

    private void notifyListeners(Location location) {
        final LatLong ll = new LatLong(location.getLatitude(), location.getLongitude());
        final double accuracy = location.getAccuracy();
        for (LocationListener listener : mListeners) {
            listener.newLocation(ll, accuracy);
        }
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
