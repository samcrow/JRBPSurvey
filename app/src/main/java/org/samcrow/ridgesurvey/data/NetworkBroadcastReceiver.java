package org.samcrow.ridgesurvey.data;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;

/**
 * Receives notifications of network changes and starts an {@link UploadService}
 */
public class NetworkBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)
                || intent.getAction().equals(Intent.ACTION_TIME_TICK)) {
            // Start the upload service
            context.startService(new Intent(context, UploadService.class));
        }
    }
}
