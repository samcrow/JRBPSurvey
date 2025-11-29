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

import android.Manifest
import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.app.PendingIntent
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresPermission
import org.maplibre.android.location.engine.LocationEngine
import org.maplibre.android.location.engine.LocationEngineCallback
import org.maplibre.android.location.engine.LocationEngineRequest
import org.maplibre.android.location.engine.LocationEngineResult

/** This is the only provider that reliably provides updates and does not rely on Google. */
private const val PROVIDER = LocationManager.GPS_PROVIDER
private const val TAG = "GpsLocationEngine"

class GpsLocationEngine(context: Context) : LocationEngine {
    private val manager: LocationManager = context.getSystemService(LocationManager::class.java)

    /**
     * A map from Maplibre callbacks to AOSP location callbacks
     */
    private val callbacks: MutableMap<LocationEngineCallback<LocationEngineResult?>, LocationListener> =
        HashMap()

    @RequiresPermission(allOf = [ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION])
    override fun getLastLocation(callback: LocationEngineCallback<LocationEngineResult?>) {
        try {
            val lastKnownLocation = manager.getLastKnownLocation(PROVIDER)
            callback.onSuccess(LocationEngineResult.create(lastKnownLocation))
        } catch (e: IllegalArgumentException) {
            logProviderUnavailable()
            callback.onFailure(e)
        }
    }

    @RequiresPermission(allOf = [ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION])
    override fun requestLocationUpdates(
        request: LocationEngineRequest,
        callback: LocationEngineCallback<LocationEngineResult?>,
        looper: Looper?
    ) {
        val innerCallback = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                callback.onSuccess(LocationEngineResult.create(location))
            }
        }
        val prevInnerCallback = callbacks.put(callback, innerCallback)
        if (prevInnerCallback != null) {
            callbacks.remove(callback)
            throw kotlin.IllegalStateException("Location request for this callback already registered")
        }
        try {
            manager.requestLocationUpdates(
                PROVIDER, request.interval, request.displacement, innerCallback, looper
            )
        } catch (e: IllegalArgumentException) {
            logProviderUnavailable()
            callback.onFailure(e)
        }
    }

    @RequiresPermission(allOf = [ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION])
    override fun requestLocationUpdates(
        request: LocationEngineRequest, pendingIntent: PendingIntent
    ) {
        try {
            manager.requestLocationUpdates(
                PROVIDER,
                request.interval,
                request.displacement,
                pendingIntent,
            )
        } catch (e: IllegalArgumentException) {
            logProviderUnavailable()
        }
    }

    override fun removeLocationUpdates(callback: LocationEngineCallback<LocationEngineResult?>) {
        val innerCallback = callbacks.remove(callback)
            ?: throw IllegalStateException("No existing location request for this callback")
        manager.removeUpdates(innerCallback)
    }

    override fun removeLocationUpdates(pendingIntent: PendingIntent) {
        manager.removeUpdates(pendingIntent)
    }
}

private fun logProviderUnavailable() {
    Log.w(TAG, "Location provider $PROVIDER not available")
}