/*
 * Copyright 2017 Sam Crow
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

package org.samcrow.ridgesurvey;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.TextView;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An activity that uses the R.layout.activity_data_entry layout and allows an activity
 * to be created or edited
 */
public abstract class ObservationActivity extends AppCompatActivity {


    private static final String TAG = ObservationActivity.class.getSimpleName();

    /**
     * The group that contains the SpeciesViews and potentially other views
     */
    protected ViewGroup mSpeciesContainer;

    /**
     * The switch used to select whether the site was observed
     */
    protected CompoundButton mObservedSwitch;
    /**
     * The field used to enter notes
     */
    protected EditText mNotesField;

    /**
     * The species view that indicates no species
     */
    private SpeciesView mNoSpeciesView;

    /**
     * A map from each species to the corresponding view
     *
     * This does not include the no species view.
     */
    private final Map<Species, SpeciesView> mSpeciesViews = new HashMap<>();

    private final OnCheckedChangeListener mNoSpeciesListener = new OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                // Uncheck every other species
                for (SpeciesView view : mSpeciesViews.values()) {
                    view.setChecked(false);
                }
            }
        }
    };

    private final OnCheckedChangeListener mOtherSpeciesListener = new OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked && mNoSpeciesView != null) {
                // Uncheck the no species box
                mNoSpeciesView.setChecked(false);
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set up layout
        setContentView(R.layout.activity_data_entry);
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mObservedSwitch = findViewById(R.id.observed_switch);
        mObservedSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                mSpeciesContainer.setVisibility(View.VISIBLE);
            } else {
                mSpeciesContainer.setVisibility(View.GONE);
            }
        });
        mNotesField = findViewById(R.id.notes_field);


        mSpeciesContainer = findViewById(R.id.species_container);

        // Add the no-species checkbox, checked by default
        mNoSpeciesView = new SpeciesView(this, new Species("None", "none", null, 0));
        mNoSpeciesView.setChecked(true);
        mNoSpeciesView.setOnCheckedChangeListener(mNoSpeciesListener);

        mSpeciesContainer.addView(mNoSpeciesView);

        // Load species
        final InputStream source = getResources().openRawResource(R.raw.species);
        try {
            final List<SpeciesGroup> groups = SpeciesStorage.loadSpeciesGroups(this, source);
            for (SpeciesGroup group : groups) {
                // Add a medium text view for the species group
                final TextView groupLabel = new TextView(this, null);
                groupLabel.setTextAppearance(this, android.R.style.TextAppearance_Medium);
                groupLabel.setText(group.getName());
                mSpeciesContainer.addView(groupLabel);

                for (Species species : group.getSpecies()) {
                    final SpeciesView speciesView = new SpeciesView(this, species);
                    speciesView.setOnCheckedChangeListener(mOtherSpeciesListener);
                    mSpeciesViews.put(species, speciesView);
                    mSpeciesContainer.addView(speciesView);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to read species input", e);
        } finally {
            IOUtils.closeQuietly(source);
        }
    }

    /**
     * Checks if the provided species view is the no species view
     * @param view the view to check
     * @return true if view is the no species view
     */
    protected final boolean isNoSpeciesView(SpeciesView view) {
        return mNoSpeciesView != null && view == mNoSpeciesView;
    }
}
