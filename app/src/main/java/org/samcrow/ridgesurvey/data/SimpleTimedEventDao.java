package org.samcrow.ridgesurvey.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface SimpleTimedEventDao {
    @Insert
    void insert(SimpleTimedEvent event);

    @Query("SELECT * FROM simpletimedevent ORDER BY time ASC LIMIT 1")
    SimpleTimedEvent getOldest();

    @Delete
    void delete(SimpleTimedEvent event);
}
