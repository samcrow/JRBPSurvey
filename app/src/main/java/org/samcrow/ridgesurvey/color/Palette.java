/*
 * Copyright (c) 2025 Sam Crow
 *
 * This file is part of JRBPSurvey.
 *
 * JRBPSurvey is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * JRBPSurvey is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Foobar.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package org.samcrow.ridgesurvey.color;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import org.samcrow.ridgesurvey.R;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Stores a color palette and allows access to a color sequence
 */
public class Palette {

    private static @ColorInt int[] sColors;

    private Palette() {
    }

    /**
     * Returns an iterator over the colors in this palette that loops through all available colors
     * and never ends
     *
     * @return a repeating color iterator
     */
    public static Iterator<Integer> getColorsRepeating(@NonNull Context context) {
        return new RepeatIterator(getColors(context));
    }

    private static @ColorInt int[] getColors(@NonNull Context context) {
        synchronized (Palette.class) {
            if (sColors == null) {
                sColors = loadColorsFromResource(context);
            }
            return sColors;
        }
    }

    private static @ColorInt int[] loadColorsFromResource(@NonNull Context context) {
        final Resources resource = context.getResources();
        try (final TypedArray array = resource.obtainTypedArray(R.array.route_colors)) {
            @ColorInt int[] colors = new int[array.length()];
            for (int i = 0; i < array.length(); i++) {
                colors[i] = array.getColor(i, 0);
            }
            return colors;
        }
    }

    private static class RepeatIterator implements Iterator<Integer> {
        private final @NonNull int[] mValues;
        private int mNextIndex;

        private RepeatIterator(@NonNull int[] values) {
            mValues = Objects.requireNonNull(values);
            mNextIndex = 0;
        }

        @Override
        public boolean hasNext() {
            return mValues.length != 0;
        }

        @Override
        public Integer next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            final int value = mValues[mNextIndex];
            mNextIndex = (mNextIndex + 1) % mValues.length;
            return value;
        }
    }
}
