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

package org.samcrow.ridgesurvey.data;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import androidx.room.TypeConverter;

public class Converters {

    private Converters() {
    }

    @TypeConverter
    public static DateTime dateTimeFromString(String dateTimeString) {
        if (dateTimeString != null) {
            return ISODateTimeFormat.dateTimeNoMillis().parseDateTime(dateTimeString);
        } else {
            return null;
        }
    }

    @TypeConverter
    public static String stringFromDateTime(DateTime dateTime) {
        if (dateTime != null) {
            return ISODateTimeFormat.dateTimeNoMillis().print(dateTime);
        } else {
            return null;
        }
    }
}
