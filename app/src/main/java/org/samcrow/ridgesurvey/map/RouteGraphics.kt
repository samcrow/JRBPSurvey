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
import android.graphics.Color
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.expressions.Expression.color
import org.maplibre.android.style.expressions.Expression.eq
import org.maplibre.android.style.expressions.Expression.get
import org.maplibre.android.style.expressions.Expression.literal
import org.maplibre.android.style.expressions.Expression.match
import org.maplibre.android.style.expressions.Expression.stop
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.Layer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import org.samcrow.ridgesurvey.R
import org.samcrow.ridgesurvey.Route
import org.samcrow.ridgesurvey.Site
import org.samcrow.ridgesurvey.color.Palette
import java.util.Collections
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
        val properties = JsonObject().apply { addProperty("route", routeName) }
        val feature = Feature.fromGeometry(lineString, properties)
        features.add(feature)
    }

    if (sites.isNotEmpty()) {
        Log.w(TAG, "Sites are not in any route: ${sites.keys}")
    }

    return FeatureCollection.fromFeatures(features)
}

/**
 * Reads routes from embedded resources/assets and returns an immutable list of routes
 */
internal fun readRoutes(context: Context): List<Route> {
    val sites = parseSites(context)
    val routeOrders = parseRouteOrders(context)

    val routes = routeOrders.map { (routeName, siteNames) ->
        val sites = siteNames.map { name ->
            val sitePoint = sites.remove(name) ?:  throw IllegalStateException("Site $name missing or already used in another route")
            Site(LatLng(sitePoint.latitude(), sitePoint.longitude()), name)
        }
        Route(routeName, sites)
    }

    if (sites.isNotEmpty()) {
        Log.w(TAG, "Sites are not in any route: ${sites.keys}")
    }
    return Collections.unmodifiableList(routes);
}

/**
 * Returns a map where each key is a route name, corresponding to a list of site names in a
 * reasonable walking order for that route
 */
private fun parseRouteOrders(context: Context): Map<String, List<Int>> {
    val routeOrderJson: JsonObject
    context.resources.openRawResource(R.raw.route_order).reader().use { stream ->
        routeOrderJson = Gson().fromJson(stream, JsonObject::class.java)
    }
    val routeOrders: MutableMap<String, List<Int>> = TreeMap()
    for ((routeName, sitesJson) in routeOrderJson.entrySet()) {
        sitesJson as JsonArray
        val sites = sitesJson.map { it.asInt }
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
private fun parseSites(context: Context): MutableMap<Int, Point> {
    val root: JsonObject
    context.assets.open(SITES_PATH).reader().use { stream ->
        root = Gson().fromJson(stream, JsonObject::class.java)
    }
    val sites: MutableMap<Int, Point> = TreeMap()
    val features = root.getAsJsonArray("features")!!
    for (feature in features) {
        feature as JsonObject
        val siteName = feature.getAsJsonObject("properties").get("name").asInt
        val coordinates = feature.getAsJsonObject("geometry").getAsJsonArray("coordinates")
        val longitude = coordinates[0].asDouble
        val latitude = coordinates[1].asDouble
        val prevSite = sites.put(siteName, Point.fromLngLat(longitude, latitude))
        if (prevSite != null) {
            throw IllegalStateException("Duplicate site $siteName")
        }
    }
    return sites
}

private fun lookUpRoute(siteNames: List<Int>, sites: MutableMap<Int, Point>): LineString {
    val sitePoints = siteNames.map { name ->
        sites.remove(name)
            ?: throw IllegalStateException("Site $name missing or already used in another route")
    }
    return LineString.fromLngLats(sitePoints)
}

internal fun createRouteLayers(context: Context): List<Layer> {
    val selectedCircle = CircleLayer("route_selected_circle", RouteLayer.SOURCE_NAME)
    selectedCircle.setProperties(
        circleRadius(literal(16)),
        circleColor(context.getColor(R.color.selected_circle))
    )
    selectedCircle.setFilter(eq(get("selected"), literal(true)))

    val color = createRouteColor(context)

    val lineLayer = LineLayer("per_route_lines", RouteLayer.SOURCE_NAME)
    lineLayer.setProperties(
        lineWidth(literal(3)),
        lineColor(color)
    )

    val circleLayer = CircleLayer("per_route_circles", RouteLayer.SOURCE_NAME)
    circleLayer.setProperties(
        circleRadius(literal(8)),
        circleColor(color)
    )

    return listOf(selectedCircle, lineLayer, circleLayer)
}

private fun createRouteColor(context: Context): Expression {
    val routeNames = parseRouteOrders(context).keys
    val colors = Palette.getColorsRepeating()
    val colorStops = routeNames.map { name ->
        val routeColor = colors.next()
        stop(literal(name), color(routeColor))
    }
    val defaultColor = color(Color.WHITE)
    return match(get("route"), defaultColor, *colorStops.toTypedArray())
}
