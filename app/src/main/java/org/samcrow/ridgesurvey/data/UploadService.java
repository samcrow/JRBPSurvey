/*
 * Copyright 2016 Sam Crow
 *
 * This file is part of JRBP Survey.
 *
 * JRBP Survey is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JRBP Survey is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JRBP Survey.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.samcrow.ridgesurvey.data;

import android.app.IntentService;
import android.content.Intent;
import android.database.SQLException;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONException;
import org.json.JSONObject;
import org.samcrow.ridgesurvey.Objects;

import java.io.IOException;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A service that uploads observations to a server and deletes observations that have been
 * uploaded
 */
public class UploadService extends IntentService {

    private static final String TAG = UploadService.class.getSimpleName();

    /**
     * The minimum age of an observation before it should be uploaded
     */
    private static final Duration UPLOAD_AGE = Duration.standardMinutes(30);

    /**
     * The minimum age of an uploaded observation before it is deleted
     */
    private static final Duration DELETE_AGE = Duration.standardDays(2);

    public UploadService() {
        super(UploadService.class.getName());
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
        final ObservationDatabase db = new ObservationDatabase(this);
        try {
            final List<IdentifiedObservation> observations = db.getObservations();
            final DateTime uploadThreshold = DateTime.now().minus(UPLOAD_AGE);
            final DateTime deleteThreshold = DateTime.now().minus(DELETE_AGE);

            for (IdentifiedObservation observation : observations) {
                // Check for upload
                if (!observation.isUploaded() && observation.getTime().isBefore(uploadThreshold)) {
                    upload(observation);
                    // Make a copy marked as uploaded
                    final IdentifiedObservation uploaded = new IdentifiedObservation(
                            observation.getTime(), true, observation.getSiteId(),
                            observation.getRouteName(), observation.getSpecies(),
                            observation.getNotes(), observation.getId());
                    db.updateObservation(uploaded);
                }
                // Check for delete
                if (observation.isUploaded() && observation.getTime().isBefore(deleteThreshold)) {
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
        }
    }

    /**
     * Uploads an observation
     *
     * @param observation the observation to upload
     */
    private void upload(@NonNull Observation observation)
            throws IOException, ParseException, UploadException {
        Objects.requireNonNull(observation);
        final Map<String, String> formData = formatObservation(observation);
        Log.v(TAG, "Formatted observation: " + formData);

        // The URL of the script macro that enters data
        final URL macroUrl = new URL(
                "https://script.google.com/macros/s/AKfycbx94BFDIWE5w9cVsAFwHH9T7282QRwjiBkmQ3lGergJkEqOQ6k/exec");
        final HttpURLConnection connection = (HttpURLConnection) macroUrl.openConnection();
        try {
            // POST
            connection.setUseCaches(false);
            connection.setDoOutput(true);
            connection.setChunkedStreamingMode(0);
            final PrintStream out = new PrintStream(connection.getOutputStream());
            writeFormEncodedData(formData, out);
            out.flush();

            final String response = IOUtils.toString(connection.getInputStream());
            Log.v(TAG, response);
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

        } catch (JSONException e) {
            final ParseException e1 = new ParseException("Failed to parse response JSON", 0);
            e1.initCause(e);
            throw e1;
        } finally {
            connection.disconnect();
        }
    }

    /**
     * Writes URL-encoded form data to a PrintStream
     *
     * @param data the key-value pairs to write, not URL encoded
     * @param out  the stream to write to
     */
    private static void writeFormEncodedData(@NonNull Map<String, String> data,
                                             @NonNull PrintStream out) {
        Objects.requireAllNonNull(data, out);
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

        map.put("SURVEY LOCATION", Integer.toString(observation.getSiteId()));
        map.put("ROUTE", observation.getRouteName());

        // Date format: month/day/year h:m:s
        final DateTimeFormatter formatter = DateTimeFormat.forPattern("MM/dd/YYYY HH:mm:ss");
        map.put("DATE", formatter.print(observation.getTime()));
        // Year field
        map.put("YEAR", Integer.toString(observation.getTime().getYear()));
        // Datestamp field: year, month, day concatenated
        final DateTimeFormatter datestampFormatter = DateTimeFormat.forPattern("YYYYMMdd");
        map.put("DATESTAMP", datestampFormatter.print(observation.getTime()));
        // Time field: hour:minute
        final DateTimeFormatter timeFormatter = DateTimeFormat.forPattern("HH:mm");
        map.put("TIME", timeFormatter.print(observation.getTime()));

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

    public static class UploadException extends Exception {
        public UploadException() {
        }

        public UploadException(String detailMessage) {
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
