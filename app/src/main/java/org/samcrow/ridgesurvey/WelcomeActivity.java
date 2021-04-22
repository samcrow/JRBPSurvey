package org.samcrow.ridgesurvey;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;
import org.samcrow.ridgesurvey.data.RouteState;

import java.io.IOException;
import java.util.List;

public class WelcomeActivity extends AppCompatActivity {

    private String mSelectedRoute = null;

    private EditText newSurveyorNameField = null;
    private EditText newTabletIdField = null;
    private EditText newSensorIdField = null;
    private TextView newRouteNameDisplay = null;
    private TextView resumeRouteDescription = null;
    private Button startButton = null;
    private Button resumeButton = null;
    private Button testModeButton = null;

    private RouteState mResumableRoute = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_welcome);
        // Find UI elements
        newSurveyorNameField = findViewById(R.id.fieldSurveyorName);
        newTabletIdField = findViewById(R.id.fieldTabletId);
        newSensorIdField = findViewById(R.id.fieldSensorId);
        newRouteNameDisplay = findViewById(R.id.labelSelectedRoute);
        resumeRouteDescription = findViewById(R.id.routeInformationView);
        startButton = findViewById(R.id.buttonStartNew);
        resumeButton = findViewById(R.id.buttonResume);
        testModeButton = findViewById(R.id.buttonTestMode);

        try {
            final String[] routeNames = loadRouteNames();

            final Button selectRouteButton = findViewById(R.id.buttonSelectRoute);
            selectRouteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(WelcomeActivity.this);
                    builder.setTitle("Select route");
                    builder.setItems(routeNames, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            mSelectedRoute = routeNames[i];
                            newRouteNameDisplay.setText(mSelectedRoute);
                        }
                    });
                    builder.show();
                }
            });
        } catch (IOException | JSONException e) {
            throw new RuntimeException("Failed to load routes", e);
        }

        final SharedPreferences prefs = getPreferences(MODE_PRIVATE);

        final String savedTabletId = prefs.getString("tablet_id", null);

        // Restore the tablet ID in the new route form
        if (savedTabletId != null) {
            newTabletIdField.setText(savedTabletId);
        }

        // Look for a previous active route
        initResumableRoute();

        // Main button click handlers
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Validate fields
                final String surveyorName = newSurveyorNameField.getText().toString();
                final String tabletId = newTabletIdField.getText().toString();
                final String sensorId = newSensorIdField.getText().toString();

                if (surveyorName.isEmpty()) {
                    showValueRequiredDialog("your name");
                    return;
                }
                if (tabletId.isEmpty()) {
                    showValueRequiredDialog("tablet ID");
                    return;
                }
                if (sensorId.isEmpty()) {
                    showValueRequiredDialog("sensor ID");
                    return;
                }
                if (mSelectedRoute == null) {
                    new AlertDialog.Builder(WelcomeActivity.this).setMessage("Please select a route").show();
                    return;
                }
                // Save settings
                final SharedPreferences.Editor editor = prefs.edit();
                editor.putString("surveyor_name", surveyorName);
                editor.putString("tablet_id", tabletId);
                editor.putString("sensor_id", sensorId);
                editor.putString("route_name", mSelectedRoute);
                editor.apply();
                // TODO: Save and upload route start event
                // Launch map activity
                final RouteState newRoute = new RouteState(surveyorName, mSelectedRoute, tabletId);
                final Intent mapIntent = new Intent(WelcomeActivity.this, MainActivity.class);
                mapIntent.putExtra(MainActivity.EXTRA_ROUTE_STATE, newRoute);
                startActivity(mapIntent);
            }
        });

        resumeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Intent mapIntent = new Intent(WelcomeActivity.this, MainActivity.class);
                mapIntent.putExtra(MainActivity.EXTRA_ROUTE_STATE, mResumableRoute);
                startActivity(mapIntent);
            }
        });

        testModeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final RouteState testMode = RouteState.testMode();
                final Intent mapIntent = new Intent(WelcomeActivity.this, MainActivity.class);
                mapIntent.putExtra(MainActivity.EXTRA_ROUTE_STATE, testMode);
                startActivity(mapIntent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        initResumableRoute();
    }

    private void showValueRequiredDialog(String fieldName) {
        new AlertDialog.Builder(this).setMessage(String.format("Please fill in the \"%s\" field", fieldName)).show();
    }

    private void initResumableRoute() {
        final SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        final String savedTabletId = prefs.getString("tablet_id", null);
        final String savedSurveyorName = prefs.getString("surveyor_name", null);
        final String savedRouteName = prefs.getString("route_name", null);
        final String savedSensorId = prefs.getString("sensor_id", null);
        if (savedTabletId != null && savedSurveyorName != null && savedRouteName != null && savedSensorId != null) {
            mResumableRoute = new RouteState(savedSurveyorName, savedRouteName, savedTabletId);
            resumeRouteDescription.setText(String.format("%s surveying %s", savedSurveyorName, savedRouteName));
            resumeButton.setEnabled(true);
        } else {
            resumeRouteDescription.setText("No previous route");
            resumeButton.setEnabled(false);
        }
    }

    @NonNull
    private String[] loadRouteNames() throws IOException, JSONException {
        final List<Route> routes = SiteStorage.readRoutes(
                getResources().openRawResource(R.raw.sites));
        final String[] routeNames = new String[routes.size()];
        for (int i = 0; i < routes.size(); i++) {
            routeNames[i] = routes.get(i).getName();
        }
        return routeNames;
    }
}

