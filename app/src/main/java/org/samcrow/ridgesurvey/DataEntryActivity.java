package org.samcrow.ridgesurvey;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

/**
 * An activity that allows the user to enter information
 */
public class DataEntryActivity extends AppCompatActivity {

    private static final String TAG = DataEntryActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_entry);


        // Load species
        final ViewGroup speciesContainer = (ViewGroup) findViewById(R.id.species_container);
        final InputStream source = getResources().openRawResource(R.raw.species);
        try {
            final List<SpeciesGroup> groups = SpeciesStorage.loadSpeciesGroups(this, source);
            for (SpeciesGroup group : groups) {
                // Add a medium text view for the species group
                final TextView groupLabel = new TextView(this, null);
                groupLabel.setTextAppearance(this, android.R.style.TextAppearance_Medium);
                groupLabel.setText(group.getName());
                speciesContainer.addView(groupLabel);

                for (Species species : group.getSpecies()) {
//                    // Add a species check box
//                    final CheckBox speciesBox = new CheckBox(this);
//                    speciesBox.setText(species.getName());
//                    final Drawable speciesImage = species.getImage();
//                    if (speciesImage != null) {
//                        speciesImage.setBounds(0, 0, 64, 64);
//                        speciesBox.setCompoundDrawables(null, null, speciesImage, null);
//                    }
//
//                    speciesContainer.addView(speciesBox);
                    final SpeciesView speciesView = new SpeciesView(this, species);
                    speciesContainer.addView(speciesView);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to read species input", e);
        } finally {
            IOUtils.closeQuietly(source);
        }
    }
}
