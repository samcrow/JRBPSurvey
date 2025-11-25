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

package org.samcrow.ridgesurvey.map

import android.content.Context
import android.util.Log
import com.google.gson.JsonObject
import org.apache.commons.io.IOUtils
import org.json.JSONObject
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import org.samcrow.ridgesurvey.R
import java.util.TreeMap

/** Asset path to a GeoJSON file containing the sites */
private const val SITES_PATH = "map_vectors/sites_wgs84.geojson"
private const val TAG = "RouteGraphics"

/**
 * Reads the sites_wgs84.geojson and route_order.json files and creates a line string for each
 * route that connects the points in the order defined in route_order.json
 */
internal fun createRouteLines(context: Context): FeatureCollection {
    val sites = parseSites(context)
    val routeOrders = parseRouteOrders(context)
    val features: MutableList<Feature> = ArrayList(routeOrders.size)

    for ((routeName, siteNames) in routeOrders) {
        val lineString = lookUpRoute(siteNames, sites)
        val properties = JsonObject().apply { addProperty("name", routeName) }
        val feature = Feature.fromGeometry(lineString, properties)
        features.add(feature)
    }

    if (sites.isNotEmpty()) {
        Log.w(TAG, "Sites are not in any route: ${sites.keys}")
    }

    return FeatureCollection.fromFeatures(features)
}

/**
 * Returns a map where each key is a route name, corresponding to a list of site names in a
 * reasonable walking order for that route
 */
private fun parseRouteOrders(context: Context): Map<String, List<String>> {
    val routeOrderJson: JSONObject
    context.resources.openRawResource(R.raw.route_order).use { stream ->
        routeOrderJson = JSONObject(IOUtils.toString(stream))
    }
    val routeOrders: MutableMap<String, List<String>> = TreeMap()
    for (routeName in routeOrderJson.keys()) {
        val sitesJson = routeOrderJson.getJSONArray(routeName)
        val sites: MutableList<String> = ArrayList(sitesJson.length())
        for (i in 0..<sitesJson.length()) {
            val name = sitesJson.get(i).toString()
            sites.add(name)
        }
        val prevRoute = routeOrders.put(routeName, sites)
        if (prevRoute != null) {
            throw IllegalStateException("Duplicate route $routeName")
        }
    }
    return routeOrders
}

/**
 * Returns a map where each key is a site name, corresponding to the site position
 */
private fun parseSites(context: Context): MutableMap<String, Point> {
    val root: JSONObject
    context.assets.open(SITES_PATH).use { stream ->
        root = JSONObject(IOUtils.toString(stream))
    }
    val sites: MutableMap<String, Point> = TreeMap()
    val features = root.getJSONArray("features")
    for (i in 0..<features.length()) {
        val feature = features.getJSONObject(i)
        val siteName = feature.getJSONObject("properties").getString("name")
        val coordinates = feature.getJSONObject("geometry").getJSONArray("coordinates")
        val longitude = coordinates.getDouble(0)
        val latitude = coordinates.getDouble(1)
        val prevSite = sites.put(siteName, Point.fromLngLat(longitude, latitude))
        if (prevSite != null) {
            throw IllegalStateException("Duplicate site $siteName")
        }
    }
    return sites
}

private fun lookUpRoute(siteNames: List<String>, sites: MutableMap<String, Point>): LineString {
    val sitePoints = siteNames.map { name ->
        sites.remove(name) ?: throw IllegalStateException("Site $name missing or already used in another route")
    }
    return LineString.fromLngLats(sitePoints)
}


