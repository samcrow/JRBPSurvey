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

import androidx.annotation.NonNull;

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.FontFamily;
import org.mapsforge.core.graphics.FontStyle;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.Layer;
import org.samcrow.ridgesurvey.Objects;
import org.samcrow.ridgesurvey.Route;
import org.samcrow.ridgesurvey.SelectionManager;
import org.samcrow.ridgesurvey.Site;
import org.samcrow.ridgesurvey.data.IdentifiedObservation;
import org.samcrow.ridgesurvey.data.ObservationDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A layer that displays a route
 */
public class RouteLayer extends Layer {

    /**
     * The radius of a site marker, in meters
     */
    private static final float MARKER_RADIUS = 10;

    /**
     * The radius of a marker for a visited site, in meters
     */
    private static final float MARKER_RADIUS_VISITED = 6;

    /**
     * The width of site-connecting lines, in meters
     */
    private static final float LINE_WIDTH = 3;
    /**
     * The height of site label text, in meters
     */
    private static final float SITE_LABEL_HEIGHT = 40;

    /**
     * Latitude/longitude distance, in degrees, that is the tolerance for clicking to select a site
     */
    private static final double CLICK_DISTANCE_THRESHOLD = 0.0005;
    /**
     * Radius of the selected site marker, in meters
     */
    private static final float SELECTED_RADIUS = 20;
    /**
     * Route label text height, pixels
     */
    private static final int ROUTE_LABEL_HEIGHT = 24;
    /**
     * Threshold to switch between showing route labels and showing site labels
     */
    private static final byte LABEL_THRESHOLD = 16;

    /**
     * The observation database
     */
    @NonNull
    private final ObservationDatabase mDatabase;

    /**
     * The approximate center of the route, derived from the route sites
     */
    @NonNull
    private final LatLong mCenter;

    /**
     * The paint used to draw site markers
     */
    @NonNull
    private final Paint mPaint;

    /**
     * A paint used to draw markers for sites that have been visited
     */
    @NonNull
    private final Paint mVisitedPaint;

    /**
     * The paint used to draw site IDs
     */
    @NonNull
    private final Paint mIdPaint;
    /**
     * The paint used to draw contrasting backgrounds of site IDs
     */
    @NonNull
    private final Paint mIdBackgroundPaint;

    /**
     * The paint used to draw the route name
     */
    @NonNull
    private final Paint mRouteNamePaint;

    /**
     * The paint used to draw a contrasting background of the route name
     */
    @NonNull
    private final Paint mRouteNameBackgroundPaint;

    /**
     * A paint used to indicate the selected site
     */
    @NonNull
    private final Paint mSelectedPaint;

    /**
     * The route to display (not ordered)
     */
    @NonNull
    private final Route mRoute;

    /**
     * The sites in the route
     */
    @NonNull
    private final List<VisitedSite> mSites;

    /**
     * The selection manager that tracks the selected site
     */
    @NonNull
    private final SelectionManager mSelectionManager;

    /**
     * Creates a new route layer
     *
     * @param database         an observation database to use. Must not be null.
     * @param route            the route to display. Must not be null.
     * @param color            the color to use for this route, in the format used by {@link android.graphics.Color}
     * @param selectionManager A selection manager to track the selected site. Must not be null.
     */
    public RouteLayer(@NonNull ObservationDatabase database, @NonNull Route route, int color,
                      @NonNull SelectionManager selectionManager) {
        Objects.requireAllNonNull(database, route, selectionManager);
        mDatabase = database;
        mRoute = route;
        mCenter = route.getCenter();

        // Copy sites in, initially not visited
        final List<Site> sites = route.getSites();
        mSites = new ArrayList<>(sites.size());
        for (Site site : sites) {
            mSites.add(new VisitedSite(site, false));
        }

        mSelectionManager = selectionManager;

        mPaint = AndroidGraphicFactory.INSTANCE.createPaint();
        mPaint.setColor(color);

        mVisitedPaint = AndroidGraphicFactory.INSTANCE.createPaint();
        mVisitedPaint.setColor(android.graphics.Color.BLACK);

        mIdPaint = AndroidGraphicFactory.INSTANCE.createPaint();
        mIdPaint.setColor(Color.BLACK);
        mIdPaint.setTypeface(FontFamily.SANS_SERIF, FontStyle.BOLD);

        mIdBackgroundPaint = AndroidGraphicFactory.INSTANCE.createPaint();
        mIdBackgroundPaint.setColor(Color.WHITE);
        mIdBackgroundPaint.setStyle(Style.STROKE);
        mIdBackgroundPaint.setTypeface(FontFamily.SANS_SERIF, FontStyle.BOLD);

        mSelectedPaint = AndroidGraphicFactory.INSTANCE.createPaint();
        mSelectedPaint.setColor(android.graphics.Color.argb(0x40, 0xFF, 0x0, 0x0));

        mRouteNamePaint = AndroidGraphicFactory.INSTANCE.createPaint();
        mRouteNamePaint.setColor(Color.BLACK);
        mRouteNamePaint.setTypeface(FontFamily.SANS_SERIF, FontStyle.BOLD);
        mRouteNamePaint.setTextSize(ROUTE_LABEL_HEIGHT);

        mRouteNameBackgroundPaint = AndroidGraphicFactory.INSTANCE.createPaint(mRouteNamePaint);
        mRouteNameBackgroundPaint.setColor(Color.WHITE);
        mRouteNameBackgroundPaint.setStyle(Style.STROKE);
        mRouteNameBackgroundPaint.setStrokeWidth(4);

        updateVisitedSites();
    }

