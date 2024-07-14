package org.samcrow.ridgesurvey.data;

import androidx.annotation.NonNull;

import org.joda.time.DateTime;
import org.samcrow.ridgesurvey.Objects;

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
     * If this observation has been uploaded
     */
    private final boolean mUploaded;

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
     * If someone was able to observe this site
     *
     * If this is false, mSpecies must be empty.
     */
    private final boolean mObserved;

    /**
     * If the observation was recorded in test mode
     */
    private final boolean mTest;

    /**
     * Notes that the user recorded
     */
    @NonNull
    private final String mNotes;

    public Observation(@NonNull DateTime time, boolean uploaded, int siteId, @NonNull String routeName,
                       @NonNull Map<String, Boolean> species, @NonNull String notes, boolean observed, boolean test) {
        Objects.requireAllNonNull(time, routeName, species, notes);
        for (Boolean value : species.values()) {
            Objects.requireNonNull(value);
        }
        mTime = time;
        mUploaded = uploaded;
        mSiteId = siteId;
        mRouteName = routeName;
        mSpecies = new HashMap<>(species);
        mNotes = notes;
        mObserved = observed;
        mTest = test;
    }

    @NonNull
    public DateTime getTime() {
        return mTime;
    }

    public boolean isUploaded() {
        return mUploaded;
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

    public boolean isTest() {
        return mTest;
    }

    public boolean isObserved() {
        return mObserved;
    }
}
