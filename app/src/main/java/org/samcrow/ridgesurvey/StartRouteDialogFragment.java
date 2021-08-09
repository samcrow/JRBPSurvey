package org.samcrow.ridgesurvey;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.json.JSONException;
import org.samcrow.ridgesurvey.data.RouteState;
import org.samcrow.ridgesurvey.data.StartRouteDatabase;

import java.io.IOException;
import java.util.List;

public class StartRouteDialogFragment extends AppCompatDialogFragment {

    public static final String TAG = "StartRouteDialog";

    private String mSelectedRoute = null;

    private EditText newSurveyorNameField = null;
    private EditText newTabletIdField = null;
    private EditText newSensorIdField = null;
    private TextView newRouteNameDisplay = null;
    private Button startButton = null;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setTitle("Start a route");
        dialog.setContentView(R.layout.start_route_form);

        newSurveyorNameField = dialog.findViewById(R.id.fieldSurveyorName);
        newTabletIdField = dialog.findViewById(R.id.fieldTabletId);
        newSensorIdField = dialog.findViewById(R.id.fieldSensorId);
        newRouteNameDisplay = dialog.findViewById(R.id.labelSelectedRoute);
        startButton = dialog.findViewById(R.id.newRouteFormStartButton);

        final Activity activity = Objects.requireNonNull(getActivity());
        final SharedPreferences prefs = activity.getPreferences(Context.MODE_PRIVATE);

        final String savedTabletId = prefs.getString("tablet_id", null);

        // Restore the tablet ID in the new route form
        if (savedTabletId != null) {
            newTabletIdField.setText(savedTabletId);
        }

        try {
            final String[] routeNames = loadRouteNames();

            final Button selectRouteButton = dialog.findViewById(R.id.buttonSelectRoute);
            selectRouteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    new AlertDialog.Builder(activity)
                            .setTitle("Select route")
                            .setItems(routeNames, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    mSelectedRoute = routeNames[i];
                                    newRouteNameDisplay.setText(mSelectedRoute);
                                }
                            })
                            .show();
                }
            });
        } catch (IOException | JSONException e) {
            throw new RuntimeException("Failed to load routes", e);
        }

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
                    new AlertDialog.Builder(activity).setMessage("Please select a route").show();
                    return;
                }
                // Save settings
                final DateTime now = DateTime.now();
                final SharedPreferences.Editor editor = prefs.edit();
                editor.putString("start_time", ISODateTimeFormat.dateTime().print(now));
                editor.putString("surveyor_name", surveyorName);
                editor.putString("tablet_id", tabletId);
                editor.putString("sensor_id", sensorId);
                editor.putString("route_name", mSelectedRoute);
                editor.apply();

                // Save global tablet ID for use in the upload service
                saveGlobalTabletId(tabletId);

                // Save route start event for upload later
                final RouteState newRoute = new RouteState(now, surveyorName, mSelectedRoute, tabletId, sensorId);

                final StartRouteDatabase db = new StartRouteDatabase(activity);
                db.saveRouteState(newRoute);

                // Launch map activity
                final Intent mapIntent = new Intent(activity, MainActivity.class);
                mapIntent.putExtra(MainActivity.EXTRA_ROUTE_STATE, newRoute);
                startActivity(mapIntent);
            }
        });

        return dialog;
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

    private void saveGlobalTabletId(String tabletId) {
        final SharedPreferences globalPrefs = Objects.requireNonNull(getContext()).getSharedPreferences("tablet_properties", Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = globalPrefs.edit();
        editor.putString("tablet_id", tabletId);
        editor.apply();
    }

    private void showValueRequiredDialog(String fieldName) {
        new AlertDialog.Builder(Objects.requireNonNull(getActivity())).setMessage(String.format("Please fill in the \"%s\" field", fieldName)).show();
    }
}
