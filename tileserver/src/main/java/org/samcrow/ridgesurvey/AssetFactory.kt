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
import android.content.res.AssetManager
import android.util.Log
import org.eclipse.jetty.util.resource.Resource
import org.eclipse.jetty.util.resource.ResourceFactory
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.nio.file.Path
import java.util.Collections
import kotlin.io.path.Path

/**
 * A ResourceFactory that creates resources backed by assets in an application
 *
 * @param context a context that can provide assets
 * @param basePath an asset path that is the root directory to serve
 */
internal class AssetFactory(context: Context, private val basePath: String) : ResourceFactory {
    private val assets: AssetManager = context.assets
    override fun newResource(uri: URI): Resource {
        val path = Path(basePath, uri.path)
        return AssetResource(assets, path)
    }

}

private class AssetResource(private val assets: AssetManager, private val path: Path) : Resource() {
    override fun getPath(): Path? {
        return null
    }

    override fun isDirectory(): Boolean {
        try {
            val entries = assets.list(path.toString())
            return entries != null && entries.isNotEmpty()
        } catch (_: IOException) {
            return false
        }
    }

    override fun isReadable(): Boolean {
        return exists()
    }

    override fun getURI(): URI {
        return URI(path.toString())
    }

    override fun getName(): String {
        return path.toString()
    }

    override fun getFileName(): String? {
        return path.fileName?.toString()
    }

    override fun resolve(subUriPath: String): Resource {
        val resolvedPath = Path(path.toString(), subUriPath)
        return AssetResource(assets, resolvedPath)
    }

    override fun exists(): Boolean {
        try {
            val stream = assets.open(path.toString())
            stream.close()
            return true
        } catch (_: FileNotFoundException) {
            return false
        } catch (e: IOException) {
            Log.w(TAG, "Failed to open asset $path", e)
            return false
        }
    }

    override fun length(): Long {
        try {
            assets.openFd(path.toString()).use { fd ->
                return fd.length
            }
        } catch (e: IOException) {
            Log.w("Failed to get length for $path", e)
            return -1L
        }
    }

    override fun newInputStream(): InputStream {
        return assets.open(path.toString(), AssetManager.ACCESS_STREAMING)
    }

    override fun list(): List<Resource> {
        try {
            val entries = assets.list(path.toString())
            if (entries == null) {
                return Collections.emptyList()
            }
            return entries.map { entryName ->
                val entryPath = path.resolve(entryName)
                AssetResource(assets, entryPath)
            }
        } catch (_: IOException) {
            return Collections.emptyList()
        }
    }
}
