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

import android.support.annotation.Nullable;

/**
 * A utility class similar to {@link java.util.Objects}
 */
public class Objects {

    /**
     * Throws a NullPointerException if any value is null
     * @param values one or more values to check
     * @throws NullPointerException if any argument is null
     */
    public static void requireAllNonNull(@Nullable Object... values) {
        requireNonNull(values);
        for (Object value : values) {
            requireNonNull(value);
        }
    }

    /**
     * Throws a NullPointerException if value is null
     * @param value the value to check
     * @param <T> the value type
     * @throws NullPointerException if value is null
     */
    public static <T> void requireNonNull(@Nullable T value) {
        requireNonNull(value, "Unexpected null value");
    }

    /**
     * Throws a NullPointerException with the specified message if value is null
     * @param value the value to check
     * @param message the message to throw in an exception if value is null
     * @param <T> the value type
     * @throws NullPointerException if value is null
     */
    public static <T> void requireNonNull(@Nullable T value, @Nullable String message) {
        if (value == null) {
            throw new NullPointerException(message);
        }
    }

    private Objects() {}
}
