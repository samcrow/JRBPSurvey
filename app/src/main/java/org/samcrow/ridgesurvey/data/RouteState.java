package org.samcrow.ridgesurvey.data;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import org.samcrow.ridgesurvey.Objects;

/**
 * Information about the route that the user is currently surveying
 */
public class RouteState implements Parcelable {
    /**
     * The name of the person (or people) surveying this route
     *
     * This may not be null, but it may be empty.
     */
    @NonNull
    private final String mSurveyorName;
    /**
     * The name of this route
     *
     * This may not be null, but it may be empty.
     */
    @NonNull
    private final String mRouteName;

    /**
     * An identifier of the tablet being used
     *
     * This may not be null, but it may be empty.
     */
    @NonNull
    private final String mTabletId;

    /**
     * If the application is in test mode, so observations are not meaningful
     */
    private final boolean mTestMode;

    private RouteState(@NonNull String surveyorName, @NonNull String routeName, @NonNull String tabletId, boolean testMode) {
        mSurveyorName = surveyorName;
        mRouteName = routeName;
        mTabletId = tabletId;
        mTestMode = testMode;
    }

    /**
     * Creates a non-test-mode RouteState
     */
    public RouteState(@NonNull String surveyorName, @NonNull String routeName, @NonNull String tabletId) {
        this(surveyorName, routeName, tabletId, false);
    }

    /**
     * Returns a new RouteState in test mode with other fields empty
     */
    public static RouteState testMode() {
        return new RouteState("", "", "", true);
    }

    @NonNull
    public String getSurveyorName() {
        return mSurveyorName;
    }

    @NonNull
    public String getRouteName() {
        return mRouteName;
    }

    @NonNull
    public String getTabletId() {
        return mTabletId;
    }

    public boolean isTestMode() {
        return mTestMode;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(mSurveyorName);
        parcel.writeString(mRouteName);
        parcel.writeString(mTabletId);
        parcel.writeByte((byte) (mTestMode ? 1 : 0));
    }

    protected RouteState(Parcel in) {
        mSurveyorName = Objects.requireNonNull(in.readString());
        mRouteName = Objects.requireNonNull(in.readString());
        mTabletId = Objects.requireNonNull(in.readString());
        mTestMode = in.readByte() != 0;
    }

    public static final Creator<RouteState> CREATOR = new Creator<RouteState>() {
        @Override
        public RouteState createFromParcel(Parcel in) {
            return new RouteState(in);
        }

        @Override
        public RouteState[] newArray(int size) {
            return new RouteState[size];
        }
    };
}
