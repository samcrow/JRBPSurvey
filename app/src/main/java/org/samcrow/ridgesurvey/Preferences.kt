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
import android.content.SharedPreferences
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng

private const val KEY_BASE = "org.samcrow.ridgesurvey.preferences."
private const val PREFS_NAME = "org.samcrow.ridgesurvey.Preferences"

private const val KEY_SELECTED_SITE = KEY_BASE + "selected_site"
private const val KEY_CAMERA_BEARING = KEY_BASE + "camera_bearing"
private const val KEY_CAMERA_PADDING_LEFT = KEY_BASE + "camera_padding_left"
private const val KEY_CAMERA_PADDING_TOP = KEY_BASE + "camera_padding_top"
private const val KEY_CAMERA_PADDING_RIGHT = KEY_BASE + "camera_padding_right"
private const val KEY_CAMERA_PADDING_BOTTOM = KEY_BASE + "camera_padding_bottom"
private const val KEY_CAMERA_TARGET_LATITUDE = KEY_BASE + "camera_target_latitude"
private const val KEY_CAMERA_TARGET_LONGITUDE = KEY_BASE + "camera_target_longitude"
private const val KEY_CAMERA_TILT = KEY_BASE + "camera_tilt"
private const val KEY_CAMERA_ZOOM = KEY_BASE + "camera_zoom"

private val ALL_CAMERA_KEYS = arrayOf(
    KEY_CAMERA_BEARING, KEY_CAMERA_PADDING_LEFT, KEY_CAMERA_PADDING_TOP, KEY_CAMERA_PADDING_RIGHT,
    KEY_CAMERA_PADDING_BOTTOM, KEY_CAMERA_TARGET_LATITUDE, KEY_CAMERA_TARGET_LONGITUDE,
    KEY_CAMERA_TILT, KEY_CAMERA_ZOOM
)

internal class Preferences(context: Context) {
    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSelectedSiteId(): Int? {
        return if (preferences.contains(KEY_SELECTED_SITE)) {
            preferences.getInt(KEY_SELECTED_SITE, -1)
        } else {
            null
        }
    }

    fun getCamera(): CameraPosition? {
        if (!haveAllCameraKeys()) {
            return null
        }
        return CameraPosition.Builder()
            .bearing(getDouble(KEY_CAMERA_BEARING))
            .padding(
                getDouble(KEY_CAMERA_PADDING_LEFT),
                getDouble(KEY_CAMERA_PADDING_TOP),
                getDouble(KEY_CAMERA_PADDING_RIGHT),
                getDouble(KEY_CAMERA_PADDING_BOTTOM)
            )
            .target(
                LatLng(
                    getDouble(KEY_CAMERA_TARGET_LATITUDE),
                    getDouble(KEY_CAMERA_TARGET_LONGITUDE)
                )
            )
            .tilt(getDouble(KEY_CAMERA_TILT))
            .zoom(getDouble(KEY_CAMERA_ZOOM))
            .build()
    }

    private fun getDouble(key: String): Double {
        return Double.fromBits(preferences.getLong(key, 0L))
    }

    private fun haveAllCameraKeys(): Boolean {
        return ALL_CAMERA_KEYS.all(preferences::contains)
    }

    fun edit(): Editor {
        return Editor(preferences.edit())
    }

    class Editor(private val inner: SharedPreferences.Editor) {

        fun setSelectedSiteId(id: Int?) {
            if (id != null) {
                inner.putInt(KEY_SELECTED_SITE, id)
            } else {
                inner.remove(KEY_SELECTED_SITE)
            }
        }

        fun setCamera(camera: CameraPosition?) {
            if (camera != null) {
                setNonNullCamera(camera)
            } else {
                for (key in ALL_CAMERA_KEYS) {
                    inner.remove(key)
                }
            }
        }

        private fun setNonNullCamera(camera: CameraPosition) {
            putDouble(KEY_CAMERA_BEARING, camera.bearing)
            val padding = camera.padding ?: doubleArrayOf(0.0, 0.0, 0.0, 0.0)
            putDouble(KEY_CAMERA_PADDING_LEFT, padding[0])
            putDouble(KEY_CAMERA_PADDING_TOP, padding[1])
            putDouble(KEY_CAMERA_PADDING_RIGHT, padding[2])
            putDouble(KEY_CAMERA_PADDING_BOTTOM, padding[3])
            val target = camera.target ?: LatLng()
            putDouble(KEY_CAMERA_TARGET_LATITUDE, target.latitude)
            putDouble(KEY_CAMERA_TARGET_LONGITUDE, target.longitude)
            putDouble(KEY_CAMERA_TILT, camera.tilt)
            putDouble(KEY_CAMERA_ZOOM, camera.zoom)
        }

        private fun putDouble(key: String, value: Double) {
            inner.putLong(key, value.toBits())
        }

        fun apply() {
            inner.apply()
        }

        fun commit(): Boolean {
            return inner.commit()
        }
    }
}