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

package org.samcrow.ridgesurvey.map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.style.sources.GeoJsonSource;
import org.maplibre.geojson.Feature;
import org.maplibre.geojson.FeatureCollection;
import org.maplibre.geojson.LineString;
import org.maplibre.geojson.Point;
import org.samcrow.ridgesurvey.Objects;
import org.samcrow.ridgesurvey.Route;
import org.samcrow.ridgesurvey.SelectionManager;
import org.samcrow.ridgesurvey.Site;
import org.samcrow.ridgesurvey.data.IdentifiedObservation;
import org.samcrow.ridgesurvey.data.ObservationDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * A dynamic data source for routes and sites
 * <p>
 * This manages a dynamic {@link org.maplibre.android.style.sources.GeoJsonSource} that contains
 * a feature collection with a point feature for each site and a line string for a route connecting
 * the sites on each route.
 * <p>
 * Each site point has these properties:
 * <ul>
 *     <li>name, string: The site name (usually a number)</li>
 *     <li>route, string: The name of the route that contains the site</li>
 *     <li>selected, boolean (optional): If the user has selected this site</li>
 *     <li>visited, boolean (optional): If the user has recently visited this site and recorded an
 *     observation</li>
 * </ul>
 * Each route has these properties:
 * <ul><li>route, string: The route name</li></ul>
 */
public class RouteLayer implements SelectionManager.SelectionListener {
    public static final String SOURCE_NAME = "sites_routes_dynamic";

    /**
     * The observation database
     */
    @NonNull
    private final ObservationDatabase mDatabase;

    /**
     * The routes to display
     * <p>
     * Each key is a route name.
     */
    @NonNull
    private final Map<String, List<VisitedSite>> mRoutes;
    /** The current selected site */
    private @Nullable Site mSelectedSite;

    private final @NonNull GeoJsonSource mSource;

    /**
     * Creates a new route layer
     *
     * @param database         an observation database to use. Must not be null.
     * @param routes           the routes to display
     * @param selectionManager A selection manager to track the selected site. Must not be null.
     */
    public RouteLayer(@NonNull ObservationDatabase database, @NonNull List<Route> routes,
                      @NonNull SelectionManager selectionManager) {
        Objects.requireAllNonNull(database, routes, selectionManager);
        mDatabase = database;

        // Copy sites in, initially not visited
        mRoutes = new TreeMap<>();
        for (Route route : routes) {
            final List<VisitedSite> sites = new ArrayList<>(route.getSites().size());
            for (Site site : route.getSites()) {
                sites.add(new VisitedSite(site, false));
            }
            mRoutes.put(route.getName(), sites);
        }
        mSelectedSite = null;
        mSource = new GeoJsonSource(SOURCE_NAME);
        updateVisitedSites();
    }

    /**
     * Updates the visited state of each site from the database
     */
    public void updateVisitedSites() {
        for (List<VisitedSite> sites : mRoutes.values()) {
            for (VisitedSite site : sites) {
                final IdentifiedObservation observation = mDatabase.getObservationForSite(site.getSite().getId());
                site.setVisited(observation != null);
            }
        }
        mSource.setGeoJson(makeFeatures(mRoutes, mSelectedSite));
    }

    public GeoJsonSource getSource() {
        return mSource;
    }

    @Override
    public void selectionChanged(@Nullable Site newSelection, @Nullable Route siteRoute) {
        mSelectedSite = newSelection;
        mSource.setGeoJson(makeFeatures(mRoutes, mSelectedSite));
    }

    /**
     * A site with information on whether the user has visited it
     */
    private static class VisitedSite {
        /**
         * The site
         */
        @NonNull
        private final Site mSite;

        /**
         * If the site has been visited
         */
        private boolean mVisited;

        /**
         * Creates a VisitedSite
         *
         * @param site    the site
         * @param visited if the site is visited
         */
        VisitedSite(@NonNull Site site, boolean visited) {
            mSite = site;
            mVisited = visited;
        }

        /**
         * Returns the site
         *
         * @return the site
         */
        @NonNull
        public Site getSite() {
            return mSite;
        }

        /**
         * Returns whether the site has been visited
         *
         * @return whether the site has been visited
         */
        boolean isVisited() {
            return mVisited;
        }

        /**
         * Sets the visited status of the site
         *
         * @param visited if the site has been visited
         */
        void setVisited(boolean visited) {
            mVisited = visited;
        }
    }

    private static @NonNull FeatureCollection makeFeatures(@NonNull Map<String, List<VisitedSite>> routes, @Nullable Site selectedSite) {
        final List<Feature> geometry = makeRoutePoints(routes, selectedSite);
        geometry.addAll(makeRouteLines(routes));
        return FeatureCollection.fromFeatures(geometry);
    }

    private static @NonNull List<Feature> makeRoutePoints(@NonNull Map<String, List<VisitedSite>> routes, @Nullable Site selectedSite) {
        final List<Feature> points = new ArrayList<>();
        for (Map.Entry<String, List<VisitedSite>> entry : routes.entrySet()) {
            final String routeName = entry.getKey();
            for (VisitedSite site : entry.getValue()) {
                final Feature siteFeature = site.getSite().asGeoJson();
                siteFeature.addStringProperty("route", routeName);
                siteFeature.addBooleanProperty("visited", site.isVisited());
                siteFeature.addBooleanProperty("selected",
                        selectedSite != null && selectedSite.getId() == site.getSite().getId());
                points.add(siteFeature);
            }
        }
        return points;
    }

    private static @NonNull List<Feature> makeRouteLines(@NonNull Map<String, List<VisitedSite>> routes) {
        final List<Feature> lines = new ArrayList<>(routes.size());
        for (Map.Entry<String, List<VisitedSite>> entry : routes.entrySet()) {
            final String routeName = entry.getKey();
            final List<Point> routePoints = new ArrayList<>(entry.getValue().size());
            for (VisitedSite site : entry.getValue()) {
                final LatLng sitePosition = site.getSite().getPosition();
                routePoints.add(Point.fromLngLat(sitePosition.getLongitude(), sitePosition.getLatitude()));
            }
            final Feature routeFeature = Feature.fromGeometry(LineString.fromLngLats(routePoints));
            routeFeature.addStringProperty("route", routeName);
            lines.add(routeFeature);
        }
        return lines;
    }
}
