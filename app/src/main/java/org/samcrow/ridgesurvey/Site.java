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
import androidx.core.os.ParcelCompat;

import org.maplibre.android.geometry.LatLng;
import org.maplibre.geojson.Feature;
import org.maplibre.geojson.Point;

/**
 * Represents a site that can be visited
 * <p>
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
    private final @NonNull LatLng mPosition;

    /**
     * Creates a new site at a position
     * @param position the position
     */
    public Site(@NonNull LatLng position, int id) {
        Objects.requireNonNull(position);
        mPosition = position;
        mId = id;
    }

    /**
     * Returns the position of this site
     * @return the position
     */
    @NonNull
    public LatLng getPosition() {
        return mPosition;
    }

    /**
     * Returns the ID of this site
     * @return the ID
     */
    public int getId() {
        return mId;
    }

    public @NonNull Feature asGeoJson() {
        final Feature feature = Feature.fromGeometry(Point.fromLngLat(mPosition.getLongitude(), mPosition.getLatitude()));
        feature.addStringProperty("name", Integer.toString(mId));
        feature.addBooleanProperty("visited", false);
        return feature;
    }

    @NonNull
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
        dest.writeParcelable(mPosition, 0);
    }

    public static Parcelable.Creator<Site> CREATOR = new Creator<Site>() {
        @Override
        public Site createFromParcel(Parcel source) {
            final int id = source.readInt();
            final LatLng position = ParcelCompat.readParcelable(source, LatLng.class.getClassLoader(), LatLng.class);
            Objects.requireNonNull(position);
            return new Site(position, id);
        }

        @Override
        public Site[] newArray(int size) {
            return new Site[size];
        }
    };
}
