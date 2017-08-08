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

import android.support.annotation.DrawableRes;
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
     * A drawable resource ID for an image of an individual of this species, or 0 if no image is
     * available
     */
    @DrawableRes
    private final int mImageResource;

    /**
     * Creates a new Species
     * @param name the species name. Must not be null.
     * @param column the column key for the species. Must not be null.
     * @param description a description of the species, or null if none is available
     * @param image an image to represent the species, or null if no image is available
     */
    public Species(@NonNull String name, @NonNull String column, @Nullable String description, @DrawableRes int image) {
        Objects.requireAllNonNull(name, column);
        mName = name;
        mDescription = description;
        mColumn = column;
        mImageResource = image;
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
     * @return A drawable resource ID for an image of an individual of this species, or 0 if no
     * image is available
     */
    @DrawableRes
    public int getImage() {
        return mImageResource;
    }

    @Override
    public String toString() {
        return "Species{" +
                "mName='" + mName + '\'' +
                ", mDescription='" + mDescription + '\'' +
                ", mColumn='" + mColumn + '\'' +
                ", mImage=" + mImageResource +
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

        if (mImageResource != species.mImageResource) {
            return false;
        }
        if (!mName.equals(species.mName)) {
            return false;
        }
        if (mDescription != null ? !mDescription.equals(
                species.mDescription) : species.mDescription != null) {
            return false;
        }
        return mColumn.equals(species.mColumn);

    }

    @Override
    public int hashCode() {
        int result = mName.hashCode();
        result = 31 * result + (mDescription != null ? mDescription.hashCode() : 0);
        result = 31 * result + mColumn.hashCode();
        result = 31 * result + mImageResource;
        return result;
    }
}
