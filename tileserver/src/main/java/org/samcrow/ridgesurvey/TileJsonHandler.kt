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

import org.eclipse.jetty.http.HttpHeader
import org.eclipse.jetty.io.Content
import org.eclipse.jetty.server.Handler
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.Response
import org.eclipse.jetty.util.Callback
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI


internal class TileJsonHandler(private val base: String, private val fileExtension: String) :
    Handler.Abstract.NonBlocking() {

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
        val tileUrl = "$base/{z}/{x}/{y}.$fileExtension"
        return JSONObject()
            .put("tilejson", "2.1.0")
            .put("tiles", jsonArrayOf(tileUrl))
    }
}

private fun jsonArrayOf(value: Any): JSONArray {
    val array = JSONArray()
    array.put(value)
    return array
}