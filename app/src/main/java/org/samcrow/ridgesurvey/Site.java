package org.samcrow.ridgesurvey;

import org.mapsforge.core.model.LatLong;

import uk.me.jstott.jcoord.LatLng;
import uk.me.jstott.jcoord.UTMRef;

/**
 * Represents a site that can be visited
 *
 * Instances of this class are immutable.
 */
public class Site {

    /**
     * The position of this site
     */
    private final LatLong mPosition;

    /**
     * Creates a new site at a position
     * @param position the position
     */
    public Site(LatLong position) {
        mPosition = position;
    }

    /**
     * Creates a Site with a position converted from UTM coordinates
     * @param latZone the UTM latitude zone
     * @param longZone the UTM longitude zone
     * @param easting the east coordinate, in meters
     * @param northing the west coordinate, in meters
     * @return a Site at the provided location
     */
    public static Site fromUtm(char latZone, int longZone, double easting, double northing) {
        final UTMRef utm = new UTMRef(easting, northing, latZone, longZone);
        final LatLng ll = utm.toLatLng();
        return new Site(new LatLong(ll.getLat(), ll.getLng()));
    }

    /**
     * Returns the position of this site
     * @return the position
     */
    public LatLong getPosition() {
        return mPosition;
    }

    @Override
    public String toString() {
        return "Site{" +
                "mPosition=" + mPosition +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Site site = (Site) o;

        return !(mPosition != null ? !mPosition.equals(site.mPosition) : site.mPosition != null);

    }

    @Override
    public int hashCode() {
        return mPosition != null ? mPosition.hashCode() : 0;
    }
}
