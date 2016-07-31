package org.samcrow.ridgesurvey;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

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


        // Load species
        mSpeciesContainer = (ViewGroup) findViewById(R.id.species_container);
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
                    mSpeciesContainer.addView(speciesView);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to read species input", e);
        } finally {
            IOUtils.closeQuietly(source);
        }
    }
}
