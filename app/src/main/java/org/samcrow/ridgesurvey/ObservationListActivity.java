package org.samcrow.ridgesurvey;

import android.database.SQLException;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ListView;

import org.samcrow.ridgesurvey.data.IdentifiedObservation;
import org.samcrow.ridgesurvey.data.ObservationDatabase;
import org.samcrow.ridgesurvey.data.ObservationListAdapter;

import java.util.List;

public class ObservationListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_observation_list);
        final ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);
        }

        setTitle("Recent observations");
    }

    @Override
    public void onResume() {
        super.onResume();
        // Load and update sites
        try {
            final ObservationDatabase db = new ObservationDatabase(this);
            final List<IdentifiedObservation> observations = db.getObservationsByTime();
            final ObservationListAdapter adapter = new ObservationListAdapter(observations);

            final ListView list = (ListView) findViewById(R.id.observation_list);
            if (list != null) {
                list.setAdapter(adapter);
            }

        } catch (SQLException e) {
            new AlertDialog.Builder(this)
                    .setTitle("Failed to load observations")
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
