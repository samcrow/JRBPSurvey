/*
 * Copyright (c) 2025 Sam Crow
 *
 * This file is part of JRBPSurvey.
 *
 * JRBPSurvey is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * JRBPSurvey is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Foobar.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package org.samcrow.ridgesurvey.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.json.JSONException;
import org.json.JSONObject;
import org.samcrow.ridgesurvey.Objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;

/**
 * Stores {@link Observation} objects in local persistent storage
 */
public final class ObservationDatabase {
    public static final String TABLE_NAME = "observations";
    private static final String TAG = ObservationDatabase.class.getSimpleName();
    /*
     * Schema:
     * id: Observation ID, INTEGER PRIMARY KEY
     * uploaded: 1/0 uploaded yet or not, INTEGER
     * site: Site ID, INTEGER
     * route: Route name, TEXT
     * time: Time recorded, ISO 8601 date+time format with milliseconds, TEXT
     * species: JSON-formatted map from column name to boolean present, TEXT
     * notes: Notes, TEXT
     * test_mode: 1/0 recorded in test mode or not, INTEGER
     * observed: 1/0 observed or not, INTEGER
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

    private static ContentValues createContentValues(@NonNull Observation observation) {
        final ContentValues values = new ContentValues();
        values.put("uploaded", observation.isUploaded() ? 1 : 0);
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
            //noinspection UnnecessaryInitCause (the cause constructor requires API 16)
            e1.initCause(e);
            throw e1;
        }

        values.put("notes", observation.getNotes());
        values.put("test_mode", observation.isTest());
        values.put("observed", observation.isObserved());

        return values;
    }

    private static IdentifiedObservation createObservation(Cursor result) throws SQLException {
        final int idIndex = result.getColumnIndexOrThrow("id");
        final int uploadedIndex = result.getColumnIndexOrThrow("uploaded");
        final int siteIndex = result.getColumnIndexOrThrow("site");
        final int routeIndex = result.getColumnIndexOrThrow("route");
        final int timeIndex = result.getColumnIndexOrThrow("time");
        final int speciesIndex = result.getColumnIndexOrThrow("species");
        final int notesIndex = result.getColumnIndexOrThrow("notes");
        final int testModeIndex = result.getColumnIndexOrThrow("test_mode");
        final int observedIndex = result.getColumnIndexOrThrow("observed");

        final int id = result.getInt(idIndex);
        final boolean uploaded = result.getInt(uploadedIndex) == 1;

        final int site = result.getInt(siteIndex);
        final String route = result.getString(routeIndex);

        final DateTimeFormatter formatter = ISODateTimeFormat.dateTime();
        final String timeString = result.getString(timeIndex);
        DateTime time;
        try {
            time = formatter.parseDateTime(timeString);
        } catch (IllegalArgumentException e) {
            final SQLException e1 = new SQLException("Invalid date/time value: " + timeString);
            //noinspection UnnecessaryInitCause
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
            final SQLException e1 = new SQLException("Species JSON could not be parsed", e);
            throw e1;
        }

        final String notes = result.getString(notesIndex);

        final boolean testMode = result.getInt(testModeIndex) != 0;
        final boolean observed = result.getInt(observedIndex) != 0;

        return new IdentifiedObservation(time, uploaded, site, route, speciesPresent, notes, id, observed, testMode);
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

    /**
     * Updates an observation in the database
     *
     * @param observation the observation
     * @throws SQLException if an error occurs
     */
    public void updateObservation(@NonNull IdentifiedObservation observation) throws SQLException {
        final ContentValues values = createContentValues(observation);
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        try {
            db.update(TABLE_NAME, values, "id = ?",
                    new String[]{Integer.toString(observation.getId())});
        } finally {
            db.close();
        }
    }

