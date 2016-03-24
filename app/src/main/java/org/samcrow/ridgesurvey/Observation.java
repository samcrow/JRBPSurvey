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

    @NonNull
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
