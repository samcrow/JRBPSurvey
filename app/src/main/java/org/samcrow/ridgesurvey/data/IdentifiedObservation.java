package org.samcrow.ridgesurvey.data;

import android.support.annotation.NonNull;

import org.joda.time.DateTime;

import java.util.Map;

/**
 * An observation that exists in a database and has an identifer
 */
public class IdentifiedObservation extends Observation {

    /**
     * The identifier
     */
    private final int mId;

    public IdentifiedObservation(@NonNull DateTime time, boolean uploaded, int siteId,
                                 @NonNull String routeName,
                                 @NonNull Map<String, Boolean> species,
                                 @NonNull String notes, int id) {
        super(time, uploaded, siteId, routeName, species, notes);
        mId = id;
    }

    /**
     * Returns the ID of this observation
     * @return
     */
    public int getId() {
        return mId;
    }
}
