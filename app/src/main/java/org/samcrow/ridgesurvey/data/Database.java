package org.samcrow.ridgesurvey.data;

import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

@androidx.room.Database(entities = {SimpleTimedEvent.class}, version = 1)
@TypeConverters({Converters.class})
public abstract class Database extends RoomDatabase {
    public abstract SimpleTimedEventDao simpleTimedEventDao();
}
