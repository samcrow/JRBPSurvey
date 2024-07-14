package org.samcrow.ridgesurvey.map;

import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.overlay.Marker;
import org.samcrow.ridgesurvey.LocationFinder.LocationListener;

/**
 * A layer that displays the user's location
 */
public class MyLocationLayer extends Marker implements LocationListener {

    /**
     * Creates a new layer
     * @param marker  the marker to display at the user's location
     */
    public MyLocationLayer(Drawable marker) {
        super(new LatLong(0, 0), AndroidGraphicFactory.convertToBitmap(marker), 0, 0);

        // Hide until location is available
        setVisible(false);
    }


    @Override
    public void newLocation(@NonNull LatLong position, double accuracy) {
        setLatLong(position);
        setVisible(true);
        requestRedraw();
    }
}
