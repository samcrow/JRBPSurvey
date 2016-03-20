package org.samcrow.ridgesurvey;

import android.content.Context;

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
     * @param ctx the context to get resources from
     * @param resid the ID of the raw resource to get
     * @return a file containing the content of the resource
     */
    public static File getResourceAsFile(Context ctx, int resid) throws IOException {
        final File storedFile = pathForResource(ctx, resid);
        if(storedFile.exists()) {
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
}
