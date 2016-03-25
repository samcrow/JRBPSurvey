package org.samcrow.ridgesurvey;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;
import android.util.Log;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Stores {@link Observation} objects in local persistent storage
 */
public final class ObservationDatabase {
    private static final String TAG = ObservationDatabase.class.getSimpleName();
    public static final String TABLE_NAME = "observations";
    /*
     * Schema:
     * site: Site ID, INTEGER
     * route: Route name, TEXT
     * time: Time recorded, ISO 8601 date+time format with milliseconds, TEXT
     * species: JSON-formatted map from column name to boolean present, TEXT
     * notes: Notes, TEXT
     */

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
    public ObservationDatabase(@NonNull Context context) {
        Objects.requireNonNull(context);
        mOpenHelper = new ObservationOpenHelper(context);
    }

    /**
     * Inserts an observation into the database
     *
     * @param observation the observation to insert
     * @throws SQLException if an error occurs
     */
    public void insertObservation(@NonNull Observation observation) throws SQLException {
        final ContentValues values = createContentValues(observation);
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        try {
            db.insertOrThrow("observations", null, values);
        } finally {
            db.close();
        }
    }

    private static ContentValues createContentValues(@NonNull Observation observation) {
        final ContentValues values = new ContentValues();
        values.put("site", observation.getSiteId());
        values.put("route", observation.getRouteName());

        final DateTimeFormatter formatter = ISODateTimeFormat.dateTime();
        values.put("time", formatter.print(observation.getTime()));

        // Convert species to JSON
        final JSONObject species = new JSONObject();
        try {
            for (Map.Entry<String, Boolean> entry : observation.getSpecies().entrySet()) {
                species.put(entry.getKey(), entry.getValue().booleanValue());
            }
            values.put("species", species.toString(0));
        } catch (JSONException e) {
            final SQLException e1 = new SQLException("JSON problem");
            e1.initCause(e);
            throw e1;
        }

        values.put("notes", observation.getNotes());
        return values;
    }

    /**
     * Loads and returns one observation from the database
     * <p/>
     * Rows that contain invalid data will be ignored.
     *
     * @return an arbitrarily chosen database, or null if the database is empty
     * @throws SQLException if an error occurs
     */
    public Observation getOneObservation() throws SQLException {
        final SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        try {
            // Select one
            final Cursor result = db.query(TABLE_NAME, null, null, null, null, null, null, "1");
            try {
                if (result.moveToNext()) {
                    return createObservation(result);
                } else {
                    return null;
                }
            } finally {
                result.close();
            }
        } finally {
            db.close();
        }
    }

    /**
     * Loads and returns all observations in the database
     *
     * @return a list of all observations in the database
     * @throws SQLException if an error occurs
     */
    public List<Observation> getObservations() throws SQLException {
        final List<Observation> observations = new ArrayList<>();

        final SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        try {
            final Cursor result = db.query(TABLE_NAME, null, null, null, null, null, null);
            try {
                while (result.moveToNext()) {
                    try {
                        observations.add(createObservation(result));
                    } catch (SQLException e) {
                        Log.w(TAG, "Invalid observation entry", e);
                        // Continue
                    }
                }
            } finally {
                result.close();
            }
        } finally {
            db.close();
        }

        return observations;
    }

    private static Observation createObservation(Cursor result) throws SQLException {
        final int siteIndex = result.getColumnIndexOrThrow("site");
        final int routeIndex = result.getColumnIndexOrThrow("route");
        final int timeIndex = result.getColumnIndexOrThrow("time");
        final int speciesIndex = result.getColumnIndexOrThrow("species");
        final int notesIndex = result.getColumnIndexOrThrow("notes");

        final int site = result.getInt(siteIndex);
        final String route = result.getString(routeIndex);

        final DateTimeFormatter formatter = ISODateTimeFormat.dateTime();
        final String timeString = result.getString(timeIndex);
        DateTime time;
        try {
            time = formatter.parseDateTime(timeString);
        } catch (IllegalArgumentException e) {
            final SQLException e1 = new SQLException("Invalid date/time value: " + timeString);
            e1.initCause(e);
            throw e1;
        }

        final String speciesJson = result.getString(speciesIndex);

        final Map<String, Boolean> speciesPresent = new HashMap<>();
        // Try to parse
        try {
            final JSONObject species = new JSONObject(speciesJson);
            for (Iterator<String> iter = species.keys(); iter.hasNext(); ) {
                final String speciesName = iter.next();
                final boolean present = species.getBoolean(speciesName);
                speciesPresent.put(speciesName, present);
            }
        } catch (JSONException e) {
            final SQLException e1 = new SQLException("Species JSON could not be parsed");
            e1.initCause(e);
            throw e1;
        }

        final String notes = result.getString(notesIndex);

        return new Observation(time, site, route, speciesPresent, notes);
    }

    /**
     * Deletes an observation from the database. Has no effect if the database does not have an
     * observation equal to the provided observation.
     *
     * @param observation the observation to delete
     * @return true if the observation was deleted, otherwise false
     */
    public boolean delete(Observation observation) {
        final ContentValues values = createContentValues(observation);
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        try {
            // Only use the time as a criterion (assume that no two observations were made
            // in the same millisecond)
            // Using all the fields as critera resulted in no rows being deleted.
            final int count = db.delete(TABLE_NAME,
                    "time = ?",
                    new String[]{values.getAsString("time")});
            return count > 0;
        } finally {
            db.close();
        }
    }

    private static class ObservationOpenHelper extends SQLiteOpenHelper {

        private static final String NAME = "observations";

        private static final int VERSION = 1;

        public ObservationOpenHelper(Context context) {
            super(context, NAME, null, VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + TABLE_NAME + " (" +
                    "site INTEGER NOT NULL, " +
                    "route TEXT NOT NULL, " +
                    "time TEXT NOT NULL, " +
                    "species TEXT NOT NULL, " +
                    "notes TEXT NOT NULL)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // Implement when version changes
        }
    }
}
