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
    public class Progress {
        /**
         * The current amount of progress
         */
        public int current;
        /**
         * The maximum amount of progress
         */
        public int max;
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
        final File cache = context.getExternalCacheDir();
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
