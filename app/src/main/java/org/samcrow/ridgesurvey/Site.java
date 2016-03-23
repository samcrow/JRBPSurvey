package org.samcrow.ridgesurvey;

import android.support.annotation.NonNull;

import org.mapsforge.core.model.LatLong;

import uk.me.jstott.jcoord.LatLng;
import uk.me.jstott.jcoord.UTMRef;

/**
 * Represents a site that can be visited
 *
 * Instances of this class are immutable.
 */
public final class Site {

    /**
     * The numerical ID of this site
     */
    private final int mId;

    /**
     * The position of this site
     */
    @NonNull
    private final LatLong mPosition;

    /**
     * Creates a new site at a position
     * @param position the position
     */
    public Site(@NonNull LatLong position, int id) {
        Objects.requireNonNull(position);
        mPosition = position;
        mId = id;
    }

    /**
     * Creates a Site with a position converted from UTM coordinates
     * @param latZone the UTM latitude zone
     * @param longZone the UTM longitude zone
     * @param easting the east coordinate, in meters
     * @param northing the west coordinate, in meters
     * @return a Site at the provided location
     */
    public static Site fromUtm(char latZone, int longZone, double easting, double northing, int id) {
        final UTMRef utm = new UTMRef(easting, northing, latZone, longZone);
        final LatLng ll = utm.toLatLng();
        return new Site(new LatLong(ll.getLat(), ll.getLng()), id);
    }

    /**
     * Returns the position of this site
     * @return the position
     */
    @NonNull
    public LatLong getPosition() {
        return mPosition;
    }

    /**
     * Returns the ID of this site
     * @return the ID
     */
    public int getId() {
        return mId;
    }

    @Override
    public String toString() {
        return "Site{" +
                "mId=" + mId +
                ", mPosition=" + mPosition +
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

        if (mId != site.mId) {
            return false;
        }
        return mPosition.equals(site.mPosition);

    }

    @Override
    public int hashCode() {
        int result = mId;
        result = 31 * result + mPosition.hashCode();
        return result;
    }
}
