package org.samcrow.ridgesurvey.data;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.SQLException;
import androidx.annotation.NonNull;

import org.samcrow.ridgesurvey.Objects;
import org.samcrow.ridgesurvey.data.UploadStatusListener.UploadState;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Keeps track of the status of uploads and updates user interface elements
 */
public class UploadStatusTracker extends BroadcastReceiver {

    /**
     * An action sent to an instance of this class when an upload begins
     */
    public static final String ACTION_UPLOAD_STARTED = UploadStatusTracker.class.getName() + ".ACTION_UPLOAD_STARTED";
    /**
     * An action sent to an instance of this class when an upload succeeds
     */
    public static final String ACTION_UPLOAD_SUCCESS = UploadStatusTracker.class.getName() + ".ACTION_UPLOAD_SUCCESS";
    /**
     * An action sent to an instance of this class when an upload fails
     */
    public static final String ACTION_UPLOAD_FAILED = UploadStatusTracker.class.getName() + ".ACTION_UPLOAD_FAILED";
    /**
     * An action sent to an instance of this class when an observation is recorded
     */
    public static final String ACTION_OBSERVATION_MADE = UploadStatusTracker.class.getName() + ".ACTION_OBSERVATION_MADE";

    /**
     * The context
     */
    @NonNull
    private final Context mContext;

    /**
     * The listeners
     */
    @NonNull
    private final Set<UploadStatusListener> mListeners;

    /**
     * Creates a new tracker
     * @param context a context
     */
    public UploadStatusTracker(@NonNull Context context) {
        Objects.requireNonNull(context);
        mContext = context;
        mListeners = new LinkedHashSet<>();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (action.equals(ACTION_OBSERVATION_MADE)) {
            setListenerStates(UploadState.NeedsUpload);
        } else if (action.equals(ACTION_UPLOAD_STARTED)) {
            setListenerStates(UploadState.Uploading);
        } else if (action.equals(ACTION_UPLOAD_SUCCESS)) {
            if (hasObservationWaiting()) {
                setListenerStates(UploadState.NeedsUpload);
            } else {
                setListenerStates(UploadState.Ok);
            }
        } else if (action.equals(ACTION_UPLOAD_FAILED)) {
            setListenerStates(UploadState.NeedsUpload);
        }
    }

    private void setListenerStates(UploadState state) {
        for (UploadStatusListener listener : mListeners) {
            listener.setState(state);
        }
    }

    public void addListener(@NonNull UploadStatusListener listener) {
        Objects.requireNonNull(listener);
        mListeners.add(listener);
    }

    /**
     * Determines if there are one or more observations waiting to be uploaded
     * @return true if one or more observations still needs to be uploaded
     */
    private boolean hasObservationWaiting() {
        final ObservationDatabase db = new ObservationDatabase(mContext);
        try {
            final List<IdentifiedObservation> observations = db.getObservationsByTime();
            for (IdentifiedObservation observation : observations) {
                if (UploadService.needsUpload(observation)) {
                    return true;
                }
            }
            return false;
        } catch (SQLException e) {
            return false;
        }
    }
}
