package org.samcrow.ridgesurvey;

import android.app.AlertDialog;
import android.content.Intent;
import android.database.SQLException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.widget.Toast;

import org.joda.time.DateTime;
import org.samcrow.ridgesurvey.data.IdentifiedObservation;
import org.samcrow.ridgesurvey.data.Observation;
import org.samcrow.ridgesurvey.data.ObservationDatabase;
import org.samcrow.ridgesurvey.data.UploadService;

import java.util.HashMap;
import java.util.Map;

public class ObservationEditActivity extends ObservationActivity {

    /**
     * An extra key for the observation to edit
     */
    public static String EXTRA_OBSERVATION = ObservationEditActivity.class.getName() + ".EXTRA_OBSERVATION";

    /**
     * The observation being edited
     */
    private IdentifiedObservation mObservation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle("Edit observation");

        mObservation = getIntent().getParcelableExtra(EXTRA_OBSERVATION);
        if (mObservation == null) {
            throw new IllegalArgumentException("ObservationEditActivity must be started with an observation extra");
        }

        // Set up user interface based on the observation
        final Map<String, Boolean> species = mObservation.getSpecies();
        for (int i = 0; i < mSpeciesContainer.getChildCount(); i++) {
            final View child = mSpeciesContainer.getChildAt(i);
            if (child instanceof SpeciesView) {
                final SpeciesView speciesView = (SpeciesView) child;
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
        // Collect species data
        final Map<String, Boolean> speciesData = mObservation.getSpecies();
        for (int i = 0; i < mSpeciesContainer.getChildCount(); i++) {
            final View view = mSpeciesContainer.getChildAt(i);
            if (view instanceof SpeciesView) {
                final SpeciesView speciesView = (SpeciesView) view;
                speciesData.put(speciesView.getSpecies().getColumn(), speciesView.isChecked());
            }
        }
        final String notes = mNotesField.getText().toString();

        final IdentifiedObservation edited = new IdentifiedObservation(DateTime.now(), false, mObservation.getSiteId(), mObservation.getRouteName(),
                speciesData, notes, mObservation.getId());

        // Store
        try {
            final ObservationDatabase db = new ObservationDatabase(this);
            db.updateObservation(edited);
            Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show();
            // Start a service to upload the observation
            startService(new Intent(this, UploadService.class));
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
