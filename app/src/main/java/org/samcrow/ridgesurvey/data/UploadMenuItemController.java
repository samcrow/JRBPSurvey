package org.samcrow.ridgesurvey.data;

import android.content.Context;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import android.view.MenuItem;

import org.samcrow.ridgesurvey.Objects;
import org.samcrow.ridgesurvey.R;

/**
 * Controls a menu item that displays and controls the upload process
 */
public class UploadMenuItemController implements UploadStatusListener {

    /**
     * The menu item being controlled
     */
    @NonNull
    private final MenuItem mItem;

    /**
     * The icon that indicates that all uploads are done
     */
    @DrawableRes
    private static final int ICON_DONE = R.drawable.ic_cloud_done_white_24dp;
    /**
     * The icon that indicates that uploads need to be done
     */
    @DrawableRes
    private static final int ICON_UPLOAD = R.drawable.ic_cloud_upload_white_24dp;

    /**
     * The icon that indicates an upload in progress
     */
    @DrawableRes
    private static final int ICON_IN_PROGRESS = R.drawable.ic_cloud_queue_white_24dp;

    public UploadMenuItemController(@NonNull Context context, @NonNull MenuItem item) {
        Objects.requireAllNonNull(context, item);
        mItem = item;

        // Set up
        mItem.setIcon(ICON_DONE);
        mItem.setEnabled(false);
    }

    @Override
    public void setState(UploadState state) {
        switch (state) {
            case Ok:
                mItem.setIcon(ICON_DONE);
                mItem.setEnabled(false);
                break;
            case Uploading:
                mItem.setIcon(ICON_IN_PROGRESS);
                mItem.setEnabled(false);
                break;
            case NeedsUpload:
                mItem.setIcon(ICON_UPLOAD);
                mItem.setEnabled(true);
                break;
        }
    }
}
