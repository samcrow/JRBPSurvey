package org.samcrow.ridgesurvey;

import android.support.annotation.NonNull;

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.Paint;
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
     * The route to display
     */
    @NonNull
    private final OrderedRoute mRoute;

    /**
     * The paint used to draw site markers
     */
    @NonNull
    private final Paint mPaint;

    /**
     * The sites in the route
     */
    @NonNull
    private final List<Site> mSites;

    /**
     * Creates a new route layer
     *
     * @param route the route to display. Must not be null.
     * @param color the color to use for this route, in the format used by {@link android.graphics.Color}
     */
    public RouteLayer(@NonNull OrderedRoute route, int color) {
        Objects.requireNonNull(route);
        mRoute = route;
        mSites = mRoute.getSites();

        mPaint = AndroidGraphicFactory.INSTANCE.createPaint();
        mPaint.setColor(color);
    }

    @Override
    public void draw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point topLeftPoint) {
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

            final double pixelX = MercatorProjection.longitudeToPixelX(ll.longitude, mapSize);
            final double pixelY = MercatorProjection.latitudeToPixelY(ll.latitude, mapSize);

            final int left = (int) (pixelX - topLeftPoint.x - markerRadius);
            final int top = (int) (pixelY - topLeftPoint.y - markerRadius);
            final int right = left + 2 * markerRadius;
            final int bottom = top + 2 * markerRadius;

            final Rectangle markerRect = new Rectangle(left, top, right, bottom);
            if (canvasRect.intersects(markerRect)) {
                canvas.drawCircle(left + markerRadius, top + markerRadius, markerRadius, mPaint);
            }

            // Draw a line from the previous site to this one
            if (lastPoint != null) {
                canvas.drawLine((int) lastPoint.x, (int) lastPoint.y, left + markerRadius,
                        top + markerRadius, mPaint);
            }
            lastPoint = new Point(left + markerRadius, top + markerRadius);
        }
    }

}
