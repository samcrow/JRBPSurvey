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

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.samcrow.utm.ConvertedLatLon.Hemisphere;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Provides utilities for loading {@link Site} objects
 */
public class SiteStorage {

    /**
     * Reads zero or more sites from a stream
     *
     * Reading will stop when the stream has no available bytes. The stream will not be closed.
     *
     * @param source a stream to read from, containing JSON-formatted route data
     * @return A list of routes read from the stream
     * @throws IOException if a read error occurred
     * @throws JSONException if the read data contained invalid JSON
     */
    public static List<Route> readRoutes(InputStream source) throws IOException, JSONException {
        final JSONObject routesJson = new JSONObject(IOUtils.toString(source));

        final List<Route> routes = new ArrayList<>();

        for (Iterator<String> keys = routesJson.keys(); keys.hasNext(); ) {
            final String routeName = keys.next();
            final JSONArray sitesJson = routesJson.getJSONArray(routeName);

            final List<Site> sites = new ArrayList<>(sitesJson.length());

            for (int i = 0; i < sitesJson.length(); i++) {
                final JSONObject siteJson = sitesJson.getJSONObject(i);

                final int id = siteJson.getInt("id");
                final double easting = siteJson.getDouble("easting");
                final double northing = siteJson.getDouble("northing");

                final Site site = Site.fromUtm(Hemisphere.North, 10, easting, northing, id);
                sites.add(site);
            }
            routes.add(new Route(routeName, sites));
        }

        return routes;
    }

    private SiteStorage() {}
}
