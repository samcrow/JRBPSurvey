package org.samcrow.ridgesurvey;

import android.support.annotation.NonNull;

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Rectangle;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.Layer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A layer that displays a route
 */
public class RouteLayer extends Layer {

    /**
     * The radius of a site marker
     */
    private static final int MARKER_RADIUS = 6;

    /**
     * The minimum contrast from the background
     */
    private static final int MIN_CONTRAST = 400;

    /**
     * The background color used for the preserve
     *
     * Format: 4 bytes: alpha, red, green, blue
     */
    private static final int BACKGROUND_COLOR = 0xFFC5F3A5;

    /**
     * The route to display
     */
    @NonNull
    private final Route mRoute;

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
     * @param route the route to display. Must not be null.
     */
    public RouteLayer(@NonNull Route route) {
        Objects.requireNonNull(route);
        mRoute = route;
        mSites = new ArrayList<>(mRoute.getSites());

        mPaint = AndroidGraphicFactory.INSTANCE.createPaint();
        mPaint.setColor(findGoodColor(route.hashCode()));
    }

    @Override
    public void draw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point topLeftPoint) {
        final long mapSize = MercatorProjection.getMapSize(zoomLevel, displayModel.getTileSize());
        final Rectangle canvasRect = new Rectangle(0, 0, canvas.getWidth(), canvas.getHeight());
        // Draw sites
        for (Site site : mSites) {
            final LatLong ll = site.getPosition();
            final double pixelX = MercatorProjection.longitudeToPixelX(ll.longitude, mapSize);
            final double pixelY = MercatorProjection.latitudeToPixelY(ll.latitude, mapSize);

            final int left = (int) (pixelX - topLeftPoint.x - MARKER_RADIUS);
            final int top = (int) (pixelY - topLeftPoint.y - MARKER_RADIUS);
            final int right = left + 2 * MARKER_RADIUS;
            final int bottom = top + 2 * MARKER_RADIUS;

            final Rectangle markerRect = new Rectangle(left, top, right, bottom);
            if (canvasRect.intersects(markerRect)) {
                canvas.drawCircle(left + MARKER_RADIUS, top + MARKER_RADIUS, MARKER_RADIUS, mPaint);
            }
        }
    }

    /**
     * Finds a good color with enough contrast, starting from an initial color
     * @param initial the color to start with
     * @return a color with enough contrast from the background
     */
    private static int findGoodColor(int initial) {
        // Ensure alpha is 100%
        int color = initial | 0xFF000000;
        final Random rand = new Random(initial);
        while (contrast(BACKGROUND_COLOR, color) < MIN_CONTRAST) {
            // Flip some bits
            final int mask = rand.nextInt() & 0xFFFFFF;
            color ^= mask;
        }
        return color;
    }

    /**
     * Calculates the contrast between two colors
     * @param color1 a color
     * @param color2 another color
     * @return a value representing the contrast between the colors
     */
    private static int contrast(int color1, int color2) {
        final int part1 = 0xFF & Math.abs((color1 & 0xFF) - (color2 & 0xFF));
        final int part2 = 0xFF & Math.abs(((color1 >>> 8) & 0xFF) - ((color2 >>> 8) & 0xFF));
        final int part3 = 0xFF & Math.abs(((color1 >>> 16) & 0xFF) - ((color2 >>> 16) & 0xFF));
        return part1 + part2 + part3;
    }
}
