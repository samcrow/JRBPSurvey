package org.samcrow.ridgesurvey;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
    @NonNull public static <T> T requireNonNull(@Nullable T value) {
        requireNonNull(value, "Unexpected null value");
        return value;
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
