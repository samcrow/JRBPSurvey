package org.samcrow.ridgesurvey;

import android.app.AlertDialog;
import android.content.Intent;
import android.database.SQLException;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An activity that allows the user to enter information
 * <p/>
 * This activity must be started with an extra with key {@link #ARG_SITE} containing the site
 * to record data at and an extra with key {@link #ARG_ROUTE} containing the name of the route
 * that contains the site.
 */
public class DataEntryActivity extends AppCompatActivity {

    private static final String TAG = DataEntryActivity.class.getSimpleName();

    /**
     * The argument key used to provide a site
     */
    public static final String ARG_SITE = DataEntryActivity.class.getName() + ".ARG_SITE";
    /**
     * The argument key used to provide the name of the route that the site is on
     */
    public static final String ARG_ROUTE = DataEntryActivity.class.getName() + ".ARG_ROUTE";
    /**
     * The group that contains the SpeciesViews and potentially other views
     */
    private ViewGroup mSpeciesContainer;
    /**
     * The site where the entry is taking place
     */
    private Site mSite;
    /**
     * The field used to enter notes
     */
    private EditText mNotesField;

    /**
     * The name of the route that contains {@link #mSite}
     */
    private String mRouteName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_entry);
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Unpack site from intent
        mSite = getIntent().getParcelableExtra(ARG_SITE);
        if (mSite == null) {
            throw new IllegalStateException("DataEntryActivity must be started with a site extra");
        }
        mRouteName = getIntent().getStringExtra(ARG_ROUTE);
        if (mRouteName == null) {
            throw new IllegalStateException("DataEntryActivity must be started with a route extra");
        }

        setTitle(String.format(getString(R.string.format_site_id), mSite.getId()));

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
        // Collect species data
        final Map<String, Boolean> speciesData = new HashMap<>();
        for (int i = 0; i < mSpeciesContainer.getChildCount(); i++) {
            final View view = mSpeciesContainer.getChildAt(i);
            if (view instanceof SpeciesView) {
                final SpeciesView speciesView = (SpeciesView) view;
                speciesData.put(speciesView.getSpecies().getColumn(), speciesView.isChecked());
            }
        }
        final String notes = mNotesField.getText().toString();

        final Observation observation = new Observation(DateTime.now(), mSite.getId(), mRouteName,
                speciesData, notes);

        // Store
        try {
            final ObservationDatabase db = new ObservationDatabase(this);
            db.insertObservation(observation);
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
}
