package org.samcrow.ridgesurvey;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Rectangle;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.Layer;

import java.util.List;

/**
 * A layer that displays a route
 */
public class RouteLayer extends Layer {

    /**
     * The radius of a site marker, in meters
     */
    private static final float MARKER_RADIUS = 6;

    /**
     * The width of site-connecting lines, in meters
     */
    private static final float LINE_WIDTH = 3;
    /**
     * The height of site label text, in meters
     */
    private static final float SITE_LABEL_HEIGHT = 20;

    /**
     * Latitude/longitude distance, in degrees, that is the tolerance for clicking to select a site
     */
    private static final double CLICK_DISTANCE_THRESHOLD = 0.0005;
    /**
     * Radius of the selected site marker, in meters
     */
    private static final float SELECTED_RADIUS = 20;

    /**
     * The paint used to draw site markers
     */
    @NonNull
    private final Paint mPaint;

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
    private final List<Site> mSites;

    /**
     * The selection manager that tracks the selected site
     */
    @NonNull
    private final SelectionManager mSelectionManager;

    /**
     * Creates a new route layer
     *
     * @param baseRoute        the route to display. Must not be null.
     * @param route            the route to display, with its points in a valid order. Must not be null.
     * @param color            the color to use for this route, in the format used by {@link android.graphics.Color}
     * @param selectionManager A selection manager to track the selected site. Must not be null.
     */
    public RouteLayer(@NonNull Route baseRoute, @NonNull OrderedRoute route, int color,
                      @NonNull SelectionManager selectionManager) {
        Objects.requireAllNonNull(route, selectionManager);

        mRoute = baseRoute;
        mSites = route.getSites();

        mSelectionManager = selectionManager;

        mPaint = AndroidGraphicFactory.INSTANCE.createPaint();
        mPaint.setColor(color);

        mIdPaint = AndroidGraphicFactory.INSTANCE.createPaint();
        mIdPaint.setColor(Color.BLACK);

        mIdBackgroundPaint = AndroidGraphicFactory.INSTANCE.createPaint();
        mIdBackgroundPaint.setColor(Color.WHITE);
        mIdBackgroundPaint.setStyle(Style.STROKE);

        mSelectedPaint = AndroidGraphicFactory.INSTANCE.createPaint();
        mSelectedPaint.setColor(android.graphics.Color.argb(0x40, 0xFF, 0x0, 0x0));
    }

    @Override
    public synchronized boolean onTap(LatLong tapLatLong, Point layerXY, Point tapXY) {
        for (Site site : mSites) {
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
        final Rectangle canvasRect = new Rectangle(0, 0, canvas.getWidth(), canvas.getHeight());

        // Draw sites
        Point lastPoint = null;
        for (Site site : mSites) {
            final LatLong ll = site.getPosition();

            // Update paint
            mPaint.setStrokeWidth(
                    (float) MercatorProjection.metersToPixels(LINE_WIDTH, ll.latitude, mapSize));
            final int markerRadius = (int) Math.ceil(
                    MercatorProjection.metersToPixels(MARKER_RADIUS, ll.latitude, mapSize));

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


            final int left = (int) (pixelX - markerRadius);
            final int top = (int) (pixelY - markerRadius);
            final int right = left + 2 * markerRadius;
            final int bottom = top + 2 * markerRadius;

            final Rectangle markerRect = new Rectangle(left, top, right, bottom);
            if (canvasRect.intersects(markerRect)) {
                canvas.drawCircle(left + markerRadius, top + markerRadius, markerRadius, mPaint);
            }

            // Draw a line from the previous site to this one
            if (lastPoint != null) {
                canvas.drawLine((int) lastPoint.x, (int) lastPoint.y, (int) pixelX,
                        (int) pixelY, mPaint);
            }
            lastPoint = new Point(pixelX, pixelY);
        }

        for (Site site : mSites) {
            final LatLong ll = site.getPosition();

            // Update paint
            final float textSize = (float) MercatorProjection.metersToPixels(SITE_LABEL_HEIGHT,
                    ll.latitude,
                    mapSize);
            mIdPaint.setTextSize(textSize);
            mIdBackgroundPaint.setTextSize(textSize);
            mIdBackgroundPaint.setStrokeWidth(0.2f * textSize);

            final int markerRadius = (int) Math.ceil(
                    MercatorProjection.metersToPixels(MARKER_RADIUS, ll.latitude, mapSize));

            final double pixelX = MercatorProjection.longitudeToPixelX(ll.longitude,
                    mapSize) - topLeftPoint.x;
            final double pixelY = MercatorProjection.latitudeToPixelY(ll.latitude,
                    mapSize) - topLeftPoint.y;


            // Draw the site ID centered below the marker
            final String idString = String.format("%d", site.getId());
            final int textWidth = mIdPaint.getTextWidth(idString);
            final int textHeight = mIdPaint.getTextHeight(idString);
            // Stroke with a contrasting background and then fill
            final int textX = (int) (pixelX - textWidth / 2.0f);
            final int textY = (int) (pixelY + 1.5 * markerRadius + textHeight);
            canvas.drawText(idString, textX, textY, mIdBackgroundPaint);
            canvas.drawText(idString, textX, textY, mIdPaint);
        }
    }

}
