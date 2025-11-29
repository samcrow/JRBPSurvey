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

package org.samcrow.ridgesurvey.data;

import android.app.BackgroundServiceStartNotAllowedException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

/**
 * Receives notifications of network changes and starts an {@link UploadService}
 */
public class NetworkBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "NetworkBroadcastReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)
                || intent.getAction().equals(Intent.ACTION_TIME_TICK)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    startUploadService(context);
                } catch (BackgroundServiceStartNotAllowedException e) {
                    Log.w(TAG, "Running in background, failed to start upload");
                }
            } else {
                startUploadService(context);
            }
        }
    }

    private void startUploadService(@NonNull Context context) {
        context.startService(new Intent(context, UploadService.class));
    }
}
