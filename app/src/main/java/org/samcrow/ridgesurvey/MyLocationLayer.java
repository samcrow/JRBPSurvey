package org.samcrow.ridgesurvey;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.overlay.Marker;

/**
 * A layer that displays the user's location
 */
public class MyLocationLayer extends Marker {

    /**
     * Minimum time between location updates, in milliseconds
     */
    private static final long MIN_TIME = 1000;

    /**
     * Minimum movement between location updates, in meters
     */
    private static final float MIN_DISTANCE = 0.0f;

    /**
     * The context
     */
    private final Context mContext;

    /**
     * The location manager that provides access to location services
     */
    private final LocationManager mLocationManager;

    /**
     * The location listener that receives updates
     */
    private final LocationListener mLocationListener;

    /**
     * If location updates are currently enabled
     */
    private boolean mUpdatesEnabled;

    /**
     * Creates a new layer
     *
     * The layer is initially invisible and does not request location information. Clients must
     * call {@link #resume()} to enable the layer.
     *
     * @param marker the marker to display at the user's location
     * @param context a context
     */
    public MyLocationLayer(Drawable marker, Context context) {
        super(new LatLong(0, 0), AndroidGraphicFactory.convertToBitmap(marker), 0, 0);
        mContext = context;
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        mLocationListener = new MyLocationListener();
        mUpdatesEnabled = false;
        // Hide until location is available
        setVisible(false);
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
                setVisible(true);
                setLatLong(new LatLong(lastLocation.getLatitude(), lastLocation.getLongitude()));
            }
            mLocationManager.requestLocationUpdates(providerName, MIN_TIME, MIN_DISTANCE, mLocationListener);
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
                        .setMessage("Please give the application permission to access your location")
                        .show();
            }
        }
    }

    private class MyLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            setVisible(true);
            setLatLong(new LatLong(location.getLatitude(), location.getLongitude()));
            requestRedraw();
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
