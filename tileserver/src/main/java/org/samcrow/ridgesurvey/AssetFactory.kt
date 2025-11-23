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
import java.nio.channels.ReadableByteChannel
import java.nio.file.Path
import java.time.Instant
import java.util.Collections
import kotlin.io.path.Path
internal class AssetFactory(context: Context, private val basePath: String) : ResourceFactory {
    private val assets: AssetManager = context.assets
    override fun newResource(uri: URI): Resource {
        Log.d(TAG, "newResource $uri")
        val path = Path(basePath, uri.path)
        Log.d(TAG, "base $basePath + URI ${uri.path} = $path")
        return ResourceWrapper(AssetResource(assets, path))
    }

}

private class AssetResource(private val assets: AssetManager, private val path: Path) : Resource() {
    override fun getPath(): Path? {
        return null
    }

    override fun isDirectory(): Boolean {
        Log.d(TAG, "isDirectory() $path")
        try {
            val entries = assets.list(path.toString())
            Log.d(TAG, "Entries list: ${entries.contentToString()}")
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
        Log.d(TAG, "Resolve relative to $path : $subUriPath = $resolvedPath")
        return ResourceWrapper(AssetResource(assets, resolvedPath))
    }

    override fun exists(): Boolean {
        Log.d(TAG, "exists() $path")
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
        Log.d(TAG, "newInputStream $path")
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

private class ResourceWrapper(private val inner: Resource) : Resource() {
    private val innerClass = inner.javaClass.simpleName
    override fun getPath(): Path? {
        val path = inner.path
        Log.d(TAG, "$innerClass getPath() returned $path")
        return path
    }

    override fun isDirectory(): Boolean {
        val value = inner.isDirectory
        Log.d(TAG, "$innerClass isDirectory() returned $value")
        return value
    }

    override fun isReadable(): Boolean {
        val value = inner.isReadable
        Log.d(TAG, "$innerClass isReadable() returned $value")
        return value
    }

    override fun getURI(): URI? {
        val value = inner.uri
        Log.d(TAG, "$innerClass getURI() returned $value")
        return value
    }

    override fun getName(): String? {
        val value = inner.name
        Log.d(TAG, "$innerClass getName() returned $value")
        return value
    }

    override fun getFileName(): String? {
        val value = inner.fileName
        Log.d(TAG, "$innerClass getFileName() returned $value")
        return value
    }

    override fun resolve(subUriPath: String?): Resource? {
        val value = inner.resolve(subUriPath)
        Log.d(TAG, "$innerClass resolve($subUriPath) returned $value")
        return value
    }

    override fun isContainedIn(container: Resource?): Boolean {
        val value = inner.isContainedIn(container)
        Log.d(TAG, "$innerClass isContainedIn(container) returned $value")
        return value
    }

    override fun contains(other: Resource?): Boolean {
        val value = inner.contains(other)
        Log.d(TAG, "$innerClass contains($other) returned $value")
        return value
    }

    override fun getPathTo(other: Resource?): Path? {
        val value = inner.getPathTo(other)
        Log.d(TAG, "$innerClass getPathTo($other) returned $value")
        return value
    }

    override fun iterator(): MutableIterator<Resource?> {
        val value = inner.iterator()
        Log.d(TAG, "$innerClass iterator() returned $value")
        return value
    }

    override fun exists(): Boolean {
        val value = inner.exists()
        Log.d(TAG, "$innerClass exists() returned $value")
        return value
    }

    override fun lastModified(): Instant? {
        val value = inner.lastModified()
        Log.d(TAG, "$innerClass lastModified() returned $value")
        return value
    }

    override fun length(): Long {
        val value = inner.length()
        Log.d(TAG, "$innerClass length() returned $value")
        return value
    }

    override fun newInputStream(): InputStream? {
        val value = inner.newInputStream()
        Log.d(TAG, "$innerClass newInputStream() returned $value")
        return value
    }

    override fun newReadableByteChannel(): ReadableByteChannel? {
        val value = inner.newReadableByteChannel()
        Log.d(TAG, "$innerClass newReadableByteChannel() returned $value")
        return value
    }

    override fun list(): List<Resource?>? {
        val value = inner.list()
        Log.d(TAG, "$innerClass list() returned $value")
        return value
    }

    override fun isAlias(): Boolean {
        val value = inner.isAlias
        Log.d(TAG, "$innerClass isAlias() returned $value")
        return value
    }

    override fun getRealURI(): URI? {
        val value = inner.realURI
        Log.d(TAG, "$innerClass getRealURI() returned $value")
        return value
    }

    override fun isSameFile(path: Path?): Boolean {
        val value = inner.isSameFile(path)
        Log.d(TAG, "$innerClass isSameFile($path) returned $value")
        return value
    }
}