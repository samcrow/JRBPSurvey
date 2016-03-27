/*
 * Copyright 2016 Sam Crow
 *
 * This file is part of JRBP Survey.
 *
 * JRBP Survey is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JRBP Survey is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JRBP Survey.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.samcrow.ridgesurvey;

import android.content.Context;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.view.MenuItem;

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
