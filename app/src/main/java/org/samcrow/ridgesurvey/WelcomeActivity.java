package org.samcrow.ridgesurvey;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.samcrow.ridgesurvey.data.RouteState;

public class WelcomeActivity extends FragmentActivity {

    private TextView resumeRouteDescription = null;
    private Button startButton = null;
    private Button resumeButton = null;
    private Button testModeButton = null;

    private RouteState mResumableRoute = null;
    private StartRouteDialogFragment mStartRouteDialog = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_welcome);
        // Find UI elements
        startButton = findViewById(R.id.buttonStartNew);
        resumeButton = findViewById(R.id.buttonResume);
        testModeButton = findViewById(R.id.buttonTestMode);
        resumeRouteDescription = findViewById(R.id.routeInformationView);



        // Look for a previous active route
        initResumableRoute();

        // Main button click handlers
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mStartRouteDialog = new StartRouteDialogFragment();
                mStartRouteDialog.show(getSupportFragmentManager(), StartRouteDialogFragment.TAG);
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
                final RouteState testMode = RouteState.testMode(DateTime.now());
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
        // If the start route dialog is still visible from earlier, hide it
        if (mStartRouteDialog != null) {
            mStartRouteDialog.dismiss();
            mStartRouteDialog = null;
        }
    }

    private void initResumableRoute() {
        final SharedPreferences prefs = getPreferences(MODE_PRIVATE);

        DateTime savedStartTime = null;
        final String savedStartTimeString = prefs.getString("start_time", null);
        if (savedStartTimeString != null) {
            try {
                savedStartTime = ISODateTimeFormat.dateTime().parseDateTime(savedStartTimeString);
            } catch (IllegalArgumentException e) { /* Time formatted incorrectly */ }
        }

        final String savedTabletId = prefs.getString("tablet_id", null);
        final String savedSurveyorName = prefs.getString("surveyor_name", null);
        final String savedRouteName = prefs.getString("route_name", null);
        final String savedSensorId = prefs.getString("sensor_id", null);
        if (savedStartTime != null && savedTabletId != null && savedSurveyorName != null && savedRouteName != null && savedSensorId != null) {
            mResumableRoute = new RouteState(savedStartTime, savedSurveyorName, savedRouteName, savedTabletId, savedSensorId);
            if (!mResumableRoute.isExpired()) {
                resumeRouteDescription.setText(String.format("%s surveying %s", savedSurveyorName, savedRouteName));
                resumeButton.setEnabled(true);
            } else {
                resumeRouteDescription.setText(R.string.no_previous_route);
                resumeButton.setEnabled(false);
            }
        } else {
            resumeRouteDescription.setText(R.string.no_previous_route);
            resumeButton.setEnabled(false);
        }
    }
}

