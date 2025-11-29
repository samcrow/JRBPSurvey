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

package org.samcrow.ridgesurvey;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.database.SQLException;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.widget.Toast;

import org.jetbrains.annotations.Nullable;
import org.joda.time.DateTime;
import org.samcrow.ridgesurvey.data.Observation;
import org.samcrow.ridgesurvey.data.ObservationDatabase;
import org.samcrow.ridgesurvey.data.RouteState;
import org.samcrow.ridgesurvey.data.UploadService;
import org.samcrow.ridgesurvey.data.UploadStatusTracker;

import java.util.HashMap;
import java.util.Map;

import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

/**
 * An activity that allows the user to enter information
 * <p/>
 * This activity must be started with an extra with key {@link #ARG_SITE} containing the site
 * to record data at and an extra with key {@link #ARG_ROUTE} containing the name of the route
 * that contains the site.
 *
 * If the user saves the observation, this activity exists with result {@link #RESULT_OK}. Otherwise,
 * it exits with result {@link #RESULT_CANCELED}.
 */
public class DataEntryActivity extends ObservationActivity {

    private static final String TAG = DataEntryActivity.class.getSimpleName();

    /**
     * The argument key used to provide a site
     */
    private static final String ARG_SITE = DataEntryActivity.class.getName() + ".ARG_SITE";
    /**
     * The argument key used to provide the name of the route that the site is on
     */
    private static final String ARG_ROUTE = DataEntryActivity.class.getName() + ".ARG_ROUTE";
    private static final String ARG_ROUTE_STATE = DataEntryActivity.class.getName() + ".ARG_ROUTE_STATE";

    public record Arguments(@NonNull Site site, @NonNull Route route, @NonNull RouteState routeState) {}
    public static class EntryContract extends ActivityResultContract<Arguments, Boolean> {
        @NonNull
        @Override
        public Intent createIntent(@NonNull Context context, Arguments arguments) {
            final Intent intent = new Intent(context, DataEntryActivity.class);
            intent.putExtra(ARG_SITE, arguments.site);
            intent.putExtra(ARG_ROUTE, arguments.route.getName());
            intent.putExtra(ARG_ROUTE_STATE, arguments.routeState);
            return intent;
        }

        @Override
        public Boolean parseResult(int i, @Nullable Intent intent) {
            return i == DataEntryActivity.RESULT_OK;
        }
    }

    /**
     * The site where the entry is taking place
     */
    private Site mSite;

    /**
     * The name of the route that contains {@link #mSite}
     */
    private String mRouteName;

    private RouteState mRouteState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Unpack site from intent
        mSite = getIntent().getParcelableExtra(ARG_SITE);
        if (mSite == null) {
            throw new IllegalStateException("DataEntryActivity must be started with a site extra");
        }
        mRouteName = getIntent().getStringExtra(ARG_ROUTE);
        if (mRouteName == null) {
            throw new IllegalStateException("DataEntryActivity must be started with a route extra");
        }
        mRouteState = getIntent().getParcelableExtra(ARG_ROUTE_STATE);
        if (mRouteState == null) {
            throw new IllegalStateException("DataEntryActivity must be started with a route state extra");
        }

        setTitle(String.format(getString(R.string.format_site_id), mSite.getId()));
        setResult(RESULT_CANCELED);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.data_entry_menu, menu);

        final MenuItem saveItem = menu.findItem(R.id.save_item);
        saveItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                submit();
                return true;
            }
        });

        return true;
    }

    private void submit() {
        final boolean observed = mObservedSwitch.isChecked();
        // Collect species data
        final Map<String, Boolean> speciesData = new HashMap<>();
        if (observed) {
            for (int i = 0; i < mSpeciesContainer.getChildCount(); i++) {
                final View view = mSpeciesContainer.getChildAt(i);
                if (view instanceof SpeciesView && !isNoSpeciesView((SpeciesView) view)) {
                    final SpeciesView speciesView = (SpeciesView) view;
                    speciesData.put(speciesView.getSpecies().getColumn(), speciesView.isChecked());
                }
            }
        }
        final String notes = mNotesField.getText().toString();

        final boolean testMode = mRouteState.isTestMode();
        final Observation observation = new Observation(DateTime.now(), false, mSite.getId(), mRouteName,
                speciesData, notes, observed, testMode);

        // Store
        try {
            final ObservationDatabase db = new ObservationDatabase(this);
            db.insertObservation(observation);
            Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show();
            // Start a service to upload the observation
            startService(new Intent(getApplicationContext(), UploadService.class));
            // Update the status bar
            LocalBroadcastManager.getInstance(this)
                    .sendBroadcast(new Intent(UploadStatusTracker.ACTION_OBSERVATION_MADE));
            setResult(RESULT_OK);
            finish();
        } catch (SQLException e) {
            new AlertDialog.Builder(this)
                    .setTitle("Failed to save")
                    .setMessage(e.getLocalizedMessage())
                    .show();
        }

    }
}
