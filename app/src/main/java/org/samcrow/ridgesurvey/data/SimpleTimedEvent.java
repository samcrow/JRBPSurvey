package org.samcrow.ridgesurvey.data;

import org.joda.time.DateTime;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class SimpleTimedEvent {

    @PrimaryKey(autoGenerate = true)
    private int id;
    @NonNull
    private final String name;
    @NonNull
    private final DateTime time;

    public SimpleTimedEvent(@NonNull DateTime time, @NonNull String name) {
        this.id = 0;
        this.name = name;
        this.time = time;
    }

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    public DateTime getTime() {
        return time;
    }

    @Override
    public String toString() {
        return "SimpleTimedEvent{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", time=" + time +
                '}';
    }
}
