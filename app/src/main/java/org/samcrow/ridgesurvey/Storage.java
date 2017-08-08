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

package org.samcrow.ridgesurvey;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources.NotFoundException;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Utilities for storage and file management
 */
public class Storage {
    private Storage() {}

    /**
     * Returns a File object containing the path to a file containing the specified resource
     *
     * @param ctx the context to get resources from
     * @param resid the ID of the raw resource to get
     * @return a file containing the content of the resource
     */
    public static File getResourceAsFile(Context ctx, int resid) throws IOException {
        final File storedFile = pathForResource(ctx, resid);
        // Check that the file exists
        // Check that the application has not been updated more recently than the file
        if(storedFile.exists() && storedFile.lastModified() > getAppUpdateTime(ctx)) {
            return storedFile;
        }
        else {
            InputStream stream = null;
            OutputStream fileOut = null;
            try {
                stream = ctx.getResources().openRawResource(resid);
                fileOut = new FileOutputStream(storedFile);
                IOUtils.copy(stream, fileOut);
                return storedFile;
            } catch (NotFoundException e) {
                throw new IOException("Resource not found", e);
            }
            finally {
                if(stream != null) { stream.close(); }
                if(fileOut != null) { fileOut.close(); }
            }
        }
    }

    private static File pathForResource(Context ctx, int resid) {
        return new File(ctx.getCacheDir().getAbsolutePath(), resid + ".resource");
    }

    /**
     * Returns the time that the current application was last updated
     * @param context the context
     * @return the time, in the format returned by {@link System#currentTimeMillis()}
     */
    private static long getAppUpdateTime(Context context) {
        final PackageManager manager = context.getPackageManager();
        try {
            final PackageInfo info = manager.getPackageInfo(BuildConfig.APPLICATION_ID, 0);
            return info.lastUpdateTime;
        } catch (NameNotFoundException e) {
            // Return now, for maximum safety
            return System.currentTimeMillis();
        }
    }
}
