package org.samcrow.ridgesurvey;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.GridLayout.Spec;
import android.widget.TextView;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

        mNotesField = (EditText) findViewById(R.id.notes_field);


        mSpeciesContainer = (ViewGroup) findViewById(R.id.species_container);

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
