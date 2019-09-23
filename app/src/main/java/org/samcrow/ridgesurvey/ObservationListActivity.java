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

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.SQLException;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

import org.samcrow.ridgesurvey.data.IdentifiedObservation;
import org.samcrow.ridgesurvey.data.ObservationDatabase;
import org.samcrow.ridgesurvey.data.ObservationListAdapter;
import org.samcrow.ridgesurvey.data.UploadService;

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
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.observations_menu, menu);

        final MenuItem forceUploadItem = menu.findItem(R.id.force_upload_item);
        forceUploadItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                new Builder(ObservationListActivity.this)
                        .setTitle(R.string.force_upload)
                        .setMessage(R.string.question_load_all_observations)
                        .setPositiveButton(android.R.string.ok, new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                final Intent uploadIntent = new Intent(getApplicationContext(),
                                        UploadService.class);
                                uploadIntent.putExtra(UploadService.EXTRA_FORCE_UPLOAD, true);
                                startService(uploadIntent);
                                Toast.makeText(ObservationListActivity.this, "Uploading", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();

                return true;
            }
        });

        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
