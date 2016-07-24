package org.samcrow.ridgesurvey;

import android.support.annotation.NonNull;

import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.layer.cache.TileStore;
import org.mapsforge.map.layer.queue.Job;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Represents a folder that contains image tiles
 */
public class TileFolder extends TileStore {

    /**
     * The root of this tile folder, which contains a folder for each zoom level
     */
    @NonNull
    private final File mRootFolder;

    /**
     * The file extension used for tile images, not including the .
     * Potential values include "jpeg" and "png".
     */
    @NonNull
    private final String mTileExtension;

    /**
     * The size of chunks in which to read from the zip file
     */
    private static final int ZIP_CHUNK_SIZE = 16384;

    /**
     * Creates a tile folder that provides files in the provided directory.
     *
     * @param rootFolder    The root folder, containing a directory for each supported zoom level.
     *                      If this is not a directory or does not exist, the constructed TileFolder
     *                      will not provide any tiles.
     * @param tileExtension The extension that each image file contains, including the .
     *                      @param factory a graphic factory to use
     * @throws NullPointerException if any parameter is null
     */
    public TileFolder(@NonNull File rootFolder, @NonNull String tileExtension, @NonNull
                      GraphicFactory factory) {
        super(rootFolder, tileExtension, factory);
        Objects.requireNonNull(rootFolder, tileExtension);
        mRootFolder = rootFolder;
        mTileExtension = tileExtension;
    }

    public File getRootFolder() {
        return mRootFolder;
    }

    @Override
    protected File findFile(Job key) {
        final byte zoom = key.tile.zoomLevel;
        final int x = key.tile.tileX;
        final int y = Tile.getMaxTileNumber(zoom) - key.tile.tileY;
        final String relativePath = File.separator + zoom
                + File.separator + x
                + File.separator + y
                + '.' + mTileExtension;
        final File imageFile = new File(mRootFolder, relativePath);
        return imageFile;
    }


    /**
     * Reuses files in an existing folder or creates a folder by decompressing a zip archive,
     * based on the time of modification of files
     *
     * @param rootFolder    the folder to store tiles in. This entry does not need to exist. However,
     *                      if it exists, it must be a directory.
     * @param zip           An input stream that yields the bytes of a zip file. The file should contain
     *                      a folder that contains folders for the available zoom levels. This function
     *                      may or may not close the stream.
     * @param fileExtension The extension of the image files, without the .
     *                      @param factory a graphic factory
     * @return a tile folder
     * @throws NullPointerException     if any parameter is null
     * @throws IllegalArgumentException if the arguments do not satisfy the criteria above
     * @throws IOException              if a problem occured reading or writing data
     */
    public static TileFolder createFromZip(@NonNull File rootFolder,
                                           @NonNull InputStream zip,
                                           @NonNull String fileExtension,
                                           @NonNull GraphicFactory factory) throws IOException {
        Objects.requireAllNonNull(rootFolder, zip, fileExtension);

        // Ensure that the root exists and is a folder
        if (rootFolder.exists() && !rootFolder.isDirectory()) {
            throw new IllegalArgumentException("Root folder is present and not a folder");
        }
        if (!rootFolder.exists()) {
            //noinspection ResultOfMethodCallIgnored
            rootFolder.mkdirs();
            if (!rootFolder.isDirectory()) {
                throw new IOException("Failed to create root directory " + rootFolder);
            }
        }
        System.out.println("Root folder: " + rootFolder.getAbsolutePath());

        // TODO Delete files that are not present in the archive

        final ZipInputStream zipStream = new ZipInputStream(new BufferedInputStream(zip));
        ZipEntry entry;
        while ((entry = zipStream.getNextEntry()) != null) {
            // Ignore resource fork files
            if (entry.getName().startsWith("__MACOSX/")) {
                continue;
            }
            // Ignore paths that might be directory traversal exploits
            if (entry.getName().contains("../")) {
                continue;
            }
            // Remove the part of the name before the first slash to ignore the folder in the archive
            final String relativePath = entry.getName().replaceFirst("^[^/]+/", "");
            final File entryFile = new File(rootFolder, relativePath);

            // Ignore .DS_Store files
            if (entryFile.getName().equals(".DS_Store")) {
                continue;
            }

            System.out.println("Investigating path " + relativePath);
            System.out.println("File: " + entryFile.getAbsolutePath());
            System.out.println("Size " + entry.getSize());

            // Copy only files
            if (entry.getSize() != 0) {
                //noinspection ResultOfMethodCallIgnored
                entryFile.getParentFile().mkdirs();
                if (!entryFile.getParentFile().isDirectory()) {
                    throw new IOException("Failed to create directory " + entryFile.getParent());
                }
                if (entryFile.exists()) {
                    // Check modification time
                    if (entry.getTime() >= entryFile.lastModified()) {
                        // Copy and overwrite
                        copyEntry(zipStream, entryFile);
                    }
                } else {
                    // Copy from archive and create
                    copyEntry(zipStream, entryFile);
                }
            }
        }

        return new TileFolder(rootFolder, fileExtension, factory);
    }

    /**
     * Reads the current entry from ZipInputStream and writes it to the provided file
     * @param zip the stream to read from
     * @param targetFile the file to write to
     * @throws IOException
     */
    private static void copyEntry(ZipInputStream zip, File targetFile) throws IOException {
        final FileOutputStream fileOut = new FileOutputStream(targetFile);
        try {
            final byte[] buffer = new byte[ZIP_CHUNK_SIZE];
            while (true) {
                final int readCount = zip.read(buffer, 0, ZIP_CHUNK_SIZE);
                if (readCount == -1) {
                    break;
                }
                fileOut.write(buffer, 0, readCount);
            }
        } finally {
            fileOut.close();
        }
    }

}
