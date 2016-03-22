package org.samcrow.ridgesurvey;

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Represents a species of ant that can be marked present
 *
 * Instances of this class are immutable.
 */
public class Species {
    /**
     * The scientific name, and optional explanatory text, of the species
     */
    @NonNull
    private final String mName;

    /**
     * An image of an individual of this species, or null if no image is available
     */
    @Nullable
    private final Drawable mImage;

    /**
     * Creates a new Species
     * @param name the species name. Must not be null.
     * @param image an image to represent the species, or null if no image is available
     */
    public Species(@NonNull String name, @Nullable Drawable image) {
        Objects.requireNonNull(name);
        mName = name;
        mImage = image;
    }

    /**
     * Returns the name of this species
     * @return the name
     */
    @NonNull
    public String getName() {
        return mName;
    }

    /**
     * Returns the image that represents this species
     * @return an image, or null if no image is available
     */
    @Nullable
    public Drawable getImage() {
        if (mImage != null) {
            return mImage.mutate();
        } else {
            return null;
        }
    }
}
