package org.samcrow.ridgesurvey;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides utilities for loading {@link Site} objects
 */
public class SiteStorage {

    /**
     * Reads zero or more sites from a stream
     *
     * Reading will stop when the stream has no available bytes. The stream will not be closed.
     *
     * If any line contains invalid information, it will be ignored.
     *
     * @param stream a stream to read from. The stream should yield comma-separated text with
     *               no fields quoted. Column 1 should be a numerical ID for the site. Column
     *               2 should be a UTM easting coordinate in zone 10 of the northern hemisphere.
     *               Column 3 should be a UTM northing coordinate in zone 10 of the northern
     *               hemisphere. Additional columns may be present.
     * @return A list of sites read from the stream
     * @throws IOException if a read error occurred
     */
    public static List<Site> readSites(InputStream stream) throws IOException {
        final List<Site> sites = new ArrayList<>();

        final BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        String line;
        while ((line = reader.readLine()) != null) {
            final String[] parts = line.split("\\s*,\\s*");
            if (parts.length >= 3) {
                try {
                    final double easting = Double.valueOf(parts[1]);
                    final double northing = Double.valueOf(parts[2]);

                    final Site site = Site.fromUtm('N', 10, easting, northing);
                    sites.add(site);
                } catch (NumberFormatException e) {
                    // Continue
                }
            }
        }
        return sites;
    }

    /**
     * Reads zero or more sites from a stream
     *
     * Reading will stop when the stream has no available bytes. The stream will not be closed.
     *
     * If any line contains invalid information, it will be ignored.
     *
     * Sites with an empty route or a route of "invalid" will be ignored.
     *
     * @param source a stream to read from. The stream should yield comma-separated text with
     *               no fields quoted. Column 1 should be a numerical ID for the site. Column
     *               2 should be a UTM easting coordinate in zone 10 of the northern hemisphere.
     *               Column 3 should be a UTM northing coordinate in zone 10 of the northern
     *               hemisphere. Column 4 must be present and may contain a route name.
     *               Additional columns may be present.
     * @return A list of routes read from the stream
     * @throws IOException if a read error occurred
     */
    public static List<Route> readRoutes(InputStream source) throws IOException {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(source));

        // Temporary map from route name to mutable list of sites
        final Map<String, List<Site>> routes = new HashMap<>();

        String line;
        while ((line = reader.readLine()) != null) {
            final String[] parts = line.split("\\s*,\\s*");
            if (parts.length >= 4) {
                try {
                    final double easting = Double.valueOf(parts[1]);
                    final double northing = Double.valueOf(parts[2]);
                    final String route = parts[3];

                    if (!route.isEmpty() && !route.equals("ignore")) {
                        if (!routes.containsKey(route)) {
                            routes.put(route, new ArrayList<Site>());
                        }

                        final Site site = Site.fromUtm('N', 10, easting, northing);
                        routes.get(route).add(site);
                    }
                } catch (NumberFormatException e) {
                    // Continue
                }
            }
        }
        final List<Route> routeList = new ArrayList<>(routes.size());
        for (Map.Entry<String, List<Site>> entry : routes.entrySet()) {
            routeList.add(new Route(entry.getKey(), entry.getValue()));
        }
        return routeList;
    }

    private SiteStorage() {}
}
