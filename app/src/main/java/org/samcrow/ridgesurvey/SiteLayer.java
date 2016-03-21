package org.samcrow.ridgesurvey;

import android.graphics.drawable.Drawable;

import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.overlay.Marker;

/**
 * A layer that draws one site
 */
public class SiteLayer extends Marker {

    public SiteLayer(Site site, Drawable drawable) {
        super(site.getPosition(), AndroidGraphicFactory.convertToBitmap(drawable), 0, 0);
    }

}
