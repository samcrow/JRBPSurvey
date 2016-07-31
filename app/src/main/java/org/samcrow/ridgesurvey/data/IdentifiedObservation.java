package org.samcrow.ridgesurvey.data;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import org.joda.time.DateTime;

import java.io.Serializable;
import java.util.Map;

/**
 * An observation that exists in a database and has an identifer
 */
public class IdentifiedObservation extends Observation implements Parcelable {

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
            return new IdentifiedObservation(time, uploaded, siteId, routeName, species, notes, id);
        }

        @Override
        public IdentifiedObservation[] newArray(int size) {
            return new IdentifiedObservation[size];
        }
    };
}
