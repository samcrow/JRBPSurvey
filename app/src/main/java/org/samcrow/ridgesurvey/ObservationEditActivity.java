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

import android.app.Activity;
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

import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;

import org.jetbrains.annotations.Nullable;
import org.joda.time.DateTime;
import org.samcrow.ridgesurvey.data.IdentifiedObservation;
import org.samcrow.ridgesurvey.data.ObservationDatabase;
import org.samcrow.ridgesurvey.data.UploadService;

import java.util.Map;

public class ObservationEditActivity extends ObservationActivity {

    /**
     * An extra key for the observation to edit
     */
    private static final String EXTRA_OBSERVATION = ObservationEditActivity.class.getName() + ".EXTRA_OBSERVATION";

    /**
     * The observation being edited
     */
    private IdentifiedObservation mObservation;

    /**
     * This activity's contract requires a non-null {@link IdentifiedObservation} and returns
     * true if the activity created/updated an observation, or false otherwise
     */
    public static class EditContract extends ActivityResultContract<IdentifiedObservation, Boolean> {
        @NonNull
        @Override
        public Intent createIntent(@NonNull Context context, IdentifiedObservation observation) {
            final Intent intent = new Intent(context, ObservationEditActivity.class);
            intent.putExtra(ObservationEditActivity.EXTRA_OBSERVATION, observation);
            return intent;
        }

        @Override
        public Boolean parseResult(int i, @Nullable Intent intent) {
            return i == Activity.RESULT_OK;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle("Edit observation");
        setResult(RESULT_CANCELED);

        mObservation = getIntent().getParcelableExtra(EXTRA_OBSERVATION);
        if (mObservation == null) {
            throw new IllegalArgumentException("ObservationEditActivity must be started with an observation extra");
        }

        // Set up user interface based on the observation
        mObservedSwitch.setChecked(mObservation.isObserved());

        final Map<String, Boolean> species = mObservation.getSpecies();
        for (int i = 0; i < mSpeciesContainer.getChildCount(); i++) {
            final View child = mSpeciesContainer.getChildAt(i);
            if (child instanceof SpeciesView speciesView && !isNoSpeciesView((SpeciesView) child)) {
                final Boolean speciesSeen = species.get(speciesView.getSpecies().getColumn());
                if (speciesSeen != null) {
                    speciesView.setChecked(speciesSeen);
                }
            }
        }

        mNotesField.setText(mObservation.getNotes());
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
        final Map<String, Boolean> speciesData = mObservation.getSpecies();
        if (observed) {
            for (int i = 0; i < mSpeciesContainer.getChildCount(); i++) {
                final View view = mSpeciesContainer.getChildAt(i);
                if (view instanceof SpeciesView speciesView && !isNoSpeciesView((SpeciesView) view)) {
                    speciesData.put(speciesView.getSpecies().getColumn(), speciesView.isChecked());
                }
            }
        } else {
            speciesData.clear();
        }
        final String notes = mNotesField.getText().toString();

        final IdentifiedObservation edited = new IdentifiedObservation(DateTime.now(), false, mObservation.getSiteId(), mObservation.getRouteName(),
                speciesData, notes, mObservation.getId(), observed, mObservation.isTest());

        // Store
        try {
            final ObservationDatabase db = new ObservationDatabase(this);
            db.updateObservation(edited);
            Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show();
            // Start a service to upload the observation
            startService(new Intent(getApplicationContext(), UploadService.class));
            setResult(RESULT_OK);
            finish();
        } catch (SQLException e) {
            new AlertDialog.Builder(this)
                    .setTitle("Failed to save")
                    .setMessage(e.getLocalizedMessage())
                    .show();
        }

    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
