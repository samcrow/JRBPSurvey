package org.samcrow.ridgesurvey.data;

import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

/**
 * Database of simple timed events
 *
 * Changelog:
 *
 * Version 2: Added route field to SimpleTimedEvent
 * Version 1: initial
 */
@androidx.room.Database(entities = {SimpleTimedEvent.class}, version = 2)
@TypeConverters({Converters.class})
public abstract class Database extends RoomDatabase {
    public abstract SimpleTimedEventDao simpleTimedEventDao();
}
