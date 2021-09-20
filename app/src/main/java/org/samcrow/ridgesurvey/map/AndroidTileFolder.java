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

import android.content.Context;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;

import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.samcrow.ridgesurvey.map.TileFolder.ProgressCallback;

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
