/*
 * Copyright 2016 Sam Crow
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

import android.support.annotation.NonNull;

import org.joda.time.DateTime;

import java.util.HashMap;
import java.util.Map;

/**
 * An observation made by a user
 */
public class Observation {
    /**
     * The time the observation was made
     */
    @NonNull
    private final DateTime mTime;

    /**
     * The ID of the site where the observation was made
     */
    private final int mSiteId;

    /**
     * The name of the route that the site is part of
     */
    @NonNull
    private final String mRouteName;

    /**
     * The species that were observed
     *
     * Keys in this map are species column names. Values may not be null.
     */
    @NonNull
    private final Map<String, Boolean> mSpecies;

    /**
     * Notes that the user recorded
     */
    @NonNull
    private final String mNotes;

    public Observation(@NonNull DateTime time, int siteId, @NonNull String routeName,
                       @NonNull Map<String, Boolean> species, @NonNull String notes) {
        Objects.requireAllNonNull(time, routeName, species, notes);
        for (Boolean value : species.values()) {
            Objects.requireNonNull(value);
        }
        mTime = time;
        mSiteId = siteId;
        mRouteName = routeName;
        mSpecies = new HashMap<>(species);
        mNotes = notes;
    }

    @NonNull
    public DateTime getTime() {
        return mTime;
    }

    public int getSiteId() {
        return mSiteId;
    }

    @NonNull
    public String getRouteName() {
        return mRouteName;
    }

    @NonNull
    public Map<String, Boolean> getSpecies() {
        return new HashMap<>(mSpecies);
    }

    @NonNull
    public String getNotes() {
        return mNotes;
    }
}
