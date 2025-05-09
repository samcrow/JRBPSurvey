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

import androidx.annotation.NonNull;

import org.mapsforge.core.model.LatLong;

import java.util.ArrayList;
import java.util.List;

/**
 * An immutable set of {@link Site sites} with a name
 */
public class Route {

    /**
     * The route name
     */
    @NonNull
    private final String mName;

    /**
     * The sites in this route, ordered for a reasonable walking route
     */
    @NonNull
    private final List<Site> mSites;

    /**
     * Creates a new route
     * @param name the route name. Must not be null.
     * @param sites The sites to include in the route. Must not be null, and must not contain any
     *              null elements.
     */
    public Route(@NonNull String name, @NonNull List<? extends Site> sites) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(sites);
        for (Site site : sites) {
            Objects.requireNonNull(site);
        }

        mName = name;
        mSites = new ArrayList<>(sites);
    }

    /**
     * Returns the name of the route
     * @return the name
     */
    @NonNull
    public String getName() {
        return mName;
    }

    /**
     * Returns the sites in this route
     * @return the sites
     */
    @NonNull
    public List<Site> getSites() {
        return new ArrayList<>(mSites);
    }

    /**
     * Returns the center point of this route, calculated from the sites with an implementation-
     * defined method
     * @return the center point
     */
    @NonNull
    public LatLong getCenter() {
        if (mSites.isEmpty()) {
            throw new IllegalStateException("Can't get the center of an empty route");
        }
        double latitudeSum = 0;
        double longitudeSum = 0;

        // For implementation, try the average
        for (Site site : mSites) {
            latitudeSum += site.getPosition().latitude;
            longitudeSum += site.getPosition().longitude;
        }

        return new LatLong(latitudeSum / mSites.size(), longitudeSum / mSites.size());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Route route = (Route) o;

        if (!mName.equals(route.mName)) {
            return false;
        }
        return mSites.equals(route.mSites);

    }

    @Override
    public int hashCode() {
        int result = mName.hashCode();
        result = 31 * result + mSites.hashCode();
        return result;
    }
}
