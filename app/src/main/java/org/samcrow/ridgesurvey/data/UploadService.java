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

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.SQLException;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.room.Room;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import android.util.Log;

import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.format.ISODateTimeFormat;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A service that uploads observations to a server and deletes observations that have been
 * uploaded
 */
public class UploadService extends IntentService {

    /**
     * An extra key for forcing uploads
     * <p>
     * If this service is started with an intent that has a boolean extra with this key and a value
     * of true, the service will upload observations that are newer than {@link #UPLOAD_AGE}.
     */
    public static final String EXTRA_FORCE_UPLOAD = UploadService.class.getName() + ".EXTRA_FORCE_UPLOAD";
    private static final String TAG = UploadService.class.getSimpleName();
    /**
     * The URL to upload to
     */
    private static final URL UPLOAD_URL;
    /**
     * The minimum age of an observation before it should be uploaded
     */
    private static final Duration UPLOAD_AGE = Duration.standardMinutes(10);
    /**
     * The minimum age of an uploaded observation before it is deleted
     */
    private static final Duration DELETE_AGE = Duration.standardDays(2);

    static {
        try {
            UPLOAD_URL = new URL("https://script.google.com/macros/s/AKfycbzZ-Q1KzInEPV5MC3B61EnZvYiTZ2kofVI3ymtnyTftAb4zHNx76PmGfIwsZup0bD5j/exec");
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid URL in source", e);
        }
    }

    public UploadService() {
        super(UploadService.class.getName());
    }

    /**
     * Writes URL-encoded form data to a PrintStream
     *
     * @param data the key-value pairs to write, not URL encoded
     * @param out  the stream to write to
     */
    private static void writeFormEncodedData(@NonNull Map<String, String> data,
                                             @NonNull PrintStream out) {
        Objects.requireNonNull(data);
        Objects.requireNonNull(out);
        int i = 0;
        for (Map.Entry<String, String> entry : data.entrySet()) {
            out.print(Uri.encode(entry.getKey()));
            out.print("=");
            out.print(Uri.encode(entry.getValue()));
            // Add the separator after each entry except the last
            if (i < data.size() - 1) {
                out.print("&");
            }
            i++;
        }
    }

    /**
     * Converts an Observation into a set of key-value pairs suitable for uploading
     *
     * @param observation the observation
     * @return a form-compatible representation of the observation
     */
    private static Map<String, String> formatObservation(@NonNull Observation observation) {
        Objects.requireNonNull(observation);
        final Map<String, String> map = new HashMap<>();

        map.put("Time", ISODateTimeFormat.dateTime().print(observation.getTime()));
        map.put("Event", "Observation");
        map.put("Test mode", observation.isTest() ? "1" : "0");
        map.put("Observed", observation.isObserved() ? "1" : "0");
        map.put("ROUTE", observation.getRouteName());
        map.put("SURVEY LOCATION", Integer.toString(observation.getSiteId()));

        // Species (each species key is already a column name)
        for (Map.Entry<String, Boolean> species : observation.getSpecies().entrySet()) {
            final Boolean present = species.getValue();
            if (present != null) {
                final String stringValue = present ? "1" : "0";
                map.put(species.getKey(), stringValue);
            }
        }

        // Notes
        map.put("NOTES", observation.getNotes());

        return map;
    }

    private static Map<String, String> formatSimpleTimedEvent(@NonNull SimpleTimedEvent event) {
        Objects.requireNonNull(event);
        final Map<String, String> map = new HashMap<>();

        map.put("Time", ISODateTimeFormat.dateTime().print(event.getTime()));
        map.put("Event", event.getName());
        map.put("ROUTE", event.getRoute());

        return map;
    }

    /**
     * Determines if this observation should be uploaded
     *
     * @param observation the observation
     * @return if the observation should be uploaded
     */
    public static boolean needsUpload(Observation observation) {
        final DateTime uploadThreshold = DateTime.now().minus(UPLOAD_AGE);
        return !observation.isUploaded() && observation.getTime().isBefore(uploadThreshold);
    }

