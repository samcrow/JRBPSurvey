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
    @NonNull
    private final String route;

    public SimpleTimedEvent(@NonNull DateTime time, @NonNull String name, @NonNull String route) {
        this.id = 0;
        this.name = name;
        this.time = time;
        this.route = route;
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

    @NonNull
    public String getRoute() {
        return route;
    }

    @Override
    public String toString() {
        return "SimpleTimedEvent{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", time=" + time +
                ", route='" + route + '\'' +
                '}';
    }
}
