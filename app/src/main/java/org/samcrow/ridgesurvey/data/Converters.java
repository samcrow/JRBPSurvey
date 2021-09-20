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