    /**
     * Performs an action provided in an intent. This method runs on a separate thread from the
     * application.
     *
     * @param intent an intent describing the action to perform. This implementation ignores the
     *               intent.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        LocalBroadcastManager.getInstance(this)
                .sendBroadcast(new Intent(UploadStatusTracker.ACTION_UPLOAD_STARTED));

        final boolean ignoreAge = intent.getBooleanExtra(EXTRA_FORCE_UPLOAD, false);

        final ObservationDatabase db = new ObservationDatabase(this);
        final StartRouteDatabase startDb = new StartRouteDatabase(this);
        final Database steDatabase = Room.databaseBuilder(this, Database.class, "events")
                .addMigrations(new Migration(1, 2) {
                    @Override
                    public void migrate(@NonNull SupportSQLiteDatabase database) {
                        database.execSQL("ALTER TABLE SimpleTimedEvent ADD COLUMN `route` TEXT NOT NULL DEFAULT ''");
                    }
                })
                .build();
        try {
            // Part 1: Route start events
            while (true) {
                final StartRouteDatabase.IdentifiedRouteState routeState = startDb.getOldestRouteState();
                if (routeState == null) {
                    break;
                }
                Log.d(TAG, "Trying to upload route start " + routeState);
                uploadStartRoute(UPLOAD_URL, routeState.mRouteState);
                startDb.deleteRouteState(routeState.mId);
            }

            // Part 2: Simple timed events
            while (true) {
                final SimpleTimedEventDao dao = steDatabase.simpleTimedEventDao();
                final SimpleTimedEvent event = dao.getOldest();
                if (event == null) {
                    break;
                }
                Log.d(TAG, "Trying to upload simple timed event " + event);
                uploadSimpleTimedEvent(UPLOAD_URL, event);
                dao.delete(event);
            }

            // Part 3: Observations
            final List<IdentifiedObservation> observations = db.getObservationsByTime();
            final DateTime deleteThreshold = DateTime.now().minus(DELETE_AGE);

            for (IdentifiedObservation observation : observations) {
                Log.d(TAG, "Loaded observation " + observation.getId());
                // Check for upload
                if (needsUpload(observation) || (ignoreAge && !observation.isUploaded())) {
                    Log.d(TAG, "Trying to upload...");
                    uploadObservation(UPLOAD_URL, observation);
                    // Make a copy marked as uploaded
                    final IdentifiedObservation uploaded = new IdentifiedObservation(
                            observation.getTime(), true, observation.getSiteId(),
                            observation.getRouteName(), observation.getSpecies(),
                            observation.getNotes(), observation.getId(), observation.isObserved(), observation.isTest());
                    db.updateObservation(uploaded);
                } else if (!observation.isUploaded()) {
                    Log.d(TAG, "Not uploading observation " + observation.getId() + " because it is not old enough");
                }
                // Check for delete
                if (observation.isUploaded() && observation.getTime().isBefore(deleteThreshold)) {
                    Log.d(TAG, "Deleting observation");
                    db.delete(observation);
                }
            }

            LocalBroadcastManager.getInstance(this)
                    .sendBroadcast(new Intent(UploadStatusTracker.ACTION_UPLOAD_SUCCESS));
        } catch (SQLException e) {
            Log.e(TAG, "Failed to load an observation", e);
            LocalBroadcastManager.getInstance(this)
                    .sendBroadcast(new Intent(UploadStatusTracker.ACTION_UPLOAD_FAILED));
        } catch (MalformedURLException e) {
            Log.e(TAG, "Invalid form URL", e);
            LocalBroadcastManager.getInstance(this)
                    .sendBroadcast(new Intent(UploadStatusTracker.ACTION_UPLOAD_FAILED));
        } catch (IOException e) {
            Log.e(TAG, "Upload IO exception", e);
            LocalBroadcastManager.getInstance(this)
                    .sendBroadcast(new Intent(UploadStatusTracker.ACTION_UPLOAD_FAILED));
        } catch (SecurityException e) {
            Log.e(TAG, "Do not have permission to upload", e);
            LocalBroadcastManager.getInstance(this)
                    .sendBroadcast(new Intent(UploadStatusTracker.ACTION_UPLOAD_FAILED));
        } catch (ParseException e) {
            Log.e(TAG, "Failed to parse page", e);
            LocalBroadcastManager.getInstance(this)
                    .sendBroadcast(new Intent(UploadStatusTracker.ACTION_UPLOAD_FAILED));
        } catch (UploadException e) {
            Log.e(TAG, "Upload server error", e);
            LocalBroadcastManager.getInstance(this)
                    .sendBroadcast(new Intent(UploadStatusTracker.ACTION_UPLOAD_FAILED));
        } catch (Exception e) {
            Log.e(TAG, "Unknown upload error", e);
            LocalBroadcastManager.getInstance(this)
                    .sendBroadcast(new Intent(UploadStatusTracker.ACTION_UPLOAD_FAILED));
        } finally {
            steDatabase.close();
        }
    }

    private void uploadGeneric(@NonNull URL url, @NonNull Map<String, String> formData) throws IOException, ParseException, UploadException {
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            // Disable response compression, which might be causing problems
            connection.setRequestProperty("Accept-Encoding", "identity");
            // POST
            connection.setUseCaches(false);
            connection.setDoOutput(true);
            connection.setChunkedStreamingMode(0);
            final PrintStream out = new PrintStream(connection.getOutputStream());
            Log.v(TAG, "Upload form data: " + formData);
            writeFormEncodedData(formData, out);
            out.flush();

            final String response = IOUtils.toString(connection.getInputStream());
            Log.v(TAG, response);


            // Check status
            final int status = connection.getResponseCode();
            if (status == 200) {
                // Check for valid JSON
                final JSONObject json = new JSONObject(response);

                final String result = json.optString("result", "");
                if (!result.equals("success")) {
                    final String message = json.optString("message", null);
                    if (message != null) {
                        throw new UploadException(message);
                    } else {
                        throw new UploadException("Unknown server error");
                    }
                }
            } else if (status == 301 || status == 302) {
                // Handle redirect and add cookies
                final String location = connection.getHeaderField("Location");
                if (location != null) {
                    final URL redirectUrl = new URL(location);
                    Log.i(TAG, "Following redirect to " + redirectUrl);
                    uploadGeneric(redirectUrl, formData);
                } else {
                    throw new UploadException("Got a 301 or 302 response with no Location header");
                }
            } else {
                throw new UploadException("Unexpected HTTP status " + status);
            }

        } catch (JSONException e) {
            final ParseException e1 = new ParseException("Failed to parse response JSON", 0);
            e1.initCause(e);
            throw e1;
        } finally {
            connection.disconnect();
        }
    }

    /**
     * Uploads an observation
     *
     * @param observation the observation to upload
     */
    private void uploadObservation(@NonNull URL url, @NonNull Observation observation)
            throws IOException, ParseException, UploadException {
        Objects.requireNonNull(observation);
        final Map<String, String> formData = formatObservation(observation);

        // Add the tablet ID, if it was set up
        final SharedPreferences prefs = getSharedPreferences("tablet_properties", MODE_PRIVATE);
        final String tabletId = prefs.getString("tablet_id", null);
        if (tabletId != null) {
            formData.put("Tablet ID", tabletId);
        }

        Log.v(TAG, "Formatted observation: " + formData);
        uploadGeneric(url, formData);
    }

