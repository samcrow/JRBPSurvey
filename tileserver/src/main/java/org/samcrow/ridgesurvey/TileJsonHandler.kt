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

package org.samcrow.ridgesurvey

import android.content.Context
import android.util.Range
import org.eclipse.jetty.http.HttpHeader
import org.eclipse.jetty.io.Content
import org.eclipse.jetty.server.Handler
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.Response
import org.eclipse.jetty.util.Callback
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.Collections


internal class TileJsonHandler(
    context: Context,
    baseAssetPath: String,
    private val baseUrl: String,
    private val fileExtension: String
) :
    Handler.Abstract.NonBlocking() {
    /** Zoom levels available in the tiles, if known */
    private val zoom: Range<Int>? = getZoomRange(context, baseAssetPath)

    override fun handle(
        request: Request,
        response: Response,
        callback: Callback
    ): Boolean {
        if (request.httpURI.path != "/tiles.json") {
            return false
        }
        response.status = 200
        response.headers.put(HttpHeader.CONTENT_TYPE, "application/json")
        val json = getTileJson().toString()
        Content.Sink.write(response, true, json, callback)
        return true
    }

    private fun getTileJson(): JSONObject {
        val tileUrl = "$baseUrl/{z}/{x}/{y}.$fileExtension"
        val json = JSONObject()
            .put("tilejson", "2.1.0")
            .put("tiles", jsonArrayOf(tileUrl))
        zoom?.let { zoom ->
            // Don't send `minzoom`; that makes the layer suddenly disappear when zoomed out
            json.put("maxzoom", zoom.upper)
        }
        return json
    }
}

private fun jsonArrayOf(value: Any): JSONArray {
    val array = JSONArray()
    array.put(value)
    return array
}

private fun getZoomRange(context: Context, base: String): Range<Int>? {
    try {
        val entries = context.assets.list(base)
        if (entries == null) {
            return null
        }
        // Filter to entries with positive integer names that are not empty
        val zoomLevels = entries.mapNotNull { entryName ->
            val level = entryName.toIntOrNull()
            if (level == null || level < 0) {
                return@mapNotNull null
            }
            val levelSubdirectories = context.assets.list("$base/$entryName")
            if (levelSubdirectories.isNullOrEmpty()) {
                return@mapNotNull null
            }
            level
        }
        if (zoomLevels.isEmpty()) {
            return null
        }
        Collections.sort(zoomLevels)
        return Range(zoomLevels.first(), zoomLevels.last())
    } catch (_: IOException) {
        return null
    }
}
