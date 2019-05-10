/*
 * Copyright 2017 Sam Crow
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

package org.samcrow.ridgesurvey.map;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RawRes;
import android.util.Log;

import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.samcrow.ridgesurvey.R;
import org.samcrow.ridgesurvey.map.TileFolder.ProgressCallback;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * A task that loads a tile folder from a resource
 */
public class TileFolderLoad extends AsyncTask<Void, TileFolderLoad.Progress, TileFolder> {

    private static final String TAG = TileFolderLoad.class.getSimpleName();

    /**
     * A progress level
     */
    static class Progress {
        /**
         * The current amount of progress
         */
        int current;
        /**
         * The maximum amount of progress
         */
        int max;
    }

    /**
     * Handlers for the completion of the task
     */
    public interface DoneHandler {
        /**
         * Called when the task is done
         * @param result the result of the task
         */
        void done(TileFolder result);
    }

    /**
     * The mContext
     */
    @NonNull
    private final Context mContext;

    /**
     * The zip file input stream
     */
    @NonNull
    private final InputStream mZip;
    /**
     * The tile folder name
     */
    @NonNull
    private final String mFolderName;
    /**
     * The mExtension to use for image files, without a .
     */
    @NonNull
    private final String mExtension;
    /**
     * The cache directory
     */
    @NonNull
    private final File mCacheDir;

    /**
     * The progress dialog
     *
     * May be null if the task is not running
     */
    @Nullable
    private ProgressDialog mDialog;

    /**
     * The handler to be executed when the task is done
     */
    @Nullable
    private DoneHandler mDoneHandler;

    /**
     * Creates a load task
     * @param context the context
     * @param archiveResource the resource ID to load the archive from
     * @param folderName the cache folder name to use
     * @param extension the extension of image files, without the .
     */
    public TileFolderLoad(@NonNull Context context, @RawRes int archiveResource, @NonNull  String folderName, @NonNull  String extension) {
        this.mContext = context;
        this.mZip = context.getResources().openRawResource(archiveResource);
        File cache = context.getExternalCacheDir();
        if (cache == null) {
            cache = context.getCacheDir();
        }
        if (cache == null) {
            throw new IllegalStateException("No cache directory");
        }
        this.mCacheDir = cache;
        this.mFolderName = folderName;
        this.mExtension = extension;
    }

    public void setDoneHandler(@Nullable DoneHandler handler) {
        mDoneHandler = handler;
    }

    @Override
    protected void onPreExecute() {
        // Set up and show the dialog
        mDialog = new ProgressDialog(mContext);
        mDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mDialog.setCancelable(false);
        mDialog.setMessage(mContext.getString(R.string.message_loading_images));
        mDialog.show();
    }

    @Override
    protected void onPostExecute(TileFolder tileFolder) {
        if (mDialog != null) {
            mDialog.hide();
        }
        if (mDoneHandler != null) {
            mDoneHandler.done(tileFolder);
        }
    }

    @Override
    protected void onCancelled(TileFolder tileFolder) {
        if (mDialog != null) {
            mDialog.hide();
        }
    }

    @Override
    protected TileFolder doInBackground(Void... params) {
        final ProgressCallback callback = new ProgressCallback() {
            @Override
            public void progress(int current, int maximum) {
                final Progress progress = new Progress();
                progress.current = current;
                progress.max = maximum;
                publishProgress(progress);
            }
        };
        final File rootFolder = new File(mCacheDir, mFolderName);
        try {
            return TileFolder.createFromZip(rootFolder, mZip, mExtension, AndroidGraphicFactory.INSTANCE, callback);
        } catch (IOException e) {
            Log.e(TAG, "Failed to load tiles", e);
            cancel(false);
            return null;
        }
    }

    @Override
    protected void onProgressUpdate(Progress... values) {
        final Progress progress = values[0];
        if (mDialog != null) {
            mDialog.setMax(progress.max);
            mDialog.setProgress(progress.current);
        }
    }
}
