package org.samcrow.ridgesurvey;

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
import org.samcrow.ridgesurvey.SelectionManager.SelectionListener;

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

    public RouteLineLayer() {
        mLinePaint = AndroidGraphicFactory.INSTANCE.createPaint();
        mLinePaint.setColor(0xFF0030B0);
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