    /**
     * Loads and returns one observation from the database
     * <p/>
     * Rows that contain invalid data will be ignored.
     *
     * @return an arbitrarily chosen database, or null if the database is empty
     * @throws SQLException if an error occurs
     */
    public IdentifiedObservation getOneObservation() throws SQLException {
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
     * Gets an observation for the site with the provided site ID
     * <p>
     * If more than one observation exists, the most recent one is returned.
     *
     * @param siteId the site ID to find an observation for
     * @return the most recent observation for the requested site, or null if none exists
     * @throws SQLException if an error occurs
     */
    public IdentifiedObservation getObservationForSite(int siteId) throws SQLException {
        final SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        try {
            final Cursor result = db.query(TABLE_NAME, null, "site = ?", new String[]{Integer.toString(siteId)}, null, null, "time DESC");
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
     * @return a list of all observations in the database, ordered by time decreasing
     * (newest first)
     * @throws SQLException if an error occurs
     */
    public List<IdentifiedObservation> getObservationsByTime() throws SQLException {
        final List<IdentifiedObservation> observations = new ArrayList<>();

        final SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        try {
            final Cursor result = db.query(TABLE_NAME, null, null, null, null, null, "time DESC");
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

    /**
     * Deletes an observation from the database. Has no effect if the database does not have an
     * observation equal to the provided observation.
     *
     * @param observation the observation to delete
     * @return true if the observation was deleted, otherwise false
     */
    public boolean delete(IdentifiedObservation observation) {
        final ContentValues values = createContentValues(observation);
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        try {
            final int count = db.delete(TABLE_NAME,
                    "id = ?",
                    new String[]{Integer.toString(observation.getId())});
            return count > 0;
        } finally {
            db.close();
        }
    }

    private static class ObservationOpenHelper extends SQLiteOpenHelper {

        private static final String NAME = "observations";

        private static final int VERSION = 4;

        ObservationOpenHelper(Context context) {
            super(context, NAME, null, VERSION);
        }

        /**
         * Returns the create table syntax for the table with the specified name
         *
         * @param tableName the name of the table to create
         * @return SQL to create the table
         */
        private static String createSyntax(String tableName) {
            return "CREATE TABLE " + tableName + " (" +
                    "id INTEGER NOT NULL PRIMARY KEY, " +
                    "uploaded INTEGER NOT NULL DEFAULT 0 CHECK (uploaded = 0 OR uploaded = 1), " +
                    "site INTEGER NOT NULL, " +
                    "route TEXT NOT NULL, " +
                    "time TEXT NOT NULL, " +
                    "species TEXT NOT NULL, " +
                    "notes TEXT NOT NULL," +
                    "test_mode INTEGER NOT NULL DEFAULT 0 CHECK (test_mode = 0 OR test_mode = 1)," +
                    "observed INTEGER NOT NULL DEFAULT 0 CHECK (observed = 0 OR observed = 1) )";
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(createSyntax(TABLE_NAME));
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion == 1 && newVersion == 2) {
                // Add an ID column
                // Create a new table
                final String copyTable = TABLE_NAME + "_temp";
                db.execSQL(createSyntax(copyTable));
                // Copy everything into the new table
                // IDs will be assigned automatically
                db.execSQL("INSERT INTO " + copyTable + " (site, route, time, species, notes)" +
                        " SELECT site, route, time, species, notes FROM " + TABLE_NAME);
                // Delete the old table
                db.execSQL("DROP TABLE " + TABLE_NAME);
                db.execSQL("ALTER TABLE " + copyTable + " RENAME TO " + TABLE_NAME);
            } else if (oldVersion == 2 && newVersion == 3) {
                db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " +
                        "test_mode INTEGER NOT NULL DEFAULT 0 CHECK (test_mode = 0 OR test_mode = 1)");
            } else if (oldVersion == 3 && newVersion == 4) {
                db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " +
                        "observed INTEGER NOT NULL DEFAULT 0 CHECK (observed = 0 OR observed = 1)");
            } else {
                throw new RuntimeException("Unsupported combination of database versions");
            }
        }
    }
}
