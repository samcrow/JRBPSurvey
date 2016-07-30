/*
 * Copyright 2016 Sam Crow
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

package org.samcrow.ridgesurvey.map;

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;

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
