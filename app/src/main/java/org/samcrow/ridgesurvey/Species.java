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
     * A description of the species, or null if none is available
     */
    @Nullable
    private final String mDescription;

    /**
     * The column key used when submitting information on this species
     */
    @NonNull
    private final String mColumn;

    /**
     * An image of an individual of this species, or null if no image is available
     */
    @Nullable
    private final Drawable mImage;

    /**
     * Creates a new Species
     * @param name the species name. Must not be null.
     * @param column the column key for the species. Must not be null.
     * @param description a description of the species, or null if none is available
     * @param image an image to represent the species, or null if no image is available
     */
    public Species(@NonNull String name, @NonNull String column, @Nullable String description, @Nullable Drawable image) {
        Objects.requireAllNonNull(name, column);
        mName = name;
        mDescription = description;
        mColumn = column;
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
     * Returns the column name used with this species
     * @return the column name
     */
    @NonNull
    public String getColumn() {
        return mColumn;
    }

    /**
     * Returns the description of this species
     * @return the description, or null if none is present
     */
    @Nullable
    public String getDescription() {
        return mDescription;
    }

    /**
     * Returns the image that represents this species
     * @return an image, or null if no image is available
     */
    @Nullable
    public Drawable getImage() {
        return mImage;
    }

    @Override
    public String toString() {
        return "Species{" +
                "mName='" + mName + '\'' +
                ", mDescription='" + mDescription + '\'' +
                ", mColumn='" + mColumn + '\'' +
                ", mImage=" + mImage +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Species species = (Species) o;

        if (!mName.equals(species.mName)) {
            return false;
        }
        if (mDescription != null ? !mDescription.equals(
                species.mDescription) : species.mDescription != null) {
            return false;
        }
        if (!mColumn.equals(species.mColumn)) {
            return false;
        }
        return !(mImage != null ? !mImage.equals(species.mImage) : species.mImage != null);

    }

    @Override
    public int hashCode() {
        int result = mName.hashCode();
        result = 31 * result + (mDescription != null ? mDescription.hashCode() : 0);
        result = 31 * result + mColumn.hashCode();
        result = 31 * result + (mImage != null ? mImage.hashCode() : 0);
        return result;
    }
}
