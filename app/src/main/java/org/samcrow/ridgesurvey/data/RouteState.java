package org.samcrow.ridgesurvey.data;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import org.joda.time.DateTime;
import org.joda.time.ReadableDateTime;
import org.samcrow.ridgesurvey.Objects;

/**
 * Information about the route that the user is currently surveying
 */
public class RouteState implements Parcelable {

    /**
     * The date and time when the user clicked on the start button
     */
    @NonNull
    private final DateTime mStartTime;

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

    @NonNull
    private final String mSensorId;

    /**
     * If the application is in test mode, so observations are not meaningful
     */
    private final boolean mTestMode;

    private RouteState(@NonNull DateTime startTime, @NonNull String surveyorName, @NonNull String routeName, @NonNull String tabletId, @NonNull String sensorId, boolean testMode) {
        mStartTime = startTime;
        mSurveyorName = surveyorName;
        mRouteName = routeName;
        mTabletId = tabletId;
        mSensorId = sensorId;
        mTestMode = testMode;
    }

    /**
     * Creates a non-test-mode RouteState
     */
    public RouteState(@NonNull DateTime startTime, @NonNull String surveyorName, @NonNull String routeName, @NonNull String tabletId, @NonNull String sensorId) {
        this(startTime, surveyorName, routeName, tabletId, sensorId, false);
    }

    /**
     * Returns a new RouteState in test mode with other fields empty
     */
    public static RouteState testMode(@NonNull DateTime startTime) {
        return new RouteState(startTime,"", "", "", "", true);
    }

    @NonNull
    public ReadableDateTime getStartTime() { return mStartTime; }

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

    @NonNull
    public String getSensorId() { return mSensorId; };

    public boolean isTestMode() {
        return mTestMode;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeSerializable(mStartTime);
        parcel.writeString(mSurveyorName);
        parcel.writeString(mRouteName);
        parcel.writeString(mTabletId);
        parcel.writeString(mSensorId);
        parcel.writeByte((byte) (mTestMode ? 1 : 0));
    }

    protected RouteState(Parcel in) {
        mStartTime = (DateTime) Objects.requireNonNull(in.readSerializable());
        mSurveyorName = Objects.requireNonNull(in.readString());
        mRouteName = Objects.requireNonNull(in.readString());
        mTabletId = Objects.requireNonNull(in.readString());
        mSensorId = Objects.requireNonNull(in.readString());
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

    @NonNull
    @Override
    public String toString() {
        return "RouteState{" +
                "mStartTime=" + mStartTime +
                ", mSurveyorName='" + mSurveyorName + '\'' +
                ", mRouteName='" + mRouteName + '\'' +
                ", mTabletId='" + mTabletId + '\'' +
                ", mSensorId='" + mSensorId + '\'' +
                ", mTestMode=" + mTestMode +
                '}';
    }
}
