package org.samcrow.ridgesurvey.data;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;

import org.joda.time.DateTime;

import java.io.Serializable;
import java.util.Map;

/**
 * An observation that exists in a database and has an identifier
 */
public class IdentifiedObservation extends Observation implements Parcelable {

    /**
     * The identifier
     */
    private final int mId;

    public IdentifiedObservation(@NonNull DateTime time, boolean uploaded, int siteId,
                                 @NonNull String routeName,
                                 @NonNull Map<String, Boolean> species,
                                 @NonNull String notes, int id, boolean observed, boolean testMode) {
        super(time, uploaded, siteId, routeName, species, notes, observed, testMode);
        mId = id;
    }

    /**
     * Returns the ID of this observation
     */
    public int getId() {
        return mId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mId);
        dest.writeSerializable(getTime());
        dest.writeByte((byte) (isUploaded() ? 1 : 0));
        dest.writeInt(getSiteId());
        dest.writeString(getRouteName());
        dest.writeSerializable((Serializable) getSpecies());
        dest.writeString(getNotes());
        dest.writeInt(isTest() ? 1 : 0);
        dest.writeInt(isObserved() ? 1 : 0);
    }

    public static final Creator<IdentifiedObservation> CREATOR = new Creator<IdentifiedObservation>() {
        @Override
        public IdentifiedObservation createFromParcel(Parcel in) {
            final int id = in.readInt();
            final DateTime time = (DateTime) in.readSerializable();
            final boolean uploaded = in.readByte() == 1;
            final int siteId = in.readInt();
            final String routeName = in.readString();
            @SuppressWarnings("unchecked")
            final Map<String, Boolean> species = (Map<String, Boolean>) in.readSerializable();
            final String notes = in.readString();
            final boolean testMode = in.readInt() != 0;
            final boolean observed = in.readInt() != 0;
            return new IdentifiedObservation(time, uploaded, siteId, routeName, species, notes, id, observed, testMode);
        }

        @Override
        public IdentifiedObservation[] newArray(int size) {
            return new IdentifiedObservation[size];
        }
    };
}
