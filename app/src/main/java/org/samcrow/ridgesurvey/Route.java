/*
 * Copyright 2017 Sam Crow
 *
 * This file is part of JRBP Survey.
 *
 * JRBP Survey is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JRBP Survey is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JRBP Survey.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.samcrow.ridgesurvey;

import android.support.annotation.NonNull;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

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
     * The sites in this route
     */
    @NonNull
    private final Set<Site> mSites;

    /**
     * Creates a new route
     * @param name the route name. Must not be null.
     * @param sites The sites to include in the route. Must not be null, and must not contain any
     *              null elements.
     */
    public Route(@NonNull String name, @NonNull Collection<? extends Site> sites) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(sites);
        for (Site site : sites) {
            Objects.requireNonNull(site);
        }

        mName = name;
        mSites = new LinkedHashSet<>(sites);
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
    public Set<Site> getSites() {
        return new LinkedHashSet<>(mSites);
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