    private void uploadStartRoute(@NonNull URL url, @NonNull RouteState routeState) throws IOException, ParseException, UploadException {
        final Map<String, String> map = new HashMap<>(4);
        map.put("Time", ISODateTimeFormat.dateTime().print(routeState.getStartTime()));
        map.put("Event", "Route start");
        map.put("SURVEYOR", routeState.getSurveyorName());
        map.put("Tablet ID", routeState.getTabletId());
        map.put("Sensor ID", routeState.getSensorId());
        map.put("ROUTE", routeState.getRouteName());

        uploadGeneric(url, map);
    }

    private void uploadSimpleTimedEvent(@NonNull URL url, @NonNull SimpleTimedEvent event) throws ParseException, UploadException, IOException {
        final Map<String, String> formData = formatSimpleTimedEvent(event);
        // Add the tablet ID, if it was set up
        final SharedPreferences prefs = getSharedPreferences("tablet_properties", MODE_PRIVATE);
        final String tabletId = prefs.getString("tablet_id", null);
        if (tabletId != null) {
            formData.put("Tablet ID", tabletId);
        }
        formData.put("ROUTE", event.getRoute());
        uploadGeneric(url, formData);
    }

    private static class UploadException extends Exception {
        public UploadException() {
        }

        UploadException(String detailMessage) {
            super(detailMessage);
        }

        public UploadException(String detailMessage, Throwable throwable) {
            super(detailMessage, throwable);
        }

        public UploadException(Throwable throwable) {
            super(throwable);
        }
    }
}
