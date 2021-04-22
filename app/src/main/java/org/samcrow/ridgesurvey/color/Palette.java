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

package org.samcrow.ridgesurvey.color;

import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;

import org.samcrow.ridgesurvey.Objects;

import java.util.Arrays;
import java.util.Iterator;

/**
 * Stores a color palette and allows access to a color sequence
 */
public class Palette {

    /**
     * Tableau 20 colors
     */
    private static final @ColorInt Integer[] COLORS = {
            Color.argb(255, 31, 119, 180),
            Color.argb(255, 152, 223, 138),
            Color.argb(255, 140, 86, 75),
            Color.argb(255, 199, 199, 199),
            Color.argb(255, 174, 199, 232),
            Color.argb(255, 214, 39, 40),
            Color.argb(255, 196, 156, 148),
            Color.argb(255, 188, 189, 34),
            Color.argb(255, 255, 127, 14),
            Color.argb(255, 255, 152, 150),
            Color.argb(255, 227, 119, 194),
            Color.argb(255, 219, 219, 141),
            Color.argb(255, 255, 187, 120),
            Color.argb(255, 148, 103, 189),
            Color.argb(255, 247, 182, 210),
            Color.argb(255, 23, 190, 207),
            Color.argb(255, 44, 160, 44),
            Color.argb(255, 197, 176, 213),
            Color.argb(255, 127, 127, 127),
            Color.argb(255, 158, 218, 229),
    };

    /**
     * Returns an iterator over the colors in this palette that loops through all available colors
     * and never ends
     * @return a repeating color iterator
     */
    public static Iterator<Integer> getColorsRepeating() {
        return new RepeatIterator<>(getColorIterable());
    }

    private static Iterable<Integer> getColorIterable() {
        return Arrays.asList(COLORS);
    }

    private Palette() {

    }

    private static class RepeatIterator<E> implements Iterator<E> {

        /**
         * The item source
         *
         * Invariant: Not null
         */
        @NonNull
        private final Iterable<E> mSource;

        /**
         * The current active iterator over the source
         *
         * Invariants:
         * Not null
         * If mSource is not empty, always has a next item to provide.
         * Otherwise, never has a next item to provide.
         */
        @NonNull
        private Iterator<E> mSourceIterator;

        /**
         * Creates a repeating iterator from an iterable source
         * @param source an item source
         */
        private RepeatIterator(@NonNull Iterable<E> source) {
            Objects.requireNonNull(source);
            mSource = source;
            mSourceIterator = mSource.iterator();
        }

        @Override
        public boolean hasNext() {
            return mSourceIterator.hasNext();
        }

        @Override
        public E next() {
            final E item = mSourceIterator.next();
            if (!mSourceIterator.hasNext()) {
                mSourceIterator = mSource.iterator();
            }
            return item;
        }
    }
}
