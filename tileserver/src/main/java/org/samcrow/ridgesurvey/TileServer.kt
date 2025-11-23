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
import android.util.Log
import org.eclipse.jetty.server.Handler
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.server.handler.ResourceHandler
import org.eclipse.jetty.util.resource.ResourceFactory
import java.io.Closeable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.FutureTask

internal const val TAG: String = "TileServer"

/**
 * A web server that binds to 127.0.0.1 and provides tiles over a loopback network interface
 *
 * The endpoint <code>/tiles.json</code> returns a
 * <a href="https://github.com/mapbox/tilejson-spec/tree/master/2.1.0">TileJSON 2.1.0</a> file with
 * a URL pattern that a map can use to load tiles.
 */
class TileServer : Closeable {
    private val server: Server

    /**
     * A URL that a map rendering library can access to get a TileJSON file with the URL pattern
     * for the other tiles
     */
    val tileJsonUrl: Future<String>

    /**
     * Creates and starts a server
     * @param context a Context that the server will use to access assets
     * @param relativeAssetPath the asset path to a directory that contains one subdirectory
     * for each available zoom level
     */
    constructor(context: Context, relativeAssetPath: String) {
        server = Server()
        val connector = ServerConnector(server)
        connector.host = "127.0.0.1"
        server.connectors = arrayOf(connector)

        val executor = Executors.newSingleThreadExecutor()
        tileJsonUrl = FutureTask {
            connector.start()
            val port = connector.localPort
            val baseUrl = "http://localhost:$port"

            val tileJson = TileJsonHandler(context, relativeAssetPath, baseUrl, "webp")

            val factory = AssetFactory(context, relativeAssetPath)
            ResourceFactory.registerResourceFactory("http", factory)
            val handler = ResourceHandler()
            handler.isDirAllowed = false
            handler.cacheControl = "public, max-age=86400"
            handler.baseResource = ResourceFactory.of(handler).newResource("http:///")

            server.setHandler(Handler.Sequence(tileJson, handler))
            server.start()
            Log.i(TAG, "Started server on port $port")
            return@FutureTask "$baseUrl/tiles.json"
        }
        executor.submit(tileJsonUrl)
        executor.shutdown()
    }

    override fun close() {
        server.stop()
    }
}

