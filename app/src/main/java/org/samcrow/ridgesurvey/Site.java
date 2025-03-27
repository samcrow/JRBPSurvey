/*
 * Copyright (c) 2025 Sam Crow
 *
 * This file is part of JRBPSurvey.
 *
 * JRBPSurvey is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * JRBPSurvey is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Foobar.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package org.samcrow.ridgesurvey;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;

import org.mapsforge.core.model.LatLong;
import org.samcrow.utm.ConvertedLatLon;

/**
 * Represents a site that can be visited
 *
 * Instances of this class are immutable.
 */
public final class Site implements Parcelable {

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
     * @param hemisphere the hemisphere of the coordinates
     * @param longZone the UTM longitude zone
     * @param easting the east coordinate, in meters
     * @param northing the west coordinate, in meters
     * @return a Site at the provided location
     */
    public static Site fromUtm(ConvertedLatLon.Hemisphere hemisphere, int longZone, double easting, double northing, int id) {
        final ConvertedLatLon converted = ConvertedLatLon.Companion.fromUtm(hemisphere, longZone, easting, northing);
        return new Site(new LatLong(converted.getLatitude(), converted.getLongitude()), id);
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
        return mId == site.mId && mPosition.equals(site.mPosition);

    }

    @Override
    public int hashCode() {
        int result = mId;
        result = 31 * result + mPosition.hashCode();
        return result;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mId);
        dest.writeDouble(mPosition.getLatitude());
        dest.writeDouble(mPosition.getLongitude());
    }

    public static Parcelable.Creator<Site> CREATOR = new Creator<Site>() {
        @Override
        public Site createFromParcel(Parcel source) {
            final int id = source.readInt();
            final double latitude = source.readDouble();
            final double longitude = source.readDouble();
            final LatLong position = new LatLong(latitude, longitude);
            return new Site(position, id);
        }

        @Override
        public Site[] newArray(int size) {
            return new Site[size];
        }
    };
}
