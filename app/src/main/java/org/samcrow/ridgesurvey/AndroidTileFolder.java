package org.samcrow.ridgesurvey;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.annotation.RawRes;

import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.samcrow.ridgesurvey.TileFolder.ProgressCallback;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Provides access to {@link TileFolder} objects in Android
 */
public class AndroidTileFolder {

    public static TileFolder fromResource(Context context, String folderName, String fileExtension,
                                          @RawRes int resid, @Nullable
                                          ProgressCallback progressCallback) throws
            IOException {
        final InputStream zip = context.getResources().openRawResource(resid);
        final File cacheDir = context.getExternalCacheDir();
        final File tilesDir = new File(cacheDir, folderName);
        return TileFolder.createFromZip(tilesDir, zip, fileExtension,
                AndroidGraphicFactory.INSTANCE, progressCallback);
    }

    private AndroidTileFolder() {
    }
}
