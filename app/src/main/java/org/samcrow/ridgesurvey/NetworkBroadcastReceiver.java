package org.samcrow.ridgesurvey;

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
        if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            final boolean noConnectivity = intent.getBooleanExtra(
                    ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
            if (!noConnectivity) {
                // Start the upload service
                context.startService(new Intent(context, UploadService.class));
            }
        }
    }
}
