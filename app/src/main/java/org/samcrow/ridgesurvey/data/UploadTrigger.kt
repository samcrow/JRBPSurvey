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

package org.samcrow.ridgesurvey.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log

private const val TAG = "UploadTrigger"

/**
 * This class listens for broadcasts and connectivity events and uses them to start the upload
 * service.
 */
class UploadTrigger(private val context: Context) {

    private val timeTickReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != Intent.ACTION_TIME_TICK) {
                return
            }
            startUploadService()
        }
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                startUploadService()
            }
        }
    }

    private val connectivity = context.getSystemService(ConnectivityManager::class.java)

    init {
        val timeTickFilter = IntentFilter(Intent.ACTION_TIME_TICK)
        context.registerReceiver(timeTickReceiver, timeTickFilter)

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivity.registerNetworkCallback(networkRequest, networkCallback)
    }

    /** Unregisters callbacks and associated resources */
    fun close() {
        context.unregisterReceiver(timeTickReceiver)
        connectivity.unregisterNetworkCallback(networkCallback)
    }

    private fun startUploadService() {
        try {
            context.startService(Intent(context, UploadService::class.java))
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Unable to start upload service", e)
        }
    }
}