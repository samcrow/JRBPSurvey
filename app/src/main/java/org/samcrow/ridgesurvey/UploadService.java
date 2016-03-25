package org.samcrow.ridgesurvey;

import android.app.IntentService;
import android.content.Intent;
import android.database.SQLException;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import org.apache.commons.io.IOUtils;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.IOException;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

/**
 * A service that uploads observations to a server
 */
public class UploadService extends IntentService {

    private static final String TAG = UploadService.class.getSimpleName();

    public UploadService() {
        super(UploadService.class.getName());
    }

    /**
     * Performs an action provided in an intent. This method runs on a separate thread from the
     * application.
     * @param intent an intent describing the action to perform. This implementation ignores the
     *               intent.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "Starting");
        final ObservationDatabase db = new ObservationDatabase(this);
        try {
            final Observation observation = db.getOneObservation();
            if (observation != null) {
                upload(observation);
            } else {
                Log.i(TAG, "No observations to upload");
            }
        } catch (SQLException e) {
            Log.e(TAG, "Failed to load an observation", e);
        } catch (MalformedURLException e) {
            Log.e(TAG, "Invalid form URL", e);
        } catch (IOException e) {
            Log.e(TAG, "Upload IO exception", e);
        } catch (SecurityException e) {
            Log.e(TAG, "Do not have permission to upload", e);
        } catch (ParseException e) {
            Log.e(TAG, "Failed to parse page", e);
        }
    }

    /**
     * Uploads an observation
     * @param observation the observation to upload
     */
    private void upload(@NonNull Observation observation) throws IOException, ParseException {
        Objects.requireNonNull(observation);
        final Map<String, String> formData = formatObservation(observation);
        Log.v(TAG, "Formatted observation: " + formData);

        // The URL of the script macro that enters data
        final URL macroUrl = new URL("https://script.google.com/macros/s/AKfycbyQqLKAlEcK9BqSAZ3r6El4T5w7fIs6SwEljTlSENKalYD7NRD7/exec");
        final HttpURLConnection connection = (HttpURLConnection) macroUrl.openConnection();
        try {
            // POST
            connection.setDoOutput(true);
            connection.setChunkedStreamingMode(0);
            final PrintStream out = new PrintStream(connection.getOutputStream());
            writeFormEncodedData(formData, out);
            out.flush();

            final String response = IOUtils.toString(connection.getInputStream());
            Log.v(TAG, response);
        } finally {
            connection.disconnect();
        }
    }

    /**
     * Writes URL-encoded form data to a PrintStream
     * @param data the key-value pairs to write, not URL encoded
     * @param out the stream to write to
     */
    private static void writeFormEncodedData(@NonNull Map<String, String> data, @NonNull PrintStream out) {
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
}
