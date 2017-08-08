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

package org.samcrow.ridgesurvey.map;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.Layer;
import org.samcrow.ridgesurvey.LocationFinder.LocationListener;
import org.samcrow.ridgesurvey.R;
import org.samcrow.ridgesurvey.Route;
import org.samcrow.ridgesurvey.SelectionManager.SelectionListener;
import org.samcrow.ridgesurvey.Site;

/**
 * Displays a line from the user's location to a selected site
 */
public class RouteLineLayer extends Layer implements LocationListener, SelectionListener {

    /**
     * The user's position, or null if it is unknown
     */
    @Nullable
    private LatLong mPosition;

    /**
     * The current selected site, or null if none is selected
     */
    @Nullable
    private Site mSelectedSite;

    /**
     * The paint used to draw the line from the user's location to the selected site
     */
    @NonNull
    private final Paint mLinePaint;

    public RouteLineLayer(@NonNull Context context) {
        // Resources.getColor(int) is another method that deprecated in API 23 with a replacement
        // not available until API 23.
        @SuppressWarnings("deprecation")
        final int lineColor = context.getResources().getColor(R.color.map_route_line);

        mLinePaint = AndroidGraphicFactory.INSTANCE.createPaint();
        mLinePaint.setColor(lineColor);
        mLinePaint.setStrokeWidth(4);
    }

    @Override
    public void newLocation(@NonNull LatLong position, double accuracy) {
        mPosition = position;
        requestRedraw();
    }

    @Override
    public void selectionChanged(@Nullable Site newSelection, @Nullable Route route) {
        mSelectedSite = newSelection;
        requestRedraw();
    }

    @Override
    public void draw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point topLeftPoint) {
        // Draw a line from the user's position to the selected site
        if (mSelectedSite != null && mPosition != null) {
            final long mapSize = MercatorProjection.getMapSize(zoomLevel,
                    displayModel.getTileSize());
            final LatLong sitePos = mSelectedSite.getPosition();
            final Point sitePoint = new Point(
                    MercatorProjection.longitudeToPixelX(sitePos.longitude,
                            mapSize) - topLeftPoint.x,
                    MercatorProjection.latitudeToPixelY(sitePos.latitude,
                            mapSize) - topLeftPoint.y);
            final Point locationPoint = new Point(
                    MercatorProjection.longitudeToPixelX(mPosition.longitude,
                            mapSize) - topLeftPoint.x,
                    MercatorProjection.latitudeToPixelY(mPosition.latitude,
                            mapSize) - topLeftPoint.y);

            canvas.drawLine((int) locationPoint.x, (int) locationPoint.y, (int) sitePoint.x,
                    (int) sitePoint.y, mLinePaint);
        }
    }
}
