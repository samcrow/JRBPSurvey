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

import java.util.ArrayList;
import java.util.List;

/**
 * An ordered route visiting zero or more sites
 *
 * Instances of this class are immutable.
 */
public class OrderedRoute {
    /**
     * The sites on this route, in order
     */
    @NonNull
    private final List<Site> mSites;

    /**
     * Creates a new ordered route
     * @param sites a list of sites. Must not be null, and must not contain any null elements.
     */
    public OrderedRoute(@NonNull List<Site> sites) {
        Objects.requireNonNull(sites);
        for (Site site : sites) {
            Objects.requireNonNull(site, "Null element in sites list");
        }
        mSites = new ArrayList<>(sites);
    }

    /**
     * Creates an OrderedRoute containing the sites in a Route in an unspecified order
     * @param route a route. Must not be null.
     * @return an OrderedRoute containing the sites in route
     */
    public static OrderedRoute arbitraryOrder(Route route) {
        Objects.requireNonNull(route);
        return new OrderedRoute(new ArrayList<>(route.getSites()));
    }

    @NonNull
    public List<Site> getSites() {
        return new ArrayList<>(mSites);
    }
}
