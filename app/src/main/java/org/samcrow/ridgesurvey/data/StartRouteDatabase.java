package org.samcrow.ridgesurvey.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.samcrow.ridgesurvey.Objects;

/**
 * Stores route-start events in a database
 */
public class StartRouteDatabase {
    /*
     * Schema:
     * id: event ID, INTEGER PRIMARY KEY
     * time: Time started, ISO 8601 date+time format with milliseconds, TEXT
     * surveyor: Surveyor name, TEXT
     * tablet: Tablet ID, TEXT
     * sensor: Sensor ID, TEXT
     * route: Route name, TEXT
     */

    private static final String TABLE_NAME = "route_start_events";

    /**
     * The open helper used to access the database
     */
    @NonNull
    private final SQLiteOpenHelper mOpenHelper;

    /**
     * Creates a database accessor
     *
     * @param context a non-null context to use
     */
    public StartRouteDatabase(@NonNull Context context) {
        Objects.requireNonNull(context);
        mOpenHelper = new StartRouteOpenHelper(context);
    }

    public void saveRouteState(@NonNull RouteState routeState) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        db.insert(TABLE_NAME, null, createContentValues(routeState));
    }

    /**
     * @return the oldest route state in the database, or null if the database is empty
     */
    public IdentifiedRouteState getOldestRouteState() {
        final SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        final Cursor cursor = db.query(TABLE_NAME, null, null, null, null, null, "time ASC", "1");
        try {
            if (cursor.moveToNext()) {
                return new IdentifiedRouteState(cursor);
            } else {
                // No rows in the table
                return null;
            }
        } finally {
            cursor.close();
        }
    }

    public void deleteRouteState(int id) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        db.delete(TABLE_NAME, "ID = ?", new String[]{Integer.toString(id)});
    }

    private static ContentValues createContentValues(@NonNull RouteState routeState) {
        final ContentValues values = new ContentValues();

        final DateTimeFormatter formatter = ISODateTimeFormat.dateTime();
        values.put("time", formatter.print(routeState.getStartTime()));

        values.put("surveyor", routeState.getSurveyorName());
        values.put("tablet", routeState.getTabletId());
        values.put("sensor", routeState.getSensorId());
        values.put("route", routeState.getRouteName());

        return values;
    }

    public static class IdentifiedRouteState {
        @NonNull
        public final RouteState mRouteState;
        public final int mId;

        private IdentifiedRouteState(RouteState routeState, int id) {
            mRouteState = routeState;
            this.mId = id;
        }

        private IdentifiedRouteState(Cursor cursor) {
            final DateTimeFormatter formatter = ISODateTimeFormat.dateTime();

            final int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
            final DateTime time = formatter.parseDateTime(cursor.getString(cursor.getColumnIndexOrThrow("time")));
            final String surveyorName = cursor.getString(cursor.getColumnIndexOrThrow("surveyor"));
            final String tabletId = cursor.getString(cursor.getColumnIndexOrThrow("tablet"));
            final String sensorId = cursor.getString(cursor.getColumnIndexOrThrow("sensor"));
            final String routeName = cursor.getString(cursor.getColumnIndexOrThrow("route"));

            mRouteState = new RouteState(time, surveyorName, routeName, tabletId, sensorId);
            mId = id;
        }

        @Override
        public String toString() {
            return "IdentifiedRouteState{" +
                    "mRouteState=" + mRouteState +
                    ", mId=" + mId +
                    '}';
        }
    }

    private static class StartRouteOpenHelper extends SQLiteOpenHelper {

        private static final int VERSION = 1;

        public StartRouteOpenHelper(Context context) {
            super(context, TABLE_NAME, null, VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + TABLE_NAME + "(" +
                    "id INTEGER NOT NULL PRIMARY KEY, " +
                    "time TEXT NOT NULL," +
                    "surveyor TEXT NOT NULL," +
                    "tablet TEXT NOT NULL," +
                    "sensor TEXT NOT NULL," +
                    "route TEXT NOT NULL)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            throw new RuntimeException("Only one version exists");
        }
    }
}