    /**
     * Updates the visited state of each site from the database
     */
    public synchronized void updateVisitedSites() {
        for (VisitedSite site : mSites) {
            final IdentifiedObservation observation = mDatabase.getObservationForSite(site.getSite().getId());
            if (observation != null) {
                site.setVisited(true);
            } else {
                site.setVisited(false);
            }
        }
    }

    @Override
    public synchronized boolean onTap(LatLong tapLatLong, Point layerXY, Point tapXY) {
        for (VisitedSite visitedSite : mSites) {
            final Site site = visitedSite.getSite();
            final double distance = tapLatLong.distance(site.getPosition());
            if (distance < CLICK_DISTANCE_THRESHOLD) {
                mSelectionManager.setSelectedSite(site, mRoute);
                requestRedraw();
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized void draw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas,
                                  Point topLeftPoint) {
        final long mapSize = MercatorProjection.getMapSize(zoomLevel, displayModel.getTileSize());

        // Draw lines between sites
        drawLines(canvas, topLeftPoint, mapSize);

        // Draw sites
        drawSites(canvas, topLeftPoint, mapSize);

        if (zoomLevel > LABEL_THRESHOLD) {
            // Draw site labels
            drawSiteLabels(canvas, topLeftPoint, mapSize);
        } else {
            // Draw route name
            drawRouteLabel(canvas, topLeftPoint, mapSize);
        }
    }

    /**
     * Draws lines between sites
     */
    private void drawLines(Canvas canvas, Point topLeftPoint, long mapSize) {
        Point lastPoint = null;
        for (VisitedSite visitedSite : mSites) {
            final Site site = visitedSite.getSite();
            final LatLong ll = site.getPosition();
            // Update paint
            mPaint.setStrokeWidth(
                    (float) MercatorProjection.metersToPixels(LINE_WIDTH, ll.latitude, mapSize));
            final double pixelX = MercatorProjection.longitudeToPixelX(ll.longitude,
                    mapSize) - topLeftPoint.x;
            final double pixelY = MercatorProjection.latitudeToPixelY(ll.latitude,
                    mapSize) - topLeftPoint.y;



            // Draw a line from the previous site to this one
            if (lastPoint != null) {
                canvas.drawLine((int) lastPoint.x, (int) lastPoint.y, (int) pixelX,
                        (int) pixelY, mPaint);
            }
            lastPoint = new Point(pixelX, pixelY);
        }
    }

    /**
     * Draws site markers
     */
    private void drawSites(Canvas canvas, Point topLeftPoint, long mapSize) {
        for (VisitedSite visitedSite : mSites) {
            final Site site = visitedSite.getSite();
            final LatLong ll = site.getPosition();

            // Update paint
            mPaint.setStrokeWidth(
                    (float) MercatorProjection.metersToPixels(LINE_WIDTH, ll.latitude, mapSize));
            final float markerRadiusMeters = visitedSite.isVisited() ? MARKER_RADIUS_VISITED : MARKER_RADIUS;
            final int markerRadius = (int) Math.ceil(
                    MercatorProjection.metersToPixels(markerRadiusMeters, ll.latitude, mapSize));

            final double pixelX = MercatorProjection.longitudeToPixelX(ll.longitude,
                    mapSize) - topLeftPoint.x;
            final double pixelY = MercatorProjection.latitudeToPixelY(ll.latitude,
                    mapSize) - topLeftPoint.y;


            // Indicate selected site
            if (site == mSelectionManager.getSelectedSite()) {
                final double selectedRadius = MercatorProjection.metersToPixels(SELECTED_RADIUS,
                        ll.latitude, mapSize);
                canvas.drawCircle((int) pixelX, (int) pixelY, (int) selectedRadius, mSelectedPaint);
            }

            // Draw the marker circle: White stroke, then fill
            canvas.drawCircle((int) pixelX, (int) pixelY, markerRadius, mIdBackgroundPaint);
            // Different marker for visited sites
            if (visitedSite.isVisited()) {
                canvas.drawCircle((int) pixelX, (int) pixelY, markerRadius, mVisitedPaint);
            } else {
                canvas.drawCircle((int) pixelX, (int) pixelY, markerRadius, mPaint);
            }
        }
    }

    /**
     * Draws a label for each site
     */
    private void drawSiteLabels(Canvas canvas, Point topLeftPoint, long mapSize) {
        for (VisitedSite visitedSite : mSites) {
            final Site site = visitedSite.getSite();
            final LatLong ll = site.getPosition();

            // Update paint
            final float textSize = (float) MercatorProjection.metersToPixels(SITE_LABEL_HEIGHT,
                    ll.latitude,
                    mapSize);
            mIdPaint.setTextSize(textSize);
            mIdBackgroundPaint.setTextSize(textSize);
            mIdBackgroundPaint.setStrokeWidth(4);

            final int markerRadius = (int) Math.ceil(
                    MercatorProjection.metersToPixels(MARKER_RADIUS, ll.latitude, mapSize));

            final double pixelX = MercatorProjection.longitudeToPixelX(ll.longitude,
                    mapSize) - topLeftPoint.x;
            final double pixelY = MercatorProjection.latitudeToPixelY(ll.latitude,
                    mapSize) - topLeftPoint.y;


            // Draw the site ID centered below the marker
            final String idString = String.format(Locale.getDefault(), "%d", site.getId());
            final int textWidth = mIdPaint.getTextWidth(idString);
            final int textHeight = mIdPaint.getTextHeight(idString);
            // Stroke with a contrasting background and then fill
            final int textX = (int) (pixelX - textWidth / 2.0f);
            final int textY = (int) (pixelY + 1.5 * markerRadius + textHeight);
            canvas.drawText(idString, textX, textY, mIdBackgroundPaint);
            canvas.drawText(idString, textX, textY, mIdPaint);
        }
    }

    private void drawRouteLabel(Canvas canvas, Point topLeftPoint, long mapSize) {
        final double centerX= MercatorProjection.longitudeToPixelX(mCenter.getLongitude(), mapSize)
                - topLeftPoint.x;
        final double centerY = MercatorProjection.latitudeToPixelY(mCenter.getLatitude(), mapSize)
                - topLeftPoint.y;
        canvas.drawText(mRoute.getName(), (int) centerX, (int) centerY, mRouteNameBackgroundPaint);
        canvas.drawText(mRoute.getName(), (int) centerX, (int) centerY, mRouteNamePaint);
    }

    /**
     * A site with information on whether the user has visited it
     */
    private static class VisitedSite {
        /**
         * The site
         */
        @NonNull
        private final Site mSite;

        /**
         * If the site has been visited
         */
        private boolean mVisited;

        /**
         * Creates a VisitedSite
         * @param site the site
         * @param visited if the site is visited
         */
        VisitedSite(@NonNull Site site, boolean visited) {
            mSite = site;
            mVisited = visited;
        }

        /**
         * Returns the site
         * @return the site
         */
        @NonNull
        public Site getSite() {
            return mSite;
        }

        /**
         * Returns whether the site has been visited
         * @return whether the site has been visited
         */
        boolean isVisited() {
            return mVisited;
        }

        /**
         * Sets the visited status of the site
         * @param visited if the site has been visited
         */
        void setVisited(boolean visited) {
            mVisited = visited;
        }
    }
}
